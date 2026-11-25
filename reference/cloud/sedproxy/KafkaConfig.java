// REFERINTA - cod existent (cloud, sedproxy, branch arval-process-context), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.sedproxy.config;

// In sedproxy/config exista si: proxy/BKRCKIMapperConfig, BKREVAMapperConfig, BKRVISMapperConfig, DNBMapperConfig,
// EventIndicatorMapperConfig, RVDRMapperConfig, SCALMapperConfig, ApplicationServiceConfig, CollectErrorConfig,
// GeneralNullOrEmptyCheckServiceConfig, MappingConfig, ObjectMapperConfig, PartialDataFetcherConfig,
// ProxyDtoBuilderConfig - adica clasele din common-api sunt declarate ca bean-uri in fiecare proxy.
public class KafkaConfig {
    /* ...campurile de pana la linia 32 (interval, maxAttempts) nu erau in poza */

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    @Bean
    public DefaultErrorHandler errorHandler(@Autowired KafkaPublisher kafkaPublisher, @Autowired KafkaErrorCollector kafkaErrorCollector) {
        return new DefaultErrorHandler((ConsumerRecord<?, ?> rec, Exception ex) -> {

            log.error("Recovery triggered for Kafka message. Topic = {}, partition = {}, offset = {}",
                    rec.topic(),
                    rec.partition(),
                    rec.offset(),
                    ex);

            ProcessingContext pc = ProcessingFailedException.findIn(ex).orElse(null);

            if (pc == null) {
                log.warn("No ProcessingContext for offset {}", rec.offset());
                return;
            }

            String correlation = new String(rec.headers().headers(CORRELATION_HEADER).iterator().next().value(), StandardCharsets.UTF_8);

            //removing elements from a map is dangerous, use an Iterator to avoid ConcurrentModificationException
            Iterator<Map.Entry<String, Signal>> errorSignalIterator = kafkaErrorCollector.getAllErrorSignalMap(pc).entrySet().iterator();

            while (errorSignalIterator.hasNext()) {
                Map.Entry<String, Signal> next = errorSignalIterator.next();
                kafkaPublisher.publishToKafka(errorTopic, correlation, next.getValue(), LocalDateTime.now(), version);
                errorSignalIterator.remove();
            }
        }, new FixedBackOff(interval, maxAttempts));
    }
}
