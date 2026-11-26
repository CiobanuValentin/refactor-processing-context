package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importurile nu erau vizibile in poze: de corectat din IDE (Optimize Imports).
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKRCommons.isSameDateOfBirth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckRvdrCreditDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: mapRVDRData primeste lista, ca in legacy; corpul din cloud pentru un rand e in mapCustomers.
 * Nu se adauga nimic pe loan. Pastrat din cloud: personNullCheck, personNameNullCheck si verifyResponse (in legacy,
 * request / nume / raspuns null dadeau NPE).
 */
@Component
@Slf4j
public class RVDRMapper {

    private static final String CIR_CHECK = "CIR OK";
    private static final String CCBR_CHECK = "CCBR OK";

    public void mapRVDRData(List<CheckRvdrCreditDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping RVDR Data started for business key {}", application.getBusinessKey());

            for (CheckRvdrCreditDto checkRvdrCreditDto : matches) {
                if (checkRvdrCreditDto != null) {
                    processingFlag = mapCustomers(checkRvdrCreditDto, application, processingFlag);
                }
            }

            if (processingFlag) {
                log.debug(" Mapping RVDR Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for RVDR, business key {}", application.getBusinessKey());
            }
        }
    }

    private boolean mapCustomers(CheckRvdrCreditDto checkRvdrCreditDto, Application application, boolean processingFlag) {
        List<Customer> customers = application.getLoan().getCustomers();
        CreditCheckPerson person = personNullCheck(checkRvdrCreditDto);

        if (customers != null && !customers.isEmpty() && person != null && person.getDateOfBirth() != null && person.getAddress() != null) {
            for (Customer customer : customers) {
                if (personNameNullCheck(person.getName()).equals(customer.getLastName()) && customer.getAddresses() != null
                        && !customer.getAddresses().isEmpty() && person.getAddress().getHouseNumber().getN() == customer.getAddresses().get(0).getHouseNumber()
                        && isSameDateOfBirth(person.getDateOfBirth(), customer.getBirthDate())) {

                    processingFlag = true;

                    Map<String, Object> classificationResults = new HashMap<>();
                    var riskAssessment = new RiskAssessment();

                    verifyResponse(classificationResults, checkRvdrCreditDto);

                    riskAssessment.setClassificationResults(classificationResults);
                    riskAssessment.setClassificationInstrumentCode("bkr.circcbr");
                    riskAssessment.setClassificationInstrumentName("BKR cir & Ccbr");
                    riskAssessment.setDescription("BKR Cir & Ccbr");

                    customer.getRiskAssessments().add(riskAssessment);
                }
            }
        }
        return processingFlag;
    }

    private String personNameNullCheck(String personName) {
        return StringUtils.isEmpty(personName) ? StringUtils.EMPTY : personName;
    }

    private CreditCheckPerson personNullCheck(CheckRvdrCreditDto checkRvdrCreditDto) {
        CreditCheckRequestDataType creditCheckRequestDataType = Objects.isNull(checkRvdrCreditDto.getRequest()) ? null : checkRvdrCreditDto.getRequest().getCreditCheckRequestData();
        return Objects.isNull(creditCheckRequestDataType) ? null : creditCheckRequestDataType.getPerson();
    }

    private void verifyResponse(Map<String, Object> classificationResults, CheckRvdrCreditDto checkRvdrCreditDto) {
        if (Objects.isNull(checkRvdrCreditDto.getResponse())) {
            classificationResults.put("CirCheck", "false");
            classificationResults.put("CcbrCheck", "false");
        } else {
            String overallOutcomeDescription = Objects.isNull(checkRvdrCreditDto.getResponse().getResult()) ? StringUtils.EMPTY : checkRvdrCreditDto.getResponse().getResult().getOverallOutcomeDescription();
            classificationResults.put("CirCheck", String.valueOf(overallOutcomeDescription.toUpperCase().contains(CIR_CHECK)));
            classificationResults.put("CcbrCheck", String.valueOf(overallOutcomeDescription.toUpperCase().contains(CCBR_CHECK)));
        }
    }
}
