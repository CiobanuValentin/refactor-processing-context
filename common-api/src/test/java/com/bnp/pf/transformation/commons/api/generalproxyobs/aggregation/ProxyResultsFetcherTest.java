package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;

class ProxyResultsFetcherTest {

    private final ProxyResultsFetcher fetcher = new ProxyResultsFetcher(null, new ProxySourceProperties());

    @Test
    void okWithSeveralResults_returnsAllOfThem() {
        ProxyContext ctx = context();
        String body = "{\"businessKey\":\"BK1\",\"flowId\":\"F1\",\"results\":["
                + "{\"partyBusinessKey\":\"P1\",\"requestJson\":\"{}\",\"responseJson\":\"{\\\"a\\\":1}\"},"
                + "{\"partyBusinessKey\":\"P2\"}]}";

        List<ProxyResultDto> results = fetcher.read(ctx, ProxySource.CKI, 200, body);

        assertEquals(2, results.size());
        assertEquals("P1", results.get(0).getPartyBusinessKey());
        assertEquals("{\"a\":1}", results.get(0).getResponseJson());
        assertEquals("P2", results.get(1).getPartyBusinessKey());
        assertFalse(ctx.hasErrors());
    }

    @Test
    void okWithEmptyBodyOrNoResults_isNotAnError() {
        ProxyContext ctx = context();

        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 204, null).isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "{\"results\":null}").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "{\"results\":[null]}").isEmpty());
        assertFalse(ctx.hasErrors());
    }

    @Test
    void errorStatusWithEmptyBody_isAnError() {
        ProxyContext ctx = context();

        assertTrue(fetcher.read(ctx, ProxySource.VIS, 500, "").isEmpty());

        assertEquals(1, ctx.getErrors().size());
        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("VIS"));
        assertTrue(ctx.getErrors().get(0).getErrorMessage().endsWith("HTTP 500"));
    }

    @Test
    void errorStatusWithBody_keepsTheBodyInTheMessage() {
        ProxyContext ctx = context();

        fetcher.read(ctx, ProxySource.VIS, 404, "{\"results\":[{}]}");

        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("HTTP 404: {\"results\""));
    }

    @Test
    void invalidJson_isAnError() {
        ProxyContext ctx = context();

        assertTrue(fetcher.read(ctx, ProxySource.RVDR, 200, "<html>").isEmpty());

        assertEquals(1, ctx.getErrors().size());
    }

    @Test
    void missingUrl_isAnErrorWithoutAnyCall() {
        ProxyContext ctx = context();

        assertTrue(fetcher.fetch(ctx, ProxySource.DNB).isEmpty());

        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("no URL configured"));
    }

    private static ProxyContext context() {
        Signal signal = new Signal();
        signal.setFlowId("F1");
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        return ProxyContext.of(message, "corr", "sedproxy");
    }
}
