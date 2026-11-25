# common-api: ce se șterge, ce se adaugă, ce e echivalent cu ce

Clasele vechi se șterg direct, fără `@Deprecated`. Ordinea de livrare: întâi common-api, apoi proxy-urile migrează pe rând.

## De șters din common-api

| Clasă | Înlocuită de |
|---|---|
| `ProcessingContext`, `ProcessingContextBase`, `ArvalProcessingContext` | `ProxyContext` |
| `CollectError`, `KafkaErrorCollector` | `ProxyContext.addError` + `ProxyResultPublisher` |
| `GeneralNullOrEmptyCheckService` | `ProxyContextValidator` |
| `ProcessingFailedException` și handlerul care apelează `findIn` | nimic: listenerul răspunde KO plus error |
| `PartialDataFetcher`, `PartialDataFetcherPG` | `ProxyResultsFetcher` |
| testele claselor de mai sus | testele din `src/test` |

Rămân neatinse: `SCALMapper`, `DNBMapper`, `RVDRMapper`, `EventIndicatorMapper`, `BKRVISMapper`, `BKRCommons`, `ProxyDtoBuilder`, `GeneralProxyObsDto`, toate `Check*Dto`, `ErrorHandler`, `Error`, `ProxyObsConstant`, `GsonInstance`, `LocalDateTimeUtils`, `OrchestrationMetaDataHandler`.

## Echivalențe, metodă cu metodă

| Vechi | Nou |
|---|---|
| `new ProcessingContext()` + `correlationEmptyCheck(...)` (inițializarea) | `ProxyContext.of(message, correlation, proxyName)` |
| `correlationEmptyCheck` (verificarea), `applicationEmptyCheck`, `businessKeyFlowIdEmptyCheck` | `proxyContextValidator.validate(ctx)` |
| toate variantele `arval...` | aceleași metode; `ProxyContext.of(ArvalOrchestrationMessage, ...)` |
| `loanNotPresent(pc)` | `ctx.addError("400", LOAN_EMPTY)` |
| `collectError.addError(pc, code, msg)` / `addError(pc, e)` | `ctx.addError(code, msg)` |
| `pc.getErrorMap().put(ERROR_MESSAGE + index, msg)` + `collectError.addErrorSignal(pc, errorMessage)` | nimic: semnalul de error se construiește la publicare |
| `collectError.setStatusMessage(...)` | nimic: statusul se construiește la publicare |
| `collectError.removeDuplicateErrors(pc)` | publisher-ul trimite mesajele identice o singură dată |
| `pc.getErrorHandler()` | `ctx.getErrorHandler()` |
| `pc.getBusinessKey()`, `getFlowId()`, `getApplication()`, `getProducer()` | la fel pe `ctx` |
| `pc.getSignalProperties()` / `getApplicationJson()` | `ctx.getProperties()` / `ctx.property(APPLICATION)` |
| `kafkaPublisher.publishToKafka(statusTopic, ...)` + bucla pe `errorSignalMap` | `proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version)` |
| `throw new ProcessingFailedException(pc, ...)` | `ctx.addUnexpectedError(e)` în `catch`, apoi publicare |
| `fetcher.getCkiData(...)` și celelalte 9 | `proxyResultsFetcher.fetch(ctx, ProxySource.CKI)`, care întoarce `List<GeneralProxyObsDto>` |
| `proxyDtoBuilder.buildCheckCkiPersonDto(dto)` + `ckiMapper.mapCKIData(...)` | aceleași două apeluri, legate în `ProxyResultMapperConfig` |
| `applicationBuilderPG.buildApplication(pc)` (în sed și rat) | `applicationAggregator.enrich(ctx, SOURCES)` |

## ApplicationService

Clasa e rescrisă întreagă în `src/main`.

| Vechi | Nou |
|---|---|
| `saveApplicationRequest(application, message, applicationJson, pc)` | `saveApplicationRequest(ctx)` |
| `arvalSaveApplicationRequest(application, arvalMessage, applicationJson, apc)` | `saveApplicationRequest(ctx)` |
| `updateApplicationStatus(businessKey, status)` | neschimbată |
| - | `findSavedApplication(businessKey)` (nefolosită deocamdată; ținea de propunerea parcată în `pending/`) |

De unde vin acum valorile:

| Valoare | Vechi | Nou |
|---|---|---|
| JSON-ul | parametrul `applicationJson` | `gson.toJson(ctx.getApplication())` |
| status | `errors` și `errorSignalMap` goale | `ctx.hasErrors()` |
| `createdDate` | `convertToNewLocalDate(message)` | `ctx.getEventTime().toLocalDate()` |
| `createdBy` / `modifiedBy` | `message.getMetadata().getProducer()` | `ctx.getProducer()` |

Diferențe:

- Varianta Arval nu elimina `\\n` din JSON; acum se elimină la ambele.
- Fără metadata, cea veche arunca NPE; acum `createdBy` devine `"null"`, iar data e cea curentă.
- Entitatea nu mai e scrisă în loguri, doar business key-ul.

## Mapperele și dto-urile

Nu se modifică nimic: mapperele, `ProxyDtoBuilder`, `GeneralProxyObsDto` și dto-urile `Check*` rămân cum sunt. Contractul endpointurilor rămâne `GeneralProxyObsDto`, doar că se întoarce o listă. Vezi secțiunea „Mapperele existente" din [LOGIC.md](LOGIC.md) pentru punctele deschise.

## Ordinea de livrare

1. **common-api**: clasele noi, `ApplicationService` rescrisă, cele două metode de mapper, ștergerea claselor vechi. Un proxy nemigrat nu mai compilează cu versiunea nouă, deci rămâne pe cea veche până e migrat.
2. **Proxy-urile sursă** (cki, vis, eva, rvdr, scal, dnb, event indicator): listener nou, endpoint de listă, ștergerea `KafkaPublisher`-ului local.
3. **rat** și **sed**: au nevoie de endpointurile de listă din pasul 2.

Endpointul vechi, cu un singur rezultat, trebuie păstrat în fiecare proxy sursă până când rat și sed sunt amândouă migrate.
