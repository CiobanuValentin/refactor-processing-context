package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;

class ProxyResultsFetcherTest {

    private final ProxyResultsFetcher fetcher =
            new ProxyResultsFetcher(null, new ProxySourceProperties(), new ObjectMapper());

    @Test
    void okWithSeveralResults_returnsAllOfThem() {
        ProxyContext ctx = context();

        assertEquals(3, fetcher.read(ctx, ProxySource.CKI, 200, "[{},{},{}]").size());
        assertFalse(ctx.hasErrors());
    }

    @Test
    void okWithEmptyBodyOrEmptyList_isNotAnError() {
        ProxyContext ctx = context();

        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 204, null).isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "[]").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "{}").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "[null]").isEmpty());
        assertTrue(fetcher.read(ctx, ProxySource.CKI, 200, "null").isEmpty());
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

        fetcher.read(ctx, ProxySource.VIS, 404, "[{}]");

        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("HTTP 404: [{}]"));
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
