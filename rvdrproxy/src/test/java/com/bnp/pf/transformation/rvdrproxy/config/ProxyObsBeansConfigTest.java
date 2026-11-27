package com.bnp.pf.transformation.rvdrproxy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

// TODO: importul pentru ProducerService este presupus - ia-l din ProxyObsBeansConfig.
import com.bnp.pf.transformation.commons.api.generalproxyobs.kafka.KafkaPublisher;
import com.bnp.pf.transformation.commons.api.kafka.ProducerService;
import com.bnp.pf.transformation.rvdrproxy.config.general.ProxyObsBeansConfig;

class ProxyObsBeansConfigTest {
    private final ProxyObsBeansConfig config = new ProxyObsBeansConfig();

    @Test
    void proxyContextValidator_shouldBeCreated() {
        assertThat(config.proxyContextValidator()).isNotNull();
    }

    @Test
    void kafkaPublisherAndResultPublisher_shouldBeCreated() {
        KafkaPublisher kafkaPublisher = config.kafkaPublisher(Map.of("status-topic", mock(ProducerService.class)), "RVDR");

        assertThat(kafkaPublisher).isNotNull();
        assertThat(config.proxyResultPublisher(kafkaPublisher)).isNotNull();
    }
}
