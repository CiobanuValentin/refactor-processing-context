// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.controller;

@Component
@Slf4j
public class RATProxyListener {

    private final KafkaPublisher kafkaPublisher;
    private final RatNullOrEmptyCheckService ratNullOrEmptyCheckService;
    private final GeneralNullOrEmptyCheckService generalNullOrEmptyCheckService;
    private final ApplicationService applicationService;

    @Value("${kafka.producers[0].topic}")
    private String statusTopic;

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    @Value("${proxy-name}")
    private String proxyName;

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage orchMessage, @Header(CORRELATION_HEADER) String correlation,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String receivedTopic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int receivedPartition,
                        @Header(KafkaHeaders.OFFSET) long receivedOffset) {
        log.info("Received signal from Granting Api {}", orchMessage);

        log.info("CONSUME topic = {}, partition = {}, offset = {}", receivedTopic, receivedPartition, receivedOffset);

        ProcessingContext processingContext = new ProcessingContext();

        try {
            generalNullOrEmptyCheckService.correlationEmptyCheck(processingContext, correlation, proxyName, orchMessage);
            generalNullOrEmptyCheckService.applicationEmptyCheck(processingContext);
            ratNullOrEmptyCheckService.businessKeyFlowIdEmptyCheck(processingContext, proxyName);
            ratNullOrEmptyCheckService.loanCheckProcess(processingContext, orchMessage);
            String businessKey = processingContext.getBusinessKey();

            Map<String, Signal> errorSignalMap = processingContext.getErrorSignalMap();
            Signal statusMessage = processingContext.getStatusMessage();
            List<Error> errors = processingContext.getErrorHandler().getErrors();

            LocalDateTime newLocalDateTime = LocalDateTimeUtils.convertToNewLocalDateTime(orchMessage);
            if (CollectionUtils.isEmpty(errors) && CollectionUtils.isEmpty(errorSignalMap)) {
                applicationService.updateApplicationStatus(businessKey, OK);
                kafkaPublisher.publishToKafka(statusTopic, correlation, statusMessage, newLocalDateTime, version);
            } else {
                applicationService.updateApplicationStatus(businessKey, KO);

                //removing elements from a map is dangerous, use an Iterator to avoid ConcurrentModificationException
                Iterator<Map.Entry<String, Signal>> errorSignalIterator = errorSignalMap.entrySet().iterator();
                statusMessage.setProperties(new HashMap<>());
                statusMessage.getProperties().put(STATUS, KO);
                kafkaPublisher.publishToKafka(statusTopic, correlation, statusMessage, newLocalDateTime, version);

                while (errorSignalIterator.hasNext()) {
                    Map.Entry<String, Signal> next = errorSignalIterator.next();
                    kafkaPublisher.publishToKafka(errorTopic, correlation, next.getValue(), LocalDateTime.now(), version);
                    errorSignalIterator.remove();
                }
            }
        } catch (RuntimeException e){
            throw new ProcessingFailedException(processingContext, e.getMessage(), e);
        }

    }
}
