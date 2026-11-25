package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Leaga un mapper existent (BKRCKIMapper, DNBMapper, ...) de agregarea pe liste, fara sa-l rescrie.
 * Fiecare rezultat primit e transformat in dto-ul pe care mapperul il primeste deja (CheckCkiPersonDto, ...)
 * si mapperul e apelat neschimbat, o data pentru fiecare rezultat.
 *
 * Cand nu exista niciun rezultat, mapperul e apelat o data cu null, exact ca pana acum, cand fetcher-ul
 * intorcea null. Asa se pastreaza si ramurile lui de "fara date".
 *
 * @param <D> dto-ul tipizat al mapperului
 */
public class DtoBackedResultMapper<D> implements ProxyResultMapper {

    private final ProxySource source;
    private final Function<ProxyResultDto, D> toDto;
    private final BiConsumer<D, Application> mapper;

    public DtoBackedResultMapper(ProxySource source, Function<ProxyResultDto, D> toDto, BiConsumer<D, Application> mapper) {
        this.source = source;
        this.toDto = toDto;
        this.mapper = mapper;
    }

    @Override
    public ProxySource source() {
        return source;
    }

    @Override
    public void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx) {
        if (results.isEmpty()) {
            mapper.accept(null, application);
            return;
        }
        for (ProxyResultDto result : results) {
            mapper.accept(toDto.apply(result), application);
        }
    }
}
