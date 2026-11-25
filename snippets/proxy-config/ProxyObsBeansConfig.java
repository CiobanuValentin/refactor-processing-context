// Clasele din common-api (pachet com.bnp...) nu sunt gasite de component scan-ul proxy-urilor (pachet com.bnpp...):
// in sedproxy/config fiecare e declarata ca bean (ApplicationServiceConfig, CollectErrorConfig, PartialDataFetcherConfig,
// ProxyDtoBuilderConfig, cate un ...MapperConfig). Clasele noi au nevoie de acelasi lucru.
//
// Se sterg din proxy: CollectErrorConfig, GeneralNullOrEmptyCheckServiceConfig, PartialDataFetcherConfig.
// Raman: ApplicationServiceConfig, ProxyDtoBuilderConfig, ObjectMapperConfig, MappingConfig, ...MapperConfig.
// TODO: nu am vazut clasele de config existente; aliniaza numele bean-urilor si RestClient-ul cu ele.

@Configuration
@Import(ProxyResultMapperConfig.class)   // doar in rat si sed; vezi snippets/common-api/ProxyResultMapperConfig.java
public class ProxyObsBeansConfig {

    @Bean
    public ProxyContextValidator proxyContextValidator() {
        return new ProxyContextValidator();
    }

    @Bean
    public KafkaPublisher kafkaPublisher(Map<String, ProducerService> producerServices,
                                         @Value("${proxy-name}") String proxyName) {
        return new KafkaPublisher(producerServices, proxyName);
    }

    @Bean
    public ProxyResultPublisher proxyResultPublisher(KafkaPublisher kafkaPublisher) {
        return new ProxyResultPublisher(kafkaPublisher);
    }

    // --- doar in rat si sed ---

    // Citeste proprietatile existente: dnb.root-url, bkrcki.endpoint, scal.person-endpoint, ...
    @Bean
    public ProxySourceProperties proxySourceProperties(Environment environment) {
        return new ProxySourceProperties(environment);
    }

    @Bean
    public ProxyResultsFetcher proxyResultsFetcher(RestClient restClient, ProxySourceProperties properties,
                                                   ObjectMapper objectMapper) {
        return new ProxyResultsFetcher(restClient, properties, objectMapper);
    }

    @Bean
    public ApplicationAggregator applicationAggregator(ProxyResultsFetcher fetcher, List<ProxyResultMapper> mappers) {
        return new ApplicationAggregator(fetcher, mappers);
    }
}
