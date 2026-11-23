package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.APPLICATION;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.CORRELATION_ID_NOT_PROVIDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import com.bnp.pf.transformation.avro.ArvalOrchestrationMessage;
import com.bnp.pf.transformation.avro.ArvalSignal;
import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;

class ProxyContextValidatorTest {

    private static final String PROXY = "visproxy";
    private static final String VALID_APP = "{\"businessKey\":\"BK1\"}";

    private final ProxyContextValidator validator = new ProxyContextValidator(new Gson());

    @Test
    void validMessage_passesAndResolvesApplication() {
        ProxyContext ctx = ProxyContext.of(message("F1", VALID_APP), "corr", PROXY);

        assertTrue(validator.validate(ctx));
        assertFalse(ctx.hasErrors());
        assertEquals("BK1", ctx.getBusinessKey());
        assertEquals("F1", ctx.getFlowId());
    }

    @Test
    void nullCorrelation_failsFirst() {
        assertSingleError(ProxyContext.of(message("F1", VALID_APP), null, PROXY), CORRELATION_ID_NOT_PROVIDED);
    }

    @Test
    void emptyCorrelation_fails() {
        assertSingleError(ProxyContext.of(message("F1", VALID_APP), "", PROXY), CORRELATION_ID_NOT_PROVIDED);
    }

    @Test
    void nullMessage_fails() {
        assertSingleError(ProxyContext.of((OrchestrationMessage) null, "corr", PROXY),
                ProxyContextValidator.ORCH_MSG_EMPTY);
    }

    @Test
    void nullData_fails() {
        assertSingleError(ProxyContext.of(new OrchestrationMessage(), "corr", PROXY),
                ProxyContextValidator.ORCH_MSG_EMPTY);
    }

    @Test
    void missingApplicationProperty_fails() {
        assertSingleError(ProxyContext.of(message("F1", null), "corr", PROXY),
                APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
    }

    @Test
    void nullProperties_fails() {
        OrchestrationMessage message = message("F1", VALID_APP);
        message.getData().setProperties(null);

        assertSingleError(ProxyContext.of(message, "corr", PROXY), APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
    }

    @Test
    void jsonNullApplication_fails() {
        assertSingleError(ProxyContext.of(message("F1", "null"), "corr", PROXY),
                APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
    }

    @Test
    void malformedApplicationJson_fails() {
        assertSingleError(ProxyContext.of(message("F1", "{not json"), "corr", PROXY),
                APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
    }

    @Test
    void missingBusinessKey_fails() {
        assertSingleError(ProxyContext.of(message("F1", "{}"), "corr", PROXY),
                ProxyContextValidator.BUSS_KEY_EMPTY);
    }

    @Test
    void emptyBusinessKey_fails() {
        assertSingleError(ProxyContext.of(message("F1", "{\"businessKey\":\"\"}"), "corr", PROXY),
                ProxyContextValidator.BUSS_KEY_EMPTY);
    }

    @Test
    void nullFlowId_fails() {
        assertSingleError(ProxyContext.of(message(null, VALID_APP), "corr", PROXY),
                ProxyContextValidator.FLOW_ID_EMPTY);
    }

    @Test
    void emptyFlowId_fails() {
        assertSingleError(ProxyContext.of(message("", VALID_APP), "corr", PROXY),
                ProxyContextValidator.FLOW_ID_EMPTY);
    }

    @Test
    void everythingMissing_reportsOnlyTheFirstProblem() {
        assertSingleError(ProxyContext.of(message(null, null), null, PROXY), CORRELATION_ID_NOT_PROVIDED);
    }

    @Test
    void stringKeysInProperties_areFoundToo() {
        OrchestrationMessage message = message("F1", null);
        Map<CharSequence, CharSequence> properties = new HashMap<>();
        properties.put(APPLICATION, VALID_APP);
        message.getData().setProperties(properties);

        assertTrue(validator.validate(ProxyContext.of(message, "corr", PROXY)));
    }

    @Test
    void arvalMessage_usesTheSameValidation() {
        ArvalSignal signal = new ArvalSignal();
        signal.setFlowId("ARVAL-1");
        Map<CharSequence, CharSequence> properties = new HashMap<>();
        properties.put(new Utf8(APPLICATION), new Utf8(VALID_APP));
        signal.setProperties(properties);
        ArvalOrchestrationMessage message = new ArvalOrchestrationMessage();
        message.setData(signal);

        ProxyContext ctx = ProxyContext.of(message, "corr", PROXY);

        assertTrue(validator.validate(ctx));
        assertEquals("BK1", ctx.getBusinessKey());
        assertEquals("ARVAL-1", ctx.getFlowId());
    }

    private void assertSingleError(ProxyContext ctx, String expectedMessage) {
        assertFalse(validator.validate(ctx));
        assertEquals(1, ctx.getErrors().size());
        assertEquals(expectedMessage, ctx.getErrors().get(0).getErrorMessage());
    }

    private static OrchestrationMessage message(String flowId, String applicationJson) {
        Signal signal = new Signal();
        signal.setFlowId(flowId);
        Map<CharSequence, CharSequence> properties = new HashMap<>();
        if (applicationJson != null) {
            properties.put(new Utf8(APPLICATION), new Utf8(applicationJson));
        }
        signal.setProperties(properties);
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        return message;
    }
}
