// REFERINTA - cod existent (legacy, common-api-1.23-sources.jar), transcris din diff-ul din IDE (stanga = legacy).
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

@Component
@Slf4j
public class RVDRMapper {

    private static final String CIR_CHECK = "CIR OK";
    private static final String CCBR_CHECK = "CCBR OK";

    // Nu adauga nimic pe loan.
    public void mapRVDRData(List<CheckRvdrCreditDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping RVDR Data started for business key {}", application.getBusinessKey());

            for (CheckRvdrCreditDto requestAndResponse : matches) {
                if (requestAndResponse != null) {
                    List<Customer> customers = application.getLoan().getCustomers();
                    CreditCheckPerson person = requestAndResponse.getRequest().getCreditCheckR/* ...taiat */;

                    if (customers != null && !customers.isEmpty() && person != null && person./* ...taiat */) {
                        for (Customer customer : customers) {
                            if (person.getName().equals(customer.getLastName()) && customer.ge/* ...taiat */
                                    && !customer.getAddresses().isEmpty() && person.getAddress/* ...taiat */
                                    && isSameDateOfBirth(person.getDateOfBirth(), customer.get/* ...taiat */)) {

                                processingFlag = true;

                                Map<String, Object> classificationResults = new HashMap<>();

                                var riskAssessment = new RiskAssessment();

                                classificationResults.put("CirCheck", String.valueOf(requestAn/* ...taiat */));
                                classificationResults.put("CcbrCheck", String.valueOf(requestA/* ...taiat */));

                                riskAssessment.setClassificationResults(classificationResults);

                                riskAssessment.setClassificationInstrumentCode("bkr.circcbr");
                                riskAssessment.setClassificationInstrumentName("BKR cir & Ccbr");
                                riskAssessment.setDescription("BKR Cir & Ccbr");

                                customer.getRiskAssessments().add(riskAssessment);
                            }
                        }
                    }
                }
            }

            if (processingFlag) {
                log.debug(" Mapping RVDR Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for RVDR, business key {}", application/* ...taiat */);
            }
        }
    }
    // Fata de cloud (12 diferente): cloud adauga doar protectii la null - personNullCheck (request null),
    // personNameNullCheck (nume gol) si verifyResponse (raspuns null -> CirCheck / CcbrCheck = "false";
    // descriere null -> ""). In legacy aceste cazuri aruncau NPE.
}
