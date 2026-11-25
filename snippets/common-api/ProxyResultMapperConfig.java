// De pus in common-api. Leaga mapperele existente de agregarea pe liste, fara sa le rescrie.
// Dto-urile tipizate se construiesc cu ProxyDtoBuilder-ul existent, deci raman la fel si tratarea raspunsului
// gol ("{}" -> obiect gol) si campul errors, pe care mapperele il citesc.
//
// Singurele schimbari in mappere: mapCKIData si mapEVAPersonData primesc lista intreaga
// (BKRCKIMapper-mapCKIData.java, BKREVAMapper-mapEVAPersonData.java).
// TODO: semnaturile map...Data(dto, application) pentru SCAL, RVDR si EventIndicator sunt presupuse.

@Configuration
@RequiredArgsConstructor
public class ProxyResultMapperConfig {

    private final ProxyDtoBuilder dtoBuilder;

    // --- un apel al mapperului pentru fiecare rezultat -------------------------------------------------------

    @Bean
    ProxyResultMapper scalPersonResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_PERSON, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestPersonJson(result.getRequestJson());
            dto.setResponsePersonJson(result.getResponseJson());
            return dtoBuilder.buildCheckScalPersonDto(dto);
        }, mapper::mapScalPersonData);
    }

    @Bean
    ProxyResultMapper scalPersonInitialResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_PERSON_INITIAL, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestPersonInitialJson(result.getRequestJson());
            dto.setResponsePersonInitialJson(result.getResponseJson());
            return dtoBuilder.buildCheckScalPersonInitialDto(dto);
        }, mapper::mapScalPersonInitialData);
    }

    @Bean
    ProxyResultMapper scalEntityResultMapper(SCALMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.SCAL_ENTITY, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestEntityJson(result.getRequestJson());
            dto.setResponseEntityJson(result.getResponseJson());
            return dtoBuilder.buildCheckScalEntityDto(dto);
        }, mapper::mapScalEntityData);
    }

    @Bean
    ProxyResultMapper dnbResultMapper(DNBMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.DNB,
                result -> dtoBuilder.buildCheckDnbCompanyDto(company(result)), mapper::mapDNBData);
    }

    @Bean
    ProxyResultMapper eventIndicatorResultMapper(EventIndicatorMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.EVENT_INDICATOR,
                result -> dtoBuilder.buildCheckEventIndicatorDto(company(result)), mapper::mapEventIndicatorData);
    }

    @Bean
    ProxyResultMapper rvdrResultMapper(RVDRMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.RVDR, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestCreditCheckEntityJson(result.getRequestJson());
            dto.setResponseCreditCheckJson(result.getResponseJson());
            return dtoBuilder.buildCheckRvdrCreditDto(dto);
        }, mapper::mapRVDRData);
    }

    // TODO: de confirmat dupa ce se vede BKRVISMapper.checkErrors - daca adauga pe loan la fiecare apel,
    // VIS trebuie mutat mai jos, la mapperele care primesc lista.
    @Bean
    ProxyResultMapper visResultMapper(BKRVISMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.VIS, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestVisJson(result.getRequestJson());
            dto.setResponseVisJson(result.getResponseJson());
            return dtoBuilder.buildCheckVisDto(dto);
        }, mapper::mapVISData);
    }

    // --- mapperul primeste lista intreaga, fiindca pune rezultate si pe loan ------------------------------------

    @Bean
    ProxyResultMapper ckiResultMapper(BKRCKIMapper mapper) {
        return listMapper(ProxySource.CKI, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setSingleBkrRequestJson(result.getRequestJson());
            dto.setSingleBkrResponseJson(result.getResponseJson());
            return dtoBuilder.buildCheckCkiPersonDto(dto);
        }, mapper::mapCKIData);
    }

    @Bean
    ProxyResultMapper evaPersonResultMapper(BKREVAMapper mapper) {
        return listMapper(ProxySource.EVA_PERSON, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestCheckEvaJson(result.getRequestJson());
            // Ca in ProxyDtoBuilder: raspunsul EVA person se citeste din responsePersonJson.
            dto.setResponsePersonJson(result.getResponseJson());
            return dtoBuilder.buildCheckEvaPersonDto(dto);
        }, mapper::mapEVAPersonData);
    }

    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return listMapper(ProxySource.EVA_COMPANY, result -> {
            GeneralProxyObsDto dto = base(result);
            dto.setRequestCheckEvaCompanyJson(result.getRequestJson());
            dto.setResponseCheckEvaCompanyJson(result.getResponseJson());
            return dtoBuilder.buildCheckEvaCompanyDto(dto);
        }, mapper::mapEVACompanyData);
    }

    // --- ajutoare -------------------------------------------------------------------------------------------------

    private static GeneralProxyObsDto base(ProxyResultDto result) {
        GeneralProxyObsDto dto = new GeneralProxyObsDto();
        dto.setErrorHandlerJson(result.getErrorHandlerJson());
        return dto;
    }

    /** DNB si EventIndicator folosesc aceleasi doua campuri. */
    private static GeneralProxyObsDto company(ProxyResultDto result) {
        GeneralProxyObsDto dto = base(result);
        dto.setRequestCompanyJson(result.getRequestJson());
        dto.setResponseRootJson(result.getResponseJson());
        return dto;
    }

    private static <D> ProxyResultMapper listMapper(ProxySource source, Function<ProxyResultDto, D> toDto,
                                                    BiConsumer<List<D>, Application> mapper) {
        return new ProxyResultMapper() {
            @Override
            public ProxySource source() {
                return source;
            }

            @Override
            public void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx) {
                mapper.accept(results.stream().map(toDto).toList(), application);
            }
        };
    }
}
