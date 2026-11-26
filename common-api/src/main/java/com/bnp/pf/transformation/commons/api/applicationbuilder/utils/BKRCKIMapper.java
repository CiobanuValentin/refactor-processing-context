package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importuri: aici sunt doar cele sigure. Pastreaza blocul de importuri din fisierul existent (liniile 1-73, inclusiv
// importurile statice pentru isSameDateOfBirth, RESULT_HOLDER, LOW, ...) si inlocuieste doar corpul clasei.
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckCkiPersonDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrCkiResponse;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrCkiResult;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrPerson;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrPersonResult;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrPersonScore;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.CreditLoan;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud:
 *  - mapCKIData primeste lista, ca in legacy: processRiskAssessment e apelat pentru fiecare rand, iar rezultatele de pe
 *    loan se adauga O SINGURA DATA, dupa bucla. Fara randuri, ca in legacy: cele doua rezultate goale pe loan.
 *  - collectAllResponses: in ramura "singlePerson are aliasuri", argumentele lui mergeCreditLoans / mergeBkrScores sunt
 *    puse ca in legacy (existing, singlePerson); in cloud erau inversate si se pierdeau creditele si scorurile.
 *
 * Restul e identic cu cloud-ul. Ce arata a bug dar e identic in legacy ramane (vezi docs/LOGIC.md, sectiunea CKI).
 */
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
            for (CheckCkiPersonDto checkCkiPersonDto : matches) {
                if (checkCkiPersonDto != null) {
                    var customers = application.getLoan().getCustomers();
                    var request = checkCkiPersonDto.getRequest();
                    var response = checkCkiPersonDto.getResponse();

                    processingFlag = processRiskAssessment(request, response, customers, processingFlag, bkrResults);
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

    private boolean processRiskAssessment(SingleBkrRequest request, SingleBkrResponse response, List<Customer> customers, boolean processingFlag, List<BkrCkiResult> bkrResults) {
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
        bkrCkiResult.setNumS150pen12m(getNumS150pen12m(bkrCkiResult.getCreditLoans()));
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
        return manualCheck.isPresent() && manualCheck.get().getNeedsManualCheck();
    }

    @SuppressWarnings("java:S3824")
    public Map<String, SingleBkrPerson> collectAllResponses(SingleBkrResponse listCkiResponse) {
        Map<String, SingleBkrPerson> listPersons = new HashMap<>();

        SingleBkrPersons persons = Objects.nonNull(listCkiResponse.getResult()) ? listCkiResponse.getResult().getPersons() : null;
        if (persons == null || persons.getPerson() == null) {
            return listPersons;
        }

        for (SingleBkrPerson singlePerson : persons.getPerson()) {
            String personId = singlePerson.getPersonID();
            SingleBkrPerson existing = listPersons.get(personId); //keep it simple, not computeIfAbsent

            if (existing == null) {
                listPersons.put(personId, singlePerson);
                continue;
            }
            if (existing.getAliases() != null) {
                mergeCreditLoans(singlePerson, existing);
                mergeBkrScores(singlePerson, existing);
                listPersons.put(personId, singlePerson);
            } else if (singlePerson.getAliases() != null) {
                // Ca in legacy: creditele si scorurile persoanei noi se adauga la cea deja din mapa.
                // In cloud argumentele erau inversate (singlePerson, existing) si se pierdeau.
                mergeCreditLoans(existing, singlePerson);
                mergeBkrScores(existing, singlePerson);
            }
        }

        return listPersons;
    }

    private void mergeBkrScores(SingleBkrPerson target, SingleBkrPerson source) {
        if (source.getBkrScores() == null) {
            return;
        }

        List<BkrScoreType> bkrScore = source.getBkrScores().getBkrScore();
        if (bkrScore == null || bkrScore.isEmpty()) {
            return;
        }

        target.getBkrScores().getBkrScore().addAll(bkrScore);
    }

    private void mergeCreditLoans(SingleBkrPerson target, SingleBkrPerson source) {
        if (source.getCreditLoans() == null) {
            return;
        }

        List<CreditLoanType> creditLoan = source.getCreditLoans().getCreditLoan();

        if (creditLoan == null || creditLoan.isEmpty()) {
            return;
        }

        target.getCreditLoans().getCreditLoan().addAll(creditLoan);
    }

    /**
     * @param bkrPerson BkrPerson
     * @return List<CreditLoan>
     */
    public List<CreditLoan> getCreditLoans(BkrPerson bkrPerson) {
        List<CreditLoan> creditLoans = bkrPerson.getCreditLoans();
        return getCreditLoansList(creditLoans);
    }

    private List<CreditLoan> getCreditLoansList(List<CreditLoan> creditLoans) {
        //Ignoring ballon loan Amount.
        List<CreditLoan> balloons = creditLoans.stream()
                .filter(Objects::nonNull)
                .filter(eachLoan -> "AK".equals(eachLoan.getTypeOfLoan()) && eachLoan.getFirstPayment().equals(eachLoan.getPlannedLastPayment()))
                .toList();

        List<CreditLoan> ignoreBalloons = creditLoans.stream()
                .filter(Objects::nonNull)
                .filter(eachLoan -> eachLoan.getFirstPayment() != null && !eachLoan.getFirstPayment().equals(eachLoan.getPlannedLastPayment()))
                .toList();

        //Comparing the Installment between the ballon and the Normal credit.
        List<CreditLoan> creditList = new ArrayList<>();
        for (CreditLoan eachCreditLoan : ignoreBalloons) {
            Optional<CreditLoan> matchCreditLoan = balloons.stream()
                    .filter(Objects::nonNull)
                    .filter(balloon -> TRUE.equals(balloon.getActive())
                            && balloon.getCreated() != null && balloon.getCreated().equals(eachCreditLoan.getCreated())
                            && (balloon.getAccountNumber() != null && balloon.getAccountNumber().equals(eachCreditLoan.getAccountNumber()) || (balloon.getAccountNumber() == null && eachCreditLoan.getAccountNumber() == null))
                            && balloon.getTypeOfLoan() != null && balloon.getTypeOfLoan().equals(eachCreditLoan.getTypeOfLoan())
                            && balloon.getFirstPayment() != null && balloon.getFirstPayment().equals(eachCreditLoan.getPlannedLastPayment()))
                    .findAny();
            //Need to take highest installment after comparing with the credit loan and Balloon credit.
            matchCreditLoan.ifPresent(creditLoan -> eachCreditLoan.setInstallment(eachCreditLoan.getInstallment().max(creditLoan.getInstallment())));
            creditList.add(eachCreditLoan);
        }

        List<CreditLoan> loansList = new ArrayList<>();
        for (CreditLoan balloon : balloons) {
            Optional<CreditLoan> matchCredit = ignoreBalloons.stream()
                    .filter(Objects::nonNull)
                    .filter(eachLoan -> (eachLoan.getCreated() != null && eachLoan.getCreated().equals(balloon.getCreated()) || (eachLoan.getCreated() == null && balloon.getCreated() == null))
                            && (eachLoan.getAccountNumber() != null && eachLoan.getAccountNumber().equals(balloon.getAccountNumber()) || (eachLoan.getAccountNumber() == null && balloon.getAccountNumber() == null))
                            && eachLoan.getTypeOfLoan() != null && eachLoan.getTypeOfLoan().equals(balloon.getTypeOfLoan())
                            && eachLoan.getPlannedLastPayment() != null && eachLoan.getPlannedLastPayment().equals(balloon.getFirstPayment()))
                    .findAny();
            if (matchCredit.isEmpty()) {
                loansList.add(balloon);
            }
        }

        return Stream.of(creditList, loansList)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    private boolean isCkiExists(Customer customers) {
        Optional<RiskAssessment> riskAssessments = customers.getRiskAssessments()
                .stream()
                .filter(Objects::nonNull)
                .filter(risk -> CLASS_INSTR_CD.equals(risk.getClassificationInstrumentCode()))
                .filter(risk -> CLASS_INSTR_NAME.equals(risk.getClassificationInstrumentName())).findAny();
        return riskAssessments.isPresent();
    }

    private RiskAssessment createRiskAssessment() {
        var riskAssessment = new RiskAssessment();
        riskAssessment.setClassificationInstrumentCode(CLASS_INSTR_CD);
        riskAssessment.setClassificationInstrumentName(CLASS_INSTR_NAME);
        riskAssessment.setApplicationStatus(ApplicationStatus.UNKNOWN.getValue());
        riskAssessment.setRawResult("");
        riskAssessment.setRiskLevel(LOW);
        riskAssessment.setDescription(RESULT_DESC);
        var classResult = new HashMap<String, Object>();
        riskAssessment.setClassificationResults(classResult);
        riskAssessment.setHasError(false);
        return riskAssessment;
    }

    private RiskAssessment createRiskAssessment(Customer customer, BkrCkiResult bkrCkiRslt) {
        var riskAssessment = createRiskAssessment();
        riskAssessment.setCustomerBusinessKey(customer.getBusinessKey());
        riskAssessment.setRawResult(createRawResult(bkrCkiRslt));
        createClassificationResult(riskAssessment, bkrCkiRslt, true);
        riskAssessment.setDescription(RESULT_DESC + customer.getBusinessKey());

        return riskAssessment;
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

    private BkrCkiResult getHolderResults(List<BkrCkiResult> listResult) {
        List<BkrCkiResult> holderList = listResult.stream().filter(Objects::nonNull)
                .filter(eachCki -> eachCki.getIsHolderHousehold() != null && eachCki.getIsHolderHousehold())
                .toList();
        return prepareEachCkiResult(holderList);
    }

    private BkrCkiResult getCoHolderResults(List<BkrCkiResult> listResult) {
        List<BkrCkiResult> holderList = listResult.stream().filter(Objects::nonNull)
                .filter(eachCki -> eachCki.getIsHolderHousehold() != null && !eachCki.getIsHolderHousehold())
                .toList();
        return prepareEachCkiResult(holderList);
    }

    private BkrCkiResult prepareEachCkiResult(List<BkrCkiResult> ckiList) {
        if (!ckiList.isEmpty()) {
            var ckiResult = new BkrCkiResult();
            ckiResult.setCreditLoans(combineCreditLoans(ckiList));
            ckiResult.setPersons(combinePersons(ckiList));
            ckiResult.setWorstCardRiskClass(getMaxWorstRiskClass(ckiList));
            ckiResult.setWorstCardScoreValue(getMaxWorstCardScore(ckiList));
            return ckiResult;
        }
        return null;
    }

    public String getMaxWorstRiskClass(List<BkrCkiResult> ckiList) {
        Optional<BkrCkiResult> cardRisk = ckiList.stream().filter(Objects::nonNull)
                .filter(ckiResult -> ckiResult.getWorstCardRiskClass() != null)
                .max(Comparator.comparing(BkrCkiResult::getWorstCardRiskClass));
        return cardRisk.map(BkrCkiResult::getWorstCardRiskClass).orElse(null);
    }

    public Integer getMaxWorstCardScore(List<BkrCkiResult> ckiList) {
        Optional<BkrCkiResult> cardRisk = ckiList.stream().filter(Objects::nonNull)
                .filter(ckiResult -> ckiResult.getWorstCardScoreValue() != null)
                .max(Comparator.comparing(BkrCkiResult::getWorstCardScoreValue));
        return cardRisk.map(BkrCkiResult::getWorstCardScoreValue).orElse(0);
    }

    private List<BkrPersonResult> combinePersons(List<BkrCkiResult> ckiList) {
        return ckiList.stream().filter(Objects::nonNull)
                .filter(persons -> persons.getPersons() != null && !persons.getPersons().isEmpty())
                .flatMap(person -> Optional.of(person.getPersons()).orElseGet(Collections::emptyList)
                        .stream().filter(Objects::nonNull)).toList();
    }

    public List<CreditLoan> combineCreditLoans(List<BkrCkiResult> loanList) {
        return loanList.stream().filter(Objects::nonNull)
                .filter(creditLoans -> creditLoans.getCreditLoans() != null && !creditLoans.getCreditLoans().isEmpty())
                .flatMap(creditLoan -> Optional.of(creditLoan.getCreditLoans()).orElseGet(Collections::emptyList)
                        .stream().filter(Objects::nonNull))
                .distinct().toList();
    }

    private void createCoHolder(BkrCkiResult ckiResponse, List<RiskAssessment> riskAssessmentList) {
        var riskAssmntCoHolder = createRiskAssessment();
        riskAssmntCoHolder.setResultType(RESULT_CO_HOLDER);
        riskAssmntCoHolder.setDescription(RESULT_CO_HOLDER_DESC);
        riskAssmntCoHolder.setRawResult(createRawResult(ckiResponse));
        riskAssmntCoHolder.setRiskLevel(LOW);
        createClassificationResult(riskAssmntCoHolder, ckiResponse, false);
        riskAssessmentList.add(riskAssmntCoHolder);
    }

    private void createHolder(BkrCkiResult ckiResponse, List<RiskAssessment> riskAssessmentList) {
        var riskAssmntHolder = createRiskAssessment();
        riskAssmntHolder.setRawResult(createRawResult(ckiResponse));
        riskAssmntHolder.setResultType(RESULT_HOLDER);
        riskAssmntHolder.setDescription(RESULT_HOLDER_DESC);
        riskAssmntHolder.setRiskLevel(LOW);
        createClassificationResult(riskAssmntHolder, ckiResponse, false);
        riskAssessmentList.add(riskAssmntHolder);
    }

    private void createClassificationResult(RiskAssessment riskAssmntHolder, BkrCkiResult ckiResponse, boolean isCustomer) {
        Map<String, Object> classResult = new HashMap<>();
        var bkrCkiResult = getCkiResult(ckiResponse);
        classResult.put("TotalMonthlyInstallments", bkrCkiResult.getTotalMonthlyInstallments());
        classResult.put("TotalLoanAmount", bkrCkiResult.getTotalLoanAmount());
        classResult.put("NumberOfLoansWithCode", bkrCkiResult.getNumberOfLoansWithCode());
        classResult.put("NumberOfLoansLowRisk", bkrCkiResult.getNumberOfLoansLowRisk());
        classResult.put("NumberOfLoansMediumRisk", bkrCkiResult.getNumberOfLoansMediumRisk());
        classResult.put("NumberOfLoansHighRisk", bkrCkiResult.getNumberOfLoansHighRisk());
        classResult.put("NumberOfLoansWithHersteldCode", bkrCkiResult.getNumberOfLoansWithHersteldCode());
        classResult.put("NumberOfActiveLoansTypeOther", bkrCkiResult.getNumberOfActiveLoansTypeOther());
        classResult.put("NumberOfActiveLoansTypeMortgage", bkrCkiResult.getNumberOfActiveLoansTypeMortgage());
        classResult.put("NumberOfActiveLoansTypeDebtCounseling", bkrCkiResult.getNumberOfActiveLoansTypeDebtCounseling());
        classResult.put("NumberOfActiveLoansTypeDebtRestructuring", bkrCkiResult.getNumberOfActiveLoansTypeDebtRestructuring());
        classResult.put("NumberOfActiveLoansTypeDebtSettlement", bkrCkiResult.getNumberOfActiveLoansTypeDebtSettlement());
        classResult.put("NumberOfActiveLoansTypeRevolvingLoan", bkrCkiResult.getNumberOfActiveLoansTypeRevolvingLoan());
        classResult.put("NumberOfActiveLoansTypeTelecomContract", bkrCkiResult.getNumberOfActiveLoansTypeTelecomContract());
        classResult.put("NumberOfActiveLoansTypeMailOrder", bkrCkiResult.getNumberOfActiveLoansTypeMailOrder());
        classResult.put("NumberOfActiveLoansTypePersonalLoan", bkrCkiResult.getNumberOfActiveLoansTypePersonalLoan());
        classResult.put("NumberOfActiveLoansTypeOperationalLease", bkrCkiResult.getNumberOfActiveLoansTypeOperationalLease());
        classResult.put("NumberOfActiveLoansTypeHouseDebtWithNhg", bkrCkiResult.getNumberOfActiveLoansTypeHouseDebtWithNhg());
        classResult.put("NumberOfActiveLoansTypeHouseDebtWithoutNhg", bkrCkiResult.getNumberOfActiveLoansTypeHouseDebtWithoutNhg());
        classResult.put("NumberOfActiveLoansTypeHo", bkrCkiResult.getNumberOfActiveLoansTypeHo());
        classResult.put("NumberOfActiveLoansTypeRemediationLoan", bkrCkiResult.getNumberOfActiveLoansTypeRemediationLoan());
        classResult.put("NumberOfActiveLoansTypeDebtAssistance", bkrCkiResult.getNumberOfActiveLoansTypeDebtAssistance());
        classResult.put("NumberOfActiveLoansTypeOtherProperty", bkrCkiResult.getNumberOfActiveLoansTypeOtherProperty());
        classResult.put("NumberOfActiveLoansTypeBusinessCredit", bkrCkiResult.getNumberOfActiveLoansTypeBusinessCredit());
        classResult.put("NumberOfActiveLoansTypeBusinessOverrun", bkrCkiResult.getNumberOfActiveLoansTypeBusinessOverrun());
        classResult.put("NumberOfActiveLoansTypeOtherFinancials", bkrCkiResult.getNumberOfActiveLoansTypeOtherFinancials());
        classResult.put("NumberOfActiveOwnedContracts", bkrCkiResult.getNumberOfActiveOwnedContracts());
        classResult.put("InstallmentActiveLoansTypeOther", bkrCkiResult.getInstallmentActiveLoansTypeOther());
        classResult.put("InstallmentActiveLoansTypeMortgage", bkrCkiResult.getInstallmentActiveLoansTypeMortgage());
        classResult.put("InstallmentActiveLoansTypeDebtCounseling", bkrCkiResult.getInstallmentActiveLoansTypeDebtCounseling());
        classResult.put("InstallmentActiveLoansTypeDebtRestructuring", bkrCkiResult.getInstallmentActiveLoansTypeDebtRestructuring());
        classResult.put("InstallmentActiveLoansTypeDebtSettlement", bkrCkiResult.getInstallmentActiveLoansTypeDebtSettlement());
        classResult.put("InstallmentActiveLoansTypeRevolvingLoan", bkrCkiResult.getInstallmentActiveLoansTypeRevolvingLoan());
        classResult.put("InstallmentActiveLoansTypeMailOrder", bkrCkiResult.getInstallmentActiveLoansTypeMailOrder());
        classResult.put("InstallmentActiveLoansTypeTelecomContract", bkrCkiResult.getInstallmentActiveLoansTypeTelecomContract());
        classResult.put("InstallmentActiveLoansTypePersonalLoan", bkrCkiResult.getInstallmentActiveLoansTypePersonalLoan());
        classResult.put("InstallmentActiveLoansTypeOperationalLease", bkrCkiResult.getInstallmentActiveLoansTypeOperationalLease());
        classResult.put("InstallmentActiveLoansTypeHouseDebtWithNhg", bkrCkiResult.getInstallmentActiveLoansTypeHouseDebtWithNhg());
        classResult.put("InstallmentActiveLoansTypeHouseDebtWithoutNhg", bkrCkiResult.getInstallmentActiveLoansTypeHouseDebtWithoutNhg());
        classResult.put("InstallmentActiveLoansTypeHo", bkrCkiResult.getInstallmentActiveLoansTypeHo());
        classResult.put("InstallmentActiveLoansTypeRemediationLoan", bkrCkiResult.getInstallmentActiveLoansTypeRemediationLoan());
        classResult.put("InstallmentActiveLoansTypeDebtAssistance", bkrCkiResult.getInstallmentActiveLoansTypeDebtAssistance());
        classResult.put("InstallmentActiveLoansTypeOtherProperty", bkrCkiResult.getInstallmentActiveLoansTypeOtherProperty());
        classResult.put("InstallmentActiveLoansTypeBusinessCredit", bkrCkiResult.getInstallmentActiveLoansTypeBusinessCredit());
        classResult.put("InstallmentActiveLoansTypeOtherFinancials", bkrCkiResult.getInstallmentActiveLoansTypeOtherFinancials());
        classResult.put("AmountOfActiveLoansTypeOther", bkrCkiResult.getAmountOfActiveLoansTypeOther());
        classResult.put("AmountOfActiveLoansTypeMortgage", bkrCkiResult.getAmountOfActiveLoansTypeMortgage());
        classResult.put("AmountOfActiveLoansTypeDebtCounseling", bkrCkiResult.getAmountOfActiveLoansTypeDebtCounseling());
        classResult.put("AmountOfActiveLoansTypeDebtRestructuring", bkrCkiResult.getAmountOfActiveLoansTypeDebtRestructuring());
        classResult.put("AmountOfActiveLoansTypeDebtSettlement", bkrCkiResult.getAmountOfActiveLoansTypeDebtSettlement());
        classResult.put("AmountOfActiveLoansTypeRevolvingLoan", bkrCkiResult.getAmountOfActiveLoansTypeRevolvingLoan());
        classResult.put("AmountOfActiveLoansTypeMailOrder", bkrCkiResult.getAmountOfActiveLoansTypeMailOrder());
        classResult.put("AmountOfActiveLoansTypeTelecomContract", bkrCkiResult.getAmountOfActiveLoansTypeTelecomContract());
        classResult.put("AmountOfActiveLoansTypePersonalLoan", bkrCkiResult.getAmountOfActiveLoansTypePersonalLoan());
        classResult.put("AmountOfActiveLoansTypeOperationalLease", bkrCkiResult.getAmountOfActiveLoansTypeOperationalLease());
        classResult.put("AmountOfActiveLoansTypeHouseDebtWithNhg", bkrCkiResult.getAmountOfActiveLoansTypeHouseDebtWithNhg());
        classResult.put("AmountOfActiveLoansTypeHouseDebtWithoutNhg", bkrCkiResult.getAmountOfActiveLoansTypeHouseDebtWithoutNhg());
        classResult.put("AmountOfActiveLoansTypeHo", bkrCkiResult.getAmountOfActiveLoansTypeHo());
        classResult.put("AmountOfActiveLoansTypeRemediationLoan", bkrCkiResult.getAmountOfActiveLoansTypeRemediationLoan());
        classResult.put("AmountOfActiveLoansTypeDebtAssistance", bkrCkiResult.getAmountOfActiveLoansTypeDebtAssistance());
        classResult.put("AmountOfActiveLoansTypeOtherProperty", bkrCkiResult.getAmountOfActiveLoansTypeOtherProperty());
        classResult.put("AmountOfActiveLoansTypeBusinessCredit", bkrCkiResult.getAmountOfActiveLoansTypeBusinessCredit());
        classResult.put("AmountOfActiveLoansTypeOtherFinancials", bkrCkiResult.getAmountOfActiveLoansTypeOtherFinancials());
        classResult.put("WorstCardRiskClass", bkrCkiResult.getWorstCardRiskClass());
        classResult.put("WorstCardScoreValue", bkrCkiResult.getWorstCardScoreValue());
        classResult.put("NumOpen12m", bkrCkiResult.getNumOpen12m());
        classResult.put("NumS150pen12m", bkrCkiResult.getNumS150pen12m());

        if (bkrCkiResult.getPersons() != null && !bkrCkiResult.getPersons().isEmpty()) {
            proceedWithClassResult(bkrCkiResult, classResult);
        } else {
            classResult.put("NeedsManualCheck", bkrCkiResult.getNeedsManualCheck() != null ? bkrCkiResult.getNeedsManualCheck().toString() : STR_FALSE);
        }
        if (isCustomer) {
            List<com.bnp.pf.transformation.commons.api.message.output.dto.CreditLoan> loanResult = getLoanResult(bkrCkiResult);
            if (loanResult != null && !loanResult.isEmpty()) {
                classResult.put("Loans", loanResult);
            }
        }
        classResult.put("Result", STR_FALSE);
        riskAssmntHolder.setClassificationResults(classResult);
    }

    private void proceedWithClassResult(BkrCkiResult bkrCkiResult, Map<String, Object> classResult) {
        for (BkrPersonResult person : bkrCkiResult.getPersons()) {
            classResult.put("BkrStatus", "02".equals(person.getOverallMatchingOutcome()) ? "0" : "1");
            classResult.put("OrgPartyType", person.getSupplierData() != null ? person.getSupplierData().get("PartyType") : "00");
            classResult.put("NeedsManualCheck", person.getNeedsManualCheck() != null ? person.getNeedsManualCheck().toString() : STR_FALSE);
            var i = 0;
            mapBkrPersonScore(classResult, person, i);
        }
    }

    private void mapBkrPersonScore(Map<String, Object> classResult, BkrPersonResult person, int i) {
        for (BkrPersonScore bkrPersonScore : person.getBkrScores()) {
            classResult.put("CardName" + i, bkrPersonScore.getCardName());
            classResult.put("CardRiskClass" + i, bkrPersonScore.getCardRiskClass());
            classResult.put("CardRiskPercentage" + i, bkrPersonScore.getCardRiskPercentage());
            classResult.put("CardScoreValue" + i, bkrPersonScore.getCardScoreValue());
            if (bkrPersonScore.getNotification() != null && !bkrPersonScore.getNotification().isEmpty()) {
                classResult.put("Notifications" + i, bkrPersonScore.getNotification().stream().filter(Objects::nonNull).filter(codes -> !codes.isEmpty()).collect(Collectors.joining(",")));
            }
            if (bkrPersonScore.getScoreData() != null && !bkrPersonScore.getScoreData().isEmpty()) {
                for (Map.Entry<String, BigDecimal> scoreData : bkrPersonScore.getScoreData().entrySet()) {
                    classResult.put("ScoreData" + i + "_" + scoreData.getKey(), scoreData.getValue());
                }
            }
            i++;
        }
    }

    public List<com.bnp.pf.transformation.commons.api.message.output.dto.CreditLoan> getLoanResult(BkrCkiResult bkrCkiResult) {
        if (bkrCkiResult == null || bkrCkiResult.getCreditLoans() == null) {
            return Collections.emptyList();
        }

        return bkrCkiResult.getCreditLoans().stream()
                .filter(Objects::nonNull)
                .map(this::mapToLoan)
                .toList();
    }

    private com.bnp.pf.transformation.commons.api.message.output.dto.CreditLoan mapToLoan(CreditLoan creditLoan) {
        var loan = new com.bnp.pf.transformation.commons.api.message.output.dto.CreditLoan();

        loan.setTypeOfLoan(creditLoan.getTypeOfLoan());
        loan.setAmount(defaultInt(creditLoan.getAmount()));
        loan.setInstallment(creditLoan.getInstallment() != null ? creditLoan.getInstallment().intValue() : 0);
        loan.setDuration(defaultInt(creditLoan.getDuration()));
        loan.setAccountNumber(creditLoan.getAccountNumber());
        loan.setDateFirstPayment(creditLoan.getFirstPayment());
        loan.setDateTheoreticalLastPayment(creditLoan.getPlannedLastPayment());
        loan.setDateLastPayment(creditLoan.getLastPayment());
        loan.setIdContract(creditLoan.getIdOwnedContract());
        loan.setIndexNumber(defaultInt(creditLoan.getIndex()));
        loan.setIdEndedAtBKR(TRUE.equals(creditLoan.getDba()) ? "1" : "0");
        loan.setDateRegistration(creditLoan.getCreated());
        loan.setDetailCodes(creditLoan.getBkrCodesString());
        loan.setCodeRiskClass(getCodeRisk(creditLoan));

        return loan;
    }

    private int defaultInt(Integer value) {
        return value != null ? value : 0;
    }

    private String getCodeRisk(CreditLoan creditLoan) {
        if (TRUE.equals(creditLoan.getHighRisk())) {
            return "high";
        }
        return TRUE.equals(creditLoan.getMediumRisk()) ? "med" : "low";
    }

    private String createRawResult(BkrCkiResult ckiResponse) {
        return ckiConvertToString(getCkiResult(ckiResponse));
    }

    private BkrCkiResult getCkiResult(BkrCkiResult ckiResponse) {
        return mapAdditionalResult(ckiResponse);
    }

    private BkrCkiResult mapAdditionalResult(BkrCkiResult ckiResponse) {
        if (Objects.isNull(ckiResponse)) {
            return new BkrCkiResult();
        }
        var bkrCkiResult = new BkrCkiResult();
        bkrCkiResult.setTotalLoanAmount(getTotalAmount(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfLoansWithCode(getNumberOfLoans(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfLoansLowRisk(getLowRiskLoans(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfLoansMediumRisk(getMediumRiskLoans(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfLoansHighRisk(getHighRiskLoans(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfLoansWithHersteldCode(getLoansHersteldCode(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumberOfActiveLoansTypeOther(getNmbrOfActiveLoansDba(ckiResponse.getCreditLoans(), RO));
        bkrCkiResult.setNumberOfActiveLoansTypeMortgage(getNmbrOfActiveLoansDba(ckiResponse.getCreditLoans(), HY));
        bkrCkiResult.setNumberOfActiveLoansTypeDebtCounseling(getNmbrOfActiveLoansDba(ckiResponse.getCreditLoans(), SH));
        bkrCkiResult.setNumberOfActiveLoansTypeDebtRestructuring(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), SK));
        bkrCkiResult.setNumberOfActiveLoansTypeRevolvingLoan(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), RK));
        bkrCkiResult.setNumberOfActiveLoansTypePersonalLoan(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), AK));
        bkrCkiResult.setNumberOfActiveLoansTypeOperationalLease(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), OA));
        bkrCkiResult.setNumberOfActiveLoansTypeHouseDebtWithNhg(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), RN));
        bkrCkiResult.setNumberOfActiveLoansTypeHouseDebtWithoutNhg(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), RH));
        bkrCkiResult.setNumberOfActiveLoansTypeHo(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), HO));
        bkrCkiResult.setNumberOfActiveLoansTypeBusinessCredit(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), ZK));
        bkrCkiResult.setNumberOfActiveLoansTypeBusinessOverrun(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), ZO));
        bkrCkiResult.setNumberOfActiveLoansTypeDebtSettlement(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), SR));
        bkrCkiResult.setNumberOfActiveLoansTypeMailOrder(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), VK));
        bkrCkiResult.setNumberOfActiveLoansTypeTelecomContract(getNmbrOfActiveLoans(ckiResponse.getCreditLoans(), TC));
        bkrCkiResult.setNumberOfActiveOwnedContracts(activeOwnedContracts(ckiResponse.getCreditLoans()));

        bkrCkiResult.setInstallmentActiveLoansTypeOther(getNmbrOfActiveOwnerLoansDba(ckiResponse.getCreditLoans(), RO));
        bkrCkiResult.setInstallmentActiveLoansTypeMortgage(getNmbrOfActiveOwnerLoansDba(ckiResponse.getCreditLoans(), HY));
        bkrCkiResult.setInstallmentActiveLoansTypeDebtCounseling(getNmbrOfActiveOwnerLoansDba(ckiResponse.getCreditLoans(), SH));
        bkrCkiResult.setInstallmentActiveLoansTypeDebtRestructuring(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), SK));
        bkrCkiResult.setInstallmentActiveLoansTypeRevolvingLoan(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), RK));
        bkrCkiResult.setInstallmentActiveLoansTypePersonalLoan(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), AK));
        bkrCkiResult.setInstallmentActiveLoansTypeOperationalLease(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), OA));
        bkrCkiResult.setInstallmentActiveLoansTypeHouseDebtWithNhg(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), RN));
        bkrCkiResult.setInstallmentActiveLoansTypeHouseDebtWithoutNhg(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), RH));
        bkrCkiResult.setInstallmentActiveLoansTypeHo(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), HO));
        bkrCkiResult.setInstallmentActiveLoansTypeMailOrder(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), VK));
        bkrCkiResult.setInstallmentActiveLoansTypeTelecomContract(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), TC));
        bkrCkiResult.setInstallmentActiveLoansTypeBusinessCredit(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), ZK));
        bkrCkiResult.setInstallmentActiveLoansTypeOtherFinancials(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), ZO));
        bkrCkiResult.setInstallmentActiveLoansTypeDebtSettlement(getNmbrOfActiveOwnerLoans(ckiResponse.getCreditLoans(), SR));

        bkrCkiResult.setAmountOfActiveLoansTypeOther(getNmbrOfActiveLoansSumDba(ckiResponse.getCreditLoans(), RO));
        bkrCkiResult.setAmountOfActiveLoansTypeMortgage(getNmbrOfActiveLoansSumDba(ckiResponse.getCreditLoans(), HY));
        bkrCkiResult.setAmountOfActiveLoansTypeDebtCounseling(getNmbrOfActiveLoansSumDba(ckiResponse.getCreditLoans(), SH));
        bkrCkiResult.setAmountOfActiveLoansTypeDebtRestructuring(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), SK));
        bkrCkiResult.setAmountOfActiveLoansTypeRevolvingLoan(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), RK));
        bkrCkiResult.setAmountOfActiveLoansTypePersonalLoan(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), AK));
        bkrCkiResult.setAmountOfActiveLoansTypeOperationalLease(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), OA));
        bkrCkiResult.setAmountOfActiveLoansTypeHouseDebtWithNhg(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), RN));
        bkrCkiResult.setAmountOfActiveLoansTypeHouseDebtWithoutNhg(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), RH));
        bkrCkiResult.setAmountOfActiveLoansTypeHo(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), HO));
        bkrCkiResult.setAmountOfActiveLoansTypeMailOrder(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), VK));
        // TODO: liniile 708-709 nu se vad in poze (IMG_7774 / IMG_7775). TelecomContract e presupus, dupa celelalte doua liste;
        //  a doua linie lipsa nu e cunoscuta. De verificat in IDE.
        bkrCkiResult.setAmountOfActiveLoansTypeTelecomContract(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), TC));
        bkrCkiResult.setAmountOfActiveLoansTypeBusinessCredit(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), ZK));
        bkrCkiResult.setAmountOfActiveLoansTypeOtherFinancials(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), ZO));
        bkrCkiResult.setAmountOfActiveLoansTypeDebtSettlement(getNmbrOfActiveLoansSum(ckiResponse.getCreditLoans(), SR));
        bkrCkiResult.setTotalMonthlyInstallments(getTotalMonthlyInstallments(bkrCkiResult));
        bkrCkiResult.setNumOpen12m(getNumOpen12m(ckiResponse.getCreditLoans()));
        bkrCkiResult.setNumS150pen12m(getNumS150pen12m(ckiResponse.getCreditLoans()));
        bkrCkiResult.setWorstCardRiskClass(ckiResponse.getWorstCardRiskClass());
        bkrCkiResult.setWorstCardScoreValue(ckiResponse.getWorstCardScoreValue());
        bkrCkiResult.setCreditLoans(ckiResponse.getCreditLoans());
        bkrCkiResult.setPersons(ckiResponse.getPersons());
        bkrCkiResult.setNeedsManualCheck(ckiResponse.getNeedsManualCheck());
        return bkrCkiResult;
    }

    private Integer getNumS150pen12m(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> numS150pen12m = creditLoans.stream()
                    .filter(Objects::nonNull)
                    .filter(creditLoan -> creditLoan.getAmount() >= 150000
                            && creditLoan.getCreated() != null
                            && creditLoan.getCreated().isAfter(LocalDateTime.now().plusMonths(-12))
                            && creditLoan.getCreated().isBefore(LocalDateTime.now())).toList();
            return !numS150pen12m.isEmpty() ? numS150pen12m.size() : 0;
        }
        return 0;
    }

    private Integer getNumOpen12m(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> numOpen12m = creditLoans.stream()
                    .filter(Objects::nonNull)
                    .filter(creditLoan -> creditLoan.getCreated() != null
                            && creditLoan.getCreated().isAfter(LocalDateTime.now().plusMonths(-12))
                            && creditLoan.getCreated().isBefore(LocalDateTime.now())).toList();
            return !numOpen12m.isEmpty() ? numOpen12m.size() : 0;
        }
        return 0;
    }

    public Double getTotalMonthlyInstallments(BkrCkiResult bkrCkiResult) {
        var instalments = new BigDecimal[]{bkrCkiResult.getInstallmentActiveLoansTypeDebtCounseling(),
                bkrCkiResult.getInstallmentActiveLoansTypeDebtRestructuring(),
                bkrCkiResult.getInstallmentActiveLoansTypeHouseDebtWithNhg(),
                bkrCkiResult.getInstallmentActiveLoansTypeHouseDebtWithoutNhg(),
                bkrCkiResult.getInstallmentActiveLoansTypeOperationalLease(),
                bkrCkiResult.getInstallmentActiveLoansTypeOther(),
                bkrCkiResult.getInstallmentActiveLoansTypePersonalLoan(),
                bkrCkiResult.getInstallmentActiveLoansTypeRevolvingLoan(),
                bkrCkiResult.getInstallmentActiveLoansTypeHo()};

        return Arrays.stream(instalments).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
    }

    private Integer activeOwnedContracts(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            // TODO: finalul liniei iese din ecran (IMG_7775, linia 763); ".findAny()" e presupus.
            Optional<Map.Entry<String, List<CreditLoan>>> amount = creditLoans.stream().filter(Objects::nonNull).filter(eachLoan -> eachLoan.getAccountNumber() != null).collect(Collectors.groupingBy(CreditLoan::getAccountNumber)).entrySet().stream().findAny();
            if (amount.isPresent()) {
                List<CreditLoan> loan = amount.get().getValue().stream()
                        .filter(creditLoan -> creditLoan.getActive() && creditLoan.getIdOwnedContract() != null && creditLoan.getIdOwnedContract().equals("EC")).toList();
                return !loan.isEmpty() ? loan.size() : 0;
            }
        }
        return 0;
    }

    private Integer getLowRiskLoans(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> lowRisk = creditLoans.stream().filter(CreditLoan::getLowRisk).toList();
            return !lowRisk.isEmpty() ? lowRisk.size() : 0;
        }
        return 0;
    }

    private Integer getMediumRiskLoans(List<CreditLoan> creditLoans) {
        if (creditLoans == null || creditLoans.isEmpty()) {
            return 0;
        }
        List<CreditLoan> mediumRisk = creditLoans.stream().filter(CreditLoan::getMediumRisk).toList();
        return !mediumRisk.isEmpty() ? mediumRisk.size() : 0;
    }

    private Integer getLoansHersteldCode(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> hersteldCode = creditLoans.stream()
                    .filter(bkrCode -> bkrCode.getBkrCodes() != null && !bkrCode.getBkrCodes().isEmpty())
                    .filter(bkrCodes -> bkrCodes.getBkrCodes().contains("H")).toList();
            return !hersteldCode.isEmpty() ? hersteldCode.size() : 0;
        }
        return 0;
    }

    private Integer getHighRiskLoans(List<CreditLoan> creditLoans) {
        if (creditLoans == null || creditLoans.isEmpty()) {
            return 0;
        }
        List<CreditLoan> highRisk = creditLoans.stream().filter(CreditLoan::getHighRisk).toList();
        return !highRisk.isEmpty() ? highRisk.size() : 0;
    }

    private Integer getNmbrOfActiveLoansSum(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            return creditLoans.stream().filter(creditLoan -> creditLoan.getActive() && creditLoan.getTypeOfLoan().equals(loanType)).mapToInt(CreditLoan::getAmount).sum();
        }
        return 0;
    }

    private Integer getNmbrOfActiveLoansSumDba(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            return creditLoans.stream().filter(creditLoan -> creditLoan.getDba() != null && !creditLoan.getDba() && creditLoan.getTypeOfLoan().equals(loanType)).mapToInt(CreditLoan::getAmount).sum();
        }
        return 0;
    }

    private Integer getNmbrOfActiveLoans(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> activeLoan = creditLoans.stream().filter(creditLoan -> creditLoan.getActive() && creditLoan.getTypeOfLoan().equals(loanType)).toList();
            // TODO: liniile 825-826 nu se vad in poze; presupus la fel ca getNmbrOfActiveLoansDba.
            return !activeLoan.isEmpty() ? activeLoan.size() : 0;
        }
        return 0;
    }

    private Integer getNmbrOfActiveLoansDba(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> activeLoan = creditLoans.stream().filter(creditLoan -> creditLoan.getDba() != null && !creditLoan.getDba() && creditLoan.getTypeOfLoan().equals(loanType)).toList();
            return !activeLoan.isEmpty() ? activeLoan.size() : 0;
        }
        return 0;
    }

    private BigDecimal getNmbrOfActiveOwnerLoans(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> activeOwnerLoan = creditLoans.stream().filter(creditLoan -> creditLoan.getActive() && creditLoan.getTypeOfLoan().equals(loanType)).toList();
            if (!activeOwnerLoan.isEmpty()) {
                return activeOwnerLoan.stream().map(CreditLoan::getInstallment).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        return BigDecimal.valueOf(0);
    }

    private BigDecimal getNmbrOfActiveOwnerLoansDba(List<CreditLoan> creditLoans, String loanType) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> activeOwnerLoan = creditLoans.stream().filter(creditLoan -> creditLoan.getDba() != null && !creditLoan.getDba() && creditLoan.getTypeOfLoan().equals(loanType)).toList();
            if (!activeOwnerLoan.isEmpty()) {
                return activeOwnerLoan.stream().map(CreditLoan::getInstallment).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        return BigDecimal.valueOf(0);
    }

    private Integer getNumberOfLoans(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> loans = creditLoans.stream()
                    .filter(bkrCode -> bkrCode.getBkrCodes() != null && !bkrCode.getBkrCodes().isEmpty())
                    .filter(bkrCode -> bkrCode.getBkrCodes().contains("A"))
                    .filter(bkrCode -> !bkrCode.getBkrCodes().contains("H")).toList();
            return !loans.isEmpty() ? loans.size() : 0;
        }
        return 0;
    }

    public Integer getTotalAmount(List<CreditLoan> creditLoans) {
        if (creditLoans != null && !creditLoans.isEmpty()) {
            List<CreditLoan> activeLoans = creditLoans.stream().filter(CreditLoan::getActive).filter(loans -> loans.getAmount() > 0).toList();
            if (!activeLoans.isEmpty()) {
                return activeLoans.stream().mapToInt(CreditLoan::getAmount).sum();
            }
        }
        return 0;
    }

    public BkrCkiResult mappingRespAndCkiResult(Loan loan, SingleBkrResponse ckiResponse) {
        if (ckiResponse != null && ckiResponse.getResult() != null) {
            var bkrCkiResult = new BkrCkiResult();
            var bkrPerson = new BkrPerson();
            final List<BkrPersonResult> persons = Optional.ofNullable(ckiResponse.getResult())
                    // TODO: liniile 885-886 nu se vad in poze; completate dupa legacy (bkrckiproxy MappingService).
                    .map(SingleBkrResponseResult::getPersons)
                    .map(SingleBkrPersons::getPerson).orElseGet(Collections::emptyList)
                    .stream()
                    .filter(Objects::nonNull)
                    .map(person -> {
                        var bkrPersonResult = new BkrPersonResult();
                        bkrPersonResult.setBusinessKey(loan.getBusinessKey());
                        bkrPerson.setMatchingPercentage(person.getMatchingScore());
                        bkrPerson.setMatchingOutcome(person.getMatchingOutcome());
                        bkrPersonResult.setOverallMatchingOutcome(ckiResponse.getResult().getOverallMatchingOutcome());
                        getBkrScores(bkrPerson, person);
                        getCreditLoans(bkrPerson, bkrCkiResult, person);
                        bkrPersonResult.setMatchedPersons(Collections.singletonList(bkrPerson));
                        bkrPersonResult.setBestMatch(getBestMatch(bkrPersonResult.getMatchedPersons()));
                        return bkrPersonResult;
                    }).toList();
            if (persons.isEmpty()) {
                return null;
            }
            bkrCkiResult.setPersons(persons);

            return bkrCkiResult;
        }
        return null;
    }

    public Integer getEachWorstCardScoreValue(List<BkrPersonResult> persons) {
        // TODO: finalul liniei iese din ecran (IMG_7778, linia 913); ultimul filtru e presupus.
        List<BkrPersonScore> scoreList = persons.stream().filter(Objects::nonNull).flatMap(person -> Optional.ofNullable(person.getBkrScores()).orElseGet(Collections::emptyList).stream().filter(Objects::nonNull)).filter(score -> score.getCardScoreValue() != null).toList();
        if (!scoreList.isEmpty()) {
            Optional<BkrPersonScore> scoreValue = scoreList.stream().filter(Objects::nonNull).max(Comparator.comparing(BkrPersonScore::getCardScoreValue));
            if (scoreValue.isPresent()) {
                return scoreValue.get().getCardScoreValue();
            }
        }
        return 0;
    }

    public String getEachWorstCardRiskClassValue(List<BkrPersonResult> persons) {
        // TODO: finalul liniei iese din ecran (IMG_7778, linia 923); ultimul filtru e presupus.
        List<BkrPersonScore> scoreList = persons.stream().filter(Objects::nonNull).flatMap(person -> Optional.ofNullable(person.getBkrScores()).orElseGet(Collections::emptyList).stream().filter(Objects::nonNull)).filter(score -> score.getCardRiskClass() != null).toList();
        if (!scoreList.isEmpty()) {
            Optional<BkrPersonScore> scoreClass = scoreList.stream().filter(Objects::nonNull).max(Comparator.comparing(BkrPersonScore::getCardRiskClass));
            if (scoreClass.isPresent()) {
                return scoreClass.get().getCardRiskClass();
            }
        }
        return null;
    }

    public BkrPerson getBestMatch(List<BkrPerson> matchedPersons) {
        Optional<BkrPerson> matchPerson = matchedPersons.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(BkrPerson::getMatchingPercentage));
        return matchPerson.orElse(null);
    }

    private void getBkrScores(BkrPerson bkrPerson, SingleBkrPerson person) {
        bkrPerson.setBkrScores(Optional.ofNullable(person.getBkrScores()).map(BkrScoresType::getBkrScore).orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(bkrScore -> {
                    // TODO: liniile 945-949 nu se vad in poze; completate dupa legacy (bkrckiproxy MappingService).
                    var score = new BkrPersonScore();
                    score.setCardName(bkrScore.getCardName());
                    if (!bkrScore.getNotification().isEmpty()) {
                        score.setNotification(bkrScore.getNotification());
                    }
                    if (bkrScore.getResult() != null) {
                        score.setCardRiskClass(bkrScore.getResult().getCardRiskClass());
                        score.setCardRiskPercentage(bkrScore.getResult().getCardRiskPercentage());
                        score.setCardScoreValue((int) bkrScore.getResult().getCardScoreValue());
                    }
                    if (bkrScore.getScoreData() != null && !bkrScore.getScoreData().isEmpty()) {
                        score.setScoreData(bkrScore.getScoreData().stream().collect(Collectors.toMap(ScoreDataType::getScoreDataVariable, ScoreDataType::getScoreDataValue)));
                    }
                    return score;
                }).distinct().toList());
    }

    private void getCreditLoans(BkrPerson bkrPerson, BkrCkiResult bkrCkiResult, SingleBkrPerson person) {
        List<CreditLoan> creditLoans = Optional.ofNullable(person.getCreditLoans()).map(CreditLoansType::getCreditLoan).orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(creditLoan -> {
                    var eachLoan = new CreditLoan();
                    eachLoan.setAccountNumber(creditLoan.getAccountNumber());
                    eachLoan.setActive(!creditLoan.isDba() && creditLoan.getLastPayment() == null);
                    eachLoan.setTypeOfLoan(creditLoan.getTypeOfLoan());
                    eachLoan.setAmount(creditLoan.getLoanAmount() * 100);
                    eachLoan.setDba(creditLoan.isDba());
                    eachLoan.setFirstPayment(getDate(creditLoan.getFirstPayment()));
                    eachLoan.setLastPayment(getDate(creditLoan.getLastPayment()));
                    eachLoan.setPlannedLastPayment(getDate(creditLoan.getHypLastPayment()));
                    eachLoan.setCreated(getDate(creditLoan.getRegistration()));
                    eachLoan.setIdOwnedContract(creditLoan.getOwnedContract());
                    eachLoan.setIndex(creditLoan.getIndex());
                    if (creditLoan.getDetailCodes() != null && creditLoan.getDetailCodes().getDetailCode() != null) {
                        eachLoan.setBkrCodes(creditLoan.getDetailCodes().getDetailCode().stream().filter(Objects::nonNull).map(DetailCodeType::getCode).toList());
                    }
                    eachLoan.setInstallment(getInstallment(eachLoan));
                    eachLoan.setDuration(getDuration(eachLoan));
                    eachLoan.setLowRisk(lowRisk(eachLoan));
                    eachLoan.setMediumRisk(mediumRisk(eachLoan));
                    eachLoan.setHighRisk(highRisk(eachLoan));
                    eachLoan.setBkrCodesString(getBkrToString(eachLoan));
                    eachLoan.setBallon(getBallon(eachLoan));

                    return eachLoan;
                }).distinct().toList();
        if (!creditLoans.isEmpty()) {
            List<CreditLoan> creditLoanList = getCreditLoansList(creditLoans);
            bkrPerson.setCreditLoans(creditLoanList);
            bkrCkiResult.setCreditLoans(creditLoanList);
        } else {
            bkrPerson.setCreditLoans(creditLoans);
            bkrCkiResult.setCreditLoans(creditLoans);
        }
    }

    private CreditLoan getBallon(CreditLoan eachLoan) {
        return eachLoan.getActive() != null && eachLoan.getActive() && eachLoan.getFirstPayment().equals(eachLoan.getPlannedLastPayment()) ? eachLoan : null;
    }

    private String getBkrToString(CreditLoan eachLoan) {
        // TODO: liniile 1007-1011 nu se vad in poze; completate dupa legacy (bkrckiproxy MappingService).
        return (eachLoan.getBkrCodes() != null && !eachLoan.getBkrCodes().isEmpty()) ? eachLoan.getBkrCodes().stream()
                .filter(Objects::nonNull)
                .filter(codes -> !codes.isEmpty())
                .collect(Collectors.joining(",")) : null;
    }

    private LocalDateTime getDate(DateType firstPayment) {
        if (firstPayment != null) {
            return LocalDateTime.of(firstPayment.getYear(), firstPayment.getMonth(), firstPayment.getDay(), 0, 0);
        }
        return null;
    }

    private Boolean lowRisk(CreditLoan eachLoan) {
        boolean lowRisk = Arrays.asList("AK", "DK", "VK", "TC", "OA", "RK").contains(eachLoan.getTypeOfLoan());
        return lowRisk && (eachLoan.getBkrCodes() == null || eachLoan.getBkrCodes().isEmpty());
    }

    private Boolean mediumRisk(CreditLoan eachLoan) {
        boolean mediumRisk = Arrays.asList("AK", "DK", "VK", "TC", "OA", "RK").contains(eachLoan.getTypeOfLoan());
        return mediumRisk && eachLoan.getBkrCodes() != null && eachLoan.getBkrCodes().size() >= 2 && eachLoan.getBkrCodes().contains("A") && eachLoan.getBkrCodes().contains("H");
    }

    /*Based on the Email(14-Apr-2023) with Incident INC5247438, Adapted the below business logic.
      - High Rosk Code ("RO", "SK", "SH", "HY", "ZO")
      - Type of Loan and BkrCodes is Contains A
      - Type of Loan and BkrCode is 1 or 2 or 3 or 4 or 5
      - When Bkr Code = A => BkrMajorPblem = 1
      - When Bkr Code = 1 and/or 2 and/or 3 and/or 4 and/or 5 => BkrMajorPblem = 1
      - When Bkr Code = A + H => BkrMinorPblem = 1 */
    public Boolean highRisk(CreditLoan eachLoan) {
        boolean highRiskCodes = Arrays.asList("RO", "SK", "SH", "HY", "ZO").contains(eachLoan.getTypeOfLoan());
        boolean highRiskCodesBkr = Arrays.asList("AK", "RK", "OA", "HO", "ZK", "RH", "RN").contains(eachLoan.getTypeOfLoan())
                && eachLoan.getBkrCodes() != null
                && eachLoan.getBkrCodes().size() == 1
                && eachLoan.getBkrCodes().contains("A");
        boolean highRiskCdBkrMore = Arrays.asList("AK", "RK", "OA", "HO", "ZK", "RH", "RN").contains(eachLoan.getTypeOfLoan())
                && eachLoan.getBkrCodes() != null
                && !eachLoan.getBkrCodes().isEmpty()
                && eachLoan.getBkrCodes().size() >= 3
                && eachLoan.getBkrCodes().contains("A") && eachLoan.getBkrCodes().contains("H")
                && (eachLoan.getBkrCodes().contains("1") || eachLoan.getBkrCodes().contains("2") || eachLoan.getBkrCodes().contains("3") || eachLoan.getBkrCodes().contains("4") || eachLoan.getBkrCodes().contains("5"));
        boolean highRiskCdBkrCodes = Arrays.asList("AK", "RK", "OA", "HO", "ZK", "RH", "RN").contains(eachLoan.getTypeOfLoan())
                && eachLoan.getBkrCodes() != null && !eachLoan.getBkrCodes().isEmpty()
                && (eachLoan.getBkrCodes().contains("1") || eachLoan.getBkrCodes().contains("2") || eachLoan.getBkrCodes().contains("3") || eachLoan.getBkrCodes().contains("4") || eachLoan.getBkrCodes().contains("5"));
        return (highRiskCodes || highRiskCodesBkr || highRiskCdBkrMore || highRiskCdBkrCodes);
    }

    public Integer getDuration(CreditLoan eachLoan) {
        if (FALSE.equals(eachLoan.getActive())) {
            return 0;
        }
        if (eachLoan.getFirstPayment().equals(eachLoan.getPlannedLastPayment())) {
            return 1;
        } else if (eachLoan.getPlannedLastPayment() == null) {
            return null;
        } else {
            var first = eachLoan.getFirstPayment() != null ? eachLoan.getFirstPayment() : LocalDateTime.now();
            var planned = eachLoan.getPlannedLastPayment() != null ? eachLoan.getPlannedLastPayment() : LocalDateTime.now();

            return getMonths(first, planned);
        }
    }

    private Integer getMonths(LocalDateTime first, LocalDateTime planned) {
        var years = planned.toLocalDate().getYear() - first.toLocalDate().getYear();
        var months = planned.toLocalDate().getMonth().getValue() - first.toLocalDate().getMonth().getValue();
        var yearMonths = 0;
        if (years > 0 || months > 0) {
            yearMonths = months + years * 12;
        }
        return yearMonths + 1;//AS per the previous logic in Old Cloverleaf
    }

    public BigDecimal getInstallment(CreditLoan eachLoan) {
        if (FALSE.equals(eachLoan.getActive())) {
            return BigDecimal.valueOf(0);
        }
        BigDecimal installment;
        if (eachLoan.getFirstPayment().equals(eachLoan.getPlannedLastPayment())) {
            installment = BigDecimal.valueOf(Math.ceil(eachLoan.getAmount() * 0.02));
        } else if (eachLoan.getPlannedLastPayment() == null) {
            installment = BigDecimal.valueOf(Math.max(Math.ceil(eachLoan.getAmount() * 0.02), 2500));
        } else {
            var first = eachLoan.getFirstPayment() != null ? eachLoan.getFirstPayment() : LocalDateTime.now();
            var planned = eachLoan.getPlannedLastPayment() != null ? eachLoan.getPlannedLastPayment() : LocalDateTime.now();
            installment = BigDecimal.valueOf((double) eachLoan.getAmount() / getMonths(first, planned));
            if (OA.equals(eachLoan.getTypeOfLoan()) && eachLoan.getFirstPayment() != null && eachLoan.getFirstPayment().isBefore(LocalDateTime.of(2022, 4, 2, 0, 0))) {
                installment = BigDecimal.valueOf((double) eachLoan.getAmount() / 0.65 / getMonths(first, planned));
            }
        }
        if (eachLoan.getBallon() != null) {
            installment = installment.max(eachLoan.getBallon().getInstallment());
        }

        return installment.setScale(0, RoundingMode.CEILING);
    }

    private String ckiConvertToString(BkrCkiResult bkrCkiResult) {
        try {
            var mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return bkrCkiResult != null ? mapper.writeValueAsString(bkrCkiResult) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
