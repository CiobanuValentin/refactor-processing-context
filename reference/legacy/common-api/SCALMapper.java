// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

public class SCALMapper {

    private static final String APP_NAME = "SCAL";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Nu adauga nimic pe loan. mapScalPersonInitialData si mapScalEntityData au aceeasi forma.
    public void mapScalPersonData(List<CheckScalPersonDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            log.debug(" Mapping SCAL person Data started for business key {}", application.getBusinessKey());
            var processingFlag = false;

            for (CheckScalPersonDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    var customers = application.getLoan().getCustomers();
                    var request = requestAndResponse.getRequest();
                    var response = requestAndResponse.getResponse();

                    if (customers != null && !customers.isEmpty() && request != null && response != null) {
                        Customer found = customers.stream()
                                .filter(customer -> {
                                    final String lastNameWithPrefix = lastNameWithPrefix(customer);
                                    return customer.getFirstName().equals(request.getFirstName())
                                            && lastNameWithPrefix != null
                                            && lastNameWithPrefix.equals(request.getName())
                                            && isSameDate(request.getDate(), customer.getBirthDate());
                                })
                                .findAny()
                                .orElse(null);

                        if (found != null) {
                            found.setHasSanction(response.getPerson1().isIsSanction());
                            found.setPep(response.getPerson1().isIsPEP());
                            addRiskAssessment(found, response, application.getBusinessKey());
                            processingFlag = true;
                        }
                    }
                }
            }
            /* ...logul de final, ca la celelalte mappere */
        }
    }

    // mapScalPersonInitialData(List<CheckScalPersonInitialDto> matches, Application): la fel, cu potrivirea
    //   formattedInitials(customer).equals(request.getInitial()) in loc de getFirstName.
    // mapScalEntityData(List<CheckScalEntityDto> matches, Application): pe companii, cu potrivirea
    //   company.getStatutoryName().equals(request.getName()) && isSameDate(request.getDate(), company.getEstablishmentDate());
    //   found.setHasSanction(response.getEntity().isIsSanction()); found.setPep(response.getEntity().isIsPEP()).
    //
    // addOrCreate(party, riskAssessment) nu apare ca diferenta, deci e identic cu cloud-ul:
    //   lista null -> party.setRiskAssessments(List.of(riskAssessment)), altfel add.
    //
    // Fata de cloud (14 diferente): cloud adauga person1NullCheck si foundNullCheck (getPerson1() / getEntity() null
    // -> hasSanction si pep raman null) si foloseste setIsPep in loc de setPep.
}
