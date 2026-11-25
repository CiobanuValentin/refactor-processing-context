// De pus in common-api. Inlocuieste corpul lui ApplicationBuilderPG.buildApplication.
//
// In legacy (common-api-1.23.jar) fetcher-ul intorcea List<...> si fiecare mapper parcurgea lista intr-o bucla.
// In cloud mapperele primesc un singur dto. Toate cele sapte au fost comparate cu diff din IDE:
//  - SCAL x3, DNB, RVDR, EventIndicator, VIS, EVA person: tot ce fac se intampla IN bucla, deci un apel al
//    mapperului din cloud pentru fiecare rand (DtoBackedResultMapper) e echivalent. Mapperele raman neschimbate.
//  - CKI: singurul care adauga dupa bucla (rezultatele de pe loan, o singura data). mapCKIData se readuce la
//    varianta din legacy (BKRCKIMapper-mapCKIData.java) si primeste lista.
//  - EVA company: primeste lista si in cloud; logica e identica.
//
// Fara niciun rand, legacy nu facea nimic, cu doua exceptii pastrate aici: VIS punea rezultate implicite
// (de aceea e apelat cu null) si CKI punea cele doua rezultate goale pe loan (il face mapCKIData cu lista goala).
// DNB si EventIndicator din cloud arunca NPE daca primesc null, deci nu sunt apelate fara randuri.

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
        return new DtoBackedResultMapper<>(ProxySource.VIS, dtoBuilder::buildCheckVisDto, mapper::mapVISData, true);
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
