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

Cauza: `BKRCKIMapper.mapCKIData` din cloud primește un singur `CheckCkiPersonDto`. Rezolvarea este pasul 1 din `APPLY-COMMON.md` (readucerea variantei din legacy, cu listă). **Nu a fost aplicat încă**, pentru că se așteaptă confirmarea de mai jos.

### bkrvisproxy

Clasele noi sunt scrise (`bkrvisproxy/` + `MIGRATION.md`). Neîncercate în proiectul real; se începe după ce common compilează și trec testele.

### sed, rat, rvdr

Clase scrise parțial din poze; vezi `MISSING.md` pentru ce lipsește. Nu se ating până nu sunt gata common și vis.

## De confirmat înainte de a merge mai departe

### A. Toate mapperele primesc listă, sau doar CKI?

Valentin crede că toate vor trebui cu listă. Starea de acum în `ProxyResultMapperConfig`:

| Sursă | Mapper cloud primește | Cum e legat acum |
|---|---|---|
| CKI | un DTO | **listă** (necesită `APPLY-COMMON.md` pasul 1) |
| EVA company | listă | listă |
| VIS | un DTO | apel pe fiecare rând; fără rânduri, apel cu `null` |
| SCAL person / person initial / entity | un DTO | apel pe fiecare rând |
| DNB, RVDR, EVA person, EventIndicator | un DTO | apel pe fiecare rând |

De ce doar CKI are listă acum: din diff-urile legacy vs cloud, la celelalte tot ce face mapperul se întâmplă **în buclă**, deci un apel pe rând dă același rezultat. La CKI, rezultatele de pe loan se adaugă **o singură dată, după buclă**; apelat pe rând, le-ar adăuga de mai multe ori.

Ce trebuie verificat ca să se închidă întrebarea, pentru fiecare mapper din tabel:

1. Deschide metoda din legacy (`common-api-1.23`, pachet `applicationbuilder`) și pe cea din cloud, cu Compare with Clipboard.
2. Caută în legacy orice se execută **în afara buclei `for`** peste listă: adăugări pe loan sau application după buclă, flag-uri sau acumulatori care trec de la un rând la altul, ramura `else` pentru listă goală sau `null`.
3. Dacă există așa ceva: mapperul se readuce la varianta cu listă din legacy și se leagă cu `listMapper(...)`. Dacă nu: rămâne cum e.

Varianta „toate cu listă" este și ea validă și mai apropiată de legacy; costul este readucerea din legacy a încă 8 metode de mapper și repararea testelor lor. Decizia e a lui Valentin și a colegului. Dacă se alege, `DtoBackedResultMapper` dispare și rămâne doar `listMapper`.

Separat de mappere, contractul HTTP e deja hotărât: **toate** endpoint-urile surselor întorc `List<GeneralProxyObsDto>`.

### B. Common și vis primele

De verificat înainte de release:

1. Common compilează și trec testele în proiectul real.
2. Cele două editări din `BKRCKIMapper` (`APPLY-COMMON.md`, pașii 1 și 2) sunt aplicate; celelalte apeluri ale lui `mapCKIData` (7 usages) sunt reparate sau dispar cu clasele șterse.
3. Clasele vechi sunt șterse (`APPLY-COMMON.md`, pasul 3) și nimic din common nu mai depinde de ele.
4. Vis pornește local cu noul common: listener, `IntegrityCheckService`, `VisSoapClient`, `ProxyObsBeansConfig`, `KafkaConfig.errorHandler` simplificat.
5. Cine consumă azi `/getVisEntities` și dacă rat/sed de pe mediul respectiv pot fi lăsate nemigrate între timp (vezi nota de compatibilitate de mai sus).

## Decizii deja luate (nu se redeschid)

Vezi `LOGIC.md`. Pe scurt: businessKey din JSON-ul aplicației; mesaje separate pentru businessKey și flowId; rezultat lipsă de la o sursă nu e eroare, dar un GET eșuat e eroare și dă KO, fără retry; excepție neașteptată în procesare dă KO + eroare, publish-ul rămâne în afara `try`; ordinea surselor ca în legacy; contractul rămâne `GeneralProxyObsDto`; `KO` și `NOT_OK` sunt același string `"KO"`.

## Importuri încă presupuse

De corectat din IDE când apar erori: `ArvalOrchestrationMessage` / `ArvalSignal` (puse în `orchestrator.avro`), `ProducerService`, `ErrorHandler` / `Error`, `Customer`, `Organisation`, `RiskAssessment`, `ScoringResult`, `RiskLevel`, `ApplicationStatus`.

## Pasul următor

1. Confirmarea A.
2. Aplică `APPLY-COMMON.md` în common real, compilează, trimite erorile rămase.
3. Rulează testele din common.
4. Treci la vis după `bkrvisproxy/MIGRATION.md`.
