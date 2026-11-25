// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// bkrvisproxy, branch feature/proxy-refactorization-2.0 - com.bnp.pf.transformation.visproxy.controller

public class ProxyListener {
    /* ...campurile de pana la linia 55 nu erau in poza */

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
        log.info("Received orchestration message from GrantingAPI {}", orchestrationMessage);

        ProcessingContext processingContext = new ProcessingContext();
        processingContext.getIndex().set(1);

        try {
            generalNullOrEmptyCheckService.correlationEmptyCheck(processingContext, correlation, proxyName, orchestrationMessage);
            if (hasErrors(processingContext)) {
                publishErrors(processingContext, correlation, LocalDateTime.now());
                return;
            }

            generalNullOrEmptyCheckService.applicationEmptyCheck(processingContext);
            generalNullOrEmptyCheckService.businessKeyFlowIdEmptyCheck(processingContext, proxyName);
            if (hasErrors(processingContext)) {
                publishStatusAndErrors(processingContext, correlation, eventTime(orchestrationMessage));
                return;
            }

            integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);

            LocalDateTime eventTime = eventTime(orchestrationMessage);
            if (hasErrors(processingContext)) {
                applicationService.updateApplicationStatus(processingContext.getBusinessKey(), KO);
                publishStatusAndErrors(processingContext, correlation, eventTime);
            } else {
                applicationService.updateApplicationStatus(processingContext.getBusinessKey(), OK);
                publishStatus(processingContext, correlation, OK, eventTime);
            }
        } catch (RuntimeException e) {
            throw new ProcessingFailedException(processingContext, e.getMessage(), e);
        }
    }

    private void publishStatusAndErrors(ProcessingContext processingContext, String correlation, LocalDateTime eventTime) {
        publishStatus(processingContext, correlation, KO, eventTime);
        publishErrors(processingContext, correlation, eventTime);
    }

    private void publishStatus(ProcessingContext processingContext, String correlation, String status,
                               LocalDateTime eventTime) {
        Map<CharSequence, CharSequence> properties = new HashMap<>();
        properties.put(STATUS, status);

        Signal statusMessage = processingContext.getStatusMessage();
        statusMessage.setProperties(properties);
        kafkaPublisher.publishToKafka(statusTopic, correlation, statusMessage, eventTime, version);
    }

    private void publishErrors(ProcessingContext processingContext, String correlation, LocalDateTime eventTime) {
        if (CollectionUtils.isEmpty(processingContext.getErrorSignalMap())) {
            List<Error> errors = errorsOf(processingContext);
            log.info("No error signal built for businessKey {}, building it from errors {}",
                    processingContext.getBusinessKey(), errors);
            errors.forEach(error -> processingContext.getErrorMap().put(
                    ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(), error.getErrorMessage()));
            collectError.addErrorSignal(processingContext,
                    Objects.requireNonNullElseGet(processingContext.getErrorMessage(), Signal::new));
        }

        processingContext.getErrorSignalMap().values().forEach(errorSignal ->
                kafkaPublisher.publishToKafka(errorTopic, correlation, errorSignal, eventTime, version));
    }

    private static boolean hasErrors(ProcessingContext processingContext) {
        return !errorsOf(processingContext).isEmpty() || !CollectionUtils.isEmpty(processingContext.getErrorSignalMap());
    }

    private static List<Error> errorsOf(ProcessingContext processingContext) {
        ErrorHandler errorHandler = processingContext.getErrorHandler();
        return errorHandler == null || errorHandler.getErrors() == null ? List.of() : errorHandler.getErrors();
    }

    private static LocalDateTime eventTime(OrchestrationMessage orchestrationMessage) {
        return orchestrationMessage.getMetadata() != null
                ? LocalDateTimeUtils.convertToNewLocalDateTime(orchestrationMessage)
                : LocalDateTime.now();
    }
}
