package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.ERROR_MESSAGE;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.OK;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.model.Application;

class ProxyResultPublisherTest {

    @Test
    void noErrors_statusOkAndNoErrorSignal() {
        ProxyContext ctx = context("corr");

        assertEquals(Map.of(STATUS, OK), ProxyResultPublisher.statusProperties(ctx));
        assertNull(ProxyResultPublisher.errorProperties(ctx));
    }

    @Test
    void errors_statusKoAndNumberedMessagesInOrder() {
        ProxyContext ctx = context("corr");
        ctx.addError("400", "first");
        ctx.addError("500", "second");

        assertEquals(Map.of(STATUS, KO), ProxyResultPublisher.statusProperties(ctx));
        Map<CharSequence, CharSequence> errors = ProxyResultPublisher.errorProperties(ctx);
        assertEquals(List.of(ERROR_MESSAGE + "1", ERROR_MESSAGE + "2"), new ArrayList<>(errors.keySet()));
        assertEquals("first", errors.get(ERROR_MESSAGE + "1"));
        assertEquals("second", errors.get(ERROR_MESSAGE + "2"));
    }

    @Test
    void missingCorrelation_noStatusOnlyError() {
        ProxyContext ctx = context(null);
        ctx.addError("400", "no correlation");

        assertNull(ProxyResultPublisher.statusProperties(ctx));
        assertEquals(1, ProxyResultPublisher.errorProperties(ctx).size());
    }

    @Test
    void signal_carriesKeysFromContext() {
        ProxyContext ctx = context("corr");
        Application application = new Application();
        application.setBusinessKey("BK1");
        ctx.setApplication(application);

        Signal signal = ProxyResultPublisher.signal(ctx, Map.of(STATUS, OK));

        assertEquals("visproxy", signal.getCommand().toString());
        assertEquals("BK1", signal.getBusinessKey().toString());
        assertEquals("F1", signal.getFlowId().toString());
        assertEquals(Map.of(STATUS, OK), signal.getProperties());
    }

    @Test
    void signal_usesEmptyKeysWhenNothingWasResolved() {
        ProxyContext ctx = ProxyContext.of((OrchestrationMessage) null, "corr", "visproxy");

        Signal signal = ProxyResultPublisher.signal(ctx, Map.of(STATUS, KO));

        assertEquals("", signal.getBusinessKey().toString());
        assertEquals("", signal.getFlowId().toString());
    }

    private static ProxyContext context(String correlation) {
        Signal signal = new Signal();
        signal.setFlowId("F1");
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        return ProxyContext.of(message, correlation, "visproxy");
    }
}
