// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.client;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoapClient {
    private static final String SOAP_FAULT_CODE = "502";
    private static final String TECHNICAL_ERROR_CODE = "555";

    private final CreditCheckPort creditCheckPort;
    private final CollectError collectError;

    public CreditCheckResult creditCheckRequest(ProcessingContext processingContext, CreditCheckRequest creditCheckRequest) {
        CreditCheckResponse creditCheckResponse;
        try {
            creditCheckResponse = creditCheckPort.creditCheck(creditCheckRequest);
        } catch (SOAPFaultException e) {
            String faultMessage = SOAP_ERROR_MESSAGE + ": " + e.getFault().getFaultString();
            log.error(faultMessage, e);
            return registerTechnicalErrors(processingContext, SOAP_FAULT_CODE, faultMessage);
        } catch (Exception e) {
            String errorMessage = "Exception occurred for creditCheckRequest: " + e.getMessage();
            log.error(errorMessage, e);
            return registerTechnicalErrors(processingContext, TECHNICAL_ERROR_CODE, errorMessage);
        }

        if (Objects.isNull(creditCheckResponse)) {
            log.warn("Null response received from CreditCheck after performing the RVDR external SOAP call");
            return registerTechnicalErrors(processingContext, TECHNICAL_ERROR_CODE, "Empty response from CreditCheck");
        }

        return new CreditCheckResult(creditCheckResponse, registerBusinessErrors(processingContext, creditCheckResponse));
    }

    private List<Error> registerBusinessErrors(ProcessingContext processingContext, CreditCheckResponse creditCheckResponse) {
        ErrorsType errorsType = creditCheckResponse.getErrors();

        if (Objects.isNull(errorsType) || CollectionUtils.isEmpty(errorsType.getError())) {
            return List.of();
        }

        List<Error> errors = new ArrayList<>();
        for (ErrorType error : errorsType.getError()) {
            log.warn("CreditCheck business error code = {}, message = {}", error.getCode(), error.getMessage());
            registerError(processingContext, error.getCode(), error.getMessage(),
                    "CreditCheck returned an error: " + error.getCode() + ":" + error.getMessage());
            errors.add(new Error(error.getCode(), error.getMessage()));
        }

        collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
        return errors;
    }

    private CreditCheckResult registerTechnicalErrors(ProcessingContext processingContext, String code, String message) {
        registerError(processingContext, code, message, "Error when executing external SOAP call creditCheckRequest(): " + message);
        collectError.addErrorSignal(processingContext, processingContext.getErrorMessage());
        return new CreditCheckResult(errorResponse(code, message), List.of(new Error(code, message)));
    }

    private void registerError(ProcessingContext processingContext, String code, String message, String errorMapMessage) {
        collectError.addError(processingContext, code, message);
        processingContext.getErrorMap().put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), errorMapMessage);
    }

    private static CreditCheckResponse errorResponse(String code, String message) {
        ErrorType errorType = new ErrorType();
        errorType.setCode(code);
        errorType.setMessage(message);

        ErrorsType errorsType = new ErrorsType();
        errorsType.getError().add(errorType);

        CreditCheckResponse response = new CreditCheckResponse();
        response.setErrors(errorsType);

        return response;
    }
}
