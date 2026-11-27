package com.bnp.pf.transformation.rvdrproxy.client;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.SOAP_ERROR_MESSAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Error, clasele generate din WSDL (CreditCheckPort, CreditCheckRequest, CreditCheckResponse,
// ErrorType, ErrorsType) si SOAPFaultException (jakarta / javax) lipsesc sau sunt presupuse - pastreaza-le pe cele
// din clasa actuala.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.rvdrproxy.model.CreditCheckResult;

/**
 * Apelul SOAP catre RVDR. Erorile se inregistreaza in context cu acelasi text care ajungea pana acum pe topicul de
 * error (cel din errorMap); CreditCheckResult pastreaza, ca inainte, codul si mesajul brut, care se salveaza pe rand.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SoapClient {
    private static final String SOAP_FAULT_CODE = "502";
    private static final String TECHNICAL_ERROR_CODE = "555";

    private final CreditCheckPort creditCheckPort;

    public CreditCheckResult creditCheckRequest(ProxyContext ctx, CreditCheckRequest creditCheckRequest) {
        CreditCheckResponse creditCheckResponse;
        try {
            creditCheckResponse = creditCheckPort.creditCheck(creditCheckRequest);
        } catch (SOAPFaultException e) {
            String faultMessage = SOAP_ERROR_MESSAGE + ": " + e.getFault().getFaultString();
            log.error(faultMessage, e);
            return technicalError(ctx, SOAP_FAULT_CODE, faultMessage);
        } catch (Exception e) {
            String errorMessage = "Exception occurred for creditCheckRequest: " + e.getMessage();
            log.error(errorMessage, e);
            return technicalError(ctx, TECHNICAL_ERROR_CODE, errorMessage);
        }

        if (Objects.isNull(creditCheckResponse)) {
            log.warn("Null response received from CreditCheck after performing the RVDR external SOAP call");
            return technicalError(ctx, TECHNICAL_ERROR_CODE, "Empty response from CreditCheck");
        }

        return new CreditCheckResult(creditCheckResponse, businessErrors(ctx, creditCheckResponse));
    }

    private List<Error> businessErrors(ProxyContext ctx, CreditCheckResponse creditCheckResponse) {
        ErrorsType errorsType = creditCheckResponse.getErrors();

        if (Objects.isNull(errorsType) || CollectionUtils.isEmpty(errorsType.getError())) {
            return List.of();
        }

        List<Error> errors = new ArrayList<>();
        for (ErrorType error : errorsType.getError()) {
            log.warn("CreditCheck business error code = {}, message = {}", error.getCode(), error.getMessage());
            ctx.addError(error.getCode(), "CreditCheck returned an error: " + error.getCode() + ":" + error.getMessage());
            errors.add(new Error(error.getCode(), error.getMessage()));
        }
        return errors;
    }

    private CreditCheckResult technicalError(ProxyContext ctx, String code, String message) {
        ctx.addError(code, "Error when executing external SOAP call creditCheckRequest(): " + message);
        return new CreditCheckResult(errorResponse(code, message), List.of(new Error(code, message)));
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
