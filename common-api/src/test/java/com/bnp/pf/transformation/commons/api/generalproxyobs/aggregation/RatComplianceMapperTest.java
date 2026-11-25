package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Organisation;
import com.bnp.pf.transformation.commons.api.model.RiskAssessment;

class RatComplianceMapperTest {

    private final RatComplianceMapper mapper = new RatComplianceMapper();
    private final Customer customer = new Customer();
    private final Customer otherCustomer = new Customer();
    private final Organisation company = new Organisation();
    private final Application application = new Application();

    RatComplianceMapperTest() {
        customer.setBusinessKey("C1");
        otherCustomer.setBusinessKey("C2");
        company.setBusinessKey("O1");
        Loan loan = new Loan();
        loan.setCustomers(new ArrayList<>(List.of(customer, otherCustomer)));
        loan.setCompanies(new ArrayList<>(List.of(company)));
        application.setLoan(loan);
    }

    @Test
    void resultsLandOnTheMatchingCustomerAndCompany() {
        mapper.apply(List.of(result("C1", "{\"customerBusinessKey\":\"C1\"}"), result("O1", "{}")), application, context());

        assertEquals(RatComplianceMapper.RAT_COMPLIANCE, customer.getRiskAssessments().get(0).getClassificationInstrumentCode());
        assertEquals(1, company.getRiskAssessments().size());
        assertNull(otherCustomer.getRiskAssessments());
    }

    @Test
    void partyKeyFallsBackToTheOneInsideTheRiskAssessment() {
        mapper.apply(List.of(result(null, "{\"customerBusinessKey\":\"C2\"}")), application, context());

        assertEquals(1, otherCustomer.getRiskAssessments().size());
    }

    @Test
    void immutableExistingList_isCopiedAndOtherResultsAreKept() {
        RiskAssessment bkr = new RiskAssessment();
        bkr.setClassificationInstrumentCode("bkr");
        customer.setRiskAssessments(List.of(bkr));

        mapper.apply(List.of(result("C1", "{}")), application, context());

        assertEquals(2, customer.getRiskAssessments().size());
    }

    @Test
    void secondRun_doesNotDuplicateTheResult() {
        mapper.apply(List.of(result("C1", "{}")), application, context());
        mapper.apply(List.of(result("C1", "{}")), application, context());

        assertEquals(1, customer.getRiskAssessments().size());
    }

    @Test
    void unknownPartyOrEmptyResponse_isIgnoredWithoutError() {
        ProxyContext ctx = context();

        mapper.apply(List.of(result("X9", "{}"), result("C1", null)), application, ctx);

        assertNull(customer.getRiskAssessments());
        assertEquals(0, ctx.getErrors().size());
    }

    private static ProxyResultDto result(String partyKey, String riskAssessmentJson) {
        ProxyResultDto result = new ProxyResultDto();
        result.setPartyBusinessKey(partyKey);
        result.setResponseJson(riskAssessmentJson);
        return result;
    }

    private static ProxyContext context() {
        Signal signal = new Signal();
        signal.setFlowId("F1");
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        return ProxyContext.of(message, "corr", "sedproxy");
    }
}
