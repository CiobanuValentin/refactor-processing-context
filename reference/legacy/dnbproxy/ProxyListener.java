// REFERINTA - cod existent (legacy, proxyobs master), transcris din poze. Nu se compileaza si poate contine
// erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile si campurile nu erau vizibile.
// com.bnp.pf.transformation.dnbproxy.controller

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage orchestrationMessage, @Header(CORRELATION_HEADER) String correlation) {
        log.debug("Received signal from Camunda {}", orchestrationMessage);

        var signal = orchestrationMessage == null ? null : orchestrationMessage.getData();

        var index = new AtomicInteger(1);
        var errorFlag = false;

        var statusMessage = new Signal();
        var errorMessage = new Signal();

        statusMessage.setCommand(PROXY_NAME);
        errorMessage.setCommand(PROXY_NAME);

        var statusResponseParameters = new HashMap<~>();
        var errorResponseParameters = new HashMap<~>();

        Optional<Application> application = Optional.empty();

        String message;

        //DEFAULT STATUS RESPONSE UNLESS REPLACED BY STATUS OK BELOW
        statusResponseParameters.put(STATUS, KO);

        String businessKey = null;
        String flowId = null;

        if (correlation == null) {
            message = "Correlation ID not provided";
            errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
            log.error(message);
            errorFlag = true;
        }

        if (signal == null) {
            message = "Orchestration message is empty";
            errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
            log.error(message);
            errorFlag = true;
        } else {
            businessKey = convertToString(signal.getBusinessKey());
            flowId = convertToString(signal.getFlowId());

            if (businessKey == null) {
                message = "Business key is empty";
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
                log.error(message);
                errorFlag = true;
            } else {
                statusMessage.setBusinessKey(businessKey);
                errorMessage.setBusinessKey(businessKey);
                application = applicationRepository.findById(businessKey);
            }
            var command = convertToString(signal.getCommand());
            if (dnb.equalsIgnoreCase(command) || eventindicator.equalsIgnoreCase(command) || dnb_eventindicator.equalsIgnoreCase(command) || eventindicator_dnb.equalsIgnoreCase(com/* ...taiat */)) {
                log.debug("{} is a Valid command from CAMUNDA", command);
            } else {
                message = command + " is Not a valid command from CAMUNDA, valid commands are dnb, eventindicator, dnb_eventindicator, eventindicator_dnb";
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
                log.error(message);
                errorFlag = true;
            }

            if (StringUtil.isNullOrEmpty(flowId)) {
                message = "Flow ID is empty";
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
                log.error(message);
                errorFlag = true;
            } else {
                statusMessage.setFlowId(flowId);
                errorMessage.setFlowId(flowId);
            }
        }

        if (application.isPresent() && !(StringUtil.isNullOrEmpty(businessKey) || StringUtil.isNullOrEmpty(flowId) || StringUtil.isNullOrEmpty(correlation) || errorFlag)) {
            log.info("Processing Application {}", businessKey);

            var found = application.get();

            if (found.getLoan() == null) {
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), "Application Loan is empty");
            } else {
                //If both customers and companies lists are empty, we can't process
                if (found.getLoan().getCompanies() == null || found.getLoan().getCompanies().isEmpty()) {
                    //When the customers list AND the companies list is empty, the proxy will just skip by returning an OK in order to not block the scoring process
                    log.warn("There are no companies in the application {}", businessKey);
                    statusResponseParameters.put(STATUS, OK);
                } else {
                    List<ErrorHandler> errorHandlerList = new ArrayList<>();
                    if (dnb.equalsIgnoreCase(convertToString(signal.getCommand()))) {
                        errorHandlerList = processDnb(found, flowId);
                    } else if (eventindicator.equalsIgnoreCase(convertToString(signal.getCommand()))) {
                        errorHandlerList = processEventIndicator(found, flowId);
                    } else if (dnb_eventindicator.equalsIgnoreCase(convertToString(signal.getCommand()))) {
                        errorHandlerList = processDnbAndEventIndicator(found, flowId);
                    } else if (eventindicator_dnb.equalsIgnoreCase(convertToString(signal.getCommand()))) {
                        errorHandlerList = processEventIndicatorAndDnb(found, flowId);
                    }

                    if (errorHandlerList.isEmpty()) {
                        statusResponseParameters.put(STATUS, OK);
                    } else {
                        for (var errors : errorHandlerList) {
                            for (var error : errors.getErrors()) {
                                log.error("External API replied with error message for business Key " + businessKey + " and flow ID " + flowId + ": Code: " + error.getCode() + " / message: " /* ...taiat */);
                                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), error.getErrorMessage());
                            }
                        }
                    }
                }
            }
        } else {
            if (application.isEmpty() && !(StringUtil.isNullOrEmpty(businessKey) || StringUtil.isNullOrEmpty(flowId) || StringUtil.isNullOrEmpty(correlation))) {
                log.error("Application with Business key {} not found", businessKey);
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), "Application not found");
            }
        }

        if (statusResponseParameters.containsValue(KO)) {
            publishMessage(errorTopic, correlation, errorMessage, errorResponseParameters, LocalDateTime.now());
        }

        publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, orchestrationMessage != null ? LocalDateTime.parse(orchestrationMessage.getMetadata().getEventTimeIso8601(), f/* ...taiat */);
    }

    private List<ErrorHandler> processDnb(Application found, String flowId) {
        return mappingService.checkCompany(found, flowId);
    }

    private List<ErrorHandler> processDnbAndEventIndicator(Application found, String flowId) {
        processDnb(found, flowId);

        return processEventIndicator(found, flowId);
    }

    private List<ErrorHandler> processEventIndicatorAndDnb(Application found, String flowId) {
        var eventIndicator = processEventIndicator(found, flowId);
        if (eventIndicator.isEmpty()) {
            return processDnb(found, flowId);
        }
        return eventIndicator;
    }

    private List<ErrorHandler> processEventIndicator(Application found, String flowId) {
        return mappingService.checkEventIndicator(found, flowId);
    }

    private void publishMessage(String topicName, String correlation, Signal response, Map<CharSequence, CharSequence> responseParameters, LocalDateTime creation) {
        response.setCommand(PROXY_NAME);
        response.setProperties(responseParameters);

        var messageToSend = new OrchestrationMessage();
        messageToSend.setData(response);
        addMetaData(messageToSend, PROXY_NAME, version, creation);

        log.debug("Publishing in topic {} the message {}", topicName, messageToSend);
        producerServices.get(topicName).produce(messageToSend, Collections.singletonMap(CORRELATION_HEADER, correlation), addKey(response.getFlowId()));
    }
}
