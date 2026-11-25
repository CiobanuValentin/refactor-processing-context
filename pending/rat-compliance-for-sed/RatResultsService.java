package com.bnpp.pf.transformation.ratproxy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ProxyResultDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ProxyResultsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;

/**
 * Da lui sed rezultatele rat: risk assessment-urile "rat.compliance" de pe clientii si companiile aplicatiei
 * salvate de rat. Contractul e cel citit de RatComplianceMapper: un rezultat per party,
 * cu responseJson = RiskAssessment-ul serializat.
 */
@Service
@RequiredArgsConstructor
public class RatResultsService {

    private final ApplicationService applicationService;

    private final Gson gson = GsonInstance.createGson();

    public ProxyResultsDto results(String businessKey, String flowId) {
        Application application = applicationService.findSavedApplication(businessKey).orElse(null);
        return toResults(businessKey, flowId, application);
    }

    /** Fara aplicatie salvata sau fara rezultate intoarce o lista goala, ceea ce pentru sed nu e o eroare. */
    ProxyResultsDto toResults(String businessKey, String flowId, Application application) {
        ProxyResultsDto dto = new ProxyResultsDto();
        dto.setBusinessKey(businessKey);
        dto.setFlowId(flowId);
        if (application == null || application.getLoan() == null) {
            return dto;
        }
        if (application.getLoan().getCustomers() != null) {
            for (Customer customer : application.getLoan().getCustomers()) {
                if (customer != null) {
                    add(dto, customer.getBusinessKey(), customer.getRiskAssessments());
                }
            }
        }
        if (application.getLoan().getCompanies() != null) {
            for (Organisation company : application.getLoan().getCompanies()) {
                if (company != null) {
                    add(dto, company.getBusinessKey(), company.getRiskAssessments());
                }
            }
        }
        return dto;
    }

    private void add(ProxyResultsDto dto, String partyBusinessKey, List<RiskAssessment> riskAssessments) {
        if (riskAssessments == null) {
            return;
        }
        for (RiskAssessment riskAssessment : riskAssessments) {
            if (riskAssessment != null
                    && RatMappingService.RAT_COMPLIANCE.equals(riskAssessment.getClassificationInstrumentCode())) {
                ProxyResultDto result = new ProxyResultDto();
                result.setPartyBusinessKey(partyBusinessKey);
                result.setResponseJson(gson.toJson(riskAssessment));
                dto.getResults().add(result);
            }
        }
    }
}
