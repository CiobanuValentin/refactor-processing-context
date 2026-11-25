package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Imbogateste aplicatia din context cu rezultatele celorlalte proxy-uri. Inlocuieste ApplicationBuilderPG
 * din fiecare proxy: proxy-ul spune doar ce surse ii trebuie, in ordinea in care trebuie aplicate.
 */
@Slf4j
@Component
public class ApplicationAggregator {

    private static final String INTERNAL_ERROR = "500";

    private final ProxyResultsFetcher fetcher;
    private final Map<ProxySource, ProxyResultMapper> mappers = new EnumMap<>(ProxySource.class);

    public ApplicationAggregator(ProxyResultsFetcher fetcher, List<ProxyResultMapper> mappers) {
        this.fetcher = fetcher;
        mappers.forEach(mapper -> this.mappers.put(mapper.source(), mapper));
    }

    /**
     * Trece prin toate sursele chiar daca una esueaza, ca raspunsul sa contina toate erorile.
     *
     * @return true daca toate sursele au fost citite si aplicate fara erori
     */
    public boolean enrich(ProxyContext ctx, Collection<ProxySource> sources) {
        Application application = ctx.getApplication();
        for (ProxySource source : sources) {
            ProxyResultMapper mapper = mappers.get(source);
            if (mapper == null) {
                ctx.addError(INTERNAL_ERROR, "No mapper registered for " + source);
                continue;
            }
            int errorsBefore = ctx.getErrors().size();
            List<GeneralProxyObsDto> results = fetcher.fetch(ctx, source);
            if (ctx.getErrors().size() == errorsBefore) {
                apply(mapper, results, application, ctx);
            }
        }
        return !ctx.hasErrors();
    }

    private void apply(ProxyResultMapper mapper, List<GeneralProxyObsDto> results, Application application, ProxyContext ctx) {
        try {
            mapper.apply(results, application, ctx);
        } catch (RuntimeException e) {
            String message = "Unable to map " + mapper.source() + " results for business key " + ctx.getBusinessKey()
                    + " and flow ID " + ctx.getFlowId() + ": " + e;
            log.error(message, e);
            ctx.addError(INTERNAL_ERROR, message);
        }
    }
}
