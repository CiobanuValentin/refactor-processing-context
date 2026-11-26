package visproxy.client;

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
import com.bnp.pf.transformation.visproxy.client.VisSoapClient;
import com.bnp.pf.transformation.visproxy.model.VisCheckResult;

import urn.bkr.wsdl.vis.ErrorType;
import urn.bkr.wsdl.vis.ErrorsType;
import urn.bkr.wsdl.vis.VISPort;
import urn.bkr.wsdl.vis.VisRequest;
import urn.bkr.wsdl.vis.VisResponse;
import visproxy.builder.ContextBuilder;

/**
 * Rescris: in loc de CollectError + errorMap, erorile se citesc din ProxyContext. Textul din context e cel care
 * ajungea pe topicul de error (errorMap); VisCheckResult pastreaza codul si mesajul brut, ca inainte.
 */
@ExtendWith(MockitoExtension.class)
class VisSoapClientTest {
    private static final String TECHNICAL_PREFIX = "Error when executing VIS external SOAP call vis(): ";

    @Mock
    private VISPort visPort;

    @InjectMocks
    private VisSoapClient visSoapClient;

    private ProxyContext ctx;
    private VisRequest request;

    @BeforeEach
    void setUp() {
        ctx = ContextBuilder.context(ContextBuilder.application("BK-001"));
        request = new VisRequest();
    }

    private static ErrorType errorType(String code, String message) {
        ErrorType errorType = new ErrorType();
        errorType.setCode(code);
        errorType.setMessage(message);

        return errorType;
    }

    /** Mesajul brut al singurei erori tehnice; verifica si ca in context a ajuns cu prefixul de pe topic. */
    private String technicalErrorMessage(VisCheckResult result, String expectedCode) {
        assertThat(result.getErrors()).hasSize(1);
        String message = result.getErrors().get(0).getErrorMessage();
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple(expectedCode, TECHNICAL_PREFIX + message));
        return message;
    }

    @Test
    void visCheckRequest_WhenResponseHasNoErrors_shouldReturnItAndRegisterNothing() {
        VisResponse response = new VisResponse();
        when(visPort.vis(request)).thenReturn(response);

        VisCheckResult result = visSoapClient.visCheck(ctx, request);

        assertThat(ctx.getErrors()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getResponse()).isSameAs(response);
    }

    @Test
    void visCheckRequest_WhenResponseHasBusinessErrors_shouldRegisterEachError() {
        ErrorsType errors = new ErrorsType();
        errors.getError().add(errorType("E1", "Invalid client"));
        errors.getError().add(errorType("E2", "Limit exceeded"));

        VisResponse response = new VisResponse();
        response.setErrors(errors);
        when(visPort.vis(request)).thenReturn(response);

        VisCheckResult result = visSoapClient.visCheck(ctx, request);

        assertThat(result.getResponse()).isSameAs(response);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(
                        tuple("E1", "VIS Returned an error: E1:Invalid client"),
                        tuple("E2", "VIS Returned an error: E2:Limit exceeded"));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("E1", "Invalid client"), new Error("E2", "Limit exceeded"));
    }

    @Test
    void visCheckRequest_whenSoapFault_shouldReturn502AndRegisterError() {
        SOAPFault fault = mock(SOAPFault.class);
        when(fault.getFaultString()).thenReturn("Service unavailable");
        SOAPFaultException soapFaultException = new SOAPFaultException(fault);
        when(visPort.vis(request)).thenThrow(soapFaultException);

        VisCheckResult result = visSoapClient.visCheck(ctx, request);

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
    void visCheckRequest_whenUnexpectedException_shouldReturn555AndRegisterError() {
        when(visPort.vis(request)).thenThrow(new RuntimeException("Read timed out"));
        VisCheckResult result = visSoapClient.visCheck(ctx, request);

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
    void visCheckRequest_whenResponseIsNull_shouldReturn555AndRegisterError() {
        when(visPort.vis(request)).thenReturn(null);
        VisCheckResult result = visSoapClient.visCheck(ctx, request);

        String message = technicalErrorMessage(result, "555");
        assertThat(message).isEqualTo("Empty response received from VIS external SOAP call");
        assertThat(result.getResponse().getErrors().getError())
                .extracting(ErrorType::getCode, ErrorType::getMessage)
                .containsExactly(tuple("555", message));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("555", message));
    }
}
