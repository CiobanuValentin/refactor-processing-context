# Handoff: unde am ajuns (25 nov 2026, seara)

Document pentru instanța de Claude Code a colegului. Citește întâi acest fișier, apoi `LOGIC.md`, `COMMON-API.md`, `APPLY-COMMON.md`, `MISSING.md`.

## Ce este acest repo

Nu este proiectul real. Este o copie de lucru, scrisă din poze de telefon, cu:

- `common-api/`, `bkrvisproxy/`, `sedproxy/`, `ratproxy/`, `rvdrproxy/`: clasele **noi**, de copiat în repo-urile reale;
- `reference/legacy/`, `reference/cloud/`: codul existent, transcris din poze, doar ca referință (nu compilează);
- `snippets/`: fragmente de lipit în clase existente;
- `pending/`: propuneri parcate;
- `docs/`: logica și pașii de aplicare.

Codul de aici a fost compilat doar împotriva unor stub-uri scrise de mână. **Nu a fost compilat în proiectul real**; acolo apar erori de import și de semnătură, care se repară pe rând.

## Reguli de lucru (stabilite cu Valentin)

1. Logica rămâne 1:1 cu legacy (`proxyobs` master + `common-api-1.23.jar`), cu excepția schimbărilor agreate: fără Elasticsearch, fiecare proxy cu DB propriu, rezultatele se citesc prin GET.
2. **Logica de mapare nu se modifică** fără comparație legacy vs cloud (IDE: Compare with Clipboard). Multe lucruri care par bug-uri sunt identice în legacy și rămân așa.
3. Clasele vechi se șterg direct, fără `@Deprecated`.
4. Se lucrează direct pe `main` în acest repo. Fiecare set de poze se transcrie în `reference/`.
5. Răspunsuri scurte, în română; fișiere întregi sau schimbări exacte, ușor de copiat.

## Ordinea de release

1. **common-api** (`proxy-obs-common`), branch real `arval-refactor-last`.
2. **bkrvisproxy** (primul proxy migrat pe noul common).
3. Restul proxy-urilor, câte unul: cki, rvdr, dnb, scal, eva, event indicator (surse), apoi rat și sed (agregatoare).

Atenție la compatibilitate: un agregator migrat citește **liste** de la surse. O sursă nemigrată întoarce un singur obiect, iar un agregator nemigrat nu știe să citească liste. Vis expune deja `GET /getVisEntities` cu listă (vechiul `/getVisEntity` nu mai există pe branch-ul lui), deci rat/sed vechi nu mai pot citi vis. Ordinea de deploy trebuie stabilită ținând cont de asta.

## Starea curentă

### common-api

Scris și urcat aici: `ProxyContext`, `ProxyContextValidator`, `ProxyResultPublisher`, `ApplicationService` (rescris întreg), `KafkaPublisher`, pachetul `aggregation` (`ProxySource`, `ProxySourceProperties`, `ProxyResultsFetcher`, `ProxyResultMapper`, `DtoBackedResultMapper`, `ApplicationAggregator`, `ProxyResultMapperConfig`) și testele lor.

În proiectul real, în curs de integrare. Reparat până acum:

- pachetul Avro real este `com.bnp.pf.transformation.orchestrator.avro`;
- `ProxyContextTest`: testul de event time depindea de un format de dată necunoscut; a rămas doar verificarea de producer.

**Blocat acum pe o eroare de compilare:**

```
ProxyResultMapperConfig.java:[87,16] method listMapper ... cannot be applied to given types
  incompatible types: java.util.List<D> cannot be converted to CheckCkiPersonDto
```

Cauza: `BKRCKIMapper.mapCKIData` din cloud primește un singur `CheckCkiPersonDto`. Rezolvarea: mapperele regenerate, cu listă (`APPLY-COMMON.md`, pasul 1). **Nu au fost aplicate încă.**

### bkrvisproxy

Clasele noi sunt scrise (`bkrvisproxy/` + `MIGRATION.md`). Neîncercate în proiectul real; se începe după ce common compilează și trec testele.

### sed, rat, rvdr

Clase scrise parțial din poze; vezi `MISSING.md` pentru ce lipsește. Nu se ating până nu sunt gata common și vis.

## De confirmat înainte de a merge mai departe

### A. Toate mapperele primesc listă (hotărât, 26 nov)

Toate mapperele primesc lista cu toate rândurile sursei, ca în legacy. `DtoBackedResultMapper` trimite acum lista, o singură dată pe sursă (goală dacă nu sunt rânduri).

Cele 7 mappere sunt regenerate întregi din pozele din 26.11, în `common-api/src/main/java/.../applicationbuilder/utils/` (vezi `APPLY-COMMON.md`, pasul 1). Au importuri presupuse și 10 rânduri marcate `TODO`, de verificat în IDE.

Separat de mappere, contractul HTTP e deja hotărât: **toate** endpoint-urile surselor întorc `List<GeneralProxyObsDto>`.

### B. Common și vis primele

De verificat înainte de release:

1. Common compilează și trec testele în proiectul real.
2. Mapperele regenerate sunt copiate (`APPLY-COMMON.md`, pasul 1), importurile și `TODO`-urile verificate; celelalte apeluri ale metodelor `map...Data` (de ex. cele 7 ale lui `mapCKIData`) sunt reparate sau dispar cu clasele șterse.
3. Clasele vechi sunt șterse (`APPLY-COMMON.md`, pasul 2) și nimic din common nu mai depinde de ele.
4. Vis pornește local cu noul common: listener, `IntegrityCheckService`, `VisSoapClient`, `ProxyObsBeansConfig`, `KafkaConfig.errorHandler` simplificat.
5. Cine consumă azi `/getVisEntities` și dacă rat/sed de pe mediul respectiv pot fi lăsate nemigrate între timp (vezi nota de compatibilitate de mai sus).

## Decizii deja luate (nu se redeschid)

Vezi `LOGIC.md`. Pe scurt: businessKey din JSON-ul aplicației; mesaje separate pentru businessKey și flowId; rezultat lipsă de la o sursă nu e eroare, dar un GET eșuat e eroare și dă KO, fără retry; excepție neașteptată în procesare dă KO + eroare, publish-ul rămâne în afara `try`; ordinea surselor ca în legacy; contractul rămâne `GeneralProxyObsDto`; `KO` și `NOT_OK` sunt același string `"KO"`.

## Importuri încă presupuse

De corectat din IDE când apar erori: `ArvalOrchestrationMessage` / `ArvalSignal` (puse în `orchestrator.avro`), `ProducerService`, `ErrorHandler` / `Error`, `Customer`, `Organisation`, `RiskAssessment`, `ScoringResult`, `RiskLevel`, `ApplicationStatus`.

## Pasul următor

1. Aplică `APPLY-COMMON.md` în common real, compilează, trimite erorile rămase.
2. Rulează testele din common.
3. Treci la vis după `bkrvisproxy/MIGRATION.md`.
