# Logica proxy-urilor: legacy, cloud și varianta nouă

Documentul descrie ce face fiecare variantă și unde diferă. Codul existent a fost citit din poze, deci fiecare afirmație despre legacy sau cloud trebuie confirmată în repo-ul real. Transcrierile sunt în `reference/`.

## 1. Fluxul unui proxy

Toate proxy-urile fac același lucru:

1. Primesc un `OrchestrationMessage` de pe Kafka, cu headerul `X-Correlation`.
2. Validează mesajul.
3. Rulează verificarea proprie (apel către un sistem extern).
4. Publică un semnal de status (`STATUS` = `OK` / `KO`) și, la eșec, un semnal de error (`Error_message_1..n`).

Rat și sed sunt agregatoare: înainte de pasul 3 au nevoie de rezultatele celorlalte proxy-uri.

## 2. Varianta nouă

```java
ProxyContext ctx = ProxyContext.of(message, correlation, proxyName);
try {
    if (proxyContextValidator.validate(ctx)) {
        service.process(ctx);   // agregatoarele apelează aici applicationAggregator.enrich(ctx, SOURCES)
        applicationService.updateApplicationStatus(ctx.getBusinessKey(), ctx.hasErrors() ? KO : OK);
    }
} catch (RuntimeException e) {
    ctx.addUnexpectedError(e);
}
proxyResultPublisher.publish(ctx, statusTopic, errorTopic, version);
```

Reguli:

- Erorile stau într-un singur loc: `ctx.addError(code, message)`.
- Un pas eșuat oprește pașii următori. Răspunsul e KO cu error, fără excepție.
- O excepție neașteptată din procesare devine tot KO cu error. Nu există retry.
- Publicarea e în afara `try`-ului: dacă ea eșuează, excepția iese către containerul Kafka.
- Semnalele de ieșire se construiesc la publicare, din context.

## 3. Validarea

| | Legacy (`checkDataMessage`) | Cloud (`GeneralNullOrEmptyCheckService`) | Nou (`ProxyContextValidator`) |
|---|---|---|---|
| Ordine | correlation → mesaj → businessKey → flowId → aplicație | correlation → aplicație → businessKey/flowId | correlation → mesaj → aplicație → businessKey → flowId |
| Oprire la prima eroare | da | depinde de listener (vis da, sed și rat nu) | da |
| Correlation | `Objects.isNull` | `StringUtils.isEmpty` | `StringUtils.isEmpty` |
| businessKey, flowId | `Objects.isNull`, două mesaje | `isEmpty`, un mesaj combinat | `isEmpty`, două mesaje |
| Sursa businessKey | semnal | JSON-ul aplicației | JSON-ul aplicației |
| Sursa aplicației | `applicationRepository.findById` | JSON din properties | JSON din properties |
| Mesaj null | eroare `ORCH_MSG_EMPTY` | NPE | eroare `ORCH_MSG_EMPTY` |

Diferențe față de legacy, toate decise:

- businessKey și aplicația vin din mesaj, nu din Elasticsearch. De aceea aplicația se verifică înaintea businessKey-ului.
- Se folosește `isEmpty` în loc de `isNull`, ca în cloud.

## 4. Publicarea

| | Legacy | Cloud (vis) | Nou (`ProxyResultPublisher`) |
|---|---|---|---|
| Fără correlation | doar error | doar error | doar error |
| Cu erori | status `NOT_OK`, apoi error | status `KO`, apoi error | status `KO`, apoi error |
| Fără erori | status `OK` | status `OK` | status `OK` + proprietățile de rezultat |
| Timp la erori de validare | `now()` la primele două | `now()` doar la correlation | timpul din metadata |
| Duplicate | nu elimină | `KafkaPublisher` elimină orice valori egale | elimină doar mesajele de eroare identice |

Diferențe față de legacy:

- Timpul evenimentului vine mereu din metadata (fallback `now()`).
- `KO` și `NOT_OK` au aceeași valoare ca string (`"ko"`), deci nu e nicio diferență pentru Camunda.

## 5. Agregarea (rat și sed)

| | Legacy | Cloud | Nou |
|---|---|---|---|
| De unde vin rezultatele | documentul comun din Elasticsearch | GET către fiecare proxy (`PartialDataFetcherPG`) | GET către fiecare proxy (`ProxyResultsFetcher`) |
| Câte rezultate | toate | unul per aplicație (`findTop...OrderByCreationDesc`) | listă de `GeneralProxyObsDto`, toate rândurile |
| Rezultat lipsă | câmpul e sărit, fără eroare | risk assessment fabricat sau `"false"` | fără eroare, fără rezultat fabricat |
| Apel eșuat | nu există cazul | eroare în context și excepție | eroare în context, răspuns KO |
| `rat.compliance` în sed | din Elasticsearch | nu ajunge deloc | deschis; propunerea e parcată în `pending/` |
| Ordinea surselor | scal ×3, dnb, rvdr, vis, eva person, eva company, cki, event indicator | scal ×3, dnb, cki, rvdr, vis, eva company, eva person, event indicator | cea din legacy |

Diferențe față de legacy:

- Nu mai există Elasticsearch, deci un apel poate eșua. Cazul nu are echivalent în legacy; s-a ales KO, ca „n-am putut întreba" să nu arate ca „nu există rezultat".
- Sursa lipsă din configurare (`No mapper registered`) este tot un caz nou. De mutat într-o verificare la pornire.

### Mapperele existente

Mapperele din `applicationbuilder.utils` și `ProxyDtoBuilder` **nu se modifică**. Regula de lucru: logica de mapare rămâne cea din cloud până e comparată, sursă cu sursă, cu legacy.

`snippets/common-api/ProxyResultMapperConfig.java` face ce făcea `ApplicationBuilderPG` pentru fiecare sursă: `ProxyDtoBuilder` construiește dto-ul tipizat din `GeneralProxyObsDto`, apoi se apelează mapperul. Singura diferență: sursa poate întoarce mai multe rânduri, iar mapperul e apelat pentru fiecare.

Stare pe surse, după comparația cu diff din IDE (legacy față de cloud):

| Sursă | Legacy | Legătura nouă | Stare |
|---|---|---|---|
| CKI | listă; pe loan se adaugă o dată, după buclă | listă, `mapCKIData` readus din legacy | confirmat |
| VIS | listă; pe loan se adaugă în buclă, la fiecare rând | un apel per rând, mapper neschimbat | confirmat echivalent |
| EVA person | listă; pe loan se adaugă în buclă | un apel per rând, mapper neschimbat | confirmat echivalent |
| EVA company | listă; pe loan se adaugă în buclă | listă, mapper neschimbat | confirmat identic |
| SCAL ×3, DNB, RVDR, EventIndicator | necomparat | un apel per rând | de comparat |

La VIS și EVA, mai multe rânduri fără erori înseamnă mai multe perechi Holder / CoHolder pe loan. Așa era și în legacy, deci nu se schimbă.

Diferențe găsite în `BKRCKIMapper` (51 în diff, văzute doar o parte):

- `mapCKIData`: listă în legacy, un singur dto în cloud. Se readuce.
- `collectAllResponses`: în cloud, ramura a doua are argumentele inversate și pierde creditele și scorurile persoanei cu aliasuri. Corecția: `snippets/common-api/BKRCKIMapper-collectAllResponses.java`.
- `getManualCheck`, `getLoanResult`, `createClassificationResult`: doar rescrise (metode extrase, `defaultInt`), același rezultat.

### Salvarea aplicației

`ApplicationService.saveApplicationRequest(ProxyContext)` (vezi [COMMON-API.md](COMMON-API.md)) păstrează logica existentă: dacă există deja un rând pentru business key, actualizează doar `modifiedDate` și `modifiedBy`. JSON-ul nu se rescrie.

## 6. CKI: legacy față de cloud (din `reference/`)

Proxy-ul cki face același lucru în ambele: pentru fiecare client eligibil construiește cererea, caută în cache, cheamă serviciul SOAP dacă e nevoie și salvează **un rând per client**. Risk assessment-urile nu se construiesc în `checkCki`, ci la agregare.

| | Legacy | Cloud |
|---|---|---|
| Rânduri salvate | unul per client eligibil | unul per client eligibil |
| Citirea pentru rat/sed | nevăzută (`applicationBuilder` din common legacy) | doar ultimul rând (`findTop...OrderByCreationDesc`) |
| Cache | răspunsul din cache cu erori nu se folosește; se cheamă din nou SOAP | orice răspuns din cache se folosește; cheia e doar `hashCode` |
| Răspuns SOAP cu erori | răspunsul nu se salvează pe rând, doar erorile | răspunsul se salvează mereu |
| Erorile de pe rând | cele ale clientului respectiv | toate erorile adunate până atunci (`collectError.removeDuplicateErrors()`) |
| Parametri CKI lipsă | eroare „CKI Parameters not received" | două texte: cel scurt în handler, unul lung în semnalul de error |
| Fără clienți | log, fără eroare | fără eroare |

Regulile de eligibilitate (`isEligibleCustomers`, `isEligibleCustomersCoHolder`, `getCompanies`) și construcția cererii sunt identice.

Maparea din legacy (`MappingService`, liniile 173-661) se regăsește în `BKRCKIMapper` din cloud. Lucrurile de mai jos arată a greșeli, dar sunt identice în legacy, deci **nu se repară**:

- `"Result"` este mereu `"false"`;
- `BkrStatus`, `OrgPartyType`, `NeedsManualCheck` sunt suprascrise la fiecare persoană;
- din credite se pune doar unul în `"Loans"` (`findAny`);
- un singur `BkrPerson` e creat în afara stream-ului și refolosit;
- `getRiskLevelHolder` întoarce LOW dacă există un holder, nu după răspuns;
- un credit cu codurile A și H plus altceva e și „medium", și „high";
- `getFirstPayment().equals(...)` fără null-check;
- numărătorile pe tip (`getNmbrOfActiveLoans`, `getLowRiskLoans`...) întorc suma unui singur credit (`findAny().getAmount()`), nu un număr.

### Agregarea în legacy (`ApplicationBuilder`, common-api-1.23.jar)

Mapperele și `PartialDataFetcher` existau deja în biblioteca comună din legacy (`commons.api.applicationbuilder`). `ApplicationBuilder.buildApplication(businessKey, flowId)` citea aplicația din Elasticsearch, apoi apela aceleași mappere ca în cloud.

- **rat** apela builderul pentru toate flow-urile; **sed** doar pentru Arval NL (altfel `findById`).
- Dacă aplicația sau loan-ul lipsesc, builderul întoarce `null`, iar proxy-ul răspunde „Application not found".

Ordinea surselor diferă:

| | Ordine |
|---|---|
| Legacy | SCAL ×3, DNB, RVDR, VIS, EVA person, EVA company, CKI, EventIndicator |
| Cloud (`ApplicationBuilderPG`) | SCAL ×3, DNB, CKI, RVDR, VIS, EVA company, EVA person, EventIndicator |

**Legacy lucra cu liste.** `PartialDataFetcher` întorcea `List<...>` pentru fiecare sursă (toate rândurile pentru businessKey și flowId, sau `null`), iar mapperele primeau lista. `BKRCKIMapper.mapCKIData(List<CheckCkiPersonDto>, Application)` parcurge toate rândurile, pune rezultatul pe fiecare client și adaugă rezultatele de pe loan o singură dată. În cloud, fetcher-ul întoarce un singur dto și mapperele au fost schimbate să primească unul; de aici se pierd rezultatele clienților de după primul.

Ce se face: `mapCKIData` se readuce la varianta din legacy (`snippets/common-api/BKRCKIMapper-mapCKIData.java`). Restul din `BKRCKIMapper` văzut în jar (`createCkiResult`, `createEachCkiResult`, `collectAllResponses`) e identic cu cloud-ul. Pentru celelalte mappere, variantele pe listă din legacy încă nu au fost văzute.

## 7. Rat

Pași: agregare → loan → clienți/companii → sanction → compliance → score → salvare.

| | Legacy | Cloud | Nou |
|---|---|---|---|
| Fără clienți și companii | KO cu error | error, apoi excepție | KO cu error |
| Eroare de la CloverLeaf | KO cu error | error, apoi `RestClientException` | KO cu error |
| `rat.compliance` în `skipCommand` | - | sare compliance, score și salvarea | sare compliance și score; salvează (`FIX`) |
| Rând în `check_loan_sanction` | - | o dată; încă o dată din `catch` la orice eroare | o singură dată |
| Stare pe bean | nu | `Application` ca și câmp al serviciului | nu |

Diferențe de confirmat:

- În cloud, la o eroare în compliance sau score, rândul de sanction se salva a doua oară cu erorile. Acum erorile rămân doar pe rândul pasului care a eșuat.
- Salvarea aplicației când compliance e sărit este o schimbare față de cloud; de revenit dacă era intenționat.

Neschimbate, marcate `TODO`: ramura `isCollateralIsPrivateUse` → endpointul de company, „OnHold" verificat diferit la aplicanți și companii, `getHasAlerts()` fără null-check.

Din listenerul legacy (`reference/legacy/ratproxy/RATProxyListener.java`):

- validarea adună toate erorile, nu se oprește la prima;
- pașii sunt `processRatSanctionDataAsync` și `getScoreResultAsync`; **compliance nu e apelat**, iar `skipCommand` nu există;
- rezultatul se salvează în Elasticsearch (`applicationRepository.save`), de unde îl lua sed;
- la excepție: eroarea e `e.getMessage()` și se salvează un rând de sanction cu erorile și `response = false`;
- se publică întâi error, apoi status.

## 8. Sed

Pași: agregare → salvare → loan → clienți/companii → parametri SED → mapare → DES → `bnpOutputMessage`.

| | Legacy | Cloud | Nou |
|---|---|---|---|
| Eroare de mapare sau DES | KO cu error | excepție (maparea e în afara `try`) | KO cu error |
| Flow Arval NL | builderul era folosit pentru ele | fără agregare | cu agregare (`TODO` de confirmat) |
| Status Arval cu scope | - | doar pe OK | doar pe OK (`TODO` pentru KO) |
| `scoringResult` în output | toate risk assessment-urile | nu pare scris înapoi; doar ultimul de pe loan | scris înapoi, toate (`FIX`) |
| 5xx de la DES | - | parsat ca răspuns valid | eroare (`FIX`) |

## 9. Proxy-urile simple (vis, rvdr, cki, dnb)

Listenerul devine cel din secțiunea 2. Serviciul propriu primește `ProxyContext`.

- rvdr: verifică doar clienții și iese cu OK dacă lipsesc (ca în cloud). Rat și sed dau eroare doar dacă lipsesc și clienții, și companiile.
- dnb: validarea comenzii (`dnb`, `eventindicator`) rămâne în proxy.

## 10. Excepții neașteptate

| | Legacy (vis, dnb, rvdr, cki) | Cloud | Nou (acum) |
|---|---|---|---|
| `try/catch` în listener | nu există | există | există |
| Eroare de la API-ul extern | serviciul de mapare o întoarce ca `List<ErrorHandler>`; răspuns KO cu error | depinde de proxy | KO cu error |
| Excepție neprinsă | iese din listener către containerul Kafka | `ProcessingFailedException` (vis, rat); sed publică doar error | status KO plus error |

În listenerele legacy nu există niciun handler. Ce se întâmplă cu o excepție neprinsă depinde de `kafkaListenerContainerFactory` din common, care nu a fost văzut. Camunda nu primește niciun răspuns de la proxy în acest caz.

Decis: `ProcessingFailedException` se scoate, iar `catch`-ul răspunde cu status KO plus error. Când excepția apare înainte de `updateApplicationStatus`, statusul aplicației din baza de date nu mai e actualizat.

## 11. Ce a fost verificat

- Clasele au fost compilate pe tipuri de probă scrise după poze, fără Lombok și Spring real.
- 46 de teste trec, rulate ca JUnit 4.
- Fluxurile sed și rat au fost rulate cu obiecte false (8, respectiv 10 scenarii).
- Nimic nu a fost compilat sau rulat pe proiectul real.
