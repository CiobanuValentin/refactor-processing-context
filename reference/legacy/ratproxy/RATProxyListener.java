// REFERINTA - cod existent (legacy, proxyobs master, ratproxy), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.ratproxy.controller;

@Component
@Slf4j
public class RATProxyListener {

    private static final String CORRELATION_HEADER = "X-Correlation";
    private static final String STATUS = "STATUS";
    private static final String OK = "OK";
    private static final String KO = "KO";
    private static final String ERROR_MESSAGE = "Error_message_";
    private static final String PROXY_NAME = "RAT";

    private final ApplicationRepository applicationRepository;
    private final RatMappingService mappingService;
    private final Map<String, ProducerService> producerServices;
    private final ApplicationBuilder applicationBuilder;

    @Autowired
    private CheckLoanSanctionDtoRepository loanSanctionRepository;

    @Value("${kafka.producers[0].topic}")
    private String statusTopic;

    @Value("${kafka.producers[1].topic}")
    private String errorTopic;

    @Value("${app.version:unknown}")
    private String version;

    public RATProxyListener(ApplicationRepository applicationRepository, RatMappingService mappingService, Map<String, ProducerService> producerServices, ApplicationBuilder applicationBuilder) {
        this.applicationRepository = applicationRepository;
        this.mappingService = mappingService;
        this.producerServices = producerServices;
        this.applicationBuilder = applicationBuilder;
    }

    @KafkaListener(topics = "${kafka.consumers[0].topic}", containerFactory = "#{kafkaListenerContainerFactory['${kafka.consumers[0].topic}']}")
    public void consume(@Payload OrchestrationMessage orchMessage, @Header(CORRELATION_HEADER) String correlation) {
        log.info("Received signal from Camunda {}", orchMessage);

        var signal = orchMessage == null ? null : orchMessage.getData();
        var index = new AtomicInteger(1);
        var errorFlag = false;

        var statusMessage = new Signal();
        var errorMessage = new Signal();

        statusMessage.setCommand(PROXY_NAME);
        errorMessage.setCommand(PROXY_NAME);

        Map<CharSequence, CharSequence> statusResponseParameters = new HashMap<>();
        Map<CharSequence, CharSequence> errorResponseParameters = new HashMap<>();

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

            if (StringUtil.isNullOrEmpty(flowId)) {
                message = "Flow ID is empty";
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
                log.error(message);
                errorFlag = true;
            } else {
                statusMessage.setFlowId(flowId);
                errorMessage.setFlowId(flowId);
            }

            if (StringUtil.isNullOrEmpty(businessKey)) {
                message = "Business key is empty";
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), message);
                log.error(message);
                errorFlag = true;
            } else {
                statusMessage.setBusinessKey(businessKey);
                errorMessage.setBusinessKey(businessKey);
                if (!(StringUtil.isNullOrEmpty(businessKey) || StringUtil.isNullOrEmpty(flowId))) {
                    application = Optional.ofNullable(applicationBuilder.buildApplication(convertToString(businessKey), convertToString(flowId)));
                    log.info("Application build for businesskey {}", businessKey);
                }
            }
        }

        if (application.isPresent() && !(StringUtil.isNullOrEmpty(businessKey) || StringUtil.isNullOrEmpty(flowId) || StringUtil.isNullOrEmpty(correlation) || errorFlag)) {
            log.info("Processing Application {}", businessKey);

            var found = application.get();

            //If both customers and companies lists are empty, we can't process
            var loan = found.getLoan();
            if (loan == null) {
                log.error("Application  with Business key {} is empty", businessKey);
                errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), "Application Loan is empty");
            } else {
                final var customers = loan.getCustomers();
                final var companies = loan.getCompanies();
                if ((customers == null || customers.isEmpty()) && (companies == null || companies.isEmpty())) {
                    log.error("There are no customers or companies int the application with the business key {}", businessKey);
                    errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), "There are no customers or companies in the application");
                } else {
                    try {
                        final Application cloneApplication = (Application) deepClone(found);
                        mappingService.processRatSanctionDataAsync(found, convertToString(flowId));
                        applicationRepository.save(mappingService.getScoreResultAsync(cloneApplication));

                        statusResponseParameters.put(STATUS, OK);
                    } catch (Exception e) {
                        log.error("RAT External API replied with error message for Business Key " + businessKey + " and flow ID " + flowId + ": message: " + e.getMessage());
                        errorResponseParameters.put(ERROR_MESSAGE + index.getAndIncrement(), e.getMessage());

                        var checkLoanSanctionDto = new CheckLoanSanctionDto();
                        checkLoanSanctionDto.setRequest(LoanMapping.map(found, true));
                        checkLoanSanctionDto.setBusinessKey(businessKey);
                        checkLoanSanctionDto.setFlowId(flowId);
                        checkLoanSanctionDto.setTime(LocalDateTime.now());
                        checkLoanSanctionDto.setErrors(addErrors(e));
                        checkLoanSanctionDto.setResponse(false);
                        loanSanctionRepository.save(checkLoanSanctionDto);
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

        publishMessage(statusTopic, correlation, statusMessage, statusResponseParameters, orchMessage != null ? LocalDateTime.parse(orchMessage.getMetadata().getEventTimeIso8601(), format) : LocalDateTi/* ...taiat */);
    }

    private ErrorHandler addErrors(Exception e) {
        var errors = new ErrorHandler();
        errors.addError(e);
        return errors;
    }

    public Object deepClone(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            log.error("Exception while doing deepClone : {}", e.getMessage());
            return null;
        }
    }

    private void publishMessage(String topicName, String correlation, Signal response, Map<CharSequence, CharSequence> responseParameters, LocalDateTime creation) {
        response.setCommand(PROXY_NAME);
        response.setProperties(responseParameters);

        OrchestrationMessage messageToSend = new OrchestrationMessage();
        messageToSend.setData(response);
        addMetaData(messageToSend, PROXY_NAME, version, creation);

        log.info("Publishing in topic {} the message {}", topicName, messageToSend);

        producerServices.get(topicName).produce(messageToSend, Collections.singletonMap(CORRELATION_HEADER, correlation), addKey(response.getFlowId()));
    }
}
