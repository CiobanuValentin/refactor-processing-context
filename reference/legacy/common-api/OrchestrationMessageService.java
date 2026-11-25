// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// common-api-1.23.jar (legacy) - com.bnp.pf.transformation.commons.api.utils
// IDE-ul afisa "Library source does not match the bytecode", deci sursa poate diferi de ce ruleaza.

public class OrchestrationMessageService {

    public static void addMetaData(OrchestrationMessage message, String applicationName, String applicationVersion, LocalDateTime creation) {
        /* ...liniile 60-74 nu erau in poza */
        metaData.setEventTimeIso8601(LocalDateTime.now().format(format));

        message.setMetadata(metaData);
    }

    public static Key addKey(CharSequence keyString) {
        if (keyString == null || keyString.length() == 0) {
            return Key.newBuilder().setValue("000000").build();
        } else {
            return Key.newBuilder().setValue(keyString).build();
        }
    }

    private boolean checkData(String applicationName, @Header(CORRELATION_HEADER) String correlation, Signal orchMessage,
                              AtomicInteger sequInteger, Signal errorMessage, Map<CharSequence, CharSequence> statusResponseParameters, Map<CharSequence, CharSequence> errorResponseParameters,
                              LocalDateTime creation, boolean aNull, String bussKeyEmpty) {
        if (aNull) {
            errorResponseParameters.put(addErrorCount(sequInteger), bussKeyEmpty);
            log.error(bussKeyEmpty);
            if (!StringUtils.isEmpty(correlation)) {
                statusResponseParameters.put(STATUS, NOT_OK);
                publishMessage(applicationName, statusTopic, correlation, orchMessage, statusResponseParameters, creation);
            }
            publishMessage(applicationName, errorTopic, correlation, errorMessage, errorResponseParameters, creation);
            return true;
        }
        return false;
    }

    private String addErrorCount(AtomicInteger sequInteger) { return ERROR_MESSAGE + sequInteger.getAndIncrement(); }

    public void publishMessage(String applicationName, String topicName, String correlation, Signal response, Map<CharSequence, CharSequence> responseParameters, LocalDateTime creation) {
        response.setCommand(applicationName);
        response.setProperties(responseParameters);

        var messageToSend = new OrchestrationMessage();
        messageToSend.setData(response);
        addMetaData(messageToSend, applicationName, version, creation);

        log.debug("Publishing in topic {} the message {}", topicName, messageToSend);
        producerServices.get(topicName).produce(messageToSend, Collections.singletonMap(CORRELATION_HEADER, correlation), addKey(response.getFlowId()));
    }

    public boolean checkDataMessage(String applicationName, String correlation, Signal orchMessage, AtomicInteger sequInteger, Signal errorMessage, CharSequence businessKey, CharSequence flowId, LocalDat/* ...taiat: creation, statusResponseParameters, errorResponseParameters */) {
        if (checkData(applicationName, correlation, new Signal(), sequInteger, errorMessage, statusResponseParameters, errorResponseParameters, LocalDateTime.now(), Objects.isNull(correlation), CORR_ID_E/* ...taiat */)) {
            return true;
        }

        if (checkData(applicationName, correlation, new Signal(), sequInteger, errorMessage, statusResponseParameters, errorResponseParameters, LocalDateTime.now(), Objects.isNull(orchMessage), ORCH_MSG_/* ...taiat */)) {
            return true;
        }

        log.debug("Process started for the business key {} ", orchMessage.getBusinessKey());

        if (checkData(applicationName, correlation, orchMessage, sequInteger, errorMessage, statusResponseParameters, errorResponseParameters, creation, Objects.isNull(businessKey), BUSS_KEY_EMPTY)) {
            return true;
        }

        if (checkData(applicationName, correlation, orchMessage, sequInteger, errorMessage, statusResponseParameters, errorResponseParameters, creation, Objects.isNull(flowId), FLOW_ID_EMPTY)) {
            return true;
        }
        return false;
    }

    private Optional<Application> getApplicationPresent(AtomicInteger sequInteger, CharSequence businessKey, Map<CharSequence, CharSequence> statusResponseParameters, Map<CharSequence, CharSequence> erro/* ...taiat */) {
        Optional<Application> application;
        application = applicationRepository.findById(convertToString(businessKey));

        if (!application.isPresent()) {
            log.error("Application for the Business key {} not found", businessKey);
            statusResponseParameters.put(STATUS, NOT_OK);
            errorResponseParameters.put(addErrorCount(sequInteger), APP_NOT_FOUND);
        }
        /* ...restul nu era in poza */
    }
}
