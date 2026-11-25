# Ce mai lipsește

## Poze de trimis

| # | Repo | Clasă | De ce |
|---|---|---|---|
| 1 | common-api (legacy și cloud) | configurarea `kafkaListenerContainerFactory` (probabil `ConsumersConfig` / `KafkaConfig`) | arată ce se întâmplă când publicarea eșuează și excepția iese din listener: retry, câte încercări, DLT |
| 2 | common-api (cloud) | `ApplicationService`, toată clasa | pentru `saveApplicationRequest(ProxyContext)` și citirea aplicației salvate |
| 3 | common-api (cloud) | `ApplicationRequestEntity` + `ApplicationRequestRepository` | GET-ul din rat citește aplicația salvată |
| 4 | ratproxy | `ApplicantBuilder.createApplicant` | primește încă `collectError` și context |
| 5 | ratproxy | `Applicant.validate` și `Company.validate` | aceeași schimbare ca la `Loan.validate` |
| 6 | ratproxy | clientul CloverLeaf (`BNPParibasCloverLeafAPI` + implementarea), versiunea cloud | semnăturile se schimbă; nu e rescris |
| 7 | ratproxy | `Loan.validate`, restul metodei (după companii) | poza se oprea acolo |
| 8 | sedproxy | `SedProcessorService.areParametersProvided`, condiția completă | linia era tăiată |
| 9 | sedproxy | `ExpertiseReturn` | de confirmat câmpul `errorMessage` din builder |
| 10 | common-api (cloud) | `ProxyObsConstant`, toată clasa | constante presupuse: `ARVAL_NL_FLOW_ID_PREFIX`, `SCOPE_KEY`, `SED_DATA_SCOPE` |
| 11 | bkrvisproxy / rvdrproxy | `CreditCheckService.checkCredit` sau echivalentul | de mutat pe `ProxyContext` |
| 12 | orice proxy sursă | entitatea și repository-ul rezultatului (ex. `CheckCkiEntity`) | pentru endpointul de listă |

## Transcrieri de referință nefăcute încă

În `reference/` sunt doar clasele recitite acum. Restul pozelor nu au fost transcrise:

- legacy: rat (listener, mapping, client CloverLeaf); sed (listener, `SedProcessorService`, `SedMappingService`);
- cloud: `PartialDataFetcherPG`, cele 7 mappere, `ProxyDtoBuilder`, `ApplicationBuilderPG` din sed, serviciile și clientul sed, `MappingService` din cki.

Comparația din `LOGIC.md` pentru aceste clase se bazează pe analiza din conversație, nu pe o recitire.

## De confirmat cu echipa

1. Arval NL: proxy-urile sursă rulează pe aceste flow-uri sau rezultatele vin în JSON-ul aplicației?
2. Arval trebuie să primească și status KO?
3. Ramura `isCollateralIsPrivateUse` din rat e intenționată?
4. Pachetul modelului: `commons.api.message.input` (văzut în `LoanMapping`) în loc de `commons.api.model` (presupus în tot repo-ul)?
