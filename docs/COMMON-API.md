# common-api: ce se șterge, ce se adaugă, ce e echivalent cu ce

Clasele vechi se șterg direct, fără `@Deprecated`. Ordinea de livrare: întâi common-api, apoi proxy-urile migrează pe rând.

## De șters din common-api

| Clasă | Înlocuită de |
|---|---|
| `ProcessingContext`, `ProcessingContextBase`, `ArvalProcessingContext` | `ProxyContext` |
| `CollectError`, `KafkaErrorCollector` | `ProxyContext.addError` + `ProxyResultPublisher` |
| `GeneralNullOrEmptyCheckService` | `ProxyContextValidator` |
| `ProcessingFailedException` | nimic: listenerul răspunde KO plus error (`findIn` e apelat din `KafkaConfig` al fiecărui proxy) |
| `PartialDataFetcher`, `PartialDataFetcherPG` | `ProxyResultsFetcher` |
| testele claselor de mai sus | testele din `src/test` |

Mapperele sunt regenerate (vezi „Mapperele și dto-urile" mai jos). Rămân neatinse: `ProxyDtoBuilder`, `GeneralProxyObsDto`, toate `Check*Dto`, `ErrorHandler`, `Error`, `ProxyObsConstant`, `GsonInstance`, `LocalDateTimeUtils`, `OrchestrationMetaDataHandler`.

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

`ProxyDtoBuilder`, `GeneralProxyObsDto` și dto-urile `Check*` rămân cum sunt. Contractul endpointurilor rămâne `GeneralProxyObsDto`, doar că se întoarce o listă.

Cele 7 mappere din `applicationbuilder.utils` sunt regenerate întregi, din pozele din 26.11 (codul din cloud, branch `arval-refactor-last`), și comparate cu legacy (`common-api-1.23`, `reference/legacy/common-api/`). Fișierele: `common-api/src/main/java/com/bnp/pf/transformation/commons/api/applicationbuilder/utils/`. Pașii de aplicare: `APPLY-COMMON.md`, pasul 1.

### De ce

În legacy, `PartialDataFetcher` întorcea toate rândurile unei surse, iar mapperele primeau lista. În cloud, fetcher-ul întorcea un singur rând și mapperele au fost schimbate să primească un dto. Acum sursele întorc din nou liste (`List<GeneralProxyObsDto>`), deci mapperele revin la listă.

### Semnături

| Mapper | Înainte (cloud) | Acum |
|---|---|---|
| `BKRCKIMapper` | `mapCKIData(CheckCkiPersonDto, Application)` | `mapCKIData(List<CheckCkiPersonDto>, Application)` |
| `BKREVAMapper` | `mapEVAPersonData(CheckEvaPersonDto, Application)` | `mapEVAPersonData(List<CheckEvaPersonDto>, Application)` |
| `BKREVAMapper` | `mapEVACompanyData(List<CheckEvaCompanyDto>, Application)` | neschimbată |
| `BKRVISMapper` | `mapVISData(CheckVisDto, Application)` | `mapVISData(List<CheckVisDto>, Application)` |
| `DNBMapper` | `mapDNBData(CheckDnbCompanyDto, Application)` | `mapDNBData(List<CheckDnbCompanyDto>, Application)` |
| `EventIndicatorMapper` | `mapEventIndicatorData(CheckEventIndicatorDto, Application)` | `mapEventIndicatorData(List<CheckEventIndicatorDto>, Application)` |
| `RVDRMapper` | `mapRVDRData(CheckRvdrCreditDto, Application)` | `mapRVDRData(List<CheckRvdrCreditDto>, Application)` |
| `SCALMapper` | `mapScalPersonData`, `mapScalPersonInitialData`, `mapScalEntityData` cu un dto | aceleași, cu `List<...>` |

Apelurile vechi `mapXData(dto, application)` devin `mapXData(List.of(dto), application)`. `mapXData(null, application)` compilează în continuare.

### Cum e construită fiecare metodă

Forma din legacy: `if (matches != null && !matches.isEmpty())`, log „started", buclă peste rânduri (rândurile `null` sunt sărite), log „success" / „No matching data", o singură dată pe sursă. Corpul din cloud pentru un rând e păstrat neschimbat, într-o metodă privată apelată în buclă:

| Mapper | Metoda privată pe un rând |
|---|---|
| `BKRCKIMapper` | `processRiskAssessment` (exista deja în cloud) |
| `BKREVAMapper` | `mapPersonRiskAssessment` (nouă; aceeași formă ca `mapRiskAssessment` de la company) |
| `BKRVISMapper` | `createRiskAssessment(customers, ...)` + `checkErrors` (existau deja) |
| `DNBMapper` | `mapCompanies` (nouă) |
| `EventIndicatorMapper` | `mapCompanies` (nouă) |
| `RVDRMapper` | `mapCustomers` (nouă) |
| `SCALMapper` | `mapScalPerson`, `mapScalPersonInitial`, `mapScalEntity` (noi) |

### Comportament, sursă cu sursă

| Sursă | Mai multe rânduri | Niciun rând |
|---|---|---|
| CKI | rezultatul pe fiecare client care se potrivește; pe loan, **o singură dată, după buclă** | cele două rezultate goale pe loan (holder, co-holder) |
| VIS | rezultatul pe client; pe loan, la fiecare rând fără erori | rezultat implicit pe fiecare client + două pe loan, cu `getResultClassCd(null)` |
| EVA person / company | rezultatul pe client / companie; pe loan, la fiecare rând fără erori | nimic |
| SCAL ×3, DNB, RVDR, EventIndicator | câmpuri și rezultate pe clienți / companii; nimic pe loan | nimic |

Toate sunt la fel ca în legacy. La VIS și EVA, mai multe rânduri fără erori înseamnă mai multe perechi Holder / CoHolder pe loan; așa era și în legacy.

### Alte schimbări față de cloud

- `BKRCKIMapper.collectAllResponses`: în ramura `else if (singlePerson.getAliases() != null)`, `mergeCreditLoans` / `mergeBkrScores` primesc `(existing, singlePerson)`, ca în legacy. În cloud erau inversate și creditele și scorurile persoanei cu aliasuri se pierdeau.
- DNB și EventIndicator nu mai aruncă NPE pe un rând `null` (e sărit).
- Logurile „started" / „success" apar o dată pe sursă, nu o dată pe rând.

### Păstrat din cloud (fără echivalent în legacy)

- protecțiile la null: SCAL `person1NullCheck` / `foundNullCheck`, RVDR `personNullCheck` / `personNameNullCheck` / `verifyResponse`, DNB `getPersonalDetails()`;
- DNB `setDnbLatestAccount` (`dnbLatestAccountFiledDate` / `Period`);
- metodele extrase din cloud (DNB `processResponse` / `processWithCcdp` / `set...`, CKI `getManualCheck`, `getLoanResult`, `mapToLoan`, ...).

### Neschimbate, deși arată a bug (identice în legacy)

- EVA și VIS `createRiskAssessments`: rezultatul co-holder e pus pe `riskAssmntHolder` (`riskAssmntHolder.setClassificationResults(classResultCoHolder)`).
- `SCALMapper.addOrCreate` creează lista cu `List.of(...)` (imuabilă); un al doilea rând pe aceeași parte ar arunca la `add`, dacă lista era `null` inițial.
- Restul listei din `LOGIC.md`, secțiunea CKI.

### Ce nu s-a văzut în poze

- **Importurile** (toate fișierele): în fișierele generate sunt doar cele sigure; se păstrează blocul de importuri din fișierul existent și se înlocuiește doar corpul clasei.
- **Rândurile incomplete din primele poze**: verificate pe pozele clare din 27.11 (`Common_Mappers_V2`); nu mai există `TODO`-uri. Diferențe față de ce fusese presupus:
  - `BKRCKIMapper.activeOwnedContracts`: `.findFirst()`, nu `.findAny()`;
  - `DNBMapper.setNumberEmployees`: ultima condiție este `getEmployeesInformationEntry() != null && ...get(0) != null`, nu „listă nevidă”.
  Restul rândurilor (inclusiv liniile 708-709 din `mapAdditionalResult`: `MailOrder` / `TelecomContract`) erau corecte.
- **`BKRCommons`**: adăugat în `utils`, ca în cloud (`RESULT`, `RESULT_TRUE`, `RESULT_FALSE`, `RESULT_HOLDER`, `RESULT_CO_HOLDER`); folosit prin `import static ...BKRCommons.*` în CKI, EVA și VIS.

### Teste

`common-api/src/test/java/com/bnp/pf/transformation/commons/api/applicationbuilder/utils/`: câte o clasă pentru fiecare mapper (`BKRCKIMapperTest`, `BKREVAMapperTest`, `BKRVISMapperTest`, `DNBMapperTest`, `EventIndicatorMapperTest`, `RVDRMapperTest`, `SCALMapperTest`) și `MapperTestData` (aplicație, clienți, companii, date). 55 de teste.

Ce acoperă, pentru fiecare mapper: listă `null` / goală, potrivire pe client sau companie, nepotrivire, mai multe rânduri cu rânduri `null` sărite, plus regulile specifice:

- CKI: rezultatele de pe loan adăugate **o singură dată** pentru mai multe rânduri; client cu CKI existent sărit; `collectAllResponses` (ambele ramuri cu aliasuri); `highRisk`, `getDuration`, `getInstallment` (balon, fără dată finală, OA dinainte de 02.04.2022), `getTotalAmount`;
- EVA / VIS: rezultatele de pe loan pentru fiecare rând fără erori; rând cu erori nu atinge loan-ul; VIS fără rânduri pune rezultate implicite; comportamentul moștenit din legacy (rezultatul co-holder pus pe holder) e fixat într-un test, ca să nu fie schimbat din greșeală;
- DNB: toate câmpurile din payload, `numberOfEmployees` lipsă când lista e `null`, răspuns fără CCDP;
- EventIndicator: `output01..25`, `active`, `addDate`;
- SCAL: person / person initial / entity, prefixul numelui, `person1` lipsă;
- RVDR: `CirCheck` / `CcbrCheck`, răspuns lipsă, număr de casă diferit.

Verificate: compilate și rulate (JUnit 5, AssertJ, toate trec) pe clase minimale pentru tipurile din proiect care nu sunt în repo (`Customer`, `Organisation`, `RiskAssessment`, tipurile WSDL, dto-urile `Check*`). La copiere: importurile se iau din mapperul testat; testele presupun setteri Lombok pe aceste clase. Compilarea a găsit un import lipsă în `BKRCKIMapper` (`Loan`), corectat.

### Legătura în agregare

`ProxyResultMapperConfig` leagă toate cele 10 surse prin `DtoBackedResultMapper`, care construiește dto-ul fiecărui rând cu `ProxyDtoBuilder` și cheamă mapperul o singură dată, cu toată lista (goală dacă sursa n-a întors nimic). Varianta veche „un apel pe rând" și opțiunea „apel cu `null` pentru VIS" au fost scoase.

## Bean-uri în proxy-uri

Clasele din common-api (`com.bnp...`) nu sunt găsite de component scan-ul proxy-urilor (`com.bnpp...`); fiecare proxy le declară ca bean-uri în `config/`. Clasele noi au nevoie de același lucru: `snippets/proxy-config/ProxyObsBeansConfig.java`. Din proxy se șterg `CollectErrorConfig`, `GeneralNullOrEmptyCheckServiceConfig` și `PartialDataFetcherConfig`, iar `KafkaConfig.errorHandler` se simplifică (`snippets/proxy-config/KafkaConfig-errorHandler.java`).

## Ordinea de livrare

1. **common-api**: clasele noi, `ApplicationService` rescrisă, cele două metode de mapper, ștergerea claselor vechi. Un proxy nemigrat nu mai compilează cu versiunea nouă, deci rămâne pe cea veche până e migrat.
2. **Proxy-urile sursă** (cki, vis, eva, rvdr, scal, dnb, event indicator): listener nou, endpoint de listă, ștergerea `KafkaPublisher`-ului local.
3. **rat** și **sed**: au nevoie de endpointurile de listă din pasul 2.

Endpointul vechi, cu un singur rezultat, trebuie păstrat în fiecare proxy sursă până când rat și sed sunt amândouă migrate.
