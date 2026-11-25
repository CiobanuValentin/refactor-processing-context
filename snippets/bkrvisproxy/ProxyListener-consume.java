// Exemplu: cum arata consume() in bkrvisproxy dupa migrare. Dispar din listener metodele
// publishStatus / publishErrors / publishStatusAndErrors / hasErrors / errorsOf / eventTime.
// KafkaPublisher-ul local se sterge; se foloseste cel din common-api prin ProxyResultPublisher.

@KafkaListener(topics = "${kafka.consumers[0].topic}",
        containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
public void consume(@Payload OrchestrationMessage orchestrationMessage,
                    @Header(name = CORRELATION_HEADER, required = false) String correlation) {
    ProxyContext ctx = ProxyContext.of(orchestrationMessage, correlation, proxyName);
    try {
        if (proxyContextValidator.validate(ctx)) {
            integrityCheckService.loanCheckProcess(ctx);
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
