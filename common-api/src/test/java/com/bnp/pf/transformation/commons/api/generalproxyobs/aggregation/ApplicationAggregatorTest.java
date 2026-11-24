package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.model.Application;

class ApplicationAggregatorTest {

    private final List<String> applied = new ArrayList<>();
    private final Set<ProxySource> failingFetch = EnumSet.noneOf(ProxySource.class);

    private final ProxyResultsFetcher fetcher = new ProxyResultsFetcher(null, new ProxySourceProperties()) {
        @Override
        public List<ProxyResultDto> fetch(ProxyContext ctx, ProxySource source) {
            if (failingFetch.contains(source)) {
                ctx.addError("503", source + " down");
                return List.of();
            }
            return List.of(new ProxyResultDto(), new ProxyResultDto());
        }
    };

    private final ApplicationAggregator aggregator = new ApplicationAggregator(fetcher,
            List.of(mapper(ProxySource.SCAL_PERSON, false), mapper(ProxySource.CKI, false),
                    mapper(ProxySource.VIS, true), mapper(ProxySource.RAT_COMPLIANCE, false)));

    @Test
    void allSourcesOk_appliedInTheGivenOrder() {
        ProxyContext ctx = context();

        assertTrue(aggregator.enrich(ctx, List.of(ProxySource.CKI, ProxySource.SCAL_PERSON)));

        assertEquals(List.of("CKI:2", "SCAL_PERSON:2"), applied);
    }

    @Test
    void failedFetch_isNotMappedButTheOtherSourcesStillRun() {
        failingFetch.add(ProxySource.CKI);
        ProxyContext ctx = context();

        assertFalse(aggregator.enrich(ctx, List.of(ProxySource.CKI, ProxySource.SCAL_PERSON)));

        assertEquals(List.of("SCAL_PERSON:2"), applied);
        assertEquals(1, ctx.getErrors().size());
    }

    @Test
    void mapperException_becomesAnError() {
        ProxyContext ctx = context();

        assertFalse(aggregator.enrich(ctx, List.of(ProxySource.VIS)));

        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("Unable to map VIS"));
    }

    @Test
    void sourceWithoutMapper_isAnError() {
        ProxyContext ctx = context();

        assertFalse(aggregator.enrich(ctx, List.of(ProxySource.DNB)));

        assertTrue(ctx.getErrors().get(0).getErrorMessage().contains("No mapper registered for DNB"));
    }

    @Test
    void skipCommand_removesTheSource() {
        ProxyContext ctx = context();
        ctx.getApplication().setSkipCommand(List.of("rat.compliance"));

        assertTrue(aggregator.enrich(ctx, List.of(ProxySource.RAT_COMPLIANCE, ProxySource.CKI)));

        assertEquals(List.of("CKI:2"), applied);
    }

    private ProxyResultMapper mapper(ProxySource source, boolean failing) {
        return new ProxyResultMapper() {
            @Override
            public ProxySource source() {
                return source;
            }

            @Override
            public void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx) {
                if (failing) {
                    throw new IllegalStateException("boom");
                }
                applied.add(source + ":" + results.size());
            }
        };
    }

    private static ProxyContext context() {
        Signal signal = new Signal();
        signal.setFlowId("F1");
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        ProxyContext ctx = ProxyContext.of(message, "corr", "sedproxy");
        Application application = new Application();
        application.setBusinessKey("BK1");
        ctx.setApplication(application);
        return ctx;
    }
}
