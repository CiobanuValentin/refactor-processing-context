// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class BKREVAMapper {
    /* ...constante: CLASS_INSTR_NAME = "Bkr Eva", RESULT_DESC = "BKR EVA results",
       RESULT_HOLDER_DESC = "BKR EVA results for loan holders", RESULT_CO_HOLDER_DESC = "BKR EVA results for loan co-holders" */

    public void mapEVAPersonData(CheckEvaPersonDto checkEvaPersonDto, Application application) {

        if (checkEvaPersonDto == null) {
            return;
        }
        var processingFlag = false;
        log.debug(" Mapping EVA person Data started for business key {}", application.getBusinessKey());

        var customers = application.getLoan().getCustomers();
        var request = checkEvaPersonDto.getRequest();
        var response = checkEvaPersonDto.getResponse();

        if (customers != null && !customers.isEmpty() && request != null && response != null) {
            for (Customer customer : customers) {
                if (request.getEvaPersonRequestData().getName().equals(customer.getLastName())
                        && isSameDateOfBirth(request.getEvaPersonRequestData().getDateOfBirth(), customer.getBirthDate())
                        && !isEvaExists(customer)) {

                    processingFlag = true;
                    customer.getRiskAssessments().add(createRiskAssessment(customer, checkEvaPersonDto.getResponse()));
                }
            }
        }

        if (checkEvaPersonDto.getErrors() == null) {
            application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan()));
            processingFlag = true;
        }

        if (processingFlag) {
            log.debug(" Mapping EVA person Data success for business key {}", application.getBusinessKey());
        } else {
            log.debug(" No matching data was found for EVA person, business key {}", application.getBusinessKey());
        }

    }

    public void mapEVACompanyData(List<CheckEvaCompanyDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping EVA company Data started for business key {}", application.getBusinessKey());

            processingFlag = mapRiskAssessment(matches, application, processingFlag);

            if (processingFlag) {
                log.debug(" Mapping EVA company Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EVA company, business key {}", application.getBusinessKey());
            }
        }
    }

    private boolean mapRiskAssessment(List<CheckEvaCompanyDto> matches, Application application, boolean processingFlag) {
        for (CheckEvaCompanyDto requestAndResponse : matches) {
            if (requestAndResponse != null) {
                var companies = application.getLoan().getCompanies();
                var request = requestAndResponse.getRequest();
                var response = requestAndResponse.getResponse();

                if (companies != null && !companies.isEmpty() && request != null && response != null) {
                    processingFlag = addRiskAssessment(processingFlag, companies, request);
                }

                if (requestAndResponse.getErrors() == null) {
                    application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan()));
                    processingFlag = true;
                }
            }
        }
        return processingFlag;
    }

    private boolean addRiskAssessment(boolean processingFlag, List<Organisation> companies, EvaCompanyRequest request) {
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
        return processingFlag;
    }
    /* ...restul clasei nu era in pozele din 25.11 */
}
