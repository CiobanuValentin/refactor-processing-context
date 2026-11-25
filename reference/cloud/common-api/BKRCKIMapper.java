// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Clasa are peste 1100 de linii; aici sunt doar metodele din pozele din 25.11.
public class BKRCKIMapper {
    /* ...constantele pe tip de credit: SK, SH, HO, ZK, ZO... */
    public static final String STR_FALSE = "false";

    public void mapCKIData(CheckCkiPersonDto checkCkiPersonDto, Application application) {
        var ckiResponse = new BkrCkiResponse();
        List<BkrCkiResult> bkrResults = new ArrayList<>();
        if (checkCkiPersonDto != null) {
            var processingFlag = false;
            log.debug(" Mapping CKI Data started for business key {}", application.getBusinessKey());
            var customers = application.getLoan().getCustomers();
            var request = checkCkiPersonDto.getRequest();
            var response = checkCkiPersonDto.getResponse();

            processingFlag = processRiskAssessment(request, response, customers, processingFlag, bkrResults);

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

    private boolean processRiskAssessment(SingleBkrRequest request, SingleBkrResponse response, List<Customer> customers, boolean processingFlag/* ...taiat: List<BkrCkiResult> bkrResults */) {
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
        return processingFlag;
    }

    private String getInitialWithOutDot(String initialsName) {
        return (initialsName != null && !initialsName.trim().isEmpty()) ? initialsName.replace(".", "") : "";
    }

    private BkrCkiResult createCkiResult(Customer customer, SingleBkrResponse response) {
        Map<String, SingleBkrPerson> overallPersons = collectAllResponses(response);
        var bkrCkiResult = createEachCkiResult(overallPersons, customer, response);
        if (bkrCkiResult != null) {
            switch (customer.getApplicantType()) {
                case "Holder", "HolderPartner":
                    bkrCkiResult.setIsHolderHousehold(true);
                    break;
                case "CoHolder", "CoHolderPartner":
                    bkrCkiResult.setIsHolderHousehold(false);
                    break;
                default:
                    log.error("No Application found for {}", customer.getLastName());
                    /* ...restul nu era in poza */
            }
        }
        /* ... */
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
                }).toList();
        if (personsList.isEmpty()) {
            return null;
        }

        var bkrPersonResult = new BkrPersonResult();
        bkrPersonResult.setBusinessKey(customer.getBusinessKey());
        bkrPersonResult.setOverallMatchingOutcome(response.getResult().getOverallMatchingOutcome());
        bkrPersonResult.setSupplierData(customer.getSupplierData());
        bkrPersonResult.setMatchedPersons(personsList);
        bkrPersonResult.setBestMatch(getBestMatch(bkrPersonResult.getMatchedPersons()));
        if (bkrPersonResult.getBestMatch() != null && bkrPersonResult.getBestMatch().getMatchingOutcome().equals("01"))
            bkrPersonResult.setCreditLoans(getCreditLoans(bkrPersonResult.getBestMatch()));
        bkrPersonResult.setBkrScores(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getBestMatch().getBkrScores() : null);
        bkrPersonResult.setNeedsManualCheck(bkrPersonResult.getBestMatch() != null && bkrPersonResult.getBestMatch().getMatchingOutcome().equals("02"/* ...taiat */));
        bkrPersonResult.setLastName(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getBusinessKey() : null);
        bkrPersonResult.setInitials(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getInitials() : null);
        bkrPersonResult.setDateOfBirth(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getDateOfBirth() : LocalDateTime.now());
        bkrPersonResult.setZipcode(bkrPersonResult.getBestMatch() != null ? bkrPersonResult.getZipcode() : null);
        /* ...restul nu era in poza */
    }

    public List<RiskAssessment> createRiskAssessments(BkrCkiResponse ckiResponse) {
        List<RiskAssessment> riskAssessmentList = new ArrayList<>();
        if (Objects.isNull(ckiResponse.getBkrResults())) {
            var bkrCkiResult = new BkrCkiResult();
            createHolder(bkrCkiResult, riskAssessmentList);
            createCoHolder(bkrCkiResult, riskAssessmentList);
        } else {
            List<BkrCkiResult> listResult = ckiResponse.getBkrResults();
            var holder = false;
            var coHolder = false;
            if (listResult != null && !listResult.isEmpty()) {
                BkrCkiResult holderCkiResult = getHolderResults(listResult);
                BkrCkiResult coHolderCkiResult = getCoHolderResults(listResult);
                if (holderCkiResult != null) {
                    createHolder(holderCkiResult, riskAssessmentList);
                    holder = true;
                }
                if (coHolderCkiResult != null) {
                    createCoHolder(coHolderCkiResult, riskAssessmentList);
                    coHolder = true;
                }
            }
            if (!holder) {
                var bkrCkiResult = new BkrCkiResult();
                createHolder(bkrCkiResult, riskAssessmentList);
            }
            if (!coHolder) {
                var bkrCkiResult = new BkrCkiResult();
                createCoHolder(bkrCkiResult, riskAssessmentList);
            }
        }
        return riskAssessmentList;
    }
}
