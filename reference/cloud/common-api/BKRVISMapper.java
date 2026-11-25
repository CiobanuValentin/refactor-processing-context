// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class BKRVISMapper {
    /* ...constante: RESULT_DESC = "BKR VIS results", RESULT_HOLDER_DESC = "BKR VIS results for loan holders",
       RESULT_CO_HOLDER_DESC = "BKR VIS results for loan co-holders" */

    public void mapVISData(CheckVisDto checkVisDto, Application application) {
        List<Customer> customers = application.getLoan().getCustomers();

        if (customers != null && !customers.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping VIS Data started for business key {}", application.getBusinessKey());

            if (checkVisDto != null) {
                var request = checkVisDto.getRequest();
                var response = checkVisDto.getResponse();

                if (request != null && response != null) {
                    processingFlag = createRiskAssessment(customers, request, processingFlag, response);
                }

                processingFlag = checkErrors(checkVisDto, application, response, processingFlag);

            } else {
                processingFlag = true;
                for (Customer customer : customers) {
                    customer.getRiskAssessments().add(createRiskAssessment(customer, null));
                }
                application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan(), getResultClassCd(null)));
            }

            if (processingFlag) {
                log.debug(" Mapping VIS Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for VIS, business key {}", application.getBusinessKey());
            }
        }
    }
    /* ...checkErrors, createRiskAssessment(s) si restul clasei nu erau in pozele din 25.11 */
}
