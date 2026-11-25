package com.bnp.pf.transformation.visproxy.client;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.SOAP_ERROR_MESSAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Error, VisCheckResult, clasele generate din WSDL si SOAPFaultException (jakarta / javax)
// sunt presupuse - pastreaza-le pe cele din clasa actuala.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.visproxy.model.VisCheckResult;

import urn.bkr.wsdl.vis.ErrorType;
import urn.bkr.wsdl.vis.ErrorsType;
import urn.bkr.wsdl.vis.VISPort;
import urn.bkr.wsdl.vis.VisRequest;
import urn.bkr.wsdl.vis.VisResponse;

/**
 * Apelul SOAP catre VIS. Erorile se inregistreaza in context cu acelasi text care ajungea pana acum pe topicul de
 * error (cel din errorMap); VisCheckResult pastreaza, ca inainte, codul si mesajul brut, care se salveaza pe rand.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisSoapClient {
    private static final String SOAP_FAULT_CODE = "502";
    private static final String TECHNICAL_ERROR_CODE = "555";

    private final VISPort visPort;

    public VisCheckResult visCheck(ProxyContext ctx, VisRequest visRequest) {
        VisResponse visResponse;
        try {
            visResponse = visPort.vis(visRequest);
        } catch (SOAPFaultException e) {
            String faultMessage = SOAP_ERROR_MESSAGE + ": " + e.getFault().getFaultString();
            log.error(faultMessage, e);
            return technicalError(ctx, SOAP_FAULT_CODE, faultMessage);
        } catch (Exception e) {
            String errorMessage = "Exception occurred for visRequest: " + e.getMessage();
            log.error(errorMessage, e);
            return technicalError(ctx, TECHNICAL_ERROR_CODE, errorMessage);
        }

        if (Objects.isNull(visResponse)) {
            log.warn("Null response received after performing the VIS external SOAP call");
            return technicalError(ctx, TECHNICAL_ERROR_CODE, "Empty response received from VIS external SOAP call");
        }

        return new VisCheckResult(visResponse, businessErrors(ctx, visResponse));
    }

    private List<Error> businessErrors(ProxyContext ctx, VisResponse visResponse) {
        ErrorsType errorsType = visResponse.getErrors();

        if (Objects.isNull(errorsType) || CollectionUtils.isEmpty(errorsType.getError())) {
            return List.of();
        }

        List<Error> errors = new ArrayList<>();
        for (ErrorType error : errorsType.getError()) {
            log.warn("VIS Business Error code = {}, message = {}", error.getCode(), error.getMessage());
            ctx.addError(error.getCode(), "VIS Returned an error: " + error.getCode() + ":" + error.getMessage());
            errors.add(new Error(error.getCode(), error.getMessage()));
        }
        return errors;
    }

    private VisCheckResult technicalError(ProxyContext ctx, String code, String message) {
        ctx.addError(code, "Error when executing VIS external SOAP call vis(): " + message);
        return new VisCheckResult(errorResponse(code, message), List.of(new Error(code, message)));
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
