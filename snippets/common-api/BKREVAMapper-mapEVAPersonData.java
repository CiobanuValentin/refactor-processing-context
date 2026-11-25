// Singura schimbare din BKREVAMapper: mapEVAPersonData primeste toate rezultatele aplicatiei.
// Partea de pe client ramane la fel, pentru fiecare rezultat. Risk assessment-urile de pe loan se adauga
// o singura data, nu o data pentru fiecare persoana.
//
// Pastrat din varianta actuala: fara niciun rezultat (inainte: dto null) nu se face nimic.
// TODO de confirmat: pe loan se adauga daca macar un rezultat are errors == null (inainte era un singur dto).

public void mapEVAPersonData(List<CheckEvaPersonDto> checkEvaPersonDtos, Application application) {
    if (checkEvaPersonDtos.isEmpty()) {
        return;
    }
    var processingFlag = false;
    log.debug(" Mapping EVA person Data started for business key {}", application.getBusinessKey());

    var customers = application.getLoan().getCustomers();
    var addToLoan = false;
    for (CheckEvaPersonDto checkEvaPersonDto : checkEvaPersonDtos) {
        var request = checkEvaPersonDto.getRequest();
        var response = checkEvaPersonDto.getResponse();

        if (customers != null && !customers.isEmpty() && request != null && response != null) {
            for (Customer customer : customers) {
                if (request.getEvaPersonRequestData().getName().equals(customer.getLastName())
                        && isSameDateOfBirth(request.getEvaPersonRequestData().getDateOfBirth(), customer.getBirthDate())
                        && !isEvaExists(customer)) {

                    processingFlag = true;
                    customer.getRiskAssessments().add(createRiskAssessment(customer, response));
                }
            }
        }
        if (checkEvaPersonDto.getErrors() == null) {
            addToLoan = true;
        }
    }

    if (addToLoan) {
        application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan()));
        processingFlag = true;
    }

    if (processingFlag) {
        log.debug(" Mapping EVA person Data success for business key {}", application.getBusinessKey());
    } else {
        log.debug(" No matching data was found for EVA person, business key {}", application.getBusinessKey());
    }
}
