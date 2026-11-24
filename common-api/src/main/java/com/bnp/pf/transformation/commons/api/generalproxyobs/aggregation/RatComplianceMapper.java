package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Application, Loan, Customer, Organisation si RiskAssessment sunt presupuse.
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.model.Application;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Loan;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;

/**
 * Pune pe clienti si companii risk assessment-urile "rat.compliance" calculate de rat.
 * Contract: un rezultat per party, cu responseJson = RiskAssessment-ul serializat.
 * Ca in legacy, un party fara rezultat ramane fara risk assessment si nu e o eroare.
 */
@Slf4j
@Component
public class RatComplianceMapper implements ProxyResultMapper {

    public static final String RAT_COMPLIANCE = "rat.compliance";

    private final Gson gson = GsonInstance.createGson();

    @Override
    public ProxySource source() {
        return ProxySource.RAT_COMPLIANCE;
    }

    @Override
    public void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx) {
        Loan loan = application == null ? null : application.getLoan();
        if (loan == null) {
            return;
        }
        for (ProxyResultDto result : results) {
            if (StringUtils.isBlank(result.getResponseJson())) {
                continue;
            }
            RiskAssessment riskAssessment = gson.fromJson(result.getResponseJson(), RiskAssessment.class);
            if (riskAssessment == null) {
                continue;
            }
            riskAssessment.setClassificationInstrumentCode(RAT_COMPLIANCE);
            String partyKey = StringUtils.defaultIfBlank(result.getPartyBusinessKey(), riskAssessment.getCustomerBusinessKey());
            if (!addToParty(loan, partyKey, riskAssessment)) {
                log.warn("No customer or company with business key {} for RAT result, flow id {}", partyKey, ctx.getFlowId());
            }
        }
    }

    private boolean addToParty(Loan loan, String partyKey, RiskAssessment riskAssessment) {
        if (partyKey == null) {
            return false;
        }
        if (loan.getCustomers() != null) {
            for (Customer customer : loan.getCustomers()) {
                if (customer != null && partyKey.equals(customer.getBusinessKey())) {
                    replace(customer.getRiskAssessments(), riskAssessment, customer::setRiskAssessments);
                    return true;
                }
            }
        }
        if (loan.getCompanies() != null) {
            for (Organisation company : loan.getCompanies()) {
                if (company != null && partyKey.equals(company.getBusinessKey())) {
                    replace(company.getRiskAssessments(), riskAssessment, company::setRiskAssessments);
                    return true;
                }
            }
        }
        return false;
    }

    /** Lucreaza pe o copie, fiindca lista existenta poate fi imutabila, si nu dubleaza rezultatul la o a doua rulare. */
    private static void replace(List<RiskAssessment> current, RiskAssessment riskAssessment,
                                Consumer<List<RiskAssessment>> setter) {
        List<RiskAssessment> updated = current == null ? new ArrayList<>() : new ArrayList<>(current);
        updated.removeIf(existing -> existing != null && RAT_COMPLIANCE.equals(existing.getClassificationInstrumentCode()));
        updated.add(riskAssessment);
        setter.accept(updated);
    }
}
