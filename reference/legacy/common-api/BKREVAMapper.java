// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class BKREVAMapper {

    public static final String CLASS_INSTR_CD = "bkr.eva";
    public static final String CLASS_INSTR_NAME = "Bkr Eva";
    public static final String RESULT_DESC = "BKR EVA results";
    public static final String RESULT_HOLDER_DESC = "BKR EVA results for loan holders";
    public static final String RESULT_CO_HOLDER_DESC = "BKR EVA results for loan co-holders";

    // De observat: rezultatele de pe loan se adauga in interiorul buclei, deci o data pentru fiecare rand fara erori.
    public void mapEVAPersonData(List<CheckEvaPersonDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping EVA person Data started for business key {}", application.getBusinessKey());

            for (CheckEvaPersonDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    var customers = application.getLoan().getCustomers();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (customers != null && !customers.isEmpty() && request != null && response != null) {
                        for (Customer customer : customers) {
                            if (request.getEvaPersonRequestData().getName().equals(customer.getLastName())
                                    && isSameDateOfBirth(request.getEvaPersonRequestData().getDateOfBirth(), customer.getBirthDate())
                                    && !isEvaExists(customer)) {

                                processingFlag = true;
                                customer.getRiskAssessments().add(createRiskAssessment(customer, /* ...taiat */));
                            }
                        }
                    }

                    if (requestAndResponse.getErrors() == null) {
                        application.getLoan().getRiskAssessments().addAll(createRiskAssessments(/* ...taiat */));
                        processingFlag = true;
                    }
                }
            }

            if (processingFlag) {
                log.debug(" Mapping EVA person Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EVA person, business key {}", application.getBusinessKey());
            }
        }
    }

    // Aceeasi logica precum in cloud; acolo corpul buclei a fost extras in mapRiskAssessment / addRiskAssessment.
    public void mapEVACompanyData(List<CheckEvaCompanyDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping EVA company Data started for business key {}", application.getBusinessKey());

            for (CheckEvaCompanyDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    var companies = application.getLoan().getCompanies();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (companies != null && !companies.isEmpty() && request != null && response != null) {
                        for (Organisation company : companies) {
                            var companyData = request.getEvaCompanyRequestData();
                            if (companyData != null && companyData.getName().equals(company.getStatutoryName())
                                    && companyData.getCOCNumber().equals(company.getLegalRegistrationNumber())
                                    && isSameZipCode(company, companyData)
                                    && !isEvaExists(company)) {

                                processingFlag = true;
                                company.getRiskAssessments().add(createRiskAssessment());
                            }
                        }
                    }

                    if (requestAndResponse.getErrors() == null) {
                        application.getLoan().getRiskAssessments().addAll(createRiskAssessments(/* ...taiat */));
                        processingFlag = true;
                    }
                }
            }

            if (processingFlag) {
                log.debug(" Mapping EVA company Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EVA company, business key {}", application.getBusinessKey());
            }
        }
    }
    /* ...restul clasei: diff-ul arata 14 diferente in total; cele nevazute nu sunt transcrise */
}
