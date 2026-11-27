// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheckService {
    private final GeneralNullOrEmptyCheckService generalNullOrEmptyCheckService;
    private final ApplicationService applicationService;
    private final CollectError collectError;
    private final CreditCheckService creditCheckService;
    private final Gson gson = GsonInstance.createGson();

    public void loanCheckProcess(ProcessingContext processingContext, OrchestrationMessage orchestrationMessage) {
        Application application = processingContext.getApplication();

        if (Objects.isNull(application)) {
            log.warn("No application in context for businessKey {}, skipping loan check", processingContext.getBusinessKey());
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

        // (confirmat din pozele din 27.11: intre verificarea loan-ului si cea a clientilor nu mai e nimic)
        if (CollectionUtils.isEmpty(loan.getCustomers())) {
            log.warn("No valid Customers found in Application with businessKey: {}", processingContext.getBusinessKey());
            return;
        }

        creditCheckService.checkCredit(processingContext, loan);
    }

    private boolean saveApplication(Application application, ProcessingContext processingContext, OrchestrationMessage orchestrationMessage) {
        try {
            String applicationJson = gson.toJson(application);
            applicationService.saveApplicationRequest(application, orchestrationMessage, applicationJson, processingContext);
            return true;
        } catch (Exception e) {
            String errorMessage = "Error occurred when trying to save application to database. Error message for Business Key "
                    + processingContext.getBusinessKey() + " and flow ID " + processingContext.getFlowId() + ": message: " + e.getMessage();
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
