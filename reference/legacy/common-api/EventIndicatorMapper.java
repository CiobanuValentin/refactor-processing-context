// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

@Component
@Slf4j
public class EventIndicatorMapper {
    // Nu adauga nimic pe loan.
    public void mapEventIndicatorData(List<CheckEventIndicatorDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping EventIndicator Data started for business key {}", application.getBusinessKey());

            for (CheckEventIndicatorDto requestAndResponse : matches) {
                if (requestAndResponse != null) {

                    List<Organisation> companies = application.getLoan().getCompanies();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (request != null && companies != null && !companies.isEmpty()) {
                        for (Organisation company : companies) {
                            if (company.getLegalRegistrationNumber().equals(request.getCompan/* ...taiat */)
                                    && company.getCountryOfResidence().equals(request.getComp/* ...taiat */)) {
                                if (response != null && response.getCompanyResponse() != null
                                        && response.getCompanyResponse().getDunsresponse() != /* ...taiat: inca 2 conditii */) {
                                    var triggerResult = response.getCompanyResponse().getDuns/* ...taiat */;

                                    // company.setOutput01 ... setOutput25(getBooleanVal(triggerResult.getOutputNN()))
                                    company.setAverageCount(triggerResult.getAverageCount());
                                    company.setStandardDeviation(triggerResult.getStandardDev/* ...taiat */);
                                    company.setMinCount(triggerResult.getMinCount());
                                    company.setMaxCount(triggerResult.getMaxCount());
                                    company.setNumberRows(triggerResult.getNumberRows());
                                    company.setActive(triggerResult.isIsActive() != null ? tr/* ...taiat */);
                                    if (triggerResult.getAddDate() != null) {
                                        company.setAddDate(DateUtil.longToLocalDateTime(trigg/* ...taiat */));
                                    }
                                    company.setResultStatus(triggerResult.getResultStatus());

                                    processingFlag = true;
                                }
                            }
                        }
                    }
                }
            }
            if (processingFlag) {
                log.debug(" Mapping EventIndicator Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EventIndicator, business key {}", /* ...taiat */);
            }
        }
    }

    private boolean getBooleanVal(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(output);
    }
    // Fata de cloud (9 diferente): in cloud corpul e spart in processResponce / setOutput, cu AtomicBoolean si un log
    // la info cu toata compania. In cloud lipseste verificarea "dto != null": apelul cu null arunca NPE.
}
