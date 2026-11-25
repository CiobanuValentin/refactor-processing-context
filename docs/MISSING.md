# Ce mai lipsește

## Poze de trimis

| # | Repo | Clasă | De ce |
|---|---|---|---|
| 1 | diff legacy / cloud | `SCALMapper`, `DNBMapper`, `RVDRMapper`, `EventIndicatorMapper`: metodele `map...Data` | unde se adaugă rezultatele față de buclă |
| 2 | diff legacy / cloud | `BKRCKIMapper`: `createRiskAssessments`, `getCreditLoans(BkrPerson)` cu baloanele, `getInstallment`, `getDuration` | restul celor 51 de diferențe, doar unde nu e simplă reformatare |
| 3 | legacy proxyobs ratproxy | `RatMappingService.processRatSanctionDataAsync`, `getScoreResultAsync` | de confirmat că compliance nu era apelat nicăieri |
| 3b | cloud bkrckiproxy | `SoapClient.checkCkiRequest`, `CachingService` | unde ajung erorile SOAP; cum se caută în cache |
| 4 | cloud common-api | `PartialDataFetcherPG.callProxyEndpoint` | cum se citește răspunsul (Jackson sau Gson) și ce întoarce când nu sunt date |
| 5 | cloud common-api | `BKRVISMapper.checkErrors` | ce adaugă pe loan |
| 6 | cloud common-api | clasa care apelează `ProcessingFailedException.findIn` | se șterge odată cu excepția |
| 7 | cloud common-api | `ApplicationRequestEntity`, `ApplicationRequestDto`, `ApplicationRequestRepository` | pachete și `getApplicationJson()` |
| 8 | cloud common-api | `ProxyObsConstant`, toată clasa | constante presupuse |
| 9 | cloud common-api | semnăturile `map...Data` din `SCALMapper`, `RVDRMapper`, `EventIndicatorMapper` | presupuse `(dto, application)` |
| 10 | cloud ratproxy | clientul CloverLeaf, `ApplicantBuilder.createApplicant`, `Applicant.validate`, `Company.validate` | primesc încă `collectError` și context |
| 11 | cloud sedproxy | `areParametersProvided` (condiția completă), `ExpertiseReturn` | linie tăiată; câmp presupus |
| 12 | legacy + cloud | cum ajunge `rat.compliance` de la rat la sed | propunerea e parcată în `pending/` |

După CKI, aceeași pereche (legacy `mappingService.checkX` + cloud mapper) pentru VIS, EVA, RVDR, SCAL, DNB și EventIndicator.

## Transcrieri de referință nefăcute încă

În `reference/` sunt doar clasele recitite acum. Restul pozelor nu au fost transcrise:

- legacy: rat (listener, mapping, client CloverLeaf); sed (listener, `SedProcessorService`, `SedMappingService`);
- cloud: `PartialDataFetcherPG`, `SCALMapper`, `RVDRMapper`, `EventIndicatorMapper`, restul din `BKRCKIMapper`, `ApplicationBuilderPG` din sed, serviciile și clientul sed, `MappingService` din cki.

Comparația din `LOGIC.md` pentru aceste clase se bazează pe analiza din conversație, nu pe o recitire.

## De confirmat cu echipa

0. `ProxyDtoBuilder.buildCheckEvaPersonDto` citește răspunsul din `getResponsePersonJson()`, același câmp ca SCAL person. E intenționat?
1. Arval NL: proxy-urile sursă rulează pe aceste flow-uri sau rezultatele vin în JSON-ul aplicației?
2. Arval trebuie să primească și status KO?
3. Ramura `isCollateralIsPrivateUse` din rat e intenționată?
4. `Application` și `Loan` sunt în `commons.api.message.input` (confirmat). `Customer`, `Organisation`, `RiskAssessment`, `ScoringResult`, `RiskLevel` și `ApplicationStatus` sunt încă presupuse în `commons.api.model`: sunt și ele în `message.input`?
