# rvdrproxy pe common-api nou

Sursa: proiectul `rvdrproxy`, branch `feature/proxy-refactorization-2.0` (pozele din 27 nov). Modelul este `bkrvisproxy/MIGRATION.md`; fluxul din listener este identic.

## Ce se face cu fiecare fișier

`src/main/java/com/bnp/pf/transformation/rvdrproxy/`

| Fișier | Ce se face |
|---|---|
| `client/SoapClient` | **înlocuit**: fără `CollectError`; erorile merg în `ProxyContext` |
| `config/general/ApplicationServiceConfig` | neschimbat (același constructor în common nou; nu e în acest folder) |
| `config/general/CachingServiceConfig`, `GsonConfig` | neschimbate (nu sunt în acest folder) |
| `config/general/CollectErrorConfig` | **șters** |
| `config/general/GeneralNullOrEmptyCheckServiceConfig` | **șters** |
| `config/general/KafkaConfig` | **înlocuit**: `errorHandler` doar cu reîncercări |
| `config/general/ProxyObsBeansConfig` | **nou**: bean-urile din common |
| `config/MappingConfig`, `config/SoapClientConfig` | neschimbate (nevăzute) |
| `controller/ProxyListener` | **înlocuit**: fluxul nou |
| `controller/RvdrController` | **înlocuit**: `/checkCredit` întoarce `List<GeneralProxyObsDto>` |
| `kafka/KafkaPublisher` | **șters** (se folosește cel din common) |
| `model/CreditCheckResult` | neschimbat |
| `repository/CreditCheckRepository` | **înlocuit**: `findAllBy...` în loc de `findTopBy...` |
| `service/IntegrityCheckService` | **înlocuit**: doar `ProxyContext` |
| `service/CreditCheckCacheService` | neschimbat (nu e în acest folder) |
| `service/CreditCheckRecordService` | **înlocuit**: `ProxyContext`; `fetchCheckCredits` întoarce toate rândurile |
| `service/CreditCheckService` | **înlocuit**: `ProxyContext`; logurile cu date de client la `debug` |
| `entity/`, `utils/BuildDTOsUtility` | neschimbate (nevăzute) |

`src/test/java/rvdrproxy/` (pachetul de test e presupus, după cel din vis)

| Fișier | Stare |
|---|---|
| `builder/ContextBuilder` | nou |
| `controller/ProxyListenerTest`, `controller/RvdrControllerTest` | scrise după modelul din vis |
| `service/IntegrityCheckServiceTest` | scris |
| `client/SoapClientTest` | scris; lipsesc importurile WSDL |
| `service/CreditCheckServiceTest`, `CreditCheckRecordServiceTest` | **de adaptat** din cele existente: `ProxyContext` din `ContextBuilder`; `fetchCheckCredit` devine `fetchCheckCredits` (listă) |
| `service/CreditCheckCacheServiceTest` | neschimbat |

Textele de pe topicul de error sunt cele din cloud: `CreditCheck returned an error: <cod>:<mesaj>` și `Error when executing external SOAP call creditCheckRequest(): <mesaj>`.

## Diferențe față de cloud

1. Endpoint-ul întoarce listă; fără rânduri întoarce listă goală, nu un dto gol.
2. Excepțiile neașteptate dau status KO + eroare, fără `ProcessingFailedException`.
3. Cererea și răspunsul SOAP se loghează la `debug`, ca în vis.

## Comparație cu legacy (`MappingService.checkCredit`)

La fel: se sare peste U0x; un rând salvat pentru fiecare client verificat, și la răspuns din cache; SOAP fault dă cod `502`.

Diferențe care există deja în cloud și au rămas neatinse:

1. Legacy salva răspunsul doar dacă avea `result`; cloud salvează orice răspuns primit.
2. Legacy prindea `ConnectException_Exception` și `SOAPFaultException`; cloud prinde orice excepție (cod `555`).
3. Condiția de cache din legacy are un al treilea termen tăiat în poză; cloud folosește răspunsul din cache dacă cererea e identică.

## De verificat

1. `CreditCheckEntity`: dacă `flowId` are `unique = true` (ca `CheckVisEntity`), al doilea client de pe același flow nu se poate salva.
2. Testele existente pentru `CreditCheckService` și `CreditCheckRecordService`.
3. Nimic nu a fost compilat.
