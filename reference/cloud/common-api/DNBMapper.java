// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

@Component
@Slf4j
public class DNBMapper {

    private static final String DIRECTOR = "Directeur";
    private static final String TRADE_REGISTRATION = "TradeRegisterNumber";

    public void mapDNBData(CheckDnbCompanyDto checkDnbCompanyDto, Application application) {
        AtomicBoolean processingFlag = new AtomicBoolean(false);

        log.debug(" Mapping DNB Data started for business key {}", application.getBusinessKey());
        List<Organisation> companies = application.getLoan().getCompanies();

        RequestCompany request = checkDnbCompanyDto.getRequest();
        ResponseRoot response = checkDnbCompanyDto.getResponse();

        if (request != null && CollectionUtils.isNotEmpty(companies)) {
            companies.forEach(company -> {
                Company requestCompany = request.getCompany();

                boolean c1 = company.getLegalRegistrationNumber().equals(requestCompany.getCocNumber());
                boolean c2 = company.getCountryOfResidence().equals(requestCompany.getCountryCode());

                if (c1 && c2) {
                    log.info("company before set all the fields is: {}", company);
                    processResponse(company, response);
                    processingFlag.set(true);
                    log.info("company after set all the fields is: {}", company);
                }
            });
        }

        if (processingFlag.get()) {
            log.debug(" Mapping DNB Data success for business key {}", application.getBusinessKey());
        }
        /* ...restul clasei nu era in pozele din 25.11 */
    }
}
