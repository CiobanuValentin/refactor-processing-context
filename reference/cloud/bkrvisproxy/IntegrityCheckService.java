// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.service;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheckService {
    private final GeneralNullOrEmptyCheckService generalNullOrEmptyCheckService;
    private final CollectError collectError;
    private final ApplicationService applicationService;
    private final VisCheckService visCheckService;
    private final Gson gson = GsonInstance.createGson();

    public void loanCheckProcess(ProcessingContext processingContext, OrchestrationMessage orchestrationMessage) {
        Application application = processingContext.getApplication();

        if (Objects.isNull(application)) {
            log.error("No application in context for businessKey {}, skipping loan check",
                    processingContext.getBusinessKey());
            return;
        }

        if (!saveApplication(application, processingContext, orchestrationMessage)) {
            return;
        }

        Loan loan = application.getLoan();
        if (Objects.isNull(loan)) {
            generalNullOrEmptyCheckService.loanNotPresent(processingContext);
            return;
        }

        if (CollectionUtils.isEmpty(loan.getCustomers()) && CollectionUtils.isEmpty(loan.getCompanies())) {
            String errorMessage = "Unable to process VIS data. " +
                    "No valid Customers or Companies found in Application with businessKey: " + processingContext.getBusinessKey();
            log.error(errorMessage);
            collectError.addError(processingContext, String.valueOf(HttpStatus.BAD_REQUEST.value()), errorMessage);
            registerErrorSignal(processingContext, errorMessage);

            return;
        }

        visCheckService.checkVis(processingContext, loan);
    }

    private boolean saveApplication(Application application, ProcessingContext processingContext,
                                    OrchestrationMessage orchestrationMessage) {
        try {
            String applicationJson = gson.toJson(application);
            applicationService.saveApplicationRequest(application, orchestrationMessage, applicationJson, processingContext);
            return true;
        } catch (Exception e) {
            String errorMessage = "Error occurred when trying to save application to database. Error message for businessKey "
                    + processingContext.getBusinessKey() + " and flowId " + processingContext.getFlowId()
                    + ": message: " + e.getMessage();

            log.error(errorMessage, e);
            collectError.addError(processingContext, e);
            registerErrorSignal(processingContext, errorMessage);

            return false;
        }
    }

    private void registerErrorSignal(ProcessingContext processingContext, String errorMessage) {
        processingContext.getErrorMap().put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), errorMessage);
        collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
    }
}
