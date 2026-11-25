package com.bnpp.pf.transformation.ratproxy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ProxyResultsDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;

class RatResultsServiceTest {

    private final RatResultsService service = new RatResultsService(null);

    @Test
    void onlyRatComplianceResults_oneForEachParty() {
        Customer customer = new Customer();
        customer.setBusinessKey("C1");
        customer.setRiskAssessments(List.of(riskAssessment("bkr", "C1"), riskAssessment("rat.compliance", "C1")));
        Customer withoutResults = new Customer();
        withoutResults.setBusinessKey("C2");
        Organisation company = new Organisation();
        company.setBusinessKey("O1");
        company.setRiskAssessments(List.of(riskAssessment("rat.compliance", "O1")));
        Loan loan = new Loan();
        loan.setCustomers(List.of(customer, withoutResults));
        loan.setCompanies(List.of(company));
        Application application = new Application();
        application.setLoan(loan);

        ProxyResultsDto dto = service.toResults("BK1", "F1", application);

        assertEquals("BK1", dto.getBusinessKey());
        assertEquals("F1", dto.getFlowId());
        assertEquals(2, dto.getResults().size());
        assertEquals("C1", dto.getResults().get(0).getPartyBusinessKey());
        assertTrue(dto.getResults().get(0).getResponseJson().contains("rat.compliance"));
        assertEquals("O1", dto.getResults().get(1).getPartyBusinessKey());
    }

    @Test
    void noSavedApplicationOrNoLoan_givesAnEmptyList() {
        assertTrue(service.toResults("BK1", "F1", null).getResults().isEmpty());
        assertTrue(service.toResults("BK1", "F1", new Application()).getResults().isEmpty());
    }

    private static RiskAssessment riskAssessment(String code, String partyKey) {
        RiskAssessment riskAssessment = new RiskAssessment();
        riskAssessment.setClassificationInstrumentCode(code);
        riskAssessment.setCustomerBusinessKey(partyKey);
        return riskAssessment;
    }
}
