// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// Copia din sedproxy (com.bnpp.pf.transformation.sedproxy.kafka); aceeasi clasa exista in fiecare proxy.

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.CORRELATION_HEADER;
import static com.bnp.pf.transformation.commons.api.utils.OrchestrationMetaDataHandler.addKey;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaPublisher {

    @Value("${proxy-name}")
    private String proxyName;

    private final Map<String, ProducerService> producerServices;

    public void publishToKafka(String topicName, String correlation, Signal response, LocalDateTime creation, String version) {

        //in case we have same values in properties map of response
        Map<CharSequence, CharSequence> propertiesMap = response.getProperties();
        Set<CharSequence> uniques = new HashSet<>(propertiesMap.values());
        List<CharSequence> duplicates = propertiesMap.values().stream().toList();

        if (uniques.size() != duplicates.size()) {
            Map<CharSequence, CharSequence> tmpReverseMap = new HashMap<>();
            Map<CharSequence, CharSequence> result = new HashMap<>();

            propertiesMap.keySet().forEach(key -> tmpReverseMap.put(propertiesMap.get(key), key));
            tmpReverseMap.keySet().forEach(key -> result.put(tmpReverseMap.get(key), key));
            response.setProperties(result);
        }

        OrchestrationMessage messageToSend = new OrchestrationMessage();
        messageToSend.setData(response);
        OrchestrationMetaDataHandler.addMetaData(messageToSend, proxyName, version, creation);

        log.info("Publishing in topic {} the message {}", topicName, messageToSend);
        producerServices.get(topicName).produce(messageToSend, Collections.singletonMap(CORRELATION_HEADER, correlation), addKey(response.getFlowId()));
    }

    // arvalPublishToKafka(String topicName, String correlation, ArvalSignal response, LocalDateTime creation,
    //     String version): identica, cu ArvalOrchestrationMessage si OrchestrationMetaDataHandler.arvalAddMetaData
}
