// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class DNBMapper {

    private static final String DIRECTOR = "Directeur";
    private static final String TRADE_REGISTRATION = "TradeRegisterNumber";

    // Nu adauga nimic pe loan; fiecare rand seteaza campuri pe compania care se potriveste.
    public void mapDNBData(List<CheckDnbCompanyDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping DNB Data started for business key {}", application.getBusinessKey());

            for (CheckDnbCompanyDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    List<Organisation> companies = application.getLoan().getCompanies();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (request != null && companies != null && !companies.isEmpty()) {
                        for (Organisation company : companies) {
                            if (company.getLegalRegistrationNumber().equals(request.getCompany()/* ...taiat */)
                                    && company.getCountryOfResidence().equals(request.getComp/* ...taiat */)) {

                                if (response != null && response.getCompanyResponse() != null
                                        && response.getCompanyResponse().getDunsresponse() != /* ...taiat: inca 4 conditii */) {

                                    var payload = response.getCompanyResponse().getDunsrespon/* ...taiat */;
                                    var header = response.getCompanyResponse().getDunsrespons/* ...taiat */;
                                    var companySummary = payload.getCompanySummary();

                                    final Boolean principalsInformationAvailabilityIndicator /* ...taiat */;
                                    if (principalsInformationAvailabilityIndicator != null && /* ...taiat */) {
                                        //Director's Age
                                        var directorInfo = payload.getPrincipalsInformation()
                                                .getCurrentPrincipalsInformation()
                                                .getCurrentPrincipalsInformationEntry()
                                                .stream()
                                                .filter(eachPricipal -> eachPricipal.getPrinc/* ...taiat */)
                                                .filter(this::isDirector)
                                                .findFirst()
                                                .orElse(null);

                                        if (directorInfo != null) {
                                            log.debug("IsDirector  {}", directorInfo.getPrinc/* ...taiat */);
                                            company.setDirectorAge(calculateAge(directorInfo./* ...taiat */));
                                        }
                                    }

                                    if (companySummary != null && companySummary.getDnbassess/* ...taiat */) {
                                        AssessmentSummaryType dnbAssessmentOverview = company/* ...taiat */;
                                        //Paydex Score
                                        company.setPaydexScore(dnbAssessmentOverview.getDnbpa/* ...taiat */);
                                        //DNB Failure Score
                                        company.setDnbFailureScore(dnbAssessmentOverview.getD/* ...taiat */);
                                        /* ...liniile 77-78 nu erau vizibile */
                                    }

                                    //Number of employees
                                    if (payload.getOperationsandHistory() != null && /* ...taiat: inca 3 conditii */) {
                                        company.setNumberOfEmployees(payload.getOperationsand/* ...taiat */);
                                    }

                                    //Industry code
                                    if (companySummary != null && companySummary.getIdentific/* ...taiat */) {
                                        company.setIndustryCode(companySummary.getIdentificat/* ...taiat */);
                                    }

                                    //Legal Form
                                    if (companySummary.getCompanyOverview() != null && compan/* ...taiat */) {
                                        company.setCompanyLegalForm(companySummary.getCompany/* ...taiat */);
                                    }

                                    //Registration Number
                                    var registrationNumber = companySummary.getIdentification/* ...taiat */;
                                    if (registrationNumber != null) {
                                        company.setLegalRegistrationNumber(registrationNumber/* ...taiat */);
                                    }

                                    if (companySummary.getPublicFilingsOverview() != null) {
                                        //Business Failure Indicator
                                        company.setBusinessFailureIndicator(companySummary.ge/* ...taiat */);
                                        //Ceased Trading Indicator
                                        company.setCeasedTradingIndicator(companySummary.getP/* ...taiat */);
                                        //Liquidation indicator
                                        company.setLiquidationIndicator(companySummary.getPub/* ...taiat */);
                                    }

                                    //Company Age
                                    if (companySummary.getCompanyOverview() != null && compan/* ...taiat */) {
                                        company.setCompanyAge(DateUtil.parseToLocalDateTime(c/* ...taiat */));
                                    }

                                    //True if there is a response either from cache or from e/* ...taiat */
                                    company.setCompanyFound(response.getCompanyResponse() != /* ...taiat */);
                                }

                                processingFlag = true;
                            }
                        }
                    }
                }
            }

            if (processingFlag) {
                log.debug(" Mapping DNB Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for DNB, business key {}", application.getBusinessKey());
            }
        }
    }
    // Fata de cloud (21 de diferente): in cloud corpul e spart in metode (processResponse, processWithCcdp,
    // setDirectorAge, setIndicators...), exista in plus setDnbLatestAccount (latestAccountFiledDate / Period),
    // null-check pe getPersonalDetails() si loguri la info cu toata compania. In cloud lipseste verificarea
    // "dto != null": mapDNBData(null, application) arunca NPE.
}
