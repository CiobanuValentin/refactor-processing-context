package com.bnp.pf.transformation.visproxy.config.general;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO: importul pentru ProducerService este presupus.
import com.bnp.pf.transformation.commons.api.generalproxyobs.kafka.KafkaPublisher;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyContextValidator;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyResultPublisher;
import com.bnp.pf.transformation.commons.api.kafka.ProducerService;

/**
 * Bean-urile noi din common-api. Inlocuieste CollectErrorConfig si GeneralNullOrEmptyCheckServiceConfig, care se sterg.
 * ApplicationServiceConfig ramane neschimbat (acelasi constructor: ModelMapper, ApplicationRequestRepository),
 * la fel CachingServiceConfig si GsonConfig.
 */
@Configuration
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
}
