# bkrvisproxy pe common-api nou (model pentru celelalte proxy-uri)

Sursa: proiectul `bkrvisproxy-cloud`, branch `feature/proxy-refactorization-2.0` (pozele din `~/Desktop/VisProxy`, 26 nov). Codul de aici are **aceeași structură de pachete și clase ca în cloud**; se copiază peste proiect fișier cu fișier, după tabelul de mai jos.

## 1. Structura și ce se face cu fiecare fișier

`src/main/java/com/bnp/pf/transformation/`

| Fișier | Ce se face |
|---|---|
| `VisProxyApplication` | neschimbat |
| `visproxy/client/VisSoapClient` | **înlocuit**: fără `CollectError`; erorile merg în `ProxyContext` |
| `visproxy/config/general/ApplicationServiceConfig` | neschimbat (același constructor în common nou) |
| `visproxy/config/general/CachingServiceConfig` | neschimbat |
| `visproxy/config/general/CollectErrorConfig` | **șters** |
| `visproxy/config/general/GeneralNullOrEmptyCheckServiceConfig` | **șters** |
| `visproxy/config/general/GsonConfig` | neschimbat |
| `visproxy/config/general/KafkaConfig` | **înlocuit**: `errorHandler` doar cu reîncercări |
| `visproxy/config/general/ProxyObsBeansConfig` | **nou**: bean-urile din common (`ProxyContextValidator`, `KafkaPublisher`, `ProxyResultPublisher`) |
| `visproxy/config/MappingConfig` | neschimbat (nu e în acest folder; conținutul nu era în poze) |
| `visproxy/config/SoapClientConfig` | neschimbat |
| `visproxy/controller/BkrVisController` | neschimbat |
| `visproxy/controller/ProxyListener` | **înlocuit**: fluxul nou |
| `visproxy/entity/CheckVisEntity` | neschimbat |
| `visproxy/kafka/KafkaPublisher` | **șters** (se folosește cel din common) |
| `visproxy/model/VisCheckResult` | neschimbat |
| `visproxy/repository/CheckVisEntityRepository` | neschimbat |
| `visproxy/service/IntegrityCheckService` | **înlocuit**: doar `ProxyContext` |
| `visproxy/service/VisCheckCacheService` | neschimbat |
| `visproxy/service/VisCheckRecordService` | **înlocuit**: `ProxyContext` + `FIX` în `fetchVisEntity` |
| `visproxy/service/VisCheckService` | **înlocuit**: `ProxyContext`; logurile cu date de client la `debug` |
| `visproxy/utils/BuildDTOsUtility` | neschimbat |

`src/test/java/visproxy/` (pachetele de test nu au prefixul `com.bnp...`, ca în cloud)

| Fișier | Ce se face |
|---|---|
| `builder/ApplicationBuilder` | neschimbat (nu e în acest folder; folosit de `IntegrityCheckServiceTest`) |
| `builder/ContextBuilder` | **nou**: construiește `ProxyContext` din mesaj, în locul `new ProcessingContext()` + setteri |
| `client/VisSoapClientTest` | **rescris**: erorile se citesc din context, nu din `CollectError` / `errorMap` |
| `controller/BkrVisControllerTest` | neschimbat |
| `controller/ProxyListenerTest` | **rescris**: fluxul nou |
| `service/IntegrityCheckServiceTest` | **rescris**: fără `GeneralNullOrEmptyCheckService` / `CollectError`; aceleași scenarii |
| `service/VisCheckCacheServiceTest` | neschimbat |
| `service/VisCheckRecordServiceTest` | adaptat: `ProxyContext` în loc de `ProcessingContext` |
| `service/VisCheckServiceTest` | adaptat: `ProxyContext`; producer-ul vine din metadata mesajului |

Fișierele marcate „neschimbat" sunt incluse ca să fie verificate împreună cu restul; nu trebuie recopiate.

## 2. Fluxul

```
Kafka (granting) ──► ProxyListener.consume(message, correlation, ...)
  1. ctx = ProxyContext.of(message, correlation, proxyName)
  2. ProxyContextValidator.validate(ctx)                       ── false ─┐
       correlation, mesaj, aplicația din JSON, businessKey, flowId       │
  3. IntegrityCheckService.loanCheckProcess(ctx)                         │
       aplicație null → log și ieșire (ca în cloud)                      │
       ApplicationService.saveApplicationRequest(ctx)  ── eroare: 500 ─┤
       loan null → 400 LOAN_EMPTY                                        │
       fără clienți și fără companii → 400 "Unable to process VIS data..."│
       VisCheckService.checkVis(ctx, loan)                               │
         pentru fiecare client: sare U0x și clienții fără date complete  │
         cache (VisCheckCacheService) → dacă e găsit, salvează rândul    │
         altfel VisSoapClient.visCheck → cache doar fără erori           │
         VisCheckRecordService.save → un rând în CHECK_VIS_ENTITY        │
  4. ApplicationService.updateApplicationStatus(bk, OK / KO)             │
  ── orice RuntimeException din 2-4: ctx.addUnexpectedError(e) ──────────┤
  5. ProxyResultPublisher.publish(ctx, statusTopic, errorTopic, version) ◄┘
       status: OK / KO (nu se trimite fără correlation)
       error: Error_message_1..n (mesajele identice o singură dată)
```

`GET /getVisEntities?businessKey=&flowId=` (folosit de rat și sed): toate rândurile pentru businessKey + flowId, cele mai noi primele.

## 3. Comparație cu cloud, scenariu cu scenariu

| Scenariu | Cloud | Acum |
|---|---|---|
| Fără correlation | doar error, fără status; stop | la fel (publisher-ul nu trimite status fără correlation) |
| Aplicație / businessKey / flowId lipsă | status KO + error; fără update în DB | la fel |
| Mesaj fără signal | `correlationEmptyCheck` (nevăzut în detaliu) | status KO + error `Orchestration message is empty` (**de confirmat**) |
| Fără erori | update OK, status OK | la fel |
| Erori de la VIS (business sau tehnice) | update KO, status KO + error | la fel; același text pe topic |
| Salvarea aplicației eșuează | eroare cu codul din excepție, fără verificare VIS | la fel, cod `500` |
| Loan null | `loanNotPresent` → 400 `LOAN_EMPTY` | la fel |
| Excepție neașteptată | `ProcessingFailedException` → reîncercări → doar error, fără status | **status KO + error, fără reîncercări** (decis, vezi `docs/LOGIC.md` §10) |
| Excepție la publicare | — | ajunge la `KafkaConfig.errorHandler` (reîncercări + log) |

Textele de pe topicul de error sunt aceleași: `VIS Returned an error: <cod>:<mesaj>` și `Error when executing VIS external SOAP call vis(): <mesaj>`. Pe rândul din `CHECK_VIS_ENTITY` se salvează, ca înainte, codul și mesajul brut.

## 4. Ce s-a schimbat, componentă cu componentă

| Cloud | Acum |
|---|---|
| `ProcessingContext` + `getIndex()` + `getErrorMap()` | `ProxyContext` |
| `GeneralNullOrEmptyCheckService.correlationEmptyCheck / applicationEmptyCheck / businessKeyFlowIdEmptyCheck` | `ProxyContextValidator.validate(ctx)` |
| `GeneralNullOrEmptyCheckService.loanNotPresent(pc)` | `ctx.addError("400", LOAN_EMPTY)` |
| `collectError.addError(pc, code, msg)` + `errorMap.put(...)` + `addErrorSignal` | `ctx.addError(code, mesajul de pe topic)` |
| `publishStatus` / `publishErrors` / `publishStatusAndErrors` / `hasErrors` / `errorsOf` / `eventTime` din listener | `ProxyResultPublisher.publish(...)` |
| `KafkaPublisher` local | `KafkaPublisher` din common (același comportament, inclusiv `addKey(flowId)`) |
| `applicationService.saveApplicationRequest(application, message, json, pc)` | `applicationService.saveApplicationRequest(ctx)` |
| `KafkaConfig.errorHandler(KafkaPublisher, KafkaErrorCollector)` | `KafkaConfig.errorHandler()` |

Alte diferențe:

- `FIX` în `VisCheckRecordService.fetchVisEntity`: se mapează fiecare entitate (înainte se mapa lista întreagă pentru fiecare element). Testul din cloud `fetchCheckVis_shouldReturnDto_whenEntityFound` aștepta deja maparea per entitate.
- Mesajul primit, cererea și răspunsul VIS se loghează la `debug` (înainte la `info`): conțin date personale.

## 5. Cum a fost verificat

- Main și teste compilate cu `javac` (JDK 22) pe bibliotecile reale (Spring 6.1, Spring Data JPA, Lombok, Gson, ModelMapper, Mockito 5, AssertJ, JUnit 5), împreună cu clasele noi din `common-api/src`.
- Tipurile din proiect care nu sunt în repo (Avro, `Error`, `ErrorHandler`, `Application`, `Customer`, clasele WSDL, `CachingService`, ...) au fost înlocuite cu clase minime, cu semnăturile din `reference/` și din poze.
- **36 de teste, toate trec.** `SoapClientConfig` nu a fost compilat (bibliotecile pfspring nu sunt disponibile; clasa e neschimbată).
- Nimic nu a fost rulat în proiectul real.

## 6. De verificat la aplicare

1. Importurile marcate `TODO` (pachetele pentru `CachingService`, `LocalDateTimeTypeAdapter`, `Customer`, `Identification`, `ProducerService`, clasele Avro): se păstrează cele din fișierele existente.
2. `IntegrityCheckServiceTest` folosește `application.setBusinessKey(...)`: business key-ul vine acum din aplicație, nu se mai setează pe context.
3. `CheckVisEntity.flowId` are `unique = true`, dar se salvează un rând pentru fiecare client cu același flowId. E așa și în cloud; de verificat cu baza de date (migrarea `20260520-003-bkr-vis-change_bu...`).
4. Compatibilitatea cu rat / sed: vis expune doar `/getVisEntities` (listă). Rat și sed nemigrate cer `/getVisEntity`. Vezi `docs/HANDOFF.md`.
5. Rulează testele proiectului; `ProxyListenerTest` și celelalte teste vechi se înlocuiesc integral.

## 7. Rețeta pentru celelalte proxy-uri

1. **Listener**: `ProxyContext.of` → `validate` → serviciul propriu → `updateApplicationStatus(bk, KO/OK)` → în `catch (RuntimeException)`: `ctx.addUnexpectedError(e)` → `publish(...)` în afara `try`.
2. **Servicii**: înlocuiește `ProcessingContext` cu `ProxyContext`; fiecare `collectError.addError` + `errorMap.put` + `addErrorSignal` devine un singur `ctx.addError(cod, textul care era în errorMap)`.
3. **Config**: șterge `CollectErrorConfig`, `GeneralNullOrEmptyCheckServiceConfig`, `KafkaPublisher` local; adaugă `ProxyObsBeansConfig`; simplifică `KafkaConfig.errorHandler`.
4. **Endpoint**: întoarce `List<GeneralProxyObsDto>` (toate rândurile pentru businessKey + flowId).
5. **Teste**: `ContextBuilder` pentru context; erorile se verifică pe `ctx.getErrors()`; `ProxyListenerTest` verifică ordinea și apelul de `publish`, nu conținutul semnalelor (acela e testat în common).
6. Compară fiecare scenariu din §3 cu proxy-ul respectiv înainte de commit.
