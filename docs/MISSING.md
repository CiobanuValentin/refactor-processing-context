# Ce mai lipsește

## Poze de trimis

| # | Repo | Clasă | De ce |
|---|---|---|---|
| 0 | common-api | clasa care apelează `ProcessingFailedException.findIn` | trebuie ștearsă odată cu excepția |
| 1 | common-api | `BKRVISMapper.checkErrors` și `createRiskAssessments(loan, ...)` | dacă adaugă pe loan la fiecare apel, VIS trebuie trecut pe listă, ca CKI |
| 2 | common-api | `ApplicationRequestEntity`, `ApplicationRequestDto`, `ApplicationRequestRepository` | de confirmat `getApplicationJson()` și pachetele |
| 3 | ratproxy | clientul CloverLeaf (`BNPParibasCloverLeafAPI` + implementarea), versiunea cloud | semnăturile se schimbă; nu e rescris |
| 4 | ratproxy | `ApplicantBuilder.createApplicant`, `Applicant.validate`, `Company.validate` | primesc încă `collectError` și context |
| 5 | sedproxy | `SedProcessorService.areParametersProvided`, condiția completă | linia era tăiată |
| 6 | sedproxy | `ExpertiseReturn` | de confirmat câmpul `errorMessage` din builder |
| 7 | common-api | `ProxyObsConstant`, toată clasa | constante presupuse: `ARVAL_NL_FLOW_ID_PREFIX`, `SCOPE_KEY`, `SED_DATA_SCOPE` |
| 8 | common-api | configurarea `kafkaListenerContainerFactory` | ce se întâmplă când publicarea eșuează și excepția iese din listener |
| 9 | common-api | `SCALMapper`, `RVDRMapper`, `EventIndicatorMapper`: doar semnăturile `map...Data` | presupuse `(dto, application)` |
| 10 | orice proxy sursă | entitatea și repository-ul rezultatului (ex. `CheckCkiEntity`) | pentru endpointul de listă |
| 11 | bkrvisproxy / rvdrproxy | `CreditCheckService.checkCredit` sau echivalentul | de mutat pe `ProxyContext` |

## Transcrieri de referință nefăcute încă

În `reference/` sunt doar clasele recitite acum. Restul pozelor nu au fost transcrise:

- legacy: rat (listener, mapping, client CloverLeaf); sed (listener, `SedProcessorService`, `SedMappingService`);
- cloud: `PartialDataFetcherPG`, cele 7 mappere, `ProxyDtoBuilder`, `ApplicationBuilderPG` din sed, serviciile și clientul sed, `MappingService` din cki.

Comparația din `LOGIC.md` pentru aceste clase se bazează pe analiza din conversație, nu pe o recitire.

## De confirmat cu echipa

0. `ProxyDtoBuilder.buildCheckEvaPersonDto` citește răspunsul din `getResponsePersonJson()`, același câmp ca SCAL person. E intenționat?
0. La reprocesarea aceluiași business key, JSON-ul aplicației salvate nu se rescrie. E acceptabil ca sed să primească rezultatele rat de la prima procesare?
1. Arval NL: proxy-urile sursă rulează pe aceste flow-uri sau rezultatele vin în JSON-ul aplicației?
2. Arval trebuie să primească și status KO?
3. Ramura `isCollateralIsPrivateUse` din rat e intenționată?
4. `Application` și `Loan` sunt în `commons.api.message.input` (confirmat). `Customer`, `Organisation`, `RiskAssessment`, `ScoringResult`, `RiskLevel` și `ApplicationStatus` sunt încă presupuse în `commons.api.model`: sunt și ele în `message.input`?
