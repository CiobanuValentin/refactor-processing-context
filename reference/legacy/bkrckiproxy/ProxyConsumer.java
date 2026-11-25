// REFERINTA - cod existent (legacy, proxyobs master), transcris din poze. Nu se compileaza si poate contine
// erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile si campurile nu erau vizibile.
// com.bnp.pf.transformation.ckiproxy.consumer

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage signal, @Header(CORRELATION_HEADER) String correlation) {
        log.debug("Received orchestration message {} ", signal);
        var orchMessage = signal == null ? null : signal.getData();
        var index = new AtomicInteger(1);
        var errorMessage = new Signal();
        var statusMessage = new Signal();
        var businessKey = orchMessage != null ? orchMessage.getBusinessKey() : null;
        var flowId = orchMessage != null ? orchMessage.getFlowId() : null;
        var creation = signal != null && signal.getMetadata() != null ? LocalDateTime.parse(signal.getMetadata().getEventTimeIso8601(), format) : LocalDateTime.now();

        Map<CharSequence, CharSequence> statusResponseParameters = new HashMap<>();
        Map<CharSequence, CharSequence> errorResponseParameters = new HashMap<>();


        if (orchestrationMessageService.checkDataMessage(PROXY_NAME, correlation, orchMessage, index, errorMessage, businessKey, flowId, creation, statusResponseParameters, errorResponseParameters))
            return;

        statusMessage.setBusinessKey(businessKey);
        errorMessage.setBusinessKey(businessKey);
        statusMessage.setFlowId(flowId);
        errorMessage.setFlowId(flowId);

        Optional<Application> application = getApplicationPresent(index, businessKey, statusResponseParameters, errorResponseParameters);

        if (application.isPresent()) {
            log.debug("Found the Application for the business key {} ", application.get().getBusinessKey());
            var ckiErrors = mappingService.checkCki(application.get(), convertToString(flowId));
            if (hasError(ckiErrors)) {
                for (var error : ckiErrors) {
                    if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                        for (var errorDetail : error.getErrors()) {
                            errorResponseParameters.put(addErrorCount(index), errorDetail.getErrorMessage());
                            log.error("CKI Proxy - External API error - Code : {}, message {}", errorDetail.getCode(), errorDetail.getErrorMessage());
                        }
                    }
                }
                statusResponseParameters.put(STATUS, NOT_OK);
            } else
                statusResponseParameters.put(STATUS, OK);
        }
        orchestrationMessageService.publishMessage(PROXY_NAME, statusTopic, correlation, orchMessage, statusResponseParameters, creation);

        if (statusResponseParameters.containsValue(NOT_OK)) {
            orchestrationMessageService.publishMessage(PROXY_NAME, errorTopic, correlation, errorMessage, errorResponseParameters, creation);
        }
    }

    private String addErrorCount(AtomicInteger sequInteger) { return ERROR_MESSAGE + sequInteger.getAndIncrement(); }

    private boolean hasError(List<ErrorHandler> requestsAndResponses) {
        return requestsAndResponses.stream().anyMatch(error -> error.getErrors() != null && !error.getErrors().isEmpty());
    }

    // getApplicationPresent(...): identica cu cea din rvdrproxy
}
