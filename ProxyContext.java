package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

// TODO: importurile pentru Signal, ArvalSignal, OrchestrationMessage, ArvalOrchestrationMessage,
// Application, ErrorHandler, Error si LocalDateTimeUtils sunt presupuse - corecteaza-le din IDE.
import com.bnp.pf.transformation.avro.ArvalOrchestrationMessage;
import com.bnp.pf.transformation.avro.ArvalSignal;
import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.model.Application;
import com.bnp.pf.transformation.commons.api.util.LocalDateTimeUtils;

/**
 * Tot ce are nevoie un proxy pentru un mesaj: inputul (citit o singura data, imutabil),
 * aplicatia parsata si erorile. Nu tine semnale de iesire - ele se construiesc la publicare.
 */
@Getter
public class ProxyContext {

    private final String proxyName;
    private final String correlation;
    /** false cand mesajul sau data lui lipsesc. */
    private final boolean signalPresent;
    private final String flowId;
    private final Map<CharSequence, CharSequence> properties;
    private final LocalDateTime eventTime;
    private final ErrorHandler errorHandler = new ErrorHandler();

    @Setter
    private Application application;

    private ProxyContext(String proxyName, String correlation, boolean signalPresent, CharSequence flowId,
                         Map<CharSequence, CharSequence> properties, LocalDateTime eventTime) {
        this.proxyName = proxyName;
        this.correlation = correlation;
        this.signalPresent = signalPresent;
        this.flowId = flowId == null ? null : flowId.toString();
        this.properties = properties == null ? Map.of() : properties;
        this.eventTime = eventTime;
    }

    public static ProxyContext of(OrchestrationMessage message, String correlation, String proxyName) {
        LocalDateTime eventTime = message != null && message.getMetadata() != null
                ? LocalDateTimeUtils.convertToNewLocalDateTime(message)
                : LocalDateTime.now();
        Signal signal = message == null ? null : message.getData();
        return signal == null
                ? new ProxyContext(proxyName, correlation, false, null, null, eventTime)
                : new ProxyContext(proxyName, correlation, true, signal.getFlowId(), signal.getProperties(), eventTime);
    }

    public static ProxyContext of(ArvalOrchestrationMessage message, String correlation, String proxyName) {
        LocalDateTime eventTime = message != null && message.getMetadata() != null
                ? LocalDateTimeUtils.convertToNewLocalDateTime(message)
                : LocalDateTime.now();
        ArvalSignal signal = message == null ? null : message.getData();
        return signal == null
                ? new ProxyContext(proxyName, correlation, false, null, null, eventTime)
                : new ProxyContext(proxyName, correlation, true, signal.getFlowId(), signal.getProperties(), eventTime);
    }

    /** Business key-ul vine din aplicatia parsata; null pana cand aplicatia e rezolvata. */
    public String getBusinessKey() {
        return application == null ? null : application.getBusinessKey();
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

    public void addError(String code, String message) {
        errorHandler.addError(code, message);
    }

    public List<Error> getErrors() {
        List<Error> errors = errorHandler.getErrors();
        return errors == null ? List.of() : errors;
    }

    public boolean hasErrors() {
        return !getErrors().isEmpty();
    }
}
