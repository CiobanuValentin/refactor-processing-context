package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.ERROR_MESSAGE;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.OK;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.STATUS;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.avro.ArvalSignal;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.kafka.KafkaPublisher;

/**
 * Construieste semnalele de iesire din context si le publica: status, apoi error.
 * Reguli (ca in legacy): fara correlation nu se trimite status; error se trimite doar daca exista erori.
 * Status-ul OK duce si proprietatile de rezultat din context; cel KO duce doar STATUS.
 */
@Service
@RequiredArgsConstructor
public class ProxyResultPublisher {

    static final String UNKNOWN_ERROR = "Unknown error";

    private final KafkaPublisher kafkaPublisher;

    public void publish(ProxyContext ctx, String statusTopic, String errorTopic, String version) {
        Map<CharSequence, CharSequence> status = statusProperties(ctx);
        if (status != null) {
            kafkaPublisher.publishToKafka(statusTopic, ctx.getCorrelation(), signal(ctx, status), ctx.getEventTime(), version);
        }
        Map<CharSequence, CharSequence> errors = errorProperties(ctx);
        if (errors != null) {
            kafkaPublisher.publishToKafka(errorTopic, ctx.getCorrelation(), signal(ctx, errors), ctx.getEventTime(), version);
        }
    }

    public void publishArval(ProxyContext ctx, String statusTopic, String errorTopic, String version) {
        Map<CharSequence, CharSequence> status = statusProperties(ctx);
        if (status != null) {
            kafkaPublisher.arvalPublishToKafka(statusTopic, ctx.getCorrelation(), arvalSignal(ctx, status), ctx.getEventTime(), version);
        }
        Map<CharSequence, CharSequence> errors = errorProperties(ctx);
        if (errors != null) {
            kafkaPublisher.arvalPublishToKafka(errorTopic, ctx.getCorrelation(), arvalSignal(ctx, errors), ctx.getEventTime(), version);
        }
    }

    /**
     * Status suplimentar pentru Arval: aceleasi proprietati ca status-ul normal, plus cele primite (ex. scope).
     * Ca si status-ul normal, nu se trimite fara correlation.
     */
    public void publishArvalStatus(ProxyContext ctx, String topic, String version, Map<String, String> extraProperties) {
        Map<CharSequence, CharSequence> status = statusProperties(ctx);
        if (status == null) {
            return;
        }
        status.putAll(extraProperties);
        kafkaPublisher.arvalPublishToKafka(topic, ctx.getCorrelation(), arvalSignal(ctx, status), ctx.getEventTime(), version);
    }

    /** @return null cand nu trebuie trimis status (correlation lipsa). */
    static Map<CharSequence, CharSequence> statusProperties(ProxyContext ctx) {
        if (StringUtils.isEmpty(ctx.getCorrelation())) {
            return null;
        }
        Map<CharSequence, CharSequence> properties = new LinkedHashMap<>();
        properties.put(STATUS, ctx.hasErrors() ? KO : OK);
        if (!ctx.hasErrors()) {
            // valorile null nu pot fi serializate in Avro, deci se sar
            ctx.getResultProperties().forEach((key, value) -> {
                if (value != null) {
                    properties.put(key, value);
                }
            });
        }
        return properties;
    }

    /**
     * @return null cand nu exista erori; altfel Error_message_1..n, in ordinea in care au aparut.
     *         Mesajele identice apar o singura data.
     */
    static Map<CharSequence, CharSequence> errorProperties(ProxyContext ctx) {
        if (!ctx.hasErrors()) {
            return null;
        }
        Set<String> messages = new LinkedHashSet<>();
        for (Error error : ctx.getErrors()) {
            messages.add(StringUtils.defaultIfBlank(error.getErrorMessage(), UNKNOWN_ERROR));
        }
        Map<CharSequence, CharSequence> properties = new LinkedHashMap<>();
        int index = 1;
        for (String message : messages) {
            properties.put(ERROR_MESSAGE + index++, message);
        }
        return properties;
    }

    static Signal signal(ProxyContext ctx, Map<CharSequence, CharSequence> properties) {
        Signal signal = new Signal();
        signal.setCommand(ctx.getProxyName());
        signal.setBusinessKey(StringUtils.defaultString(ctx.getBusinessKey()));
        signal.setFlowId(StringUtils.defaultString(ctx.getFlowId()));
        signal.setProperties(properties);
        return signal;
    }

    static ArvalSignal arvalSignal(ProxyContext ctx, Map<CharSequence, CharSequence> properties) {
        ArvalSignal signal = new ArvalSignal();
        signal.setCommand(ctx.getProxyName());
        signal.setBusinessKey(StringUtils.defaultString(ctx.getBusinessKey()));
        signal.setFlowId(StringUtils.defaultString(ctx.getFlowId()));
        signal.setProperties(properties);
        return signal;
    }
}
