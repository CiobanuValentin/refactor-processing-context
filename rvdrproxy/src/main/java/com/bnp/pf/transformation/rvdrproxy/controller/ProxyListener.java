package com.bnp.pf.transformation.rvdrproxy.controller;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.CORRELATION_HEADER;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.OK;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importul pentru OrchestrationMessage este presupus - pastreaza-l pe cel din clasa actuala.
import com.bnp.pf.transformation.orchestrator.avro.OrchestrationMessage;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyContextValidator;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ProxyResultPublisher;
import com.bnp.pf.transformation.rvdrproxy.service.IntegrityCheckService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyListener {
    private final ProxyContextValidator proxyContextValidator;
    private final IntegrityCheckService integrityCheckService;
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

    @KafkaListener(topics = "${kafka.consumers[0].topic}",
            containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload @NonNull OrchestrationMessage orchestrationMessage,
                        @Header(name = CORRELATION_HEADER, required = false) String correlation,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int receivedPartition,
                        @Header(KafkaHeaders.OFFSET) long receivedOffset) {
        log.info("CONSUME topic = {}, partition = {}, offset = {}, correlation = {}",
                receivedTopic, receivedPartition, receivedOffset, correlation);
        // Mesajul intreg contine JSON-ul aplicatiei, deci se logheaza doar la debug (inainte: la info).
        log.debug("Received signal from Granting Api {}", orchestrationMessage);

        ProxyContext ctx = ProxyContext.of(orchestrationMessage, correlation, proxyName);
        try {
            if (proxyContextValidator.validate(ctx)) {
                integrityCheckService.loanCheckProcess(ctx);
                applicationService.updateApplicationStatus(ctx.getBusinessKey(), ctx.hasErrors() ? KO : OK);
            }
        } catch (RuntimeException e) {
            // Orice exceptie din procesare inseamna status KO plus error; nu se mai arunca ProcessingFailedException.
            log.error("Unexpected error - correlation {}, flow id {}", correlation, ctx.getFlowId(), e);
            ctx.addUnexpectedError(e);
        }
        // In afara try-ului: daca publicarea insasi esueaza, exceptia iese catre errorHandler-ul din KafkaConfig.
        proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version);
    }
}
