package com.bnp.pf.transformation.rvdrproxy.controller;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.APPLICATION;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.CORRELATION_ID_NOT_PROVIDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyContextValidator;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyResultPublisher;
import com.bnp.pf.transformation.orchestrator.avro.OrchestrationMessage;
import com.bnp.pf.transformation.rvdrproxy.service.IntegrityCheckService;

import com.bnp.pf.transformation.rvdrproxy.builder.ContextBuilder;

/**
 * Rescris pentru fluxul nou: ProxyContext.of -> ProxyContextValidator -> IntegrityCheckService -> status in DB -> publish.
 * Validatorul e cel real (spy); publicarea propriu-zisa (ce se trimite pe status / error) e testata in common-api
 * (ProxyResultPublisherTest), aici se verifica doar ca listenerul o cere, o singura data, cu contextul complet.
 */
@ExtendWith(MockitoExtension.class)
class ProxyListenerTest {

    private static final String STATUS_TOPIC = "status-topic";
    private static final String ERROR_TOPIC = "error-topic";
    private static final String VERSION = "1.0.0";
    private static final String PROXY_NAME = ContextBuilder.PROXY_NAME;
    private static final String CORRELATION = ContextBuilder.CORRELATION;
    private static final String BUSINESS_KEY = "BK-001";
    private static final String FLOW_ID = ContextBuilder.FLOW_ID;
    private static final String OK = "OK";
    private static final String KO = "KO";

    @Spy
    private ProxyContextValidator proxyContextValidator = new ProxyContextValidator();
    @Mock
    private IntegrityCheckService integrityCheckService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private ProxyResultPublisher proxyResultPublisher;

    @InjectMocks
    private ProxyListener proxyListener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(proxyListener, "statusTopic", STATUS_TOPIC);
        ReflectionTestUtils.setField(proxyListener, "errorTopic", ERROR_TOPIC);
        ReflectionTestUtils.setField(proxyListener, "version", VERSION);
        ReflectionTestUtils.setField(proxyListener, "proxyName", PROXY_NAME);
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================
    private static OrchestrationMessage message(String businessKey, String flowId) {
        String applicationJson = "{\"businessKey\":\"" + businessKey + "\"}";
        return ContextBuilder.message(flowId, Map.of(APPLICATION, applicationJson));
    }

    private void consume(OrchestrationMessage message, String correlation) {
        proxyListener.consume(message, correlation, "granting-topic", 0, 42L);
    }

    /** Contextul trimis la publicare; verifica si ca publish e cerut o singura data, cu topicurile si versiunea. */
    private ProxyContext published() {
        ArgumentCaptor<ProxyContext> ctx = ArgumentCaptor.forClass(ProxyContext.class);
        verify(proxyResultPublisher).publish(ctx.capture(), eq(STATUS_TOPIC), eq(ERROR_TOPIC), eq(VERSION));
        return ctx.getValue();
    }


    // =====================================================================================
    // Tests
    // =====================================================================================
    @Test
    void consume_whenNoErrors_shouldSetStatusOkAndPublish() {
        consume(message(BUSINESS_KEY, FLOW_ID), CORRELATION);

        ProxyContext ctx = published();
        InOrder inOrder = inOrder(proxyContextValidator, integrityCheckService, applicationService, proxyResultPublisher);
        inOrder.verify(proxyContextValidator).validate(ctx);
        inOrder.verify(integrityCheckService).loanCheckProcess(ctx);
        inOrder.verify(applicationService).updateApplicationStatus(BUSINESS_KEY, OK);
        inOrder.verify(proxyResultPublisher).publish(ctx, STATUS_TOPIC, ERROR_TOPIC, VERSION);

        assertThat(ctx.getErrors()).isEmpty();
        assertThat(ctx.getCorrelation()).isEqualTo(CORRELATION);
        assertThat(ctx.getProxyName()).isEqualTo(PROXY_NAME);
        assertThat(ctx.getFlowId()).isEqualTo(FLOW_ID);
        assertThat(ctx.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    }

    @Test
    void consume_whenIntegrityCheckRegistersErrors_shouldSetStatusKoAndPublish() {
        doAnswer(invocation -> {
            ProxyContext ctx = invocation.getArgument(0);
            ctx.addError("502", "SOAP Call Error");
            return null;
        }).when(integrityCheckService).loanCheckProcess(any());

        consume(message(BUSINESS_KEY, FLOW_ID), CORRELATION);

        ProxyContext ctx = published();
        verify(applicationService).updateApplicationStatus(BUSINESS_KEY, KO);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("502", "SOAP Call Error"));
    }

    @Test
    void consume_whenCorrelationMissing_shouldPublishErrorWithoutProcessing() {
        consume(message(BUSINESS_KEY, FLOW_ID), null);

        ProxyContext ctx = published();
        verifyNoInteractions(integrityCheckService, applicationService);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorMessage)
                .containsExactly(CORRELATION_ID_NOT_PROVIDED);
    }

    @Test
    void consume_whenBusinessKeyMissing_shouldPublishKoWithoutCheckingCredit() {
        consume(message("", FLOW_ID), CORRELATION);

        ProxyContext ctx = published();
        verifyNoInteractions(integrityCheckService, applicationService);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("400", ProxyContextValidator.BUSS_KEY_EMPTY));
    }

    @Test
    void consume_whenFlowIdMissing_shouldPublishKoWithoutCheckingCredit() {
        consume(message(BUSINESS_KEY, null), CORRELATION);

        ProxyContext ctx = published();
        verifyNoInteractions(integrityCheckService, applicationService);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("400", ProxyContextValidator.FLOW_ID_EMPTY));
    }

    @Test
    void consume_whenIntegrityCheckThrows_shouldPublishKoWithErrorInsteadOfThrowing() {
        doThrow(new RuntimeException("Error Occurred")).when(integrityCheckService).loanCheckProcess(any());

        consume(message(BUSINESS_KEY, FLOW_ID), CORRELATION);

        ProxyContext ctx = published();
        verifyNoInteractions(applicationService);
        assertThat(ctx.getErrors()).hasSize(1);
        assertThat(ctx.getErrors().get(0).getErrorCode()).isEqualTo("500");
        assertThat(ctx.getErrors().get(0).getErrorMessage()).contains("Error Occurred", BUSINESS_KEY, FLOW_ID);
    }

    @Test
    void consume_whenPublishFails_shouldLetTheExceptionReachTheKafkaErrorHandler() {
        RuntimeException cause = new RuntimeException("Kafka down");
        doThrow(cause).when(proxyResultPublisher).publish(any(), any(), any(), any());

        assertThatThrownBy(() -> consume(message(BUSINESS_KEY, FLOW_ID), CORRELATION)).isSameAs(cause);
        verify(applicationService).updateApplicationStatus(BUSINESS_KEY, OK);
    }
}
