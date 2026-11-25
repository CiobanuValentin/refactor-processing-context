// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// sedproxy, branch arval-process-context - com.bnpp.pf.transformation.sedproxy.controller
// Varianta din 20.11; in 24.11 a fost aratata o varianta mai noua, care nu e transcrisa aici.

public class SedProxyListener {
    /* ...campurile de pana la linia 66 nu erau in poza */
    private static final String ERROR_MESSAGE = "Error_message_";

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage orchestrationMessage, @Header(CORRELATION_HEADER) String correlation) {
        log.debug("Received orchestration message {} ", orchestrationMessage);
        ProcessingContext processingContext = new ProcessingContext();
        processingContext.setIndex(new AtomicInteger(1));

        try{
            generalNullOrEmptyCheckService.correlationEmptyCheck(processingContext, correlation, proxyName, orchestrationMessage);
            generalNullOrEmptyCheckService.applicationEmptyCheck(processingContext);
            sedNullOrEmptyCheckService.businessKeyFlowIdEmptyCheck(orchestrationMessage, processingContext, proxyName);
            ckiNullOrEmptyCheckService.loanCheckProcessor(processingContext);

            Signal statusMessage = processingContext.getStatusMessage();
            Map<String, Signal> errorSignalMap = processingContext.getErrorSignalMap();
            List<Error> errors = processingContext.getErrorHandler().getErrors();

            if (CollectionUtils.isEmpty(errors) && CollectionUtils.isEmpty(errorSignalMap)) {
                applicationService.updateApplicationStatus(processingContext.getBusinessKey(), OK);
                LocalDateTime newLocalDateTime = LocalDateTimeUtils.convertToNewLocalDateTime(orchestrationMessage);
                kafkaPublisher.publishToKafka(statusTopic, correlation, statusMessage, newLocalDateTime, version);
                if (processingContext.getFlowId().startsWith(ARVAL_NL_FLOW_ID_PREFIX)) {
                    ArvalSignal arvalStatusMessage = mapSignalToArvalSignal(statusMessage);
                    //to avoid UnsupportedOperationException
                    Map<CharSequence, CharSequence> propertiesCki = new HashMap<>(arvalStatusMessage.getProperties());
                    propertiesCki.put(SCOPE_KEY, SED_DATA_SCOPE);
                    arvalStatusMessage.setProperties(propertiesCki);
                    kafkaPublisher.arvalPublishToKafka(arvalStatusTopic, correlation, arvalStatusMessage, newLocalDateTime, version);
                }
            } else {
                applicationService.updateApplicationStatus(processingContext.getBusinessKey(), KO);

                //removing elements from a map is dangerous, use an Iterator to avoid ConcurrentModificationException
                Iterator<Map.Entry<String, Signal>> errorSignalIterator = errorSignalMap.entrySet().iterator();

                while (errorSignalIterator.hasNext()) {
                    Map.Entry<String, Signal> next = errorSignalIterator.next();
                    kafkaPublisher.publishToKafka(errorTopic, correlation, next.getValue(), LocalDateTime.now(), version);
                    errorSignalIterator.remove();
                }
            }
        } catch (Exception e){
//            throw new ProcessingFailedException(processingContext, e.getMessage(), e);
            Signal errorSignal = new Signal();
            errorSignal.setCommand(proxyName);
            errorSignal.setFlowId(processingContext.getFlowId());
            errorSignal.setBusinessKey(processingContext.getBusinessKey());
            Map<CharSequence,CharSequence> properties = new HashMap<>();
            properties.put(ERROR_MESSAGE + processingContext.getIndex().getAndIncrement(),e.getMessage());
            errorSignal.setProperties(properties);

            kafkaPublisher.publishToKafka(errorTopic, correlation, errorSignal, LocalDateTime.now(), version);
        }
    }

    private ArvalSignal mapSignalToArvalSignal(Signal statusMessage) {
        ArvalSignal arvalNlSignal = new ArvalSignal();
        arvalNlSignal.setProperties(statusMessage.getProperties());
        /* ...restul nu era in poza */
    }
}
