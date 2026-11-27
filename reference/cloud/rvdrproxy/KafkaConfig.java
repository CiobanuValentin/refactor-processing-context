// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
// Liniile 1-29 nu erau in poza (adnotarile clasei si campul maxAttempts).
package com.bnp.pf.transformation.rvdrproxy.config.general;

// Folderul config/: general/{ApplicationServiceConfig, CachingServiceConfig, CollectErrorConfig,
// GeneralNullOrEmptyCheckServiceConfig, GsonConfig, KafkaConfig}, MappingConfig, SoapClientConfig.
public class KafkaConfig {
    /* ...maxAttempts */

    @Value("${kafka.retries.interval}")
    private int interval;

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    @Bean
    public DefaultErrorHandler errorHandler(@Autowired KafkaPublisher kafkaPublisher, @Autowired KafkaErrorCollector kafkaErrorCollector) {
        return new DefaultErrorHandler((ConsumerRecord<?, ?> rec, Exception ex) -> {
            log.error("Recovery triggered for Kafka message. Topic = {}, partition = {}, offset = {}",
                    rec.topic(), rec.partition(), rec.offset(), ex);

            ProcessingContext pc = ProcessingFailedException.findIn(ex).orElse(null);

            if (Objects.isNull(pc)) {
                log.warn("No ProcessingContext for offset {}", rec.offset());
                return;
            }

            String correlation = new String(rec.headers().headers(CORRELATION_HEADER).iterator().next().value(), StandardCharsets.UTF_8);

            Iterator<Map.Entry<String, Signal>> errorSignalIterator = kafkaErrorCollector.getAllErrorSignalMap(pc).entrySet().iterator();

            while (errorSignalIterator.hasNext()) {
                Map.Entry<String, Signal> next = errorSignalIterator.next();
                kafkaPublisher.publishToKafka(errorTopic, correlation, next.getValue(), LocalDateTime.now(), version);
                errorSignalIterator.remove();
            }
        }, new FixedBackOff(interval, maxAttempts));
    }
}
