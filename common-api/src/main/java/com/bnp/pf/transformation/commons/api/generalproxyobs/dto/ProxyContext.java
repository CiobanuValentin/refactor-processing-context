package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.ARVAL_NL_FLOW_ID_PREFIX;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru clasele Avro Arval, ErrorHandler si Error sunt presupuse - corecteaza-le din IDE.
import com.bnp.pf.transformation.orchestrator.avro.ArvalOrchestrationMessage;
import com.bnp.pf.transformation.orchestrator.avro.ArvalSignal;
import com.bnp.pf.transformation.orchestrator.avro.OrchestrationMessage;
import com.bnp.pf.transformation.orchestrator.avro.Signal;
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;

/**
 * Tot ce are nevoie un proxy pentru un mesaj: inputul (citit o singura data, imutabil),
 * aplicatia, erorile si proprietatile care pleaca in status. Semnalele de iesire se construiesc la publicare.
 */
@Slf4j
@Getter
public class ProxyContext {

    private final String proxyName;
    private final String correlation;
    /** false cand mesajul sau data lui lipsesc. */
    private final boolean signalPresent;
    private final String flowId;
    private final Map<CharSequence, CharSequence> properties;
    private final LocalDateTime eventTime;
    /** Producer-ul din metadata mesajului primit; null daca lipseste. */
    private final String producer;
    private final ErrorHandler errorHandler = new ErrorHandler();
    /** Proprietati in plus pentru status-ul OK (ex. bnpOutputMessage la sed). */
    private final Map<String, String> resultProperties = new LinkedHashMap<>();

    @Setter
    private Application application;

    private ProxyContext(String proxyName, String correlation, boolean signalPresent, CharSequence flowId,
                         Map<CharSequence, CharSequence> properties, LocalDateTime eventTime, CharSequence producer) {
        this.proxyName = proxyName;
        this.correlation = correlation;
        this.signalPresent = signalPresent;
        this.flowId = flowId == null ? null : flowId.toString();
        this.properties = properties == null ? Map.of() : properties;
        this.eventTime = eventTime;
        this.producer = producer == null ? null : producer.toString();
    }

    public static ProxyContext of(OrchestrationMessage message, String correlation, String proxyName) {
        boolean hasMetadata = message != null && message.getMetadata() != null;
        LocalDateTime eventTime = hasMetadata
                ? eventTime(() -> LocalDateTimeUtils.convertToNewLocalDateTime(message)) : LocalDateTime.now();
        CharSequence producer = hasMetadata ? message.getMetadata().getProducer() : null;
        Signal signal = message == null ? null : message.getData();
        return signal == null
                ? new ProxyContext(proxyName, correlation, false, null, null, eventTime, producer)
                : new ProxyContext(proxyName, correlation, true, signal.getFlowId(), signal.getProperties(), eventTime, producer);
    }

    public static ProxyContext of(ArvalOrchestrationMessage message, String correlation, String proxyName) {
        boolean hasMetadata = message != null && message.getMetadata() != null;
        LocalDateTime eventTime = hasMetadata
                ? eventTime(() -> LocalDateTimeUtils.convertToNewLocalDateTime(message)) : LocalDateTime.now();
        CharSequence producer = hasMetadata ? message.getMetadata().getProducer() : null;
        ArvalSignal signal = message == null ? null : message.getData();
        return signal == null
                ? new ProxyContext(proxyName, correlation, false, null, null, eventTime, producer)
                : new ProxyContext(proxyName, correlation, true, signal.getFlowId(), signal.getProperties(), eventTime, producer);
    }

    /** Un timestamp lipsa sau in alt format nu trebuie sa opreasca procesarea: se foloseste ora curenta, cu un warning. */
    private static LocalDateTime eventTime(Supplier<LocalDateTime> fromMetadata) {
        try {
            LocalDateTime eventTime = fromMetadata.get();
            if (eventTime != null) {
                return eventTime;
            }
            log.warn("Event time missing in message metadata, using the current time");
        } catch (RuntimeException e) {
            log.warn("Event time in message metadata could not be read, using the current time: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    /** Business key-ul vine din aplicatia parsata; null pana cand aplicatia e rezolvata. */
    public String getBusinessKey() {
        return application == null ? null : application.getBusinessKey();
    }

    public boolean isArvalNlFlow() {
        return flowId != null && flowId.startsWith(ARVAL_NL_FLOW_ID_PREFIX);
    }

    /** Cauta dupa text, deci merge si cu chei Utf8, si cu chei String. */
    public String property(String key) {
        for (Map.Entry<CharSequence, CharSequence> entry : properties.entrySet()) {
            if (entry.getKey() != null && key.contentEquals(entry.getKey())) {
                return entry.getValue() == null ? null : entry.getValue().toString();
            }
        }
        return null;
    }

    /** Se publica in status doar daca procesarea se termina fara erori. */
    public void putResultProperty(String key, String value) {
        resultProperties.put(key, value);
    }

    public void addError(String code, String message) {
        errorHandler.addError(code, message);
    }

    /** O exceptie neasteptata din procesare devine o eroare obisnuita: raspunsul va fi KO cu error. */
    public void addUnexpectedError(RuntimeException e) {
        String detail = e.getMessage() == null || e.getMessage().isBlank() ? e.toString() : e.getMessage();
        addError("500", "Unexpected error for business key " + getBusinessKey() + " and flow ID " + flowId + ": " + detail);
    }

    public List<Error> getErrors() {
        List<Error> errors = errorHandler.getErrors();
        return errors == null ? List.of() : errors;
    }

    public boolean hasErrors() {
        return !getErrors().isEmpty();
    }
}
