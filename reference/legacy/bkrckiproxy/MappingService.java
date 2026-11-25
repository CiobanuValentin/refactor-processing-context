// REFERINTA - cod existent (legacy, bkrckiproxy master), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.ckiproxy.service;

// Clasa are peste 665 de linii; aici sunt liniile 36-385 din poze. Restul (mappingRespAndCkiResult, getBestMatch,
// getTotalAmount, getNmbrOfActiveLoans..., createSingleBrokerRequest) nu a fost vazut.

@Service
@Slf4j
public class MappingService {

    private final CachingService cachingService;
    private final SoapClient soapClient;
    private final CheckCkiSingleBrokerRequestAndResponseRepository ckiRequestRepo;

    public MappingService(CachingService cachingService, SoapClient soapClient, CheckCkiSingleBrokerRequestAndResponseRepository ckiRequestRepo) {
        this.cachingService = cachingService;
        this.soapClient = soapClient;
        this.ckiRequestRepo = ckiRequestRepo;
    }

    public List<ErrorHandler> checkCki(Application application, String flowId) {
        log.info("Checking the Cki for business key {} ", application.getBusinessKey());
        var errors = new ArrayList<ErrorHandler>();
        var customers = application.getLoan().getCustomers();
        if (customers != null && !customers.isEmpty()) {
            CheckCkiRequestAndResponse response;
            var parameters = application.getCkiParameters();
            var parametersProvided = areParametersProvided(parameters);
            if (isEligibleCustomers(application.getLoan()) && parametersProvided) {
                for (Customer customer : customers) {
                    if (!COMPANY_PERSON.getApplicant().equals(customer.getApplicantType()) && isEligibleCustomersCoHolder(application.getLoan(), customer)) {
                        var ckiRequest = createSingleBrokerRequest(customer, parameters);
                        response = cachingService.checkCkiRequest(ckiRequest);
                        if (Objects.isNull(response) || Objects.isNull(response.getResponse()) || (Objects.nonNull(response.getResponse().getErrors()) && !response.getResponse().getErrors().getError().i/* ...taiat */)) {
                            log.info("Invoking the SingleBkrRequest for Customer {} for business key {} ", customer.getLastName(), application.getBusinessKey());
                            var ckiResponse = soapClient.checkCkiRequest(ckiRequest);
                            var errorHandler = processErrors(ckiResponse, application.getBusinessKey(), flowId);
                            if (errorHandler != null) {
                                errors.add(errorHandler);
                            }
                            createAndSaveCki(application, errorHandler, ckiResponse, ckiRequest, flowId);
                        } else {
                            log.info("Found response in cache for Customer {} for business key {}, No need to call SingleBkr ", customer.getFirstName(), application.getBusinessKey());
                            if (response.getErrors() != null) {
                                errors.add(response.getErrors());
                            }
                            createAndSaveCki(application, response, flowId);
                        }
                    } else {
                        log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}", customer.getApplicantType(), application.getLoan().getBusinessKey());
                    }
                }
            } else {
                if (!parametersProvided) {
                    log.error("Parameters for CKI request not provided in the application request for business key {} and flowId {}", application.getBusinessKey(), flowId);
                    var errorHandler = new ErrorHandler();
                    errorHandler.addError("400", "CKI Parameters not received");
                    errors.add(errorHandler);
                }
                log.info("Not the Eligible customer with credit id {} for business Key {}", application.getLoan().getCreditType(), application.getLoan().getBusinessKey());
            }
        } else log.info("No Customers found for business key {}", application.getBusinessKey());

        log.info("updating the Cki for business key {} ", application.getBusinessKey());

        return errors;
    }

    private boolean areParametersProvided(CkiParameters parameters) {
        return parameters != null && !(StringUtil.isNullOrEmpty(parameters.getChannel()) || StringUtil.isNullOrEmpty(parameters.getCompany()) || StringUtil.isNullOrEmpty(parameters.getLabel()));
    }

    // isEligibleCustomers, isEligibleCustomersCoHolder, getCompanies: identice cu cele din cloud.

    public boolean validApplicantName(Loan loan) {
        return loan.getCustomers().stream().anyMatch(customer -> HOLDER.getApplicant().equals(customer.getApplicantType())
                || HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType())
                || CO_HOLDER.getApplicant().equals(customer.getApplicantType())
                || CO_HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType()));
    }

    public boolean isCkiExists(Customer customers) {
        Optional<RiskAssessment> riskAssessments = customers.getRiskAssessments()
                .stream()
                .filter(Objects::nonNull)
                .filter(risk -> CLASS_INSTR_CD.equals(risk.getClassificationInstrumentCode()))
                .filter(risk -> CLASS_INSTR_NAME.equals(risk.getClassificationInstrumentName())).findAny();
        return riskAssessments.isPresent();
    }

    private ErrorHandler processErrors(SingleBkrResponse ckiResponse, String businessKey, String flowId) {
        if (ckiResponse.getErrors() != null && !ckiResponse.getErrors().getError().isEmpty()) {
            var errors = new ErrorHandler();

            for (ErrorType errorType : ckiResponse.getErrors().getError()) {
                log.error("BKR CKI External API error for businessKey {} and flow ID {} -> code: {}, message: {}", businessKey, flowId, errorType.getCode(), errorType.getMessage());
                errors.addError(errorType.getCode(), errorType.getMessage());
            }
            return errors;
        }
        return null;
    }

    // raspuns gasit in cache
    private void createAndSaveCki(Application application, CheckCkiRequestAndResponse ckiReqResp, String flowId) {
        var ckiRequestAndResponse = new CheckCkiRequestAndResponse();
        ckiRequestAndResponse.setBusinessKey(application.getBusinessKey());
        ckiRequestAndResponse.setRequest(ckiReqResp.getRequest());
        ckiRequestAndResponse.setResponse(ckiReqResp.getResponse());
        ckiRequestAndResponse.setErrors(ckiReqResp.getErrors());
        ckiRequestAndResponse.setFlowId(flowId);
        ckiRequestAndResponse.setTime(ckiReqResp.getTime());
        ckiRequestRepo.save(ckiRequestAndResponse);
    }

    // raspuns nou de la serviciul SOAP
    private void createAndSaveCki(Application application, ErrorHandler errors, SingleBkrResponse ckiResponse, SingleBkrRequest ckiRequest, String flowId) {
        var ckiRequestAndResponse = new CheckCkiRequestAndResponse();
        ckiRequestAndResponse.setBusinessKey(application.getBusinessKey());
        ckiRequestAndResponse.setRequest(ckiRequest);
        if (ckiResponse.getErrors() == null) ckiRequestAndResponse.setResponse(ckiResponse);
        ckiRequestAndResponse.setErrors(errors);
        ckiRequestAndResponse.setFlowId(flowId);
        ckiRequestAndResponse.setTime(LocalDateTime.now());
        ckiRequestRepo.save(ckiRequestAndResponse);
    }

    public RiskAssessment createRiskAssessment() {
        var riskAssessment = new RiskAssessment();
        riskAssessment.setClassificationInstrumentCode(CLASS_INSTR_CD);
        riskAssessment.setClassificationInstrumentName(CLASS_INSTR_NAME);
        riskAssessment.setApplicationStatus(ApplicationStatus.UNKNOWN.getValue());
        riskAssessment.setRawResult("");
        riskAssessment.setRiskLevel(LOW);
        riskAssessment.setDescription(RESULT_DESC);
        Map<String, Object> classResult = new HashMap<>();
        riskAssessment.setClassificationResults(classResult);
        riskAssessment.setHasError(false);
        return riskAssessment;
    }

    public List<RiskAssessment> createRiskAssessments(Loan loan, SingleBkrResponse ckiResponse) {
        List<RiskAssessment> riskAssessmentList = new ArrayList<>();
        var riskAssmntHolder = createRiskAssessment();
        riskAssmntHolder.setRawResult(createRawResult(loan, ckiResponse));
        riskAssmntHolder.setResultType(RESULT_HOLDER);
        riskAssmntHolder.setDescription(RESULT_HOLDER_DESC);
        riskAssmntHolder.setRiskLevel(getRiskLevelHolder(loan));
        createClassificationResult(riskAssmntHolder, loan, ckiResponse);
        riskAssessmentList.add(riskAssmntHolder);

        var riskAssmntCoHolder = createRiskAssessment();
        riskAssmntCoHolder.setResultType(RESULT_CO_HOLDER);
        riskAssmntCoHolder.setDescription(RESULT_CO_HOLDER_DESC);
        riskAssmntCoHolder.setRawResult(createRawResult(loan, ckiResponse));
        riskAssmntCoHolder.setRiskLevel(getRiskLevelCoHolder(loan));
        createClassificationResult(riskAssmntCoHolder, loan, ckiResponse);
        riskAssessmentList.add(riskAssmntCoHolder);

        return riskAssessmentList;
    }

    private void createClassificationResult(RiskAssessment riskAssmntHolder, Loan loan, SingleBkrResponse ckiResponse) {
        Map<String, Object> classResult = new HashMap<>();
        var bkrCkiResult = getCkiResult(loan, ckiResponse);
        if (bkrCkiResult != null) {
            classResult.put("TotalMonthlyInstallments", bkrCkiResult.getTotalMonthlyInstallments());
            classResult.put("TotalLoanAmount", bkrCkiResult.getTotalLoanAmount());
            classResult.put("NumberOfLoansWithCode", bkrCkiResult.getNumberOfLoansWithCode());
            classResult.put("NumberOfLoansLowRisk", bkrCkiResult.getNumberOfLoansLowRisk());
            classResult.put("NumberOfLoansMediumRisk", bkrCkiResult.getNumberOfLoansMediumRisk());
            classResult.put("NumberOfLoansHighRisk", bkrCkiResult.getNumberOfLoansHighRisk());
            classResult.put("NumberOfLoansWithHersteldCode", bkrCkiResult.getNumberOfLoansWithHersteldCode());
            // Cate o cheie pentru fiecare dintre cele 10 tipuri, in aceasta ordine la primele doua grupuri:
            // Other, Mortgage, DebtSettlement, RevolvingLoan, TelecomContract, MailOrder, PersonalLoan,
            // OperationalLease, HouseDebtWithNhg, HouseDebtWithoutNhg
            //   "NumberOfActiveLoansType<Tip>"      -> getNumberOfActiveLoansType<Tip>()
            classResult.put("NumberOfActiveOwnedContracts", bkrCkiResult.getNumberOfActiveOwnedContracts());
            //   "InstallmentActiveLoansType<Tip>"   -> getInstallmentActiveLoansType<Tip>()
            //   "AmountOfActiveLoansType<Tip>"      -> getAmountOfActiveLoansType<Tip>()
            classResult.put("WorstCardRiskClass", bkrCkiResult.getWorstCardRiskClass());
            classResult.put("WorstCardScoreValue", bkrCkiResult.getWorstCardScoreValue());
            classResult.put("NumOpen12m", bkrCkiResult.getNumOpen12m());
            classResult.put("NumS15Open12m", bkrCkiResult.getNumS15Open12m());

            if (bkrCkiResult.getPersons() != null && !bkrCkiResult.getPersons().isEmpty()) {
                for (BkrPersonResult person : bkrCkiResult.getPersons()) {
                    classResult.put("BkrStatus", "02".equals(person.getOverallMatchingOutcome()) ? 0 : 1);
                    classResult.put("OrgPartyType", person.getSupplierData() != null ? person.getSupplierData().get("PartyType") : "00");
                    classResult.put("NeedsManualCheck", person.getNeedsManualCheck());
                }
            }
            Stream<Map<String, Object>> loanResult = getLoanResult(bkrCkiResult);
            Optional<Map<String, Object>> loans = loanResult.findAny();
            loans.ifPresent(stringObjectMap -> classResult.put("Loans", stringObjectMap));
        }
        classResult.put("Result", "false");
        riskAssmntHolder.setClassificationResults(classResult);
    }

    private Stream<Map<String, Object>> getLoanResult(BkrCkiResult bkrCkiResult) {
        return bkrCkiResult.getCreditLoans().stream()
                .filter(Objects::nonNull)
                .map(creditLoan -> {
                    Map<String, Object> loans = new HashMap<>();
                    loans.put("TypeOfLoan", creditLoan.getTypeOfLoan());
                    loans.put("Amount", creditLoan.getAmount());
                    loans.put("Installment", creditLoan.getInstallment());
                    loans.put("Duration", creditLoan.getDuration());
                    loans.put("AccountNumber", creditLoan.getAccountNumber());
                    loans.put("DateFirstPayment", creditLoan.getFirstPayment());
                    loans.put("DateTheoreticalLastPayment", creditLoan.getPlannedLastPayment());
                    loans.put("DateLastPayment", creditLoan.getLastPayment());
                    loans.put("IdContract", creditLoan.getIdOwnedContract());
                    loans.put("IndexNumber", creditLoan.getIndex());
                    loans.put("IdEndedAtBKR", TRUE.equals(creditLoan.getDba()) ? "1" : "0");
                    loans.put("DateRegistration", creditLoan.getCreated());
                    loans.put("DetailCodes", creditLoan.getBkrCodesString());
                    loans.put("CodeRiskClass", getCodeRisk(creditLoan));

                    return loans;
                });
    }

    private String getCodeRisk(CreditLoan creditLoan) {
        if (TRUE.equals(creditLoan.getHighRisk())) {
            return "high";
        }
        return TRUE.equals(creditLoan.getMediumRisk()) ? "med" : "low";
    }

    private String createRawResult(Loan loan, SingleBkrResponse ckiResponse) {
        var bkrCkiResult = mappingRespAndCkiResult(loan, ckiResponse);
        mapAdditionalResult(bkrCkiResult);
        return bkrCkiResult != null ? bkrCkiResult.toString() : "";
    }

    private BkrCkiResult getCkiResult(Loan loan, SingleBkrResponse ckiResponse) {
        var bkrCkiResult = mappingRespAndCkiResult(loan, ckiResponse);
        mapAdditionalResult(bkrCkiResult);
        return bkrCkiResult;
    }

    private void mapAdditionalResult(BkrCkiResult bkrCkiResult) {
        if (bkrCkiResult != null) {
            bkrCkiResult.setTotalLoanAmount(getTotalAmount(bkrCkiResult));
            bkrCkiResult.setNumberOfLoansWithCode(getNumberOfLoans(bkrCkiResult));
            bkrCkiResult.setNumberOfLoansLowRisk(getLowRiskLoans(bkrCkiResult));
            bkrCkiResult.setNumberOfLoansMediumRisk(getMediumRiskLoans(bkrCkiResult));
            bkrCkiResult.setNumberOfLoansHighRisk(getHighRiskLoans(bkrCkiResult));
            bkrCkiResult.setNumberOfLoansWithHersteldCode(getLoansHersteldCode(bkrCkiResult));
            // Pentru cele 10 tipuri, cu codurile: Other=RO, Mortgage=HY, DebtSettlement=SR, RevolvingLoan=RK,
            // MailOrder=VK, TelecomContract=TC, PersonalLoan=AK, OperationalLease=OA, HouseDebtWithNhg=RN,
            // HouseDebtWithoutNhg=RH
            //   setNumberOfActiveLoansType<Tip>(getNmbrOfActiveLoans(bkrCkiResult, <COD>))
            bkrCkiResult.setNumberOfActiveOwnedContracts(activeOwnedContracts(bkrCkiResult));
            //   setInstallmentActiveLoansType<Tip>(getNmbrOfActiveOwnerLoans(bkrCkiResult, <COD>))
            //   setAmountOfActiveLoansType<Tip>(getNmbrOfActiveLoansSum(bkrCkiResult, <COD>))
            bkrCkiResult.setTotalMonthlyInstallments(getTotalMonthlyInstallments(bkrCkiResult));
            bkrCkiResult.setNumOpen12m(getNumOpen12m(bkrCkiResult));
            bkrCkiResult.setNumS15Open12m(getNumS15Open12m(bkrCkiResult));
            bkrCkiResult.setWorstCardRiskClass(getWorstCardRiskClass(bkrCkiResult));
            bkrCkiResult.setWorstCardScoreValue(getWorstCardScoreValue(bkrCkiResult));
        }
    }

    private Integer getWorstCardScoreValue(BkrCkiResult bkrCkiResult) {
        Stream<Stream<BkrPersonScore>> scoreResult = bkrCkiResult.getPersons().stream()
                .filter(Objects::nonNull)
                .map(persons -> persons.getMatchedPersons()
                        .stream()
                        .filter((Objects::nonNull))
                        .filter(bkrScores -> !bkrScores.getBkrScores().isEmpty())
                        .map(person -> person.getBkrScores().stream().max(Comparator.comparing(BkrPersonScore::getCardScoreValue)).get()));
        Optional<BkrPersonScore> resultValue = scoreResult.findAny().flatMap(Stream::findAny);
        return resultValue.map(BkrPersonScore::getCardScoreValue).orElse(null);
    }

    private String getWorstCardRiskClass(BkrCkiResult bkrCkiResult) {
        Stream<Stream<BkrPersonScore>> scoreResult = bkrCkiResult.getPersons().stream()
                .filter(Objects::nonNull)
                .map(persons -> persons.getMatchedPersons().stream()
                        .filter(Objects::nonNull)
                        .filter(bkrScores -> !bkrScores.getBkrScores().isEmpty())
                        .map(person -> person.getBkrScores().stream()
                                .filter(Objects::nonNull)
                                .max(Comparator.comparing(BkrPersonScore::getCardRiskClass)).get()));
        Optional<BkrPersonScore> resultValue = scoreResult.findAny().flatMap(Stream::findAny);
        /* ...restul nu era in poza */
    }
    /* ...liniile de dupa 385 nu au fost vazute */
}
