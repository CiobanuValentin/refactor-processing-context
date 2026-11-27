package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importuri: aici sunt doar cele sigure. Pastreaza blocul de importuri din fisierul existent (inclusiv importurile
// statice pentru isSameDateOfBirth, RESULT, RESULT_TRUE, HOLDER, LOW, ...) si inlocuieste doar corpul clasei.

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.BKRCommons.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckVisDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: mapVISData primeste lista, ca in legacy. Pentru fiecare rand se apeleaza createRiskAssessment si
 * checkErrors din cloud (in legacy, acelasi cod era direct in bucla). Fara randuri (lista null sau goala), ramura din
 * legacy: rezultat implicit pe fiecare client si doua rezultate pe loan, cu getResultClassCd(null).
 */
@Component
@Slf4j
public class BKRVISMapper {

    public static final String CLASS_INSTR_CD = "bkr.vis";
    public static final String CLASS_INSTR_NAME = "Bkr Vis";
    public static final String RESULT_DESC = "BKR VIS results";
    public static final String RESULT_HOLDER_DESC = "BKR VIS results for loan holders";
    public static final String RESULT_CO_HOLDER_DESC = "BKR VIS results for loan co-holders";

    public void mapVISData(List<CheckVisDto> matches, Application application) {
        List<Customer> customers = application.getLoan().getCustomers();

        if (customers != null && !customers.isEmpty()) {
            var processingFlag = false;
            log.debug(" Mapping VIS Data started for business key {}", application.getBusinessKey());

            if (matches != null && !matches.isEmpty()) {
                for (CheckVisDto checkVisDto : matches) {
                    if (checkVisDto != null) {
                        var request = checkVisDto.getRequest();
                        var response = checkVisDto.getResponse();

                        if (request != null && response != null) {
                            processingFlag = createRiskAssessment(customers, request, processingFlag, response);
                        }

                        processingFlag = checkErrors(checkVisDto, application, response, processingFlag);
                    }
                }
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

    private boolean checkErrors(CheckVisDto checkVisDto, Application application, VisResponse response, boolean processingFlag) {
        if (checkVisDto.getErrors() == null) {
            application.getLoan().getRiskAssessments().addAll(createRiskAssessments(application.getLoan(), getResultClassCd(response)));
            processingFlag = true;
        }
        return processingFlag;
    }

    private boolean createRiskAssessment(List<Customer> customers, VisRequest request, boolean processingFlag, VisResponse response) {
        for (Customer customer : customers) {
            if (request.getVisRequestData().getCountryCode().equals(customer.getIdentification().getIsoIssuedCountryCode()) &&
                    request.getVisRequestData().getType().equals(getType(customer.getIdentification())) &&
                    request.getVisRequestData().getDocumentNumber().equals(customer.getIdentification().getCode()) &&
                    isSameDateOfBirth(request.getVisRequestData().getDateOfBirth(), customer.getBirthDate()) &&
                    !isVisExists(customer)) {

                processingFlag = true;
                customer.getRiskAssessments().add(createRiskAssessment(customer, response));
            }
        }
        return processingFlag;
    }

    private String getResultClassCd(VisResponse visResponse) {
        return String.valueOf((visResponse != null && visResponse.getResult() != null && visResponse.getResult().getVis() != null && visResponse.getResult().getVis().getCode() > 0) ? visResponse.getResult().getVis().getCode() : "1");
    }

    private String getRiskLevelHolder(Loan loan, String rsltClassCd) {
        Optional<Customer> holder = loan.getCustomers().stream().filter(customer -> HOLDER.getApplicant().equals(customer.getApplicantType()) || HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType())).findAny();
        return holder.isPresent() ? rsltClassCd : RESULT_FALSE;
    }

    private String getRiskLevelCoHolder(Loan loan, String rsltClassCd) {
        Optional<Customer> coHolder = loan.getCustomers().stream().filter(customer -> CO_HOLDER.getApplicant().equals(customer.getApplicantType()) || CO_HOLDER_PARTNER.getApplicant().equals(customer.getApplicantType())).findAny();
        return coHolder.isPresent() ? rsltClassCd : RESULT_FALSE;
    }

    private boolean isVisExists(Customer customer) {
        Optional<RiskAssessment> riskAssessments = customer.getRiskAssessments()
                .stream()
                .filter(Objects::nonNull)
                .filter(risk -> CLASS_INSTR_CD.equals(risk.getClassificationInstrumentCode()))
                .filter(risk -> CLASS_INSTR_NAME.equals(risk.getClassificationInstrumentName())).findAny();
        return riskAssessments.isPresent();
    }

    private String getType(Identification identificationType) {
        return (identificationType.getIdentificationType() != null && identificationType.getIsoIssuedCountryCode() != null
                && identificationType.getIdentificationType().equals(IdentificationType.ID_CARD.getIdentification()) && !identificationType.getIsoIssuedCountryCode().equalsIgnoreCase("NL")) ? "VN" : "PP";
    }

    private RiskAssessment createRiskAssessment() {
        var riskAssessment = new RiskAssessment();
        riskAssessment.setClassificationInstrumentCode(CLASS_INSTR_CD);
        riskAssessment.setClassificationInstrumentName(CLASS_INSTR_NAME);
        riskAssessment.setRiskLevel(LOW);
        riskAssessment.setDescription(RESULT_DESC);
        riskAssessment.setApplicationStatus(ApplicationStatus.UNKNOWN.getValue());
        riskAssessment.setHasError(false);
        return riskAssessment;
    }

    private RiskAssessment createRiskAssessment(Customer customer, VisResponse visResponse) {
        var riskAssessment = createRiskAssessment();
        riskAssessment.setCustomerBusinessKey(customer.getBusinessKey());
        Map<String, Object> classResult = new HashMap<>();
        classResult.put(RESULT, visResponse != null ? RESULT_FALSE : null);
        riskAssessment.setClassificationResults(classResult);
        return riskAssessment;
    }

    private List<RiskAssessment> createRiskAssessments(Loan loan, String rsltClassCd) {
        List<RiskAssessment> riskAssessmentList = new ArrayList<>();
        var riskAssmntHolder = createRiskAssessment();
        riskAssmntHolder.setResultType(RESULT_HOLDER);
        riskAssmntHolder.setDescription(RESULT_HOLDER_DESC);
        Map<String, Object> classResultHolder = new HashMap<>();
        classResultHolder.put(RESULT, getRiskLevelHolder(loan, rsltClassCd));
        riskAssmntHolder.setClassificationResults(classResultHolder);
        riskAssessmentList.add(riskAssmntHolder);

        var riskAssmntCoHolder = createRiskAssessment();
        riskAssmntCoHolder.setResultType(RESULT_CO_HOLDER);
        riskAssmntCoHolder.setDescription(RESULT_CO_HOLDER_DESC);
        Map<String, Object> classResultCoHolder = new HashMap<>();
        classResultCoHolder.put(RESULT, getRiskLevelCoHolder(loan, rsltClassCd));
        riskAssmntHolder.setClassificationResults(classResultCoHolder);
        riskAssessmentList.add(riskAssmntCoHolder);

        return riskAssessmentList;
    }
}
