// Singura schimbare din BKRCKIMapper: mapCKIData primeste toate rezultatele aplicatiei, cate unul per persoana.
// Restul clasei ramane neschimbat. Rezultatele tuturor persoanelor se aduna intr-un singur BkrCkiResponse,
// iar risk assessment-urile de pe loan (Holder / CoHolder) se adauga o singura data.
//
// Pastrat din varianta actuala:
//  - fara niciun rezultat (inainte: dto null) se adauga risk assessment-urile goale pentru Holder si CoHolder;
//  - cu rezultate, dar fara nicio potrivire, nu se adauga nimic pe loan.

public void mapCKIData(List<CheckCkiPersonDto> checkCkiPersonDtos, Application application) {
    var ckiResponse = new BkrCkiResponse();
    if (checkCkiPersonDtos.isEmpty()) {
        application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
        return;
    }

    log.debug(" Mapping CKI Data started for business key {}", application.getBusinessKey());
    List<BkrCkiResult> bkrResults = new ArrayList<>();
    var customers = application.getLoan().getCustomers();
    var processingFlag = false;
    for (CheckCkiPersonDto dto : checkCkiPersonDtos) {
        processingFlag = processRiskAssessment(dto.getRequest(), dto.getResponse(), customers, processingFlag, bkrResults);
    }

    if (!bkrResults.isEmpty()) {
        ckiResponse.setBkrResults(bkrResults);
        application.getLoan().getRiskAssessments().addAll(createRiskAssessments(ckiResponse));
        processingFlag = true;
    }

    if (processingFlag) {
        log.debug(" Mapping CKI Data success for business key {}", application.getBusinessKey());
    } else {
        log.debug(" No matching data was found for CKI, business key {}", application.getBusinessKey());
    }
}
