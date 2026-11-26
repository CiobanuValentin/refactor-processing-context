package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importurile nu erau vizibile in poze: de corectat din IDE (Optimize Imports).
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalEntityDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonInitialDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: cele trei metode publice primesc lista, ca in legacy. Corpul din cloud pentru un rand e in
 * mapScalPerson / mapScalPersonInitial / mapScalEntity. Nu se adauga nimic pe loan.
 *
 * Pastrat din cloud: person1NullCheck si foundNullCheck (raspuns fara person1 / entity -> hasSanction si pep raman null;
 * in legacy dadea NPE).
 */
@Component
@Slf4j
public class SCALMapper {

    private static final String APP_NAME = "SCAL";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void mapScalPersonData(List<CheckScalPersonDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            log.debug(" Mapping SCAL person Data started for business key {}", application.getBusinessKey());
            var processingFlag = false;

            for (CheckScalPersonDto checkScalPersonDto : matches) {
                if (checkScalPersonDto != null) {
                    processingFlag = mapScalPerson(checkScalPersonDto, application) || processingFlag;
                }
            }

            if (processingFlag) {
                log.debug(" Mapping SCAL person Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for SCAL person, business key {}", application.getBusinessKey());
            }
        }
    }

    public void mapScalPersonInitialData(List<CheckScalPersonInitialDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            log.debug(" Mapping SCAL person initial Data started for business key {}", application.getBusinessKey());
            var processingFlag = false;

            for (CheckScalPersonInitialDto checkScalPersonInitialDto : matches) {
                if (checkScalPersonInitialDto != null) {
                    processingFlag = mapScalPersonInitial(checkScalPersonInitialDto, application) || processingFlag;
                }
            }

            if (processingFlag) {
                log.debug(" Mapping SCAL person initial Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for SCAL person initial, business key {}", application.getBusinessKey());
            }
        }
    }

    public void mapScalEntityData(List<CheckScalEntityDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            log.debug(" Mapping SCAL entity Data started for business key {}", application.getBusinessKey());
            var processingFlag = false;

            for (CheckScalEntityDto checkScalEntityDto : matches) {
                if (checkScalEntityDto != null) {
                    processingFlag = mapScalEntity(checkScalEntityDto, application) || processingFlag;
                }
            }

            if (processingFlag) {
                log.debug(" Mapping SCAL entity Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for SCAL entity, business key {}", application.getBusinessKey());
            }
        }
    }

    private boolean mapScalPerson(CheckScalPersonDto checkScalPersonDto, Application application) {
        var customers = application.getLoan().getCustomers();
        var request = checkScalPersonDto.getRequest();
        var response = checkScalPersonDto.getResponse();

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
                person1NullCheck(response.getPerson1(), found);
                addRiskAssessment(found, response, application.getBusinessKey());
                return true;
            }
        }
        return false;
    }

    private boolean mapScalPersonInitial(CheckScalPersonInitialDto checkScalPersonInitialDto, Application application) {
        var customers = application.getLoan().getCustomers();
        var request = checkScalPersonInitialDto.getRequest();
        var response = checkScalPersonInitialDto.getResponse();

        if (customers != null && !customers.isEmpty() && request != null && response != null) {
            Customer found = customers.stream()
                    .filter(customer -> {
                        final String lastNameWithPrefix = lastNameWithPrefix(customer);
                        return formattedInitials(customer).equals(request.getInitial())
                                && lastNameWithPrefix != null
                                && lastNameWithPrefix.equals(request.getName())
                                && isSameDate(request.getDate(), customer.getBirthDate());
                    })
                    .findAny()
                    .orElse(null);

            if (found != null) {
                person1NullCheck(response.getPerson1(), found);
                addRiskAssessment(found, response, application.getBusinessKey());
                return true;
            }
        }
        return false;
    }

    private boolean mapScalEntity(CheckScalEntityDto checkScalEntityDto, Application application) {
        var companies = application.getLoan().getCompanies();
        var request = checkScalEntityDto.getRequest();
        var response = checkScalEntityDto.getResponse();

        if (companies != null && !companies.isEmpty() && request != null && response != null) {
            var found = companies.stream()
                    .filter(company -> company.getStatutoryName().equals(request.getName())
                            && isSameDate(request.getDate(), company.getEstablishmentDate()))
                    .findAny()
                    .orElse(null);

            return foundNullCheck(found, response, application.getBusinessKey());
        }
        return false;
    }

    private void person1NullCheck(CheckPersonResult person1, Customer found) {
        if (Objects.isNull(person1)) {
            found.setHasSanction(null);
            found.setIsPep(null);
        } else {
            found.setHasSanction(person1.isIsSanction());
            found.setIsPep(person1.isIsPEP());
        }
    }

    private boolean isSameDate(Calendar wsdlDate, LocalDateTime javaDate) {
        return wsdlDate != null && javaDate != null &&
                wsdlDate.get(Calendar.DAY_OF_MONTH) == javaDate.getDayOfMonth()
                && wsdlDate.get(Calendar.MONTH) + 1 == javaDate.getMonthValue()
                && wsdlDate.get(Calendar.YEAR) == javaDate.getYear();
    }

    private String lastNameWithPrefix(Customer customer) {
        if (customer.getPrefixLastName() == null) {
            return null;
        } else {
            String prefixLastName = customer.getPrefixLastName();

            return StringUtil.isNullOrEmpty(prefixLastName) ? customer.getLastName() : customer.getPrefixLastName() + " " + customer.getLastName();
        }
    }

    private String formattedInitials(Customer customer) {
        return customer == null ? null : customer.getInitialsName().trim().replace(" ", "").replace(".", "");
    }

    private void addRiskAssessment(Party party, CheckResult result, String businessKey) {

        var riskAssessment = new RiskAssessment();

        riskAssessment.setLoanBusinessKey(businessKey);
        riskAssessment.setDescription("SCAL Sanction and PEP Checks");
        riskAssessment.setClassificationInstrumentName(APP_NAME);
        riskAssessment.setClassificationInstrumentCode(APP_NAME);
        try {
            riskAssessment.setRawResult(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Error adding risk Assessment, details: {}", e.getMessage());
        }

        addOrCreate(party, riskAssessment);
    }

    private void addOrCreate(Party party, RiskAssessment riskAssessment) {
        if (party.getRiskAssessments() == null) {
            party.setRiskAssessments(List.of(riskAssessment));
        } else {
            party.getRiskAssessments().add(riskAssessment);
        }
    }

    private boolean foundNullCheck(Organisation found, CheckResult response, String businessKey) {
        if (found != null) {
            Boolean sanction = Objects.isNull(response.getEntity()) ? null : response.getEntity().isIsSanction();
            Boolean pep = Objects.isNull(response.getEntity()) ? null : response.getEntity().isIsPEP();
            found.setHasSanction(sanction);
            found.setIsPep(pep);
            addRiskAssessment(found, response, businessKey);
            return true;
        }
        return false;
    }
}
