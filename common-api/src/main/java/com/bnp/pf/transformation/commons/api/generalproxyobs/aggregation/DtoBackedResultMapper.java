package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Leaga un mapper existent (BKRCKIMapper, DNBMapper, ...) de agregare, fara sa-l modifice. Face exact ce facea
 * ApplicationBuilderPG pentru o sursa: ProxyDtoBuilder construieste dto-ul tipizat, apoi se apeleaza mapperul.
 * Diferenta: daca sursa intoarce mai multe rezultate, mapperul e apelat pentru fiecare.
 *
 * Fara niciun rezultat, mapperul e apelat o data cu null.
 * TODO: de confirmat ca PartialDataFetcherPG intorcea null in acest caz.
 *
 * @param <D> dto-ul tipizat al mapperului (CheckCkiPersonDto, ...)
 */
public class DtoBackedResultMapper<D> implements ProxyResultMapper {

    private final ProxySource source;
    private final Function<GeneralProxyObsDto, D> toDto;
    private final BiConsumer<D, Application> mapper;

    public DtoBackedResultMapper(ProxySource source, Function<GeneralProxyObsDto, D> toDto,
                                 BiConsumer<D, Application> mapper) {
        this.source = source;
        this.toDto = toDto;
        this.mapper = mapper;
    }

    @Override
    public ProxySource source() {
        return source;
    }

    @Override
    public void apply(List<GeneralProxyObsDto> results, Application application, ProxyContext ctx) {
        if (results.isEmpty()) {
            mapper.accept(null, application);
            return;
        }
        for (GeneralProxyObsDto result : results) {
            mapper.accept(toDto.apply(result), application);
        }
    }
}
