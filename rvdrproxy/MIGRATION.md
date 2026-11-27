# rvdrproxy pe common-api nou

Sursa: proiectul `rvdrproxy`, branch `feature/proxy-refactorization-2.0` (pozele din 27 nov). Modelul este `bkrvisproxy/MIGRATION.md`; fluxul din listener este identic.

## Ce se face cu fiecare fișier

| Fișier | Ce se face | Stare aici |
|---|---|---|
| `controller/ProxyListener` | **înlocuit**: fluxul nou (context, validator, procesare, status, publish) | scris |
| `controller/RvdrController` | **înlocuit**: `/checkCredit` întoarce `List<GeneralProxyObsDto>` | scris |
| `service/IntegrityCheckService` | **înlocuit**: doar `ProxyContext` | scris |
| `service/CreditCheckService` | **înlocuit**: `ProxyContext`; logurile cu date de client la `debug` | scris |
| `service/CreditCheckRecordService` | **înlocuit**: `ProxyContext`; `fetchCheckCredits` întoarce toate rândurile | scris |
| `service/CreditCheckCacheService` | neschimbat | - |
| `repository/CreditCheckRepository` | **de adăugat** `List<CreditCheckEntity> findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(String businessKey, String flowId)` | de făcut de mână |
| `client/SoapClient` | **de înlocuit**: fără `CollectError`; erorile în `ProxyContext` | **lipsește poza** |
| `config/general/ProxyObsBeansConfig` | **nou**: bean-urile din common | scris |
| `config/…/CollectErrorConfig`, `GeneralNullOrEmptyCheckServiceConfig` | **șterse** (dacă există) | lipsește poza cu `config/` |
| `config/…/KafkaConfig` | `errorHandler` doar cu reîncercări (vezi `snippets/proxy-config/KafkaConfig-errorHandler.java`) | lipsește poza |
| `kafka/KafkaPublisher` | **șters** (se folosește cel din common) | - |
| `model/CreditCheckResult`, `entity/`, `utils/BuildDTOsUtility` | neschimbate | nevăzute |
| teste: `builder/ContextBuilder`, `controller/ProxyListenerTest`, `controller/RvdrControllerTest`, `service/IntegrityCheckServiceTest` | după modelul din vis; pachetul de test `rvdrproxy.*` e presupus | scrise, necompilate |
| teste: `CreditCheckServiceTest`, `CreditCheckRecordServiceTest`, `CreditCheckCacheServiceTest`, `SoapClientTest` | de adaptat după ce se văd cele existente | lipsesc pozele |

## Diferențe față de cloud

1. Endpoint-ul întoarce listă; fără rânduri întoarce listă goală, nu un dto gol.
2. Excepțiile neașteptate dau status KO + eroare, fără `ProcessingFailedException`.
3. Cererea și răspunsul SOAP se loghează la `debug`, ca în vis.

## Ce mai lipsește (poze)

1. `client/SoapClient` (întreg).
2. Folderul `config/` (lista claselor și `KafkaConfig`).
3. `model/CreditCheckResult`, `repository/CreditCheckRepository`.
4. Lista testelor din `src/test`.
5. Legacy: `MappingService.checkCredit` din rvdrproxy, pentru comparație.
