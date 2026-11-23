// Exemplu: cum arata consume() in bkrvisproxy dupa migrare (inlocuieste metodele
// publishStatus / publishErrors / publishStatusAndErrors / hasErrors / errorsOf / eventTime din listener).

@KafkaListener(topics = "${kafka.consumers[0].topic}",
        containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
public void consume(@Payload OrchestrationMessage orchestrationMessage,
                    @Header(name = CORRELATION_HEADER, required = false) String correlation) {
    ProxyContext ctx = ProxyContext.of(orchestrationMessage, correlation, proxyName);
    try {
        if (proxyContextValidator.validate(ctx)) {
            // TODO: loanCheckProcess trebuie sa primeasca ProxyContext si sa foloseasca ctx.addError(code, message)
            integrityCheckService.loanCheckProcess(ctx);
            applicationService.updateApplicationStatus(ctx.getBusinessKey(), ctx.hasErrors() ? KO : OK);
        }
        proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version);
    } catch (RuntimeException e) {
        // TODO: ProcessingFailedException are nevoie de un constructor care primeste ProxyContext
        throw new ProcessingFailedException(ctx, e.getMessage(), e);
    }
}
