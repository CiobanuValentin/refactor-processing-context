package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.BIRTH_DATE;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.company;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.customer;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.date;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCompanies;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCustomers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaCompanyDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaPersonDto;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.message.input.Application;

class BKREVAMapperTest {

    private final BKREVAMapper mapper = new BKREVAMapper();

    private static EvaResponse response(String evaResult) {
        Eva eva = new Eva();
        eva.setResult(evaResult);
        EvaResult result = new EvaResult();
        result.setEva(eva);
        EvaResponse response = new EvaResponse();
        response.setResult(result);
        return response;
    }

    private static CheckEvaPersonDto personRow(String name, String evaResult, boolean withErrors) {
        EvaPersonRequestData data = new EvaPersonRequestData();
        data.setName(name);
        data.setDateOfBirth(date(BIRTH_DATE));
        EvaRequest request = new EvaRequest();
        request.setEvaPersonRequestData(data);
        CheckEvaPersonDto dto = new CheckEvaPersonDto();
        dto.setRequest(request);
        dto.setResponse(response(evaResult));
        dto.setErrors(withErrors ? new ErrorHandler() : null);
        return dto;
    }

    private static CheckEvaCompanyDto companyRow(String name, String coc, String zipLetters, int zipNumber) {
        ZipCodeType zip = new ZipCodeType();
        zip.setA(zipLetters);
        zip.setN(zipNumber);
        EvaCompanyRequestData data = new EvaCompanyRequestData();
        data.setName(name);
        data.setCOCNumber(coc);
        data.setZipCode(zip);
        EvaCompanyRequest request = new EvaCompanyRequest();
        request.setEvaCompanyRequestData(data);
        CheckEvaCompanyDto dto = new CheckEvaCompanyDto();
        dto.setRequest(request);
        dto.setResponse(response("0"));
        return dto;
    }

    private static Organisation companyAt(String zipCode) {
        Organisation company = company("ACME", "123", "NL");
        Address address = new Address();
        address.setZipCode(zipCode);
        company.setAddresses(new ArrayList<>(List.of(address)));
        return company;
    }

    @Test
    void person_matchingCustomer_getsResultAndLoanGetsHolderAndCoHolder() {
        Customer customer = customer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapEVAPersonData(List.of(personRow("Jansen", "1", false)), application);

        assertThat(customer.getRiskAssessments()).singleElement().satisfies(risk -> {
            assertThat(risk.getClassificationInstrumentCode()).isEqualTo("bkr.eva");
            assertThat(risk.getCustomerBusinessKey()).isEqualTo("C1");
            assertThat(risk.getClassificationResults()).containsEntry("Result", "true");
        });
        assertThat(application.getLoan().getRiskAssessments())
                .extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
    }

    @Test
    void person_evaResultNotOne_isFalse() {
        Customer customer = customer("C1", "Jansen", "Holder");

        mapper.mapEVAPersonData(List.of(personRow("Jansen", "0", false)), withCustomers(customer));

        assertThat(customer.getRiskAssessments().get(0).getClassificationResults()).containsEntry("Result", "false");
    }

    /** Identic cu legacy: rezultatul co-holder e pus pe riskAssmntHolder, iar cel co-holder ramane cu "false". */
    @Test
    void person_loanResults_coHolderResultEndsUpOnHolder() {
        Customer coHolder = customer("C2", "Jansen", "CoHolder");
        Application application = withCustomers(coHolder);

        mapper.mapEVAPersonData(List.of(personRow("Jansen", "1", false)), application);

        List<RiskAssessment> loan = application.getLoan().getRiskAssessments();
        assertThat(loan.get(0).getResultType()).isEqualTo("Holder");
        assertThat(loan.get(0).getClassificationResults()).containsEntry("Result", "true");
        assertThat(loan.get(1).getResultType()).isEqualTo("CoHolder");
        assertThat(loan.get(1).getClassificationResults()).containsEntry("Result", "false");
    }

    @Test
    void person_rowWithErrors_customerMatchedButLoanUntouched() {
        Customer customer = customer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapEVAPersonData(List.of(personRow("Jansen", "1", true)), application);

        assertThat(customer.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }

    @Test
    void person_severalRowsWithoutErrors_loanResultsAddedForEachRow_customerOnlyOnce() {
        Customer customer = customer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapEVAPersonData(Arrays.asList(personRow("Jansen", "1", false), null, personRow("Jansen", "1", false)),
                application);

        assertThat(customer.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments()).hasSize(4);
    }

    @Test
    void person_nullOrEmptyList_changesNothing() {
        Customer customer = customer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapEVAPersonData(null, application);
        mapper.mapEVAPersonData(List.of(), application);

        assertThat(customer.getRiskAssessments()).isEmpty();
        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }

    @Test
    void company_matchingNameCocAndZip_getsResult() {
        Organisation company = companyAt("1234AB");
        Application application = withCompanies(company);

        mapper.mapEVACompanyData(List.of(companyRow("ACME", "123", "AB", 1234)), application);

        assertThat(company.getRiskAssessments()).singleElement()
                .satisfies(r -> assertThat(r.getClassificationInstrumentName()).isEqualTo("Bkr Eva"));
        assertThat(application.getLoan().getRiskAssessments()).hasSize(2);
    }

    @Test
    void company_differentZip_noCompanyResult_loanStillGetsResults() {
        Organisation company = companyAt("1234AB");
        Application application = withCompanies(company);

        mapper.mapEVACompanyData(List.of(companyRow("ACME", "123", "CD", 1234)), application);

        assertThat(company.getRiskAssessments()).isEmpty();
        assertThat(application.getLoan().getRiskAssessments()).hasSize(2);
    }
}
