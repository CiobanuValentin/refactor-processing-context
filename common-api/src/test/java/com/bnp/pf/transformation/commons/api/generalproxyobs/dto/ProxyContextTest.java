package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.ARVAL_NL_FLOW_ID_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.avro.Metadata;
import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;

class ProxyContextTest {

    @Test
    void arvalNlFlow_isRecognisedByPrefix() {
        assertTrue(context(ARVAL_NL_FLOW_ID_PREFIX + "123", null).isArvalNlFlow());
        assertFalse(context("123", null).isArvalNlFlow());
        assertFalse(context(null, null).isArvalNlFlow());
    }

    @Test
    void metadata_givesProducerAndEventTime() {
        Metadata metadata = new Metadata();
        metadata.setProducer("granting-api");
        metadata.setEventTimeIso8601("2026-01-01T10:00:00");

        ProxyContext ctx = context("F1", metadata);

        assertEquals("granting-api", ctx.getProducer());
        assertEquals("2026-01-01T10:00", ctx.getEventTime().toString());
    }

    @Test
    void unreadableEventTime_fallsBackToNow() {
        Metadata metadata = new Metadata();
        metadata.setEventTimeIso8601("not a date");

        assertNotNull(context("F1", metadata).getEventTime());
    }

    @Test
    void noMetadata_noProducerButStillAnEventTime() {
        ProxyContext ctx = context("F1", null);

        assertNull(ctx.getProducer());
        assertNotNull(ctx.getEventTime());
    }

    private static ProxyContext context(String flowId, Metadata metadata) {
        Signal signal = new Signal();
        signal.setFlowId(flowId);
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        message.setMetadata(metadata);
        return ProxyContext.of(message, "corr", "sedproxy");
    }
}
