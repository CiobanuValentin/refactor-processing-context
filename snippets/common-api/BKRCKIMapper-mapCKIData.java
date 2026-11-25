// De readus in BKRCKIMapper din cloud: mapCKIData exact ca in legacy (common-api-1.23.jar, liniile 78-123).
// In cloud metoda a fost schimbata sa primeasca un singur CheckCkiPersonDto; legacy primea lista cu toate
// randurile aplicatiei si adauga rezultatele de pe loan o singura data, dupa ce parcurge toti clientii.
// Metoda processRiskAssessment adaugata in cloud dispare; restul clasei ramane neschimbat.

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
