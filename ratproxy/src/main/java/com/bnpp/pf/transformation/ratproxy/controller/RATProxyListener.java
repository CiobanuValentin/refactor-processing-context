package com.bnpp.pf.transformation.ratproxy.controller;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.CORRELATION_HEADER;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.OK;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyContextValidator;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyResultPublisher;
import com.bnpp.pf.transformation.ratproxy.service.RatProcessingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RATProxyListener {

    private final ProxyContextValidator proxyContextValidator;
    private final RatProcessingService ratProcessingService;
    private final ApplicationService applicationService;
    private final ProxyResultPublisher proxyResultPublisher;

    @Value("${kafka.producers[0].topic}")
    private String statusTopic;

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    @Value("${proxy-name}")
    private String proxyName;

    // Headerul de correlation nu mai e obligatoriu: lipsa lui e raportata de validator, nu de Spring.
    @KafkaListener(topics = "${kafka.consumers[0].topic}",
            containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage orchestrationMessage,
                        @Header(name = CORRELATION_HEADER, required = false) String correlation) {
        ProxyContext ctx = ProxyContext.of(orchestrationMessage, correlation, proxyName);
        log.info("Received orchestration message - correlation {}, flow id {}", correlation, ctx.getFlowId());
        try {
            if (proxyContextValidator.validate(ctx)) {
                ratProcessingService.process(ctx);
                applicationService.updateApplicationStatus(ctx.getBusinessKey(), ctx.hasErrors() ? KO : OK);
            }
        } catch (RuntimeException e) {
            // Ca in legacy, nu exista retry: orice exceptie din procesare inseamna status KO plus error.
            log.error("Unexpected error - correlation {}, flow id {}", correlation, ctx.getFlowId(), e);
            ctx.addUnexpectedError(e);
        }
        // In afara try-ului: daca publicarea insasi esueaza, exceptia iese catre containerul Kafka.
        proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version);
    }
    }
}
