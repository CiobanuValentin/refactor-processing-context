// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.service;

/* ...adnotarile dinainte de @Getter nu erau vizibile */
@Getter
@Setter
public class RatNullOrEmptyCheckService {

    private final RatMappingService mappingService;
    private final ModelMapper modelMapper;
    private final CollectError collectError;
    private final ApplicationBuilderPG applicationBuilderPG;
    private final GeneralNullOrEmptyCheckService generalNullOrEmptyCheckService;
    private final ApplicationService applicationService;

    private final Gson gson = GsonInstance.createGson();
    private Application application;

    public void businessKeyFlowIdEmptyCheck(ProcessingContext processingContext, String proxyName) {
        //all the fields initializations are happening inside businessKeyFlowIdEmptyCheck() in common-api, that's why we are doing generalNullOrEmptyCheckService.get(....)
        if (!generalNullOrEmptyCheckService.businessKeyFlowIdEmptyCheck(processingContext, proxyName)) {
            application = applicationBuilderPG.buildApplication(processingContext);
        } else {
            application = processingContext.getApplication();
        }
    }

    public void loanCheckProcess(ProcessingContext processingContext, OrchestrationMessage orchestrationMessage) {

        Loan loan = application.getLoan();
        Optional.ofNullable(loan)
                .ifPresentOrElse(loanPresent -> {
                            //If both customers and companies lists are empty, we can't process
                            if (CollectionUtils.isEmpty(loanPresent.getCustomers()) && CollectionUtils.isEmpty(loanPresent.getCompanies())) {
                                String message = "Unable to add RAT data. No valid Customers or Companies found in Application with businessKey: " + processingContext.getBusinessKey();
                                log.error(message);
                                collectError.addError(processingContext, String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
                                processingContext.getErrorMap().put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), message);
                                collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
                                throw new ValidationException("Unable to add RAT data. No valid Customers or Companies found in Application.");
                            } else {
                                processApplication(processingContext, orchestrationMessage);
                            }
                        },
                        () -> generalNullOrEmptyCheckService.loanNotPresent(processingContext)
                );
    }

    private void processApplication(ProcessingContext processingContext, OrchestrationMessage orchestrationMessage) {
        try {
            mappingService.processRatSanctionDataAsync(application, processingContext.getFlowId(), gson, modelMapper, collectError, processingContext.getIndex(), processingContext.getErrorMessage(), /* ...taiat */);

            final List<String> skipCommandList = application.getSkipCommand();

            if (CollectionUtils.isEmpty(skipCommandList) || !skipCommandList.contains("rat.compliance")) {
                mappingService.processRatComplianceDataAsync(application, processingContext.getFlowId(), gson, modelMapper, collectError, processingContext.getIndex(), processingContext.getErrorMessage(), /* ...taiat */);
                Application applicationWithScore = mappingService.getScoreResultAsync(application, collectError, processingContext.getIndex(), processingContext.getErrorMessage(), processingContext);
                String applicationJson = gson.toJson(applicationWithScore);
                applicationService.saveApplicationRequest(applicationWithScore, orchestrationMessage, applicationJson, processingContext);
            }

        } catch (Exception e) {
            String message = "RAT External API replied with error message for Business Key " + processingContext.getBusinessKey() + " and flow ID " + processingContext.getFlowId() + ": message: " + e.ge/* ...taiat */;
            log.error(message);
            collectError.addError(processingContext, e);
            processingContext.getErrorMap().put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());

            mappingService.saveCheckLoanSanction(collectError, application, processingContext.getErrorMessage(), gson, processingContext.getBusinessKey(), processingContext.getFlowId(), modelMapper, /* ...taiat */);

            throw new RestClientException("RAT External API replied with error message for Business Key " + processingContext.getBusinessKey() + " and flow ID " + processingContext.getFlowId() + ": mess/* ...taiat */);
        }
    }
}
