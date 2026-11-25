// REFERINTA - cod existent (legacy, proxyobs master), transcris din poze. Nu se compileaza si poate contine
// erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile si campurile nu erau vizibile.
// com.bnp.pf.transformation.rvdrproxy.controller

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

        //DEFAULT STATUS RESPONSE UNLESS REPLACED BY STATUS OK BELOW
        statusResponseParameters.put(STATUS, NOT_OK);

        Optional<Application> application = getApplicationPresent(index, businessKey, statusResponseParameters, errorResponseParameters);

        if (application.isPresent()) {
            log.info("Processing Application {}", businessKey);

            var found = application.get();

            if (found.getLoan() == null) {
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), "Application Loan is empty");
            } else {
                //If both customers and companies lists are empty, we can't process
                if (found.getLoan().getCustomers() == null || found.getLoan().getCustomers().isEmpty()) {
                    //When the customers list AND the companies list is empty, the proxy will just skip by returning an OK in order to not block the scoring process
                    log.warn("There are no customers in the application {}", businessKey);
                    statusResponseParameters.put(STATUS, OK);
                } else {
                    var checkCreditErrors = mappingService.checkCredit(found, flowId != null ? flowId.toString() : null);

                    if (checkCreditErrors.isEmpty()) {
                        statusResponseParameters.put(STATUS, OK);
                    } else {
                        for (var errors : checkCreditErrors) {
                            for (var error : errors.getErrors()) {
                                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), error.getErrorMessage());
                            }
                        }
                    }
                }
            }
        }
        orchestrationMessageService.publishMessage(PROXY_NAME, statusTopic, correlation, orchMessage, statusResponseParameters, creation);

        if (statusResponseParameters.containsValue(NOT_OK)) {
            orchestrationMessageService.publishMessage(PROXY_NAME, errorTopic, correlation, errorMessage, errorResponseParameters, creation);
        }
    }

    private Optional<Application> getApplicationPresent(AtomicInteger sequInteger, CharSequence businessKey, Map<CharSequence, CharSequence> statusResponseParameters, Map<CharSequence, CharSequence> err/* ...taiat */) {
        Optional<Application> application = applicationRepository.findById(convertToString(businessKey));

        if (application.isEmpty()) {
            log.error("Application for the Business key {} not found", businessKey);
            statusResponseParameters.put(STATUS, NOT_OK);
            errorResponseParameters.put(addErrorCount(sequInteger), APP_NOT_FOUND);
        }
        return application;
    }
}
