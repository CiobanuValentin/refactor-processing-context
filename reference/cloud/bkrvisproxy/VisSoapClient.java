// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.client;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisSoapClient {
    private static final String SOAP_FAULT_CODE = "502";
    private static final String TECHNICAL_ERROR_CODE = "555";

    private final CollectError collectError;
    private final VISPort visPort;

    public VisCheckResult visCheck(ProcessingContext processingContext, VisRequest visRequest) {
        VisResponse visResponse;
        try {
            visResponse = visPort.vis(visRequest);
        } catch (SOAPFaultException e) {
            String faultMessage = SOAP_ERROR_MESSAGE + ": " + e.getFault().getFaultString();
            log.error(faultMessage, e);
            return registerTechnicalErrors(processingContext, SOAP_FAULT_CODE, faultMessage);
        } catch (Exception e) {
            String errorMessage = "Exception occurred for visRequest: " + e.getMessage();
            log.error(errorMessage, e);
            return registerTechnicalErrors(processingContext, TECHNICAL_ERROR_CODE, errorMessage);
        }

        if (Objects.isNull(visResponse)) {
            log.warn("Null response received after performing the VIS external SOAP call");
            return registerTechnicalErrors(processingContext, TECHNICAL_ERROR_CODE, "Empty response received from VIS external SOAP call");
        }

        return new VisCheckResult(visResponse, registerBusinessErrors(processingContext, visResponse));
    }

    private List<Error> registerBusinessErrors(ProcessingContext processingContext, VisResponse visResponse) {
        ErrorsType errorsType = visResponse.getErrors();

        if (Objects.isNull(errorsType) || CollectionUtils.isEmpty(errorsType.getError())) {
            return List.of();
        }

        List<Error> errors = new ArrayList<>();
        for (ErrorType error : errorsType.getError()) {
            log.warn("VIS Business Error code = {}, message = {}", error.getCode(), error.getMessage());
            registerError(processingContext, error.getCode(), error.getMessage(),
                    "VIS Returned an error: " + error.getCode() + ":" + error.getMessage());
            errors.add(new Error(error.getCode(), error.getMessage()));
        }

        collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
        return errors;
    }

    private VisCheckResult registerTechnicalErrors(ProcessingContext processingContext, String code, String message) {
        registerError(processingContext, code, message, "Error when executing VIS external SOAP call vis(): " + message);
        collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
        return new VisCheckResult(errorResponse(code, message), List.of(new Error(code, message)));
    }

    private void registerError(ProcessingContext processingContext, String code, String message, String errorMapMessage) {
        collectError.addError(processingContext, code, message);
        processingContext.getErrorMap().put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), errorMapMessage);
    }

    private static VisResponse errorResponse(String code, String message) {
        ErrorType errorType = new ErrorType();
        errorType.setCode(code);
        errorType.setMessage(message);

        ErrorsType errorsType = new ErrorsType();
        errorsType.getError().add(errorType);

        VisResponse response = new VisResponse();
        response.setErrors(errorsType);

        return response;
    }
}
