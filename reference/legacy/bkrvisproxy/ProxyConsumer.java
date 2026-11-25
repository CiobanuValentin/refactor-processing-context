// REFERINTA - cod existent (legacy, proxyobs master), transcris din poze. Nu se compileaza si poate contine
// erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile si campurile nu erau vizibile.
// com.bnp.pf.transformation.visproxy.consumer

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage signal, @Header(CORRELATION_HEADER) String correlation) {
        log.debug("Received orchestration message {} ", signal);
        var orchMessage = signal == null ? null : signal.getData();
        var sequInteger = new AtomicInteger(1);
        var errorMessage = new Signal();
        var statusMessage = new Signal();
        Map<CharSequence, CharSequence> statusResponseParameters = new HashMap<>();
        Map<CharSequence, CharSequence> errorResponseParameters = new HashMap<>();
        Optional<Application> application;

        if (Objects.isNull(correlation)) {
            errorResponseParameters.put(addErrorCount(sequInteger), CORR_ID_EMPTY);
            log.error(CORR_ID_EMPTY);
            publishMessage(errorTopic, null, errorMessage, errorResponseParameters, LocalDateTime.now());
            return;
        }

        if (Objects.isNull(orchMessage)) {
            errorResponseParameters.put(addErrorCount(sequInteger), ORCH_MSG_EMPTY);
            log.error(ORCH_MSG_EMPTY);
            statusResponseParameters.put(STATUS, NOT_OK);
            publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, LocalDateTime.now());
            publishMessage(errorTopic, correlation, errorMessage, errorResponseParameters, LocalDateTime.now());
            return;
        }
        log.debug("Process started for the business key {} ", orchMessage.getBusinessKey());
        var businessKey = orchMessage.getBusinessKey();
        var flowId = orchMessage.getFlowId();
        var creation = signal.getMetadata() != null ? LocalDateTime.parse(signal.getMetadata().getEventTimeIso8601(), format) : LocalDateTime.now();

        if (Objects.isNull(businessKey)) {
            errorResponseParameters.put(addErrorCount(sequInteger), BUSS_KEY_EMPTY);
            log.error(BUSS_KEY_EMPTY);
            statusResponseParameters.put(STATUS, NOT_OK);
            publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, creation);
            publishMessage(errorTopic, correlation, errorMessage, errorResponseParameters, creation);
            return;
        }

        if (Objects.isNull(flowId)) {
            errorResponseParameters.put(addErrorCount(sequInteger), FLOW_ID_EMPTY);
            log.error(FLOW_ID_EMPTY);
            statusResponseParameters.put(STATUS, NOT_OK);
            publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, creation);
            publishMessage(errorTopic, correlation, errorMessage, errorResponseParameters, creation);
            return;
        }

        statusMessage.setBusinessKey(businessKey);
        errorMessage.setBusinessKey(businessKey);
        statusMessage.setFlowId(flowId);
        errorMessage.setFlowId(flowId);
        application = applicationRepository.findById(convertToString(businessKey));

        if (application.isPresent()) {
            var visErrors = mappingService.checkVis(application.get(), convertToString(flowId));
            if (hasError(visErrors)) {
                for (var error : visErrors) {
                    if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                        for (var errorDetail : error.getErrors()) {
                            errorResponseParameters.put(addErrorCount(sequInteger), errorDetail.getErrorMessage());
                            log.error("External API error - Code : {}, message {}", errorDetail.getCode(), errorDetail.getErrorMessage());
                        }
                    }
                }
                statusResponseParameters.put(STATUS, NOT_OK);
            } else
                statusResponseParameters.put(STATUS, OK);
        } else {
            log.error("Application for the Business key {} not found", businessKey);
            statusResponseParameters.put(STATUS, NOT_OK);
            errorResponseParameters.put(addErrorCount(sequInteger), APP_NOT_FOUND);
        }
        log.debug("Publishing the message for the business key {} ", businessKey);

        publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, creation);

        if (statusResponseParameters.containsValue(NOT_OK)) {
            publishMessage(errorTopic, correlation, errorMessage, errorResponseParameters, creation);
        }

        log.debug("Process completed for the business key {} ", businessKey);
    }

    private String addErrorCount(AtomicInteger sequInteger) { return ERROR_MESSAGE + sequInteger.getAndIncrement(); }

    private void publishMessage(String topicName, String correlation, Signal response, Map<CharSequence, CharSequence> responseParameters, LocalDateTime creation) {
        response.setCommand(COMMAND);
        response.setProperties(responseParameters);

        OrchestrationMessage messageToSend = new OrchestrationMessage();
        messageToSend.setData(response);
        addMetaData(messageToSend, COMMAND, version, creation);

        log.debug("Publishing in topic {} the message {}", topicName, messageToSend);
        producerServices.get(topicName).produce(messageToSend, Collections.singletonMap(CORRELATION_HEADER, correlation), addKey(response.getFlowId()));
    }

    private boolean hasError(List<ErrorHandler> requestsAndResponses) {
        return requestsAndResponses.stream().anyMatch(error -> error.getErrors() != null && !error.getErrors().isEmpty());
    }
}
