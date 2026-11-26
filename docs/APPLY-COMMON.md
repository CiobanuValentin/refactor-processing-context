# common-api: modificări de aplicat

Clasele noi din `common-api/src` se copiază ca fișiere întregi, inclusiv mapperele.

## 1. Mapperele: fișiere întregi, cu listă

În `common-api/src/main/java/com/bnp/pf/transformation/commons/api/applicationbuilder/utils/` sunt cele 7 mappere, regenerate din pozele din 26.11 (codul din cloud) și comparate cu legacy (`reference/legacy/common-api/`):

`BKRCKIMapper`, `BKREVAMapper`, `BKRVISMapper`, `DNBMapper`, `EventIndicatorMapper`, `RVDRMapper`, `SCALMapper`.

Se copiază peste cele existente din `applicationbuilder.utils`. `BKRCommons` nu e în poze și rămâne neatins.

Ce s-a schimbat față de cloud:

- Toate metodele publice `map...Data` primesc `List<Check...Dto>`, ca în legacy. Corpul din cloud pentru un rând e păstrat într-o metodă privată, apelată în buclă. Rândurile `null` sunt sărite.
- **CKI**: rezultatele de pe loan se adaugă o singură dată, după buclă (ca în legacy). Fără rânduri: cele două rezultate goale pe loan.
- **CKI `collectAllResponses`**: în ramura `else if (singlePerson.getAliases() != null)`, argumentele sunt ca în legacy (`existing, singlePerson`); în cloud erau inversate și se pierdeau creditele și scorurile.
- **VIS**: fără rânduri, rezultatele implicite pe clienți și pe loan (ramura din legacy).
- **EVA person**: rezultatele de pe loan se adaugă la fiecare rând fără erori (ca în legacy; la fel ca EVA company).
- Logurile „started" / „success" apar o dată pe sursă, ca în legacy.

Păstrat din cloud (fără echivalent în legacy): null-check-urile (SCAL `person1NullCheck` / `foundNullCheck`, RVDR `verifyResponse`, DNB `getPersonalDetails`), DNB `setDnbLatestAccount`.

### Ce trebuie verificat în IDE

1. **Importurile**: nu erau vizibile în poze. Șterge blocul de importuri din fiecare fișier și lasă IDE-ul să le refacă (Optimize Imports), apoi compară cu fișierul vechi.
2. **Rândurile marcate `TODO`** (10): rânduri care ieșeau din ecran sau erau ascunse în poze. Compară-le cu fișierul vechi din IDE (Compare with Clipboard) înainte de commit. Cele mai importante:
   - `BKRCKIMapper.mapAdditionalResult`, liniile 708-709 din original: nevăzute; `AmountOfActiveLoansTypeTelecomContract` e presupus, a doua linie lipsește;
   - `BKRCKIMapper.getEachWorstCardScoreValue` / `getEachWorstCardRiskClassValue`: ultimul `filter` e presupus;
   - `DNBMapper.setDnbLatestAccount` și `setNumberEmployees`: finalul condiției e presupus.
3. **Celelalte apeluri** ale metodelor `map...Data` (teste, alte clase): `mapXData(dto, application)` devine `mapXData(List.of(dto), application)`. `mapXData(null, application)` compilează în continuare.

Neschimbate, deși arată a bug (identice în legacy): în `createRiskAssessments` din EVA și VIS, rezultatul co-holder e pus pe `riskAssmntHolder`; `SCALMapper.addOrCreate` folosește `List.of(...)` (listă imuabilă); restul listei din `docs/LOGIC.md`, secțiunea CKI.

## 2. De șters din common-api

- `ProcessingContext`, `ProcessingContextBase`, `ArvalProcessingContext`
- `CollectError`, `KafkaErrorCollector`
- `GeneralNullOrEmptyCheckService`
- `ProcessingFailedException`
- `PartialDataFetcher`, `PartialDataFetcherPG`
- testele lor

Vezi [COMMON-API.md](COMMON-API.md) pentru echivalențe.
