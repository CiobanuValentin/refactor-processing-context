// De pus in common-api. Inlocuieste corpul lui ApplicationBuilderPG.buildApplication: pentru fiecare sursa,
// acelasi ProxyDtoBuilder construieste dto-ul tipizat si acelasi mapper il pune pe aplicatie.
// Mapperele si ProxyDtoBuilder NU se modifica.
//
// DESCHIS, de discutat cu poze din legacy inainte de integrare:
//  - CKI, EVA person si VIS adauga la fiecare apel risk assessment-uri si pe loan (Holder / CoHolder).
//    Daca o sursa intoarce mai multe randuri pentru o aplicatie, apelul per rand le-ar dubla pe loan.
//  - Semnaturile map...Data(dto, application) pentru SCAL, RVDR si EventIndicator sunt presupuse.

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
    ProxyResultMapper ckiResultMapper(BKRCKIMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.CKI, dtoBuilder::buildCheckCkiPersonDto, mapper::mapCKIData);
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
    ProxyResultMapper eventIndicatorResultMapper(EventIndicatorMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVENT_INDICATOR, dtoBuilder::buildCheckEventIndicatorDto, mapper::mapEventIndicatorData);
    }

    // EVA company: mapperul primeste deja o lista (inainte: Collections.singletonList(un singur dto)).
    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return new ProxyResultMapper() {
            @Override
            public ProxySource source() {
                return ProxySource.EVA_COMPANY;
            }

            @Override
            public void apply(List<GeneralProxyObsDto> results, Application application, ProxyContext ctx) {
                mapper.mapEVACompanyData(results.stream().map(dtoBuilder::buildCheckEvaCompanyDto).toList(), application);
            }
        };
    }
}
