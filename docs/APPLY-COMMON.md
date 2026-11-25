# common-api: modificări de aplicat de mână

Clasele noi din `common-api/src` se copiază ca fișiere întregi. Singurul fișier existent în care trebuie editat de mână este `BKRCKIMapper` (peste 1100 de linii, văzut doar parțial, deci nu poate fi livrat întreg). Sunt două modificări.

## 1. `BKRCKIMapper.mapCKIData`: primește lista, ca în legacy

**Șterge** aceste două metode întregi:

- `public void mapCKIData(CheckCkiPersonDto checkCkiPersonDto, Application application)`
- `private boolean processRiskAssessment(SingleBkrRequest request, SingleBkrResponse response, List<Customer> customers, boolean processingFlag, List<BkrCkiResult> bkrResults)`

**Pune în locul lor** (copiată din `common-api-1.23`):

```java
    public void mapCKIData(List<CheckCkiPersonDto> matches, Application application) {
        var ckiResponse = new BkrCkiResponse();
        List<BkrCkiResult> bkrResults = new ArrayList<>();
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping CKI Data started for business key {}", application.getBusinessKey());
            for (CheckCkiPersonDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    var customers = application.getLoan().getCustomers();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (request != null && response != null && customers != null && !customers.isEmpty()) {
                        for (var customer : customers) {
                            if (!isCkiExists(customer) && request.getPerson().getName().equals(customer.getLastName())
                                    && isSameDateOfBirth(request.getPerson().getDateOfBirth(), customer.getBirthDate())
                                    && request.getPerson().getInitials().equals(getInitialWithOutDot(customer.getInitialsName()))) {
                                processingFlag = true;
                                var bkrCkiRslt = createCkiResult(customer, response);
                                if (Objects.nonNull(bkrCkiRslt)) {
                                    bkrResults.add(bkrCkiRslt);
                                }
                                customer.getRiskAssessments().add(createRiskAssessment(customer, bkrCkiRslt));
                            }
                        }
                    }
                }
            }
            if (!bkrResults.isEmpty()) {
                ckiResponse.setBkrResults(bkrResults);
            }
            if (ckiResponse.getBkrResults() != null && !ckiResponse.getBkrResults().isEmpty()) {
                application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
                processingFlag = true;
            }

            if (processingFlag) {
                log.debug(" Mapping CKI Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for CKI, business key {}", application.getBusinessKey());
            }
        } else {
            application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
        }
    }
```

Până nu faci această înlocuire, `ProxyResultMapperConfig` nu compilează (`List<D> cannot be converted to CheckCkiPersonDto` la `ckiResultMapper`), pentru că `mapCKIData` din cloud primește un singur DTO.

După înlocuire, celelalte apeluri ale lui `mapCKIData` (7 usages în IDE) nu mai compilează:

- cele din clasele care se șterg oricum (`PartialDataFetcher`, `ApplicationBuilder` vechi) dispar odată cu ele;
- în teste, `mapCKIData(dto, application)` devine `mapCKIData(List.of(dto), application)`, iar `mapCKIData(null, application)` rămâne valid, dar cere cast: `mapCKIData((List<CheckCkiPersonDto>) null, application)` nu e necesar dacă există o singură metodă cu acest nume.

Nu e nevoie de importuri noi: `List`, `ArrayList` și `Objects` sunt deja folosite în clasă.

## 2. `BKRCKIMapper.collectAllResponses`: două linii

În ramura `else if (singlePerson.getAliases() != null)`, argumentele sunt inversate față de legacy.

**Înlocuiește:**

```java
            mergeCreditLoans(singlePerson, existing);
            mergeBkrScores(singlePerson, existing);
```

**cu:**

```java
            mergeCreditLoans(existing, singlePerson);
            mergeBkrScores(existing, singlePerson);
```

Atenție: aceleași două linii apar și în ramura de deasupra, `if (existing.getAliases() != null)`. Acolo sunt corecte și rămân neschimbate; se modifică doar a doua apariție.

De ce: în legacy, când persoana nouă are aliasuri, creditele și scorurile ei se adaugă la persoana deja din mapă. În cloud se adaugă invers, în persoana nouă, care nu ajunge în mapă, deci se pierd.

## 3. De șters din common-api

- `ProcessingContext`, `ProcessingContextBase`, `ArvalProcessingContext`
- `CollectError`, `KafkaErrorCollector`
- `GeneralNullOrEmptyCheckService`
- `ProcessingFailedException`
- `PartialDataFetcher`, `PartialDataFetcherPG`
- testele lor

Vezi [COMMON-API.md](COMMON-API.md) pentru echivalențe.
