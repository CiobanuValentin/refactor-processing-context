// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class BKRVISMapper {

    // De observat: ca la EVA, rezultatele de pe loan se adauga in bucla, o data pentru fiecare rand fara erori.
    public void mapVISData(List<CheckVisDto> matches, Application application) {
        List<Customer> customers = application.getLoan().getCustomers();

        if (customers != null && !customers.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping VIS Data started for business key {}", application.getBusinessKey());

            if (matches != null && !matches.isEmpty()) {
                for (CheckVisDto requestAndResponse : matches) {
                    if (requestAndResponse != null) {
                        var request = requestAndResponse.getRequest();
                        var response = requestAndResponse.getResponse();

                        if (request != null && response != null) {
                            for (Customer customer : customers) {
                                if (request.getVisRequestData().getCountryCode().equals(customer.getIdentification().getIsoIssuedCountryCode()) &&
                                        request.getVisRequestData().getType().equals(getType(customer.getIdentification())) &&
                                        request.getVisRequestData().getDocumentNumber().equals(customer.getIdentification().getCode()) &&
                                        isSameDateOfBirth(request.getVisRequestData().getDateOfBirth(), customer.getBirthDate()) &&
                                        !isVisExists(customer)) {

                                    processingFlag = true;
                                    customer.getRiskAssessments().add(createRiskAssessment(customer, response));
                                }
                            }
                        }

                        if (requestAndResponse.getErrors() == null) {
                            application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan(), getResultClassCd(response)));
                            processingFlag = true;
                        }
                    }
                }
            } else {
                /* ...ramura pentru "fara date" nu era vizibila in diff; in cloud e nemarcata ca diferenta,
                   deci e identica: un risk assessment pe fiecare client si doua pe loan, cu getResultClassCd(null) */
            }

            if (processingFlag) {
                log.debug(" Mapping VIS Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for VIS, business key {}", application.getBusinessKey());
            }
        }
    }
    /* ...restul clasei: 6 diferente in total fata de cloud (semnatura, bucla, extragerea in checkErrors /
       createRiskAssessment); getResultClassCd, getRiskLevelHolder, isVisExists, getType nu apar ca diferente */
}
