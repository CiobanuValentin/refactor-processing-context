package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKRCKIMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKREVAMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKRVISMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.DNBMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.EventIndicatorMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.RVDRMapper;
import com.bnp.pf.transformation.commons.api.applicationbuilder.utils.SCALMapper;
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.ProxyDtoBuilder;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Leaga fiecare sursa de mapperul ei. Inlocuieste corpul lui ApplicationBuilderPG.buildApplication.
 * Proxy-urile agregatoare (rat, sed) o importa in config: @Import(ProxyResultMapperConfig.class).
 *
 * In legacy fetcher-ul intorcea liste si fiecare mapper parcurgea lista intr-o bucla; in cloud mapperele primesc
 * un singur dto. Comparat cu diff intre common-api-1.23 si cloud:
 *  - SCAL x3, DNB, RVDR, EventIndicator, VIS, EVA person: tot ce fac se intampla in bucla, deci un apel al
 *    mapperului din cloud pentru fiecare rand e echivalent. Mapperele raman neschimbate.
 *  - CKI: adauga rezultatele de pe loan o singura data, dupa bucla, deci primeste lista
 *    (mapCKIData readus din legacy - vezi docs/APPLY-COMMON.md).
 *  - EVA company: primeste lista si in cloud.
 *
 * Fara niciun rand, legacy nu facea nimic, cu doua exceptii pastrate: VIS punea rezultate implicite (de aceea e
 * apelat cu null), iar CKI punea cele doua rezultate goale pe loan (o face mapCKIData cu lista goala).
 */
@Configuration
@RequiredArgsConstructor
public class ProxyResultMapperConfig {

    private final ProxyDtoBuilder dtoBuilder;

    @Bean
    ProxyResultMapper scalPersonResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_PERSON, dtoBuilder::buildCheckScalPersonDto, mapper::mapScalPersonData);
    }

    @Bean
    ProxyResultMapper scalPersonInitialResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_PERSON_INITIAL, dtoBuilder::buildCheckScalPersonInitialDto, mapper::mapScalPersonInitialData);
    }

    @Bean
    ProxyResultMapper scalEntityResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_ENTITY, dtoBuilder::buildCheckScalEntityDto, mapper::mapScalEntityData);
    }

    @Bean
    ProxyResultMapper dnbResultMapper(DNBMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.DNB, dtoBuilder::buildCheckDnbCompanyDto, mapper::mapDNBData);
    }

    @Bean
    ProxyResultMapper rvdrResultMapper(RVDRMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.RVDR, dtoBuilder::buildCheckRvdrCreditDto, mapper::mapRVDRData);
    }

    @Bean
    ProxyResultMapper visResultMapper(BKRVISMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.VIS, dtoBuilder::buildCheckVisDto, mapper::mapVISData, true);
    }

    @Bean
    ProxyResultMapper evaPersonResultMapper(BKREVAMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVA_PERSON, dtoBuilder::buildCheckEvaPersonDto, mapper::mapEVAPersonData);
    }

    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return listMapper(ProxySource.EVA_COMPANY, dtoBuilder::buildCheckEvaCompanyDto, mapper::mapEVACompanyData);
    }

    @Bean
    ProxyResultMapper ckiResultMapper(BKRCKIMapper mapper) {
        return listMapper(ProxySource.CKI, dtoBuilder::buildCheckCkiPersonDto, mapper::mapCKIData);
    }

    @Bean
    ProxyResultMapper eventIndicatorResultMapper(EventIndicatorMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVENT_INDICATOR, dtoBuilder::buildCheckEventIndicatorDto, mapper::mapEventIndicatorData);
    }

    /** Ca in legacy: mapperul primeste toate randurile sursei; fara randuri primeste o lista goala. */
    private static <D> ProxyResultMapper listMapper(ProxySource source, Function<GeneralProxyObsDto, D> toDto,
                                                    BiConsumer<List<D>, Application> mapper) {
        return new ProxyResultMapper() {
            @Override
            public ProxySource source() {
                return source;
            }

            @Override
            public void apply(List<GeneralProxyObsDto> results, Application application, ProxyContext ctx) {
                mapper.accept(results.stream().map(toDto).toList(), application);
            }
        };
    }
}
