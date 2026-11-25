// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.error;

@Getter
@Setter
@Component
public class CollectError implements KafkaErrorCollector {

    @Override
    public void addError(ProcessingContextBase pc, Exception exception) { pc.getErrorHandler().addError(exception); }

    @Override
    public void addError(ProcessingContextBase pc,String errorCode, String errorMessage) {
        pc.getErrorHandler().addError(errorCode, errorMessage);
    }

    @Override
    public void addErrorSignal(ProcessingContext pc, Signal errorSignal) {
        errorSignal.setProperties(pc.getErrorMap());
        pc.getErrorSignalMap().put(String.valueOf(errorSignal.getBusinessKey()), errorSignal);
    }

    @Override
    public void addArvalErrorSignal(ArvalProcessingContext apc, ArvalSignal arvalErrorSignal) {
        arvalErrorSignal.setProperties(apc.getErrorMap());
        apc.getArvalErrorSignalMap().put(String.valueOf(arvalErrorSignal.getBusinessKey()), arvalErrorSignal);
    }

    @Override
    public void setStatusMessage(ProcessingContext pc, String businessKey, String flowId, String proxyName) {
        pc.getStatusMessage().setBusinessKey(businessKey);
        pc.getStatusMessage().setFlowId(flowId);
        pc.getStatusMessage().setCommand(proxyName);
        pc.getStatusMessage().setProperties(new HashMap<>());
        pc.getStatusMessage().getProperties().put(STATUS, OK);
    }

    @Override
    public void setArvalStatusMessage(ArvalProcessingContext apc, String businessKey, String flowId, String proxyName) {
        apc.getArvalStatusMessage().setBusinessKey(businessKey);
        apc.getArvalStatusMessage().setFlowId(flowId);
        apc.getArvalStatusMessage().setCommand(proxyName);
        apc.getArvalStatusMessage().setProperties(new HashMap<>());
        apc.getArvalStatusMessage().getProperties().put(STATUS, OK);
    }

    @Override
    public Map<String, Signal> getAllErrorSignalMap(ProcessingContext pc) {
        return new HashMap<>(pc.getErrorSignalMap());
    }

    @Override
    public Map<String, ArvalSignal> getAllArvalErrorSignalMap(ArvalProcessingContext apc) {
        return new HashMap<>(apc.getArvalErrorSignalMap());
    }

    @Override
    public ErrorHandler removeDuplicateErrors(ProcessingContextBase pc) {
        ErrorHandler eHandler = pc.getErrorHandler();
        List<Error> errors = eHandler.getErrors();

        //remove duplicate errors, if any
        for (int i = errors.size() - 1; i > 0; i--) {
            for (int j = i - 1; j >= 0; j--) {
                if (errors.get(i).getErrorMessage().equals(errors.get(j).getErrorMessage())) {
                    errors.remove(i);
                    break;
                }
            }
        }
        return eHandler;
    }
}
