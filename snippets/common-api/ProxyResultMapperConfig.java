// De pus in common-api. Inlocuieste corpul lui ApplicationBuilderPG.buildApplication: pentru fiecare sursa,
// ProxyDtoBuilder construieste dto-urile tipizate si mapperul le pune pe aplicatie.
//
// In legacy (common-api-1.23.jar) fetcher-ul intorcea List<...> si mapperele primeau lista. Comparat cu diff din IDE:
//  - CKI: legacy adauga rezultatele de pe loan O SINGURA DATA, dupa bucla. mapCKIData se readuce la varianta
//    din legacy (BKRCKIMapper-mapCKIData.java) si primeste lista.
//  - VIS si EVA person: legacy adauga pe loan IN bucla, la fiecare rand. Un apel al mapperului din cloud pentru
//    fiecare rand (DtoBackedResultMapper) face exact acelasi lucru, deci mapperele raman neschimbate.
//  - EVA company: primeste lista si in cloud; logica e identica.
//  - SCAL x3, DNB, RVDR, EventIndicator: un apel per rand, necomparate inca cu legacy.

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
        return listMapper(ProxySource.CKI, dtoBuilder::buildCheckCkiPersonDto, mapper::mapCKIData);
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

    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return listMapper(ProxySource.EVA_COMPANY, dtoBuilder::buildCheckEvaCompanyDto, mapper::mapEVACompanyData);
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
