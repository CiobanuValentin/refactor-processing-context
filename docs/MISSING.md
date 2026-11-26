# Ce mai lipsește

Actualizat 26 nov 2026, după regenerarea mapperelor. Comparat cu ce e în repo, nu cu pozele.

## Făcut

- [x] Mapperele din `applicationbuilder.utils` (CKI, EVA, VIS, DNB, EventIndicator, RVDR, SCAL): regenerate, primesc `List<Check...Dto>`, comparate cu legacy. Vezi `COMMON-API.md`, „Mapperele și dto-urile".
- [x] Eroarea de compilare `ckiResultMapper` (`List<D> cannot be converted to CheckCkiPersonDto`): rezolvată, `mapCKIData` primește listă.
- [x] `BKRCKIMapper.collectAllResponses`: argumentele inversate din cloud, corectate ca în legacy.
- [x] `DtoBackedResultMapper` / `ProxyResultMapperConfig`: toate sursele primesc lista, o singură dată pe sursă.
- [x] Perechile legacy + cloud pentru VIS, EVA, RVDR, SCAL, DNB și EventIndicator (făcute odată cu mapperele).
- [x] Transcrieri cloud: `SCALMapper`, `RVDRMapper`, `EventIndicatorMapper`, restul din `BKRCKIMapper` (acum fișiere întregi în `common-api/`); `MappingService` din cki și `PartialDataFetcherPG` sunt în `reference/cloud/`.
- [x] `BKRCommons`: nu se generează (e o clasă goală, cu un constructor, în legacy și în cloud).

## De verificat la copierea în proiectul real

- **Importurile**: în fișierele generate sunt doar cele sigure. Se păstrează blocul de importuri din fișierul existent și se înlocuiește doar corpul clasei.
- **Rândurile marcate `TODO` din mappere** (8 în `BKRCKIMapper`, 2 în `DNBMapper`): rânduri lungi care ieșeau din ecran sau erau acoperite în poze și au fost completate după logică sau după legacy. La copiere, fiecare se compară cu rândul corespunzător din fișierul existent; lista e în `COMMON-API.md`.

## Poze de trimis (pentru proxy-uri, mai târziu)

| # | Repo | Clasă | De ce | Unde se vede în cod |
|---|---|---|---|---|
| 3 | legacy proxyobs ratproxy | `RatMappingService.processRatSanctionDataAsync`, `getScoreResultAsync` | de confirmat că compliance nu era apelat nicăieri | `RatProcessingService.java:65` |
| 3b | cloud bkrckiproxy | `SoapClient.checkCkiRequest`, `CachingService` | unde ajung erorile SOAP; cum se caută în cache | proxy-ul cki nu e încă migrat |
| 10 | cloud ratproxy | `ApplicantBuilder.createApplicant`, `Applicant.validate`, `Company.validate` | primesc încă `collectError` și context | `snippets/ratproxy/LoanMapping-changes.java`, `BNPParibasCloverleafAPIImpl.java:80` |
| 10b | cloud, orice proxy | o clasă din `config/` (ex. `PartialDataFetcherConfig`, `RestClientConfig`) | cum se declară bean-urile și ce `RestClient` primește fetcher-ul | `snippets/proxy-config/ProxyObsBeansConfig.java` |
| 11 | cloud sedproxy | `areParametersProvided` (condiția completă), `ExpertiseReturn` | linie tăiată; câmp presupus | `SedProcessorService.java:145` |
| 12 | legacy + cloud | cum ajunge `rat.compliance` de la rat la sed | propunerea e parcată în `pending/` | `pending/rat-compliance-for-sed/` |

## Mai târziu

- Bean-urile pentru mappere în rat (`ProxyResultMapperConfig` are nevoie de ele): se adaugă la generarea rat proxy.
- Testele mapperelor: nu acum.
- Transcrieri de referință rămase, doar dacă vor fi necesare: legacy rat (listener, mapping, client CloverLeaf) și sed (listener, `SedProcessorService`, `SedMappingService`); cloud: `ApplicationBuilderPG` din sed, serviciile și clientul sed.

## De confirmat cu echipa

0. `ProxyDtoBuilder.buildCheckEvaPersonDto` citește răspunsul din `getResponsePersonJson()`, același câmp ca SCAL person (confirmat în cod). E intenționat?
1. Arval NL: proxy-urile sursă rulează pe aceste flow-uri sau rezultatele vin în JSON-ul aplicației?
2. Arval trebuie să primească și status KO?
3. Ramura `isCollateralIsPrivateUse` din rat e intenționată?
4. `Application` și `Loan` sunt în `commons.api.message.input` (confirmat). `Customer`, `Organisation`, `RiskAssessment`, `ScoringResult`, `RiskLevel` și `ApplicationStatus` sunt încă presupuse în `commons.api.model`: sunt și ele în `message.input`?
