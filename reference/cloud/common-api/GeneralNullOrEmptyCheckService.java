// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

public class GeneralNullOrEmptyCheckService {
    /* ...campurile (collectError, gson) si adnotarile nu erau in poze */

    public void applicationEmptyCheck(ProcessingContext pc) {

        if (MapUtils.isEmpty(pc.getSignalProperties()) || StringUtils.isEmpty(pc.getApplicationJson())) {
            log.error(APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);

            //to avoid ListenerExecutionFailedException thrown by kafka listener
            Application application = Application.builder().businessKey(StringUtils.EMPTY).build();
            pc.setApplication(application);

            pc.getErrorMessage().setBusinessKey(StringUtils.EMPTY);
            pc.getErrorMessage().setFlowId(StringUtils.EMPTY);

            collectError.addError(pc, String.valueOf(HttpStatus.BAD_REQUEST.value()), APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
            pc.getErrorMap().put(ERROR_MESSAGE + pc.getIndex().getAndIncrement(), APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
            collectError.addErrorSignal(pc, pc.getErrorMessage());
        } else {
            pc.setApplication(gson.fromJson(pc.getApplicationJson(), Application.class));
            log.debug("Processing Application {}", pc.getApplication());
        }
    }

    // arvalApplicationEmptyCheck(ArvalProcessingContext apc): identica, cu getArvalErrorMessage() si addArvalErrorSignal

    public boolean businessKeyFlowIdEmptyCheck(ProcessingContext pc, String proxyName) {

        String businessKey = pc.getApplication().getBusinessKey();
        log.info("businessKey is: {}", businessKey);
        String flowId = String.valueOf(pc.getSignal().getFlowId());
        pc.setBusinessKey(businessKey);
        pc.setFlowId(flowId);

        pc.getErrorMessage().setBusinessKey(businessKey);
        pc.getErrorMessage().setFlowId(flowId);

        collectError.setStatusMessage(pc, businessKey, flowId, proxyName);

        if (StringUtils.isEmpty(businessKey) || StringUtils.isEmpty(flowId)) {
            log.error("business key: {} or flow id: {} is empty", businessKey, flowId);
            collectError.addError(pc, String.valueOf(HttpStatus.BAD_REQUEST.value()), BUSINESS_KEY_WITH_SPACE + businessKey + OR_FLOW_ID + flowId + IS_EMPTY);
            pc.getErrorMap().put(ERROR_MESSAGE + pc.getIndex().getAndIncrement(), BUSINESS_KEY_WITH_SPACE + businessKey + OR_FLOW_ID + flowId + IS_EMPTY);
            collectError.addErrorSignal(pc, pc.getErrorMessage());
            return true;
        } else {
            return false;
        }
    }

    // arvalBusinessKeyFlowIdEmptyCheck(ArvalProcessingContext, String proxyName): identica, cu getArvalSignal(),
    // getArvalErrorMessage(), setArvalStatusMessage si addArvalErrorSignal; nu seteaza businessKey/flowId pe context

    public void correlationEmptyCheck(ProcessingContext pc, String correlation, String proxyName, OrchestrationMessage orchestrationMessage) {
        //first initialization
        Signal errorMessage = new Signal();
        errorMessage.setCommand(proxyName);
        errorMessage.setProperties(new HashMap<>());

        pc.setErrorMessage(errorMessage);

        pc.setSignal(orchestrationMessage.getData());
        pc.setProducer(String.valueOf(orchestrationMessage.getMetadata().getProducer()));
        pc.setSignalProperties(pc.getSignal().getProperties());
        pc.setApplicationJson(String.valueOf(pc.getSignalProperties().get(new Utf8(APPLICATION))));

        if (StringUtils.isEmpty(correlation)) {
            String message = CORRELATION_ID_NOT_PROVIDED;
            log.error(message);
            collectError.addError(pc, String.valueOf(HttpStatus.BAD_REQUEST.value()), CORRELATION_ID_NOT_PROVIDED);
            pc.getErrorMap().put(ERROR_MESSAGE + pc.getIndex().getAndIncrement(), message);
            collectError.addErrorSignal(pc, errorMessage);
        }
    }

    // arvalCorrelationEmptyCheck(ArvalProcessingContext, String correlation, String proxyName,
    //     ArvalOrchestrationMessage): identica, cu ArvalSignal, setArvalErrorMessage, setArvalSignal

    public void loanNotPresent(ProcessingContext pc) {
        log.error(LOAN_EMPTY);
        collectError.addError(pc, String.valueOf(HttpStatus.BAD_REQUEST.value()), LOAN_EMPTY);
        pc.getErrorMap().put(ERROR_MESSAGE + pc.getIndex().getAndIncrement(), LOAN_EMPTY);
        collectError.addErrorSignal(pc, pc.getErrorMessage());
    }

    // arvalLoanNotPresent(ArvalProcessingContext apc): identica, cu addArvalErrorSignal
}
