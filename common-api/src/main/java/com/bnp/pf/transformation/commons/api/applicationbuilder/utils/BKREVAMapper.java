package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importurile nu erau vizibile in poze: de corectat din IDE (Optimize Imports).
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKRCommons.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaCompanyDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaPersonDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: mapEVAPersonData primeste lista, ca in legacy. Corpul de pe un rand e cel din cloud, mutat in
 * mapPersonRiskAssessment (aceeasi forma ca mapRiskAssessment de la company). Ca in legacy, rezultatele de pe loan se
 * adauga in bucla, o data pentru fiecare rand fara erori.
 *
 * Neschimbat, identic in legacy: in createRiskAssessments, rezultatul co-holder e pus pe riskAssmntHolder.
 */
@Component
@Slf4j
public class BKREVAMapper {

    public static final String CLASS_INSTR_CD = "bkr.eva";
    public static final String CLASS_INSTR_NAME = "Bkr Eva";
    public static final String RESULT_DESC = "BKR EVA results";
    public static final String RESULT_HOLDER_DESC = "BKR EVA results for loan holders";
    public static final String RESULT_CO_HOLDER_DESC = "BKR EVA results for loan co-holders";

    public void mapEVAPersonData(List<CheckEvaPersonDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping EVA person Data started for business key {}", application.getBusinessKey());

            processingFlag = mapPersonRiskAssessment(matches, application, processingFlag);

            if (processingFlag) {
                log.debug(" Mapping EVA person Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EVA person, business key {}", application.getBusinessKey());
            }
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

    private boolean mapPersonRiskAssessment(List<CheckEvaPersonDto> matches, Application application, boolean processingFlag) {
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
                            customer.getRiskAssessments().add(createRiskAssessment(customer, response));
                        }
                    }
                }

                if (requestAndResponse.getErrors() == null) {
                    application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan()));
                    processingFlag = true;
                }
            }
        }
        return processingFlag;
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

    private boolean isSameZipCode(Organisation company, EvaCompanyRequestData companyData) {
        final ZipCodeType zipCode = getZipCode(company.getAddresses().get(0));
        if (zipCode != null) {
            final ZipCodeType zipCodeCompany = companyData.getZipCode();
            if (zipCodeCompany != null) {
                return zipCodeCompany.getA().equals(zipCode.getA()) && zipCodeCompany.getN() == zipCode.getN();
            }
        }
        return false;
    }

    private boolean isEvaExists(Customer customers) {
        Optional<RiskAssessment> riskAssessments = customers.getRiskAssessments()
                .stream()
                .filter(Objects::nonNull)
                .filter(risk -> CLASS_INSTR_CD.equals(risk.getClassificationInstrumentCode()))
                .filter(risk -> CLASS_INSTR_NAME.equals(risk.getClassificationInstrumentName())).findAny();
        return riskAssessments.isPresent();
    }

    private boolean isEvaExists(Organisation organisation) {
        Optional<RiskAssessment> riskAssessments = organisation.getRiskAssessments()
                .stream()
                .filter(Objects::nonNull)
                .filter(risk -> CLASS_INSTR_CD.equals(risk.getClassificationInstrumentCode()))
                .filter(risk -> CLASS_INSTR_NAME.equals(risk.getClassificationInstrumentName())).findAny();
        return riskAssessments.isPresent();
    }

    private RiskAssessment createRiskAssessment() {
        var riskAssessment = new RiskAssessment();
        riskAssessment.setClassificationInstrumentCode(CLASS_INSTR_CD);
        riskAssessment.setClassificationInstrumentName(CLASS_INSTR_NAME);
        riskAssessment.setRiskLevel(RiskLevel.LOW);
        riskAssessment.setDescription(RESULT_DESC);
        Map<String, Object> classResult = new HashMap<>();
        classResult.put(RESULT, "false");
        riskAssessment.setClassificationResults(classResult);
        riskAssessment.setHasError(false);
        return riskAssessment;
    }

    private RiskAssessment createRiskAssessment(Customer customer, EvaResponse evaResponse) {
        var riskAssessment = createRiskAssessment();
        riskAssessment.setCustomerBusinessKey(customer.getBusinessKey());
        Map<String, Object> classResult = new HashMap<>();

        if (evaResponse != null && evaResponse.getResult() != null) {
            if (evaResponse.getResult().getEva().getResult().equals("1")) {
                classResult.put(RESULT, RESULT_TRUE);
            } else {
                classResult.put(RESULT, RESULT_FALSE);
            }
        } else {
            classResult.put(RESULT, null);
        }

        riskAssessment.setClassificationResults(classResult);
        return riskAssessment;
    }

    private String getRiskLevelHolder(Loan loan) {
        Optional<Customer> holder = loan.getCustomers().stream().filter(customer -> HOLDER.getApplicant().equals(customer.getApplicantType()) || HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType())).findAny();
        return holder.isPresent() ? RESULT_TRUE : RESULT_FALSE;
    }

    private String getRiskLevelCoHolder(Loan loan) {
        Optional<Customer> coHolder = loan.getCustomers().stream().filter(customer -> CO_HOLDER.getApplicant().equals(customer.getApplicantType()) || CO_HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType())).findAny();
        return coHolder.isPresent() ? RESULT_TRUE : RESULT_FALSE;
    }

    private ZipCodeType getZipCode(Address address) {
        if (address.getZipCode() != null) {
            var zipCode = new ZipCodeType();
            zipCode.setA(address.getZipCode().substring(4));
            zipCode.setN(Integer.parseInt(address.getZipCode().substring(0, 4)));
            return zipCode;
        }

        return null;
    }

    private List<RiskAssessment> createRiskAssessments(Loan loan) {
        var riskAssessmentList = new ArrayList<RiskAssessment>();
        var riskAssmntHolder = createRiskAssessment();
        riskAssmntHolder.setResultType(RESULT_HOLDER);
        riskAssmntHolder.setDescription(RESULT_HOLDER_DESC);
        Map<String, Object> classResultHolder = new HashMap<>();
        classResultHolder.put(RESULT, getRiskLevelHolder(loan));
        riskAssmntHolder.setClassificationResults(classResultHolder);
        riskAssessmentList.add(riskAssmntHolder);

        var riskAssmntCoHolder = createRiskAssessment();
        riskAssmntCoHolder.setResultType(RESULT_CO_HOLDER);
        riskAssmntCoHolder.setDescription(RESULT_CO_HOLDER_DESC);
        Map<String, Object> classResultCoHolder = new HashMap<>();
        classResultCoHolder.put(RESULT, getRiskLevelCoHolder(loan));
        riskAssmntHolder.setClassificationResults(classResultCoHolder);
        riskAssessmentList.add(riskAssmntCoHolder);

        return riskAssessmentList;
    }
}
