// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.config.general;

// In config/general: ApplicationServiceConfig, CachingServiceConfig, CollectErrorConfig,
// GeneralNullOrEmptyCheckServiceConfig, GsonConfig, KafkaConfig. In config: MappingConfig, SoapClientConfig.
public class KafkaConfig {
    /* ...liniile de pana la 32 (interval, maxAttempts) nu erau in poza */

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    // Identic cu cel din sedproxy (reference/cloud/sedproxy/KafkaConfig.java):
    // DefaultErrorHandler care, dupa FixedBackOff(interval, maxAttempts), ia ProcessingContext din
    // ProcessingFailedException.findIn(ex) si publica semnalele de error din kafkaErrorCollector.getAllErrorSignalMap(pc).
    @Bean
    public DefaultErrorHandler errorHandler(@Autowired KafkaPublisher kafkaPublisher, @Autowired KafkaErrorCollector kafkaErrorCollector) { /* ... */ }
}
