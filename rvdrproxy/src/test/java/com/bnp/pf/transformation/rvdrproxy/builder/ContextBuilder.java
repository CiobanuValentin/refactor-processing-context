package com.bnp.pf.transformation.rvdrproxy.builder;

import java.util.Map;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.orchestrator.avro.Metadata;
import com.bnp.pf.transformation.orchestrator.avro.OrchestrationMessage;
import com.bnp.pf.transformation.orchestrator.avro.Signal;

/** Nou: inlocuieste "new ProcessingContext()" + setteri din testele vechi. ProxyContext se construieste din mesaj. */
public final class ContextBuilder {
    public static final String CORRELATION = "corr-123";
    public static final String PROXY_NAME = "RVDR";
    public static final String FLOW_ID = "FLOW-1";
    public static final String PRODUCER = "producer";

    private ContextBuilder() {
    }

    public static OrchestrationMessage message(String flowId, Map<CharSequence, CharSequence> properties) {
        Signal signal = new Signal();
        signal.setFlowId(flowId);
        signal.setProperties(properties);
        Metadata metadata = new Metadata();
        metadata.setProducer(PRODUCER);
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        message.setMetadata(metadata);
        return message;
    }

    /** Context dupa validare: aplicatia e deja pusa in context. */
    public static ProxyContext context(Application application) {
        ProxyContext ctx = ProxyContext.of(message(FLOW_ID, Map.of()), CORRELATION, PROXY_NAME);
        ctx.setApplication(application);
        return ctx;
    }

    public static Application application(String businessKey) {
        Application application = new Application();
        application.setBusinessKey(businessKey);
        return application;
    }
}
