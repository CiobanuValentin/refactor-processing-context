# bkrvisproxy: migrarea pe common-api nou

Clasele din `src/` înlocuiesc clasele cu același nume din proxy. Pachetul e `com.bnp.pf.transformation.visproxy`.

## Ce se înlocuiește

| Clasă | Ce s-a schimbat |
|---|---|
| `controller/ProxyListener` | flux nou: `ProxyContext.of` → `validate` → `loanCheckProcess` → `publish`; dispar metodele `publishStatus`, `publishErrors`, `publishStatusAndErrors`, `hasErrors`, `errorsOf`, `eventTime` |
| `service/IntegrityCheckService` | primește doar `ProxyContext`; aceiași pași |
| `service/VisCheckService` | doar `ProxyContext` în loc de `ProcessingContext` |
| `service/VisCheckRecordService` | la fel; plus `FIX` în `fetchVisEntity` |
| `client/VisSoapClient` | fără `CollectError`; erorile merg în context |
| `config/general/ProxyObsBeansConfig` | nou: bean-urile din common |

Neschimbate: `VisCheckCacheService`, `BuildDTOsUtility`, `VisCheckResult`, entitatea, repository-ul, `BkrVisController`.

## Ce se șterge

- `kafka/KafkaPublisher` (cel local), în același commit;
- `config/general/CollectErrorConfig`, `config/general/GeneralNullOrEmptyCheckServiceConfig`.

## KafkaConfig

`errorHandler` rămâne doar cu reîncercările; nu mai are ce semnale să publice:

```java
@Bean
public DefaultErrorHandler errorHandler() {
    return new DefaultErrorHandler((ConsumerRecord<?, ?> rec, Exception ex) ->
            log.error("Giving up on Kafka message after retries. Topic = {}, partition = {}, offset = {}",
                    rec.topic(), rec.partition(), rec.offset(), ex),
            new FixedBackOff(interval, maxAttempts));
}
```

## Ce rămâne la fel ca în cloud

- Textul erorilor de pe topicul de error: `VIS Returned an error: <cod>:<mesaj>` și `Error when executing VIS external SOAP call vis(): <mesaj>`.
- Pe rândul din `CHECK_VIS_ENTITY` se salvează codul și mesajul brut.
- Răspunsurile cu erori nu se pun în cache.

## Diferențe față de legacy, existente deja în cloud (nemodificate)

- Legacy publica mesajul brut de la VIS; cloud adaugă prefixul de mai sus.
- Legacy răspundea OK când nu existau clienți; cloud dă eroare dacă lipsesc și clienții, și companiile, și dacă loan-ul e null.
- Cloud cere și data nașterii ca să apeleze VIS; legacy nu o verifica.
- Cloud salvează aplicația în `APPLICATION_REQUEST` înainte de verificare.

## Endpointul pentru rat și sed

Vis e deja pe listă: `GET /getVisEntities?businessKey=&flowId=` întoarce `List<GeneralProxyObsDto>`. Nu mai e nimic de adăugat aici.

- Endpointul vechi, `/getVisEntity` (un singur obiect), nu mai există pe acest branch. Rat și sed de pe common-ul vechi îl cer încă (`bkrvis.credit-endpoint: "/getVisEntity"`) și citesc un singur obiect, deci nu merg cu acest vis.
- Când rat și sed trec pe common-ul nou, în `application.yml`-ul lor: `bkrvis.credit-endpoint: "/getVisEntities"`.
- Până atunci, fie vis se livrează împreună cu rat și sed, fie se păstrează temporar și endpointul vechi.

## De verificat

1. Importurile marcate `TODO`.
2. Testele existente (`ProxyListenerTest`) folosesc `ProcessingContext` și trebuie rescrise.
