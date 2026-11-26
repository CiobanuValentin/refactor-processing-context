package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Leaga un mapper existent (DNBMapper, SCALMapper, ...) de agregare.
 *
 * Ca in legacy: ProxyDtoBuilder construieste dto-ul tipizat al fiecarui rand, iar mapperul primeste lista cu toate
 * randurile sursei, o singura data. Fara niciun rand, mapperul primeste o lista goala si decide singur ce face
 * (VIS pune rezultate implicite, CKI pune cele doua rezultate goale pe loan, restul nu fac nimic).
 *
 * @param <D> dto-ul tipizat al mapperului (CheckDnbCompanyDto, ...)
 */
public class DtoBackedResultMapper<D> implements ProxyResultMapper {

    private final ProxySource source;
    private final Function<GeneralProxyObsDto, D> toDto;
    private final BiConsumer<List<D>, Application> mapper;

    public DtoBackedResultMapper(ProxySource source, Function<GeneralProxyObsDto, D> toDto,
                                 BiConsumer<List<D>, Application> mapper) {
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
        mapper.accept(results.stream().map(toDto).toList(), application);
    }
}
