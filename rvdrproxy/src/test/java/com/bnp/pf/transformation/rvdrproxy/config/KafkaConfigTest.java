package com.bnp.pf.transformation.rvdrproxy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import com.bnp.pf.transformation.rvdrproxy.config.general.KafkaConfig;

class KafkaConfigTest {

    private static KafkaConfig config(int maxAttempts) {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "maxAttempts", maxAttempts);
        ReflectionTestUtils.setField(config, "interval", 0);
        return config;
    }

    @Test
    void errorHandler_shouldBeCreated() {
        assertThat(config(1).errorHandler()).isNotNull();
    }

    /** Fara reincercari ramase, handlerul doar logheaza si considera mesajul tratat (nu mai publica nimic). */
    @Test
    void errorHandler_whenRetriesExhausted_shouldRecoverByLoggingOnly() {
        DefaultErrorHandler errorHandler = config(0).errorHandler();
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("granting-topic", 0, 42L, "key", "value");

        boolean recovered = errorHandler.handleOne(new RuntimeException("Kafka down"), rec,
                mock(Consumer.class), mock(MessageListenerContainer.class));

        assertThat(recovered).isTrue();
    }
}
