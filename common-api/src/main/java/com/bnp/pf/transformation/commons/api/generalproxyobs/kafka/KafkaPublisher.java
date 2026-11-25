package com.bnp.pf.transformation.commons.api.generalproxyobs.kafka;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.CORRELATION_HEADER;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru clasele Avro si ProducerService sunt presupuse - corecteaza-le din IDE.
import com.bnp.pf.transformation.orchestrator.avro.ArvalOrchestrationMessage;
import com.bnp.pf.transformation.orchestrator.avro.ArvalSignal;
import com.bnp.pf.transformation.orchestrator.avro.OrchestrationMessage;
import com.bnp.pf.transformation.orchestrator.avro.Signal;
import com.bnp.pf.transformation.commons.api.kafka.ProducerService;
import com.bnp.pf.transformation.commons.api.utils.OrchestrationMetaDataHandler;

/**
 * Impacheteaza un semnal intr-un mesaj de orchestrare (cu metadata) si il trimite pe topic.
 * Nu modifica semnalul primit.
 */
@Slf4j
@Component
public class KafkaPublisher {

    private final Map<String, ProducerService> producerServices;
    private final String proxyName;

    public KafkaPublisher(Map<String, ProducerService> producerServices, @Value("${proxy-name}") String proxyName) {
        this.producerServices = producerServices;
        this.proxyName = proxyName;
    }

    public void publishToKafka(String topicName, String correlation, Signal signal,
                               LocalDateTime creation, String version) {
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        OrchestrationMetaDataHandler.addMetaData(message, proxyName, version, creation);
        send(topicName, correlation, message, signal.getFlowId());
    }

    public void arvalPublishToKafka(String topicName, String correlation, ArvalSignal signal,
                                    LocalDateTime creation, String version) {
        ArvalOrchestrationMessage message = new ArvalOrchestrationMessage();
        message.setData(signal);
        OrchestrationMetaDataHandler.arvalAddMetaData(message, proxyName, version, creation);
        send(topicName, correlation, message, signal.getFlowId());
    }

    private void send(String topicName, String correlation, Object message, CharSequence flowId) {
        ProducerService producer = producerServices.get(topicName);
        if (producer == null) {
            throw new IllegalStateException("No Kafka producer configured for topic " + topicName);
        }
        Map<String, String> headers = correlation == null
                ? Collections.emptyMap()
                : Collections.singletonMap(CORRELATION_HEADER, correlation);

        log.info("Publishing in topic {} - correlation {}, flow id {}", topicName, correlation, flowId);
        log.debug("Message published in topic {}: {}", topicName, message);
        producer.produce(message, headers, OrchestrationMetaDataHandler.addKey(flowId));
    }
}
