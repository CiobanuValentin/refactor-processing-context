package com.bnpp.pf.transformation.sedproxy.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru dto-uri, entitati, repository-uri si model sunt presupuse - pastreaza-le pe cele actuale.
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.model.Application;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;
import com.bnp.pf.transformation.commons.api.model.ScoringResult;
import com.bnpp.pf.transformation.sedproxy.client.RestExternalService;
import com.bnpp.pf.transformation.sedproxy.dto.BnpOutputMessageDto;
import com.bnpp.pf.transformation.sedproxy.entity.BnpOutputMessageEntity;
import com.bnpp.pf.transformation.sedproxy.entity.CheckExpertiseEntity;
import com.bnpp.pf.transformation.sedproxy.model.ExpertiseData;
import com.bnpp.pf.transformation.sedproxy.model.ExpertiseReturn;
import com.bnpp.pf.transformation.sedproxy.repository.BnpOutputMessageRepository;
import com.bnpp.pf.transformation.sedproxy.repository.CheckExpertiseRepository;

/** Trimite aplicatia la DES si construieste bnpOutputMessage. Orice esec devine o singura eroare in context. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SedProcessorService {

    static final String BNP_OUTPUT_MESSAGE = "bnpOutputMessage";
    private static final String CLASSIFICATION_NAME = "Expert System Full Check";
    private static final String BKR = "bkr";
    private static final String RAT_COMPLIANCE = "rat.compliance";
    private static final String BAD_REQUEST = "400";
    private static final String SERVICE_UNAVAILABLE = "503";

    private final BnpOutputMessageRepository bnpOutputMessageRepository;
    private final CheckExpertiseRepository checkExpertiseRepository;
    private final ModelMapper modelMapper;
    private final SedMappingService sedMappingService;
    private final RestExternalService restExternalService;

    private final Gson gson = GsonInstance.createGson();

    public void process(ProxyContext ctx) {
        Application application = ctx.getApplication();
        GeneralProxyObsDto checkExpertise = new GeneralProxyObsDto();
        checkExpertise.setBusinessKey(ctx.getBusinessKey());
        checkExpertise.setFlowId(ctx.getFlowId());

        if (!areParametersProvided(application)) {
            log.error("Parameters for SED request not provided for business key {} and flowId {}",
                    ctx.getBusinessKey(), ctx.getFlowId());
            ctx.addError(BAD_REQUEST, "SED Parameters not received");
        } else {
            try {
                callExpertSystem(ctx, application, checkExpertise);
            } catch (RuntimeException e) {
                // Ca in legacy: si o eroare de mapare, si una de retea inseamna raspuns KO, nu retry.
                String message = "SED request failed for business key " + ctx.getBusinessKey() + " and flow ID "
                        + ctx.getFlowId() + ": " + StringUtils.defaultIfBlank(e.getMessage(), e.toString());
                log.error(message, e);
                ctx.addError(SERVICE_UNAVAILABLE, message);
            }
        }

        // Cererea, raspunsul si erorile se salveaza mereu, si la esec.
        checkExpertise.setErrorHandlerJson(gson.toJson(ctx.getErrorHandler()));
        checkExpertiseRepository.save(modelMapper.map(checkExpertise, CheckExpertiseEntity.class));
    }

    private void callExpertSystem(ProxyContext ctx, Application application, GeneralProxyObsDto checkExpertise) {
        ExpertiseData expertiseData = sedMappingService.mapApplicationToExpertise(application, ctx.getFlowId());
        checkExpertise.setExpertiseRequestJson(gson.toJson(expertiseData));

        ExpertiseReturn expertiseReturn = restExternalService.expertise(expertiseData, ctx.getFlowId());
        checkExpertise.setExpertiseReturnJson(gson.toJson(expertiseReturn));

        if (expertiseReturn == null || expertiseReturn.isError()) {
            String reason = expertiseReturn == null ? null : expertiseReturn.getErrorMessage();
            String message = StringUtils.defaultIfBlank(reason, "SED replied with an error for business key "
                    + ctx.getBusinessKey() + " and flow ID " + ctx.getFlowId());
            log.error(message);
            ctx.addError(BAD_REQUEST, message);
            return;
        }

        BnpOutputMessageDto output = sedMappingService.mapExpertiseReturnToBnpOutputMessage(
                expertiseReturn, application.getBusinessKey(), CLASSIFICATION_NAME);

        // FIX fata de codul actual: scoringResult-ul completat se scrie inapoi in output (inainte se pierdea)
        // si se pastreaza toate risk assessment-urile de pe loan, nu doar ultimul. Legacy le trimitea pe toate.
        ScoringResult scoringResult = scoringResultOf(output);
        addRiskAssessments(scoringResult, application);
        output.setScoringResultJson(gson.toJson(scoringResult));

        output.setChannel(application.getChannel());
        output.setBusinessKey(application.getLoan().getBusinessKey());
        bnpOutputMessageRepository.save(modelMapper.map(output, BnpOutputMessageEntity.class));

        ctx.putResultProperty(BNP_OUTPUT_MESSAGE, gson.toJson(output));
    }

    private ScoringResult scoringResultOf(BnpOutputMessageDto output) {
        ScoringResult scoringResult = StringUtils.isBlank(output.getScoringResultJson())
                ? null : gson.fromJson(output.getScoringResultJson(), ScoringResult.class);
        return scoringResult == null ? new ScoringResult() : scoringResult;
    }

    /** Rezultatele de pe loan, apoi bkr si rat.compliance de pe clienti, apoi rat.compliance de pe companii. */
    private void addRiskAssessments(ScoringResult scoringResult, Application application) {
        add(scoringResult, application.getLoan().getRiskAssessments(), null);
        if (application.getLoan().getCustomers() != null) {
            for (Customer customer : application.getLoan().getCustomers()) {
                add(scoringResult, customer.getRiskAssessments(), List.of(BKR, RAT_COMPLIANCE));
            }
        }
        if (application.getLoan().getCompanies() != null) {
            for (Organisation company : application.getLoan().getCompanies()) {
                add(scoringResult, company.getRiskAssessments(), List.of(RAT_COMPLIANCE));
            }
        }
    }

    private static void add(ScoringResult scoringResult, List<RiskAssessment> riskAssessments, List<String> onlyCodes) {
        if (riskAssessments == null) {
            return;
        }
        for (RiskAssessment riskAssessment : riskAssessments) {
            if (riskAssessment != null
                    && (onlyCodes == null || onlyCodes.contains(riskAssessment.getClassificationInstrumentCode()))) {
                scoringResult.addRiskAssessment(riskAssessment);
            }
        }
    }

    // TODO: conditia din poza e taiata dupa getOrganisation() - copiaz-o intreaga din metoda actuala.
    private boolean areParametersProvided(Application application) {
        var sedParameters = application.getSedParameters();
        return sedParameters != null && !(StringUtils.isEmpty(sedParameters.getApplication())
                || StringUtils.isEmpty(sedParameters.getCountry())
                || StringUtils.isEmpty(sedParameters.getOrganisation()));
    }
}
