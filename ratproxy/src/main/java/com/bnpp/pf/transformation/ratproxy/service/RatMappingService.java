package com.bnpp.pf.transformation.ratproxy.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: pachetele pentru modelul common, client, entitati, repository-uri si modelul CloverLeaf
// (in afara de LoanMapping si datasource.model.Loan, vazute in cod) sunt presupuse.
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.model.ApplicationStatus;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;
import com.bnp.pf.transformation.commons.api.model.RiskLevel;
import com.bnp.pf.transformation.commons.api.model.ScoringResult;
import com.bnpp.pf.transformation.ratproxy.client.BNPParibasCloverLeafAPI;
import com.bnpp.pf.transformation.ratproxy.datasource.model.ApplicantScore;
import com.bnpp.pf.transformation.ratproxy.datasource.model.LegalEntityScore;
import com.bnpp.pf.transformation.ratproxy.datasource.model.Loan;
import com.bnpp.pf.transformation.ratproxy.datasource.model.Risk;
import com.bnpp.pf.transformation.ratproxy.entity.CheckLoanComplianceEntity;
import com.bnpp.pf.transformation.ratproxy.entity.CheckLoanSanctionEntity;
import com.bnpp.pf.transformation.ratproxy.repository.CheckLoanComplianceRepository;
import com.bnpp.pf.transformation.ratproxy.repository.CheckLoanSanctionRepository;
import com.bnpp.pf.transformation.ratproxy.utils.mapper.LoanMapping;

/**
 * Cei trei pasi catre CloverLeaf: sanction, compliance si score. Fiecare pas intoarce false daca a esuat,
 * dupa ce a pus o singura eroare in context; niciunul nu arunca pentru o eroare a API-ului sau de mapare.
 *
 * TODO: metodele din BNPParibasCloverLeafAPI nu mai primesc collectError / index / errorMessage / context:
 * arunca exceptia, iar eroarea se inregistreaza aici, o singura data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatMappingService {

    static final String RAT_COMPLIANCE = "rat.compliance";
    // TODO: foloseste constanta MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD existenta
    static final String MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD = "MediumRiskOrAlertsOrOnHold";
    private static final String BAD_REQUEST = "400";
    private static final String INTERNAL_ERROR = "500";
    private static final String SERVICE_UNAVAILABLE = "503";

    private final BNPParibasCloverLeafAPI cloverLeafApi;
    private final CheckLoanComplianceRepository loanComplianceRepository;
    private final CheckLoanSanctionRepository checkLoanSanctionRepo;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    private final Gson gson = GsonInstance.createGson();

    /** Trimite loan-ul la verificarea de sanctiuni si salveaza cererea in check_loan_sanction, si la esec. */
    public boolean processSanction(ProxyContext ctx) {
        String businessKey = ctx.getBusinessKey();
        log.info("Process RAT sanction data for businessKey {}", businessKey);
        Loan loan = toRestLoan(ctx, true);
        if (loan == null) {
            return false;
        }
        Boolean response = null;
        boolean succeeded = true;
        try {
            response = cloverLeafApi.putSanctionWithHttpMessages(businessKey, loan);
        } catch (RuntimeException e) {
            apiFailed(ctx, e);
            succeeded = false;
        }
        // O singura salvare; inainte, la eroare, randul se salva a doua oara din catch.
        checkLoanSanctionRepo.save(modelMapper.map(checkDto(ctx, loan, response), CheckLoanSanctionEntity.class));
        return succeeded;
    }

    /** Trimite loan-ul la verificarea de compliance si salveaza cererea in check_loan_compliance, si la esec. */
    public boolean processCompliance(ProxyContext ctx) {
        String businessKey = ctx.getBusinessKey();
        log.info("Process RAT compliance data for businessKey {}", businessKey);
        Loan loan = toRestLoan(ctx, false);
        if (loan == null) {
            return false;
        }
        Boolean response = null;
        boolean succeeded = true;
        try {
            // TODO: de confirmat cu businessul - "private use" merge pe endpointul de company, ceea ce pare inversat.
            response = ctx.getApplication().isCollateralIsPrivateUse()
                    ? cloverLeafApi.putComplianceCompanyWithHttpMessages(businessKey, loan)
                    : cloverLeafApi.putComplianceWithHttpMessages(businessKey, loan);
        } catch (RuntimeException e) {
            apiFailed(ctx, e);
            succeeded = false;
        }
        loanComplianceRepository.save(modelMapper.map(checkDto(ctx, loan, response), CheckLoanComplianceEntity.class));
        return succeeded;
    }

    /** Citeste scorul din CloverLeaf si pune rezultatul pe aplicatie si pe fiecare client sau companie. */
    public boolean applyScore(ProxyContext ctx) {
        Application application = ctx.getApplication();
        log.info("Get RAT scores for businessKey {}", application.getBusinessKey());
        try {
            Risk risk = cloverLeafApi.getScoreWithHttpMessages(application.getBusinessKey());
            if (risk != null) {
                ScoringResult scoringResult = toScoringResult(risk, application, ctx);
                addToParties(scoringResult, application);
                application.setScoringResult(scoringResult);
            }
            return true;
        } catch (RuntimeException e) {
            apiFailed(ctx, e);
            return false;
        }
    }

    ScoringResult toScoringResult(Risk risk, Application application, ProxyContext ctx) {
        ScoringResult result = new ScoringResult();
        result.setLoanBusinessKey(risk.getApplicationId());
        // Cel mai rau caz dintre aplicanti; porneste de la accepted.
        result.setApplicationStatus(ApplicationStatus.ACCEPTED);

        String rawResult;
        try {
            rawResult = objectMapper.writeValueAsString(risk);
        } catch (JsonProcessingException e) {
            String message = "Error during the serialization of Risk Object";
            log.error(message, e);
            rawResult = e.getMessage();
            ctx.addError(INTERNAL_ERROR, message);
        }
        addApplicants(risk, application, rawResult, result);
        addLegalEntities(risk, rawResult, result);
        return result;
    }

    private void addApplicants(Risk risk, Application application, String rawResult, ScoringResult result) {
        if (CollectionUtils.isEmpty(risk.getApplicants())) {
            return;
        }
        List<Customer> customers = application.getLoan().getCustomers();
        for (ApplicantScore applicant : risk.getApplicants()) {
            RiskLevel applicantRisk = getRiskLevel(applicant.getRisk());
            Map<String, Object> results = new HashMap<>();
            results.put("Reason", applicant.getReason());
            results.put("HasAlerts", applicant.getHasAlerts().toString());
            results.put("Risk", applicantRisk.getRiskValue());
            if (applicant.getScore() != null) {
                results.put("Score", applicant.getScore().toString());
            }
            boolean mediumRiskOrAlerts = applicantRisk != RiskLevel.LOW || applicant.getHasAlerts();
            results.put("MediumRiskOrAlerts", String.valueOf(mediumRiskOrAlerts));
            // TODO: aici "on hold" se compara cu ApplicationStatus.ON_HOLD.getValue(), la legal entities cu "OnHold".
            results.put(MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD, String.valueOf(mediumRiskOrAlerts
                    || ApplicationStatus.ON_HOLD.getValue().equals(risk.getLoanStatus())));

            Map<String, Object> supplierData = supplierDataOf(customers, applicant.getApplicantId());
            if (MapUtils.isNotEmpty(supplierData)) {
                results.put("OrgPartyType", supplierData.get("PartyType"));
            }

            RiskAssessment riskAssessment = newRiskAssessment(risk, rawResult, applicantRisk, applicant.getApplicantId());
            riskAssessment.setClassificationResults(results);
            result.addRiskAssessment(riskAssessment);
            markOnHoldIfRisky(result, applicant.getHasAlerts(), applicantRisk);

            log.info("Individual BusinessKey: {}. RAT score with risk score {} ({}). Alerts found: {}. Loan status {}.",
                    applicant.getApplicantId(), applicantRisk.getRiskValue(), applicant.getScore(),
                    applicant.getHasAlerts(), risk.getLoanStatus());
        }
    }

    private void addLegalEntities(Risk risk, String rawResult, ScoringResult result) {
        if (CollectionUtils.isEmpty(risk.getLegalEntities())) {
            return;
        }
        for (LegalEntityScore legalEntity : risk.getLegalEntities()) {
            RiskLevel legalEntityRisk = getRiskLevel(legalEntity.getRisk());
            Map<String, Object> results = new HashMap<>();
            results.put("Reason", legalEntity.getReason());
            results.put("HasAlerts", legalEntity.getHasAlerts().toString());
            results.put("Risk", legalEntityRisk.getRiskValue());
            if (legalEntity.getScore() != null) {
                results.put("Score", legalEntity.getScore().toString());
            }
            boolean mediumRiskOrAlerts = legalEntityRisk != RiskLevel.LOW || legalEntity.getHasAlerts();
            results.put("MediumRiskOrAlerts", Boolean.toString(mediumRiskOrAlerts));
            results.put(MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD,
                    Boolean.toString(mediumRiskOrAlerts || risk.getLoanStatus().equals("OnHold")));

            RiskAssessment riskAssessment = newRiskAssessment(risk, rawResult, legalEntityRisk, legalEntity.getApplicantId());
            riskAssessment.setClassificationResults(results);
            result.addRiskAssessment(riskAssessment);
            markOnHoldIfRisky(result, legalEntity.getHasAlerts(), legalEntityRisk);

            log.info("Legal entity BusinessKey: {}. RAT score with risk score {} ({}). Alerts found: {}. Loan status {}.",
                    legalEntity.getApplicantId(), legalEntity.getRisk(), legalEntity.getScore(),
                    legalEntity.getHasAlerts(), risk.getLoanStatus());
        }
    }

    /** RAT raspunde cu medium, low, high, Low, Medium, High; fara valoare se considera HIGH. */
    public RiskLevel getRiskLevel(String risk) {
        if (!StringUtils.isEmpty(risk)) {
            return RiskLevel.getRiskLevel(StringUtils.capitalize(risk.toLowerCase()));
        }
        return RiskLevel.HIGH;
    }

    private static RiskAssessment newRiskAssessment(Risk risk, String rawResult, RiskLevel riskLevel, String partyId) {
        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setLoanBusinessKey(risk.getApplicationId());
        riskAssessment.setCustomerBusinessKey(partyId);
        riskAssessment.setRiskLevel(riskLevel);
        riskAssessment.setClassificationInstrumentCode(RAT_COMPLIANCE);
        riskAssessment.setClassificationInstrumentName("RAT compliance");
        riskAssessment.setRawResult(rawResult);
        return riskAssessment;
    }

    /** Fosta getScoreReason: un risc peste LOW sau o alerta pune toata aplicatia pe ON_HOLD. */
    private static void markOnHoldIfRisky(ScoringResult result, boolean hasAlerts, RiskLevel riskLevel) {
        if (riskLevel != RiskLevel.LOW || hasAlerts) {
            result.setApplicationStatus(ApplicationStatus.ON_HOLD);
        }
    }

    private static Map<String, Object> supplierDataOf(List<Customer> customers, String applicantId) {
        if (customers == null) {
            return null;
        }
        for (Customer customer : customers) {
            if (customer != null && applicantId != null && applicantId.equals(customer.getBusinessKey())) {
                return customer.getSupplierData();
            }
        }
        return null;
    }

    /** Pune fiecare rezultat pe clientul sau compania cu acelasi business key, ca sa ajunga la DES prin sed. */
    private static void addToParties(ScoringResult scoringResult, Application application) {
        List<Customer> customers = application.getLoan().getCustomers();
        List<Organisation> companies = application.getLoan().getCompanies();
        for (RiskAssessment riskAssessment : scoringResult.getRiskAssessments()) {
            String partyKey = riskAssessment.getCustomerBusinessKey();
            if (partyKey == null) {
                continue;
            }
            if (customers != null) {
                for (Customer customer : customers) {
                    if (customer != null && partyKey.equals(customer.getBusinessKey())) {
                        add(customer.getRiskAssessments(), riskAssessment, customer::setRiskAssessments);
                    }
                }
            }
            if (companies != null) {
                for (Organisation company : companies) {
                    if (company != null && partyKey.equals(company.getBusinessKey())) {
                        add(company.getRiskAssessments(), riskAssessment, company::setRiskAssessments);
                    }
                }
            }
        }
    }

    /** FIX: lucreaza pe o copie, fiindca lista pusa de SCALMapper este imutabila (List.of). */
    private static void add(List<RiskAssessment> current, RiskAssessment riskAssessment,
                            Consumer<List<RiskAssessment>> setter) {
        List<RiskAssessment> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
        updated.add(riskAssessment);
        setter.accept(updated);
    }

    /** @return null daca maparea a esuat; eroarea e deja in context. */
    private Loan toRestLoan(ProxyContext ctx, boolean isSanction) {
        try {
            // TODO: LoanMapping.map primeste ProxyContext - vezi snippets/ratproxy/LoanMapping-changes.java
            return LoanMapping.map(ctx.getApplication(), isSanction, ctx);
        } catch (RuntimeException e) {
            String message = "Exception occurred while Loan Mapping " + e.getMessage();
            log.error(message, e);
            ctx.addError(BAD_REQUEST, message);
            return null;
        }
    }

    private GeneralProxyObsDto checkDto(ProxyContext ctx, Loan loan, Boolean response) {
        GeneralProxyObsDto dto = new GeneralProxyObsDto();
        dto.setRequestLoanJson(gson.toJson(loan));
        dto.setBusinessKey(ctx.getBusinessKey());
        dto.setFlowId(ctx.getFlowId());
        dto.setRatResponse(response);
        dto.setErrorHandlerJson(gson.toJson(ctx.getErrorHandler()));
        return dto;
    }

    private static void apiFailed(ProxyContext ctx, RuntimeException e) {
        String message = "RAT External API replied with error message for Business Key " + ctx.getBusinessKey()
                + " and flow ID " + ctx.getFlowId() + ": message: " + StringUtils.defaultIfBlank(e.getMessage(), e.toString());
        log.error(message, e);
        ctx.addError(SERVICE_UNAVAILABLE, message);
    }
}
