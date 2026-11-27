package com.bnp.pf.transformation.rvdrproxy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;

import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.rvdrproxy.model.CreditCheckResult;

// TODO: importurile pentru clasele generate din WSDL (CreditCheckPort, CreditCheckRequest, CreditCheckResponse,
// ErrorType, ErrorsType, ConnectException_Exception) lipsesc - pastreaza-le pe cele din testul actual.
import com.bnp.pf.transformation.rvdrproxy.builder.ContextBuilder;

/**
 * Dupa modelul din vis: in loc de CollectError + errorMap, erorile se citesc din ProxyContext. Textul din context e cel care
 * ajungea pe topicul de error (errorMap); CreditCheckResult pastreaza codul si mesajul brut, ca inainte.
 */
@ExtendWith(MockitoExtension.class)
class SoapClientTest {
    private static final String TECHNICAL_PREFIX = "Error when executing external SOAP call creditCheckRequest(): ";

    @Mock
    private CreditCheckPort creditCheckPort;

    @InjectMocks
    private SoapClient soapClient;

    private ProxyContext ctx;
    private CreditCheckRequest request;

    @BeforeEach
    void setUp() {
        ctx = ContextBuilder.context(ContextBuilder.application("BK-001"));
        request = new CreditCheckRequest();
    }

    private static ErrorType errorType(String code, String message) {
        ErrorType errorType = new ErrorType();
        errorType.setCode(code);
        errorType.setMessage(message);

        return errorType;
    }

    /** Mesajul brut al singurei erori tehnice; verifica si ca in context a ajuns cu prefixul de pe topic. */
    private String technicalErrorMessage(CreditCheckResult result, String expectedCode) {
        assertThat(result.getErrors()).hasSize(1);
        String message = result.getErrors().get(0).getErrorMessage();
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple(expectedCode, TECHNICAL_PREFIX + message));
        return message;
    }

    @Test
    void creditCheckRequest_WhenResponseHasNoErrors_shouldReturnItAndRegisterNothing() throws ConnectException_Exception {
        CreditCheckResponse response = new CreditCheckResponse();
        when(creditCheckPort.creditCheck(request)).thenReturn(response);

        CreditCheckResult result = soapClient.creditCheckRequest(ctx, request);

        assertThat(ctx.getErrors()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getResponse()).isSameAs(response);
    }

    @Test
    void creditCheckRequest_WhenResponseHasBusinessErrors_shouldRegisterEachError() throws ConnectException_Exception {
        ErrorsType errors = new ErrorsType();
        errors.getError().add(errorType("E1", "Invalid client"));
        errors.getError().add(errorType("E2", "Limit exceeded"));

        CreditCheckResponse response = new CreditCheckResponse();
        response.setErrors(errors);
        when(creditCheckPort.creditCheck(request)).thenReturn(response);

        CreditCheckResult result = soapClient.creditCheckRequest(ctx, request);

        assertThat(result.getResponse()).isSameAs(response);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(
                        tuple("E1", "CreditCheck returned an error: E1:Invalid client"),
                        tuple("E2", "CreditCheck returned an error: E2:Limit exceeded"));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("E1", "Invalid client"), new Error("E2", "Limit exceeded"));
    }

    @Test
    void creditCheckRequest_whenSoapFault_shouldReturn502AndRegisterError() throws ConnectException_Exception {
        SOAPFault fault = mock(SOAPFault.class);
        when(fault.getFaultString()).thenReturn("Service unavailable");
        SOAPFaultException soapFaultException = new SOAPFaultException(fault);
        when(creditCheckPort.creditCheck(request)).thenThrow(soapFaultException);

        CreditCheckResult result = soapClient.creditCheckRequest(ctx, request);

        String message = technicalErrorMessage(result, "502");
        assertThat(message).contains("Service unavailable");
        assertThat(result.getResponse().getErrors().getError())
                .extracting(ErrorType::getCode, ErrorType::getMessage)
                .containsExactly(tuple("502", message));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("502", message));
    }

    @Test
    void creditCheckRequest_whenUnexpectedException_shouldReturn555AndRegisterError() throws ConnectException_Exception {
        when(creditCheckPort.creditCheck(request)).thenThrow(new RuntimeException("Read timed out"));
        CreditCheckResult result = soapClient.creditCheckRequest(ctx, request);

        String message = technicalErrorMessage(result, "555");
        assertThat(message).contains("Read timed out");
        assertThat(result.getResponse().getErrors().getError())
                .extracting(ErrorType::getCode, ErrorType::getMessage)
                .containsExactly(tuple("555", message));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("555", message));
    }

    @Test
    void creditCheckRequest_whenResponseIsNull_shouldReturn555AndRegisterError() throws ConnectException_Exception {
        when(creditCheckPort.creditCheck(request)).thenReturn(null);
        CreditCheckResult result = soapClient.creditCheckRequest(ctx, request);

        String message = technicalErrorMessage(result, "555");
        assertThat(message).isEqualTo("Empty response from CreditCheck");
        assertThat(result.getResponse().getErrors().getError())
                .extracting(ErrorType::getCode, ErrorType::getMessage)
                .containsExactly(tuple("555", message));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("555", message));
    }
}
