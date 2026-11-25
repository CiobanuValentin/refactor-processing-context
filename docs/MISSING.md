# Ce mai lipsește

## Poze de trimis

| # | Repo | Clasă | De ce |
|---|---|---|---|
| 3 | legacy proxyobs ratproxy | `RatMappingService.processRatSanctionDataAsync`, `getScoreResultAsync` | de confirmat că compliance nu era apelat nicăieri |
| 3b | cloud bkrckiproxy | `SoapClient.checkCkiRequest`, `CachingService` | unde ajung erorile SOAP; cum se caută în cache |
| 10 | cloud ratproxy | `ApplicantBuilder.createApplicant`, `Applicant.validate`, `Company.validate` | primesc încă `collectError` și context |
| 10b | cloud, orice proxy | o clasă din `config/` (ex. `PartialDataFetcherConfig`, `ApplicationServiceConfig`) și proprietățile cu URL-urile surselor | cum se declară bean-urile și cum se numesc proprietățile |
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
