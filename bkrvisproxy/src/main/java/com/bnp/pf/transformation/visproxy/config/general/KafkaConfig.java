package com.bnp.pf.transformation.visproxy.config.general;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

/**
 * Doar reincercarile raman. Inainte, dupa reincercari, handlerul lua ProcessingContext din ProcessingFailedException
 * si publica semnalele de error din KafkaErrorCollector; acum listenerul raspunde singur cu status KO plus error,
 * iar aici ajunge doar o exceptie aparuta la publicare. Campurile errorTopic si version nu mai sunt folosite.
 */
@Configuration
@Slf4j
public class KafkaConfig {
    @Value("${kafka.retries.max.attempts}")
    private int maxAttempts;

    @Value("${kafka.retries.interval}")
    private int interval;

    @Bean
    public DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler((ConsumerRecord<?, ?> rec, Exception ex) ->
                log.error("Recovery triggered for Kafka message. Topic = {}, partition = {}, offset = {}",
                        rec.topic(), rec.partition(), rec.offset(), ex),
                new FixedBackOff(interval, maxAttempts));
    }
}
