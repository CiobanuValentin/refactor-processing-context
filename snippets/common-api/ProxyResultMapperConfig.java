// De pus in common-api, langa mapperele existente (com.bnp.pf.transformation.commons.api.applicationbuilder).
// Mapperele raman neschimbate; aici doar se leaga de agregarea pe liste, cate un bean pentru fiecare sursa.
//
// TODO: tipurile de request/response si semnaturile map...Data(dto, application) sunt luate din poze pentru
// CKI, DNB si RVDR; pentru celelalte surse copiaza-le din ProxyDtoBuilder si din ApplicationBuilderPG.

@Configuration
public class ProxyResultMapperConfig {

    private final Gson gson = GsonInstance.createGson();

    @Bean
    ProxyResultMapper ckiResultMapper(BKRCKIMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.CKI,
                result -> CheckCkiPersonDto.builder()
                        .request(gson.fromJson(result.getRequestJson(), SingleBkrRequest.class))
                        .response(gson.fromJson(result.getResponseJson(), SingleBkrResponse.class))
                        .build(),
                mapper::mapCKIData);
    }

    @Bean
    ProxyResultMapper dnbResultMapper(DNBMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.DNB,
                result -> CheckDnbCompanyDto.builder()
                        .request(gson.fromJson(result.getRequestJson(), RequestCompany.class))
                        .response(gson.fromJson(result.getResponseJson(), ResponseRoot.class))
                        .build(),
                mapper::mapDNBData);
    }

    @Bean
    ProxyResultMapper rvdrResultMapper(RVDRMapper mapper) {
        return new DtoBackedResultMapper<>(ProxySource.RVDR,
                result -> CheckRvdrCreditDto.builder()
                        .request(gson.fromJson(result.getRequestJson(), CreditCheckRequest.class))
                        .response(gson.fromJson(result.getResponseJson(), CreditCheckResponse.class))
                        .build(),
                mapper::mapRVDRData);
    }

    // La fel pentru: SCAL_PERSON, SCAL_PERSON_INITIAL, SCAL_ENTITY, VIS, EVA_PERSON, EVENT_INDICATOR.
    // Daca ProxyDtoBuilder are deja metoda care construieste dto-ul din doua JSON-uri, foloseste-o in loc de gson.

    // EVA_COMPANY: mapperul primeste deja o lista, deci nu are nevoie de DtoBackedResultMapper.
    @Bean
    ProxyResultMapper evaCompanyResultMapper(BKREVAMapper mapper) {
        return new ProxyResultMapper() {
            @Override
            public ProxySource source() {
                return ProxySource.EVA_COMPANY;
            }

            @Override
            public void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx) {
                // TODO: tipurile CheckEvaCompanyDto sunt presupuse
                mapper.mapEVACompanyData(results.stream().map(result -> CheckEvaCompanyDto.builder()
                        .request(gson.fromJson(result.getRequestJson(), EvaCompanyRequest.class))
                        .response(gson.fromJson(result.getResponseJson(), EvaCompanyResponse.class))
                        .build()).toList(), application);
            }
        };
    }
}
