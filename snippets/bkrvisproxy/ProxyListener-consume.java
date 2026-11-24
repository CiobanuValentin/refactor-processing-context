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
            integrityCheckService.loanCheckProcess(ctx, orchestrationMessage);
            applicationService.updateApplicationStatus(ctx.getBusinessKey(), ctx.hasErrors() ? KO : OK);
        }
        proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version);
    } catch (RuntimeException e) {
        throw new ProxyProcessingFailedException(ctx, e.getMessage(), e);
    }
}
