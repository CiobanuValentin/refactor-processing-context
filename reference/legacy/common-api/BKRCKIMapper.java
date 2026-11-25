// REFERINTA - cod existent (legacy, common-api-1.23.jar), transcris din poze. Nu se compileaza si poate contine erori
// de transcriere. IDE-ul afisa "Library source does not match the bytecode". Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Liniile 52-222 din poze. Restul clasei nu a fost vazut.
@Component
@Slf4j
public class BKRCKIMapper {

    public static final String CLASS_INSTR_CD = "bkr";
    public static final String CLASS_INSTR_NAME = "BKR CKI";
    public static final String RESULT_DESC = "BKR CKI results for ";
    public static final String RESULT_HOLDER_DESC = "BKR CKI results for loan holders";
    public static final String RESULT_CO_HOLDER_DESC = "BKR CKI results for loan co-holders";
    public static final String RO = "RO";
    public static final String HY = "HY";
    public static final String SR = "SR";
    public static final String RK = "RK";
    public static final String VK = "VK";
    public static final String TC = "TC";
    public static final String AK = "AK";
    public static final String OA = "OA";
    public static final String RN = "RN";
    public static final String RH = "RH";
    /*AS per the email from Sjir on 22 Sep 2022, Adding additional Type of Loans(SK, SH, ZK, ZO and HO  */
    public static final String SK = "SK";
    public static final String SH = "SH";
    public static final String HO = "HO";
    public static final String ZK = "ZK";
    public static final String ZO = "ZO";
    public static final String STR_FALSE = "false";

    public void mapCKIData(List<CheckCkiPersonDto> matches, Application application) {
        var ckiResponse = new BkrCkiResponse();
        List<BkrCkiResult> bkrResults = new ArrayList<>();
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping CKI Data started for business key {}", application.getBusinessKey());
            for (CheckCkiPersonDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    var customers = application.getLoan().getCustomers();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (request != null && response != null && customers != null && !customers.isEmpty()) {
                        for (var customer : customers) {
                            if (!isCkiExists(customer) && request.getPerson().getName().equals(customer.getLastName())
                                    && isSameDateOfBirth(request.getPerson().getDateOfBirth(), customer.getBirthDate())
                                    && request.getPerson().getInitials().equals(getInitialWithOutDot(customer.getInitialsName()))) {
                                processingFlag = true;
                                var bkrCkiRslt = createCkiResult(customer, response);
                                if (Objects.nonNull(bkrCkiRslt)) {
                                    bkrResults.add(bkrCkiRslt);
                                }
                                customer.getRiskAssessments().add(createRiskAssessment(customer, bkrCkiRslt));
                            }
                        }
                    }

                }
            }
            if (!bkrResults.isEmpty()) {
                ckiResponse.setBkrResults(bkrResults);
            }
            if (ckiResponse.getBkrResults() != null && !ckiResponse.getBkrResults().isEmpty()) {
                application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
                processingFlag = true;
            }

            if (processingFlag) {
                log.debug(" Mapping CKI Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for CKI, business key {}", application.getBusinessKey());
            }
        } else {
            application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
        }
    }

    private String getInitialWithOutDot(String initialsName) {
        return (initialsName != null && initialsName.trim().length() > 0) ? initialsName.replace(".", "") : "";
    }

    private BkrCkiResult createCkiResult(Customer customer, SingleBkrResponse response) {
        Map<String, SingleBkrPerson> overallPersons = collectAllResponses(response);
        var bkrCkiResult = createEachCkiResult(overallPersons, customer, response);
        if (bkrCkiResult != null) {
            switch (customer.getApplicantType()) {
                case "Holder":
                case "HolderPartner":
                    bkrCkiResult.setIsHolderHousehold(true);
                    break;
                case "CoHolder":
                case "CoHolderPartner":
                    bkrCkiResult.setIsHolderHousehold(false);
                    break;
                default:
                    log.error("No Application found for {}", customer.getLastName());
            }
        }
        return bkrCkiResult;
    }

    private BkrCkiResult createEachCkiResult(Map<String, SingleBkrPerson> overallPersons, Customer customer, SingleBkrResponse response) {
        Collection<SingleBkrPerson> personList = overallPersons.values();
        //Create A Person First
        //Create Overall BkrPersonResult
        var bkrCkiResult = new BkrCkiResult();
        List<BkrPerson> personsList = personList.stream()
                .filter(Objects::nonNull)
                .map(eachPerson -> {
                    var bkrPerson = new BkrPerson();
                    bkrPerson.setBusinessKey(customer.getBusinessKey());
                    bkrPerson.setMatchingPercentage(eachPerson.getMatchingScore());
                    bkrPerson.setMatchingOutcome(eachPerson.getMatchingOutcome());
                    getBkrScores(bkrPerson, eachPerson);
                    getCreditLoans(bkrPerson, bkrCkiResult, eachPerson);
                    return bkrPerson;
                }).collect(toList());
        if (personsList.isEmpty()) {
            return null;
        }

        var bkrPersonResult = new BkrPersonResult();
        bkrPersonResult.setBusinessKey(customer.getBusinessKey());
        bkrPersonResult.setOverallMatchingOutcome(response.getResult().getOverallMatchingOutcome());
        bkrPersonResult.setSupplierData(customer.getSupplierData());
        bkrPersonResult.setMatchedPersons(personsList);
        bkrPersonResult.setBestMatch(getBestMatch(bkrPersonResult.getMatchedPersons()));
        if (bkrPersonResult.getBestMatch() != null && bkrPersonResult.getBestMatch().getMatchingOutcome().equals("01")) bkrPersonResult.setCreditLoans(getCreditLoans(bkrPersonResult.getBestMatch()));
        bkrPersonResult.setBkrScores(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getBestMatch().getBkrScores() : null);
        bkrPersonResult.setNeedsManualCheck(bkrPersonResult.getBestMatch() != null && bkrPersonResult.getBestMatch().getMatchingOutcome().equals("02"));
        bkrPersonResult.setLastName(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getBusinessKey() : null);
        bkrPersonResult.setInitials(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getInitials() : null);
        bkrPersonResult.setDateOfBirth(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getDateOfBirth() : LocalDateTime.now());
        bkrPersonResult.setZipcode(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getZipcode() : null);
        bkrPersonResult.setHouseNumber(getHouseNumber(bkrPersonResult.getBestMatch()));
        bkrPersonResult.setHouseNumberSuffix(bkrPersonResult.getBusinessKey() != null ? bkrPersonResult.getHouseNumberSuffix() : null);
        var persons = Collections.singletonList(bkrPersonResult);
        bkrCkiResult.setPersons(persons);
        bkrCkiResult.setCreditLoans(bkrPersonResult.getCreditLoans());
        bkrCkiResult.setWorstCardScoreValue(getEachWorstCardScoreValue(persons));
        bkrCkiResult.setWorstCardRiskClass(getEachWorstCardRiskClassValue(persons));
        bkrCkiResult.setNeedsManualCheck(bkrPersonResult.getNeedsManualCheck() != null && TRUE.equals(bkrPersonResult.getNeedsManualCheck()));
        bkrCkiResult.setNumOpen12m(getNumOpen12m(bkrCkiResult.getCreditLoans()));
        bkrCkiResult.setNumS15Open12m(getNumS15Open12m(bkrCkiResult.getCreditLoans()));
        return bkrCkiResult;
    }

    private Integer getHouseNumber(BkrPerson bestMatch) {
        if (Objects.isNull(bestMatch)) {
            return 0;
        }
        return bestMatch.getHouseNumber() != null ? bestMatch.getHouseNumber() : 0;
    }

    public Boolean getManualCheck(List<BkrPersonResult> persons) {
        Optional<BkrPersonResult> manualCheck = persons.stream().filter(person -> person.getNeedsManualCheck() != null && TRUE.equals(person.getNeedsManualCheck())).findAny();
        return manualCheck.isPresent() ? manualCheck.get().getNeedsManualCheck() : false;
    }

    public Map<String, SingleBkrPerson> collectAllResponses(SingleBkrResponse listCkiResponse) {
        Map<String, SingleBkrPerson> listPersons = new HashMap<>();
        final SingleBkrPersons persons = listCkiResponse.getResult().getPersons();
        if (persons != null && persons.getPerson() != null) {
            List<SingleBkrPerson> personList = persons.getPerson();
            for (SingleBkrPerson singlePerson : personList) {
                SingleBkrPerson aliasesPerson = listPersons.get(singlePerson.getPersonID());
                if (Objects.isNull(aliasesPerson)) {
                    listPersons.put(singlePerson.getPersonID(), singlePerson);
                } else if (aliasesPerson.getAliases() != null) {
                    if (aliasesPerson.getCreditLoans() != null && !aliasesPerson.getCreditLoans().getCreditLoan().isEmpty()) {
                        singlePerson.getCreditLoans().getCreditLoan().addAll(aliasesPerson.getCreditLoans().getCreditLoan());
                    }
                    if (aliasesPerson.getBkrScores() != null && !aliasesPerson.getBkrScores().getBkrScore().isEmpty()) {
                        singlePerson.getBkrScores().getBkrScore().addAll(aliasesPerson.getBkrScores().getBkrScore());
                    }
                    listPersons.put(singlePerson.getPersonID(), singlePerson);
                    /* ...restul nu era in poza */
                }
            }
        }
        /* ... */
    }
    /* ...restul clasei (dupa linia 222) nu a fost vazut */
}
