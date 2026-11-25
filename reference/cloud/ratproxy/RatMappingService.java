// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RatMappingService {

    private final BNPParibasCloverLeafAPI cloverLeafApi;
    private final CheckLoanComplianceRepository loanComplianceRepository;
    private final CheckLoanSanctionRepository checkLoanSanctionRepo;
    private final ObjectMapper objectMapper;

    /**
     * Takes a quotation and stores it in the CloverLeaf REST API
     */
    public void processRatSanctionDataAsync(Application application, String flowId, Gson gson, ModelMapper modelMapper,
                                            CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {

        final var businessKey = application.getBusinessKey();
        log.info("Process RAT sanction data for businessKey {}", businessKey);

        Loan loan = applicationToRestLoan(application, true, collectError, index, errorMessage, processingContext);
        final var response = cloverLeafApi.putSanctionWithHttpMessages(businessKey, loan, collectError, index, errorMessage, processingContext);

        saveCheckLoanSanction(collectError, application, errorMessage, gson, businessKey, flowId, modelMapper, index, response, processingContext);

        log.info("RAT sanction data processed for businessKey {}", businessKey);
    }

    /**
     * Create ScoringResult from Quotation
     */
    public Application getScoreResultAsync(Application msg, CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        // Get score from RAT API
        log.info("Get RAT scores for businessKey: {}", msg.getBusinessKey());

        Risk risk = cloverLeafApi.getScoreWithHttpMessages(msg.getBusinessKey(), collectError, index, errorMessage, processingContext);

        log.info("RAT score results received for businessKey: {}", msg.getBusinessKey());
        if (risk != null) {
            final var scoringResult = restRiskToBnpScoringResult(risk, msg, collectError, index, errorMessage, processingContext);
            addRiskAssessment(scoringResult, msg);
            msg.setScoringResult(scoringResult);
        }

        return msg;
    }

    private void addRiskAssessment(ScoringResult scoringResult, Application application) {
        if (scoringResult != null) {
            final var customers = application.getLoan().getCustomers();
            final var companies = application.getLoan().getCompanies();

            scoringResult.getRiskAssessments().forEach(riskAssessment -> {

                riskAssessment.setClassificationInstrumentCode("rat.compliance");

                //Add results to input customer, to feed to DES
                Customer customer;
                Organisation organisation;
                if (customers != null) {
                    customer = customers.stream()
                            .filter(customer1 -> customer1.getBusinessKey().equals(riskAssessment.getCustomerBusinessKey()))
                            .findFirst()
                            .orElse(Customer.builder()
                                    .riskAssessments(new ArrayList<>())
                                    .build());
                    customer.getRiskAssessments().add(riskAssessment);
                }
                if (companies != null) {
                    organisation = companies.stream()
                            .filter(company1 -> company1.getBusinessKey().equals(riskAssessment.getCustomerBusinessKey()))
                            .findFirst()
                            .orElse(Organisation.builder()
                                    .riskAssessments(new ArrayList<>())
                                    .build());
                    organisation.getRiskAssessments().add(riskAssessment);
                }

                //Add score output to running output object
                var scoringResultApplication = application.getScoringResult();
                if (scoringResultApplication == null) {
                    scoringResultApplication = new ScoringResult();
                }
                var riskAssessments = scoringResultApplication.getRiskAssessments();
                if (riskAssessments == null) {
                    riskAssessments = new ArrayList<>();
                }
                riskAssessments.add(riskAssessment);
            });
        }
    }

    /**
     * Takes a quotation and stores it in the CloverLeaf REST API
     */
    public void processRatComplianceDataAsync(Application application, String flowId, Gson gson, ModelMapper modelMapper,
                                              CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {

        final var businessKey = application.getBusinessKey();
        log.info("Process RAT compliance data for businessKey {}", businessKey);

        var loan = applicationToRestLoan(application, false, collectError, index, errorMessage, processingContext);
        Boolean response;
        if (application.isCollateralIsPrivateUse()) {
            response = cloverLeafApi.putComplianceCompanyWithHttpMessages(businessKey, loan, collectError, index, errorMessage, processingContext);
        } else {
            response = cloverLeafApi.putComplianceWithHttpMessages(businessKey, loan, collectError, index, errorMessage, processingContext);
        }

        saveCheckLoanCompliance(gson, loan, businessKey, flowId, response, collectError, modelMapper, processingContext);
    }

    private Loan applicationToRestLoan(Application application, Boolean isSanction, CollectError collectError,
                                       AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        try {
            return LoanMapping.map(application, isSanction, collectError, errorMessage, index, processingContext);
        } catch (Exception e) {
            String message = "Exception occurred while Loan Mapping " + e.getMessage();
            log.error(message);
            collectError.addError(processingContext, e);
            processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, errorMessage);
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * Convert REST API Risk object and it's applicants to BNP ScoringResult
     */
    public ScoringResult restRiskToBnpScoringResult(Risk risk, Application inputMessage, CollectError collectError,
                                                    AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        log.info("RAT risk assessment processing for applicants {} and Legal Entities {} ", risk.getApplicants(), risk.getLegalEntities());
        var result = new ScoringResult();
        result.setLoanBusinessKey(risk.getApplicationId());
        result.setApplicationStatus(ApplicationStatus.ACCEPTED);//Overall worst case risk for applicants. initiate to accepted.

        AtomicReference<String> rawResult = new AtomicReference<>();
        try {
            rawResult.set(objectMapper.writeValueAsString(risk));
        } catch (JsonProcessingException e) {
            String message = "Error during the serialization of Risk Object";
            log.error(message);
            rawResult.set(e.getMessage());
            collectError.addError(processingContext, e);
            processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, errorMessage);
        }

        checkRiskApplicants(risk, inputMessage, rawResult.get(), result);
        checkRiskLegalEntities(risk, rawResult.get(), result);
        return result;
    }

    private void checkRiskApplicants(Risk risk, Application inputMessage, String rawResult, ScoringResult result) {
        //Add REST API applicants to BNP RiskAssessments list
        if (CollectionUtils.isNotEmpty(risk.getApplicants())) {
            risk.getApplicants().forEach(applicant -> {
                var applicantRisk = getRiskLevel(applicant.getRisk());
                var customer = inputMessage.getLoan().getCustomers().stream()
                        .filter(customer1 -> customer1.getBusinessKey().equals(applicant.getApplicantId()))
                        .findFirst()
                        .orElse(Customer.builder()
                                .riskAssessments(new ArrayList<>())
                                .supplierData(new HashMap<>())
                                .build());
                RiskAssessment riskAssessment = getRiskAssessment(risk, rawResult, applicantRisk, applicant.getApplicantId());
                var classificationResults = new HashMap<String, Object>();
                classificationResults.put("Reason", applicant.getReason());
                classificationResults.put("HasAlerts", applicant.getHasAlerts().toString());
                classificationResults.put("Risk", applicantRisk.getRiskValue());
                final var score = applicant.getScore();
                if (score != null) {
                    classificationResults.put("Score", score.toString());
                }
                boolean mediumRiskOrAlerts = applicantRisk != RiskLevel.LOW || applicant.getHasAlerts();
                classificationResults.put("MediumRiskOrAlerts", String.valueOf(mediumRiskOrAlerts));
                classificationResults.put(MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD, mediumRiskOrAlerts || ApplicationStatus.ON_HOLD.getValue().equals(risk.getLoanStatus()) ? "true" : "false");

                final var supplierData = customer.getSupplierData();
                if (MapUtils.isNotEmpty(supplierData)) {
                    classificationResults.put("OrgPartyType", supplierData.get("PartyType"));
                }
                riskAssessment.setClassificationResults(classificationResults);
                result.addRiskAssessment(riskAssessment);
                var scoreReason = getScoreReason(result, applicant.getHasAlerts(), applicant.getReason(), applicantRisk);

                log.info("Individual BusinessKey: {}. RAT score with risk score {} ({}). Alerts found: {}. Loan status {}. TEST : {}",
                        applicant.getApplicantId(), applicantRisk.getRiskValue(), score, scoreReason, risk.getLoanStatus(), riskAssessment.getClassificationResults().get(MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD));
            });
        }
    }

    private void checkRiskLegalEntities(Risk risk, String rawResult, ScoringResult result) {
        // Add legal entities to scoring result if the legal entity is the holder
        if (CollectionUtils.isNotEmpty(risk.getLegalEntities())) {
            risk.getLegalEntities().forEach(legalEntity -> {
                var legalEntityRisk = getRiskLevel(legalEntity.getRisk());
                RiskAssessment riskAssessmentLegalEntity = getRiskAssessment(risk, rawResult, legalEntityRisk, legalEntity.getApplicantId());
                var classificationLegalEntityResults = new HashMap<String, Object>();
                classificationLegalEntityResults.put("Reason", legalEntity.getReason());
                classificationLegalEntityResults.put("HasAlerts", legalEntity.getHasAlerts().toString());
                classificationLegalEntityResults.put("Risk", legalEntityRisk.getRiskValue());
                final Integer score = legalEntity.getScore();
                if (score != null) {
                    classificationLegalEntityResults.put("Score", score.toString());
                }
                boolean mediumRiskOrAlertsLegalEntity = legalEntityRisk != RiskLevel.LOW || legalEntity.getHasAlerts();
                classificationLegalEntityResults.put("MediumRiskOrAlerts", Boolean.toString(mediumRiskOrAlertsLegalEntity));
                boolean mediumRiskOrAlertsOrOnHoldLegalEntity = legalEntityRisk != RiskLevel.LOW || legalEntity.getHasAlerts() || risk.getLoanStatus().equals("OnHold");
                classificationLegalEntityResults.put(MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD, Boolean.toString(mediumRiskOrAlertsOrOnHoldLegalEntity));
                riskAssessmentLegalEntity.setClassificationResults(classificationLegalEntityResults);
                result.addRiskAssessment(riskAssessmentLegalEntity);

                var scoreReason = getScoreReason(result, legalEntity.getHasAlerts(), legalEntity.getReason(), legalEntityRisk);
                log.info("Legal entity BusinessKey: {}. RAT score with risk score {} ({})." +
                                " Alerts found: {}. Loan status {}. ",
                        legalEntity.getApplicantId(), legalEntity.getRisk(), score, scoreReason, risk.getLoanStatus()
                );
            });
        }
    }

    // we are receiving the response from rat with below combination, so we are capitalising the first character
    // medium, low, high, Low, Medium, High.
    public RiskLevel getRiskLevel(String risk) {
        if (!StringUtils.isEmpty(risk)) {
            String riskLevel = StringUtils.capitalize(risk.toLowerCase());
            return RiskLevel.getRiskLevel(riskLevel);
        }
        return RiskLevel.HIGH;
    }

    private RiskAssessment getRiskAssessment(Risk risk, String rawResult, RiskLevel applicantRisk, String applicantId) {
        var riskAssessment = new RiskAssessment();

        riskAssessment.setLoanBusinessKey(risk.getApplicationId());
        riskAssessment.setCustomerBusinessKey(applicantId);
        riskAssessment.setRiskLevel(applicantRisk);
        riskAssessment.setClassificationInstrumentName("RAT compliance");
        riskAssessment.setRawResult(rawResult);
        return riskAssessment;
    }

    private String getScoreReason(ScoringResult result, boolean alerts, String reason, RiskLevel applicantRisk) {
        //Current applicant risk is not set to low so set overall worst case risk to OnHold
        if (applicantRisk != RiskLevel.LOW || alerts) {
            result.setApplicationStatus(ApplicationStatus.ON_HOLD);
            return StringUtils.defaultIfEmpty(reason, null);
        }
        return StringUtils.EMPTY;
    }

    public void saveCheckLoanSanction(CollectError collectError, Application application, Signal errorMessage, Gson gson,
                                      String businessKey, String flowId, ModelMapper modelMapper, AtomicInteger index, Boolean response, ProcessingContext processingContext) {

        ErrorHandler errorHandler = collectError.removeDuplicateErrors(processingContext);

        GeneralProxyObsDto generalProxyObsDto = new GeneralProxyObsDto();
        Loan loan = applicationToRestLoan(application, true, collectError, index, errorMessage, processingContext);
        generalProxyObsDto.setRequestLoanJson(gson.toJson(loan));
        generalProxyObsDto.setBusinessKey(businessKey);
        generalProxyObsDto.setFlowId(flowId);
        generalProxyObsDto.setErrorHandlerJson(gson.toJson(errorHandler));
        generalProxyObsDto.setRatResponse(response);

        CheckLoanSanctionEntity checkLoanSanctionEntity = modelMapper.map(generalProxyObsDto, CheckLoanSanctionEntity.class);
        checkLoanSanctionRepo.save(checkLoanSanctionEntity);
        log.info("Saving CheckLoanSanctionEntity in table check_loan_sanction: {}", checkLoanSanctionEntity);
    }

    public void saveCheckLoanCompliance(Gson gson, Loan loan, String businessKey, String flowId, Boolean response,
                                        CollectError collectError, ModelMapper modelMapper, ProcessingContext processingContext) {

        ErrorHandler errorHandler = collectError.removeDuplicateErrors(processingContext);

        GeneralProxyObsDto generalProxyObsDto = new GeneralProxyObsDto();
        generalProxyObsDto.setRequestLoanJson(gson.toJson(loan));
        generalProxyObsDto.setBusinessKey(businessKey);
        generalProxyObsDto.setFlowId(flowId);
        generalProxyObsDto.setRatResponse(response);
        generalProxyObsDto.setErrorHandlerJson(gson.toJson(errorHandler));

        CheckLoanComplianceEntity checkLoanComplianceEntity = modelMapper.map(generalProxyObsDto, CheckLoanComplianceEntity.class);
        loanComplianceRepository.save(checkLoanComplianceEntity);
        log.info("Saving CheckLoanComplianceEntity in table check_loan_compliance: {}", checkLoanComplianceEntity);
    }
    /* ...restul clasei (dupa linia 328) nu era in poze */
}
