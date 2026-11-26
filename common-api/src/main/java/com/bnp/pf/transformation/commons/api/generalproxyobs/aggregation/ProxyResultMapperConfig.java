package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

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

/**
 * Leaga fiecare sursa de mapperul ei. Inlocuieste corpul lui ApplicationBuilderPG.buildApplication.
 * Proxy-urile agregatoare (rat, sed) o importa in config: @Import(ProxyResultMapperConfig.class).
 *
 * Ca in legacy (common-api-1.23), toate mapperele primesc lista cu toate randurile sursei
 * (vezi docs/APPLY-COMMON.md, pasul 1).
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
        return new DtoBackedResultMapper<>(ProxySource.VIS, dtoBuilder::buildCheckVisDto, mapper::mapVISData);
    }

    @Bean
    ProxyResultMapper evaPersonResultMapper(BKREVAMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVA_PERSON, dtoBuilder::buildCheckEvaPersonDto, mapper::mapEVAPersonData);
    }

    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVA_COMPANY, dtoBuilder::buildCheckEvaCompanyDto, mapper::mapEVACompanyData);
    }

    @Bean
    ProxyResultMapper ckiResultMapper(BKRCKIMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.CKI, dtoBuilder::buildCheckCkiPersonDto, mapper::mapCKIData);
    }

    @Bean
    ProxyResultMapper eventIndicatorResultMapper(EventIndicatorMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVENT_INDICATOR, dtoBuilder::buildCheckEventIndicatorDto, mapper::mapEventIndicatorData);
    }
}
