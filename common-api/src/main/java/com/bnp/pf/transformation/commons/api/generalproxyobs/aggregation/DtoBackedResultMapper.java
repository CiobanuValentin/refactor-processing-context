package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Leaga un mapper existent (DNBMapper, SCALMapper, ...) de agregare, fara sa-l modifice.
 *
 * In legacy mapperele primeau lista cu toate randurile sursei si le parcurgeau intr-o bucla; in cloud primesc
 * un singur dto. Clasa aceasta reface bucla: ProxyDtoBuilder construieste dto-ul tipizat al fiecarui rand si
 * mapperul din cloud e apelat pentru fiecare.
 *
 * Fara niciun rand, legacy nu facea nimic - cu o exceptie, VIS, care punea rezultate implicite. De aceea
 * mapperul e apelat cu null doar daca i se cere explicit (callWithNullWhenEmpty).
 *
 * @param <D> dto-ul tipizat al mapperului (CheckDnbCompanyDto, ...)
 */
public class DtoBackedResultMapper<D> implements ProxyResultMapper {

    private final ProxySource source;
    private final Function<GeneralProxyObsDto, D> toDto;
    private final BiConsumer<D, Application> mapper;
    private final boolean callWithNullWhenEmpty;

    public DtoBackedResultMapper(ProxySource source, Function<GeneralProxyObsDto, D> toDto,
                                 BiConsumer<D, Application> mapper) {
        this(source, toDto, mapper, false);
    }

    public DtoBackedResultMapper(ProxySource source, Function<GeneralProxyObsDto, D> toDto,
                                 BiConsumer<D, Application> mapper, boolean callWithNullWhenEmpty) {
        this.source = source;
        this.toDto = toDto;
        this.mapper = mapper;
        this.callWithNullWhenEmpty = callWithNullWhenEmpty;
    }

    @Override
    public ProxySource source() {
        return source;
    }

    @Override
    public void apply(List<GeneralProxyObsDto> results, Application application, ProxyContext ctx) {
        if (results.isEmpty()) {
            if (callWithNullWhenEmpty) {
                mapper.accept(null, application);
            }
            return;
        }
        for (GeneralProxyObsDto result : results) {
            mapper.accept(toDto.apply(result), application);
        }
    }
}
