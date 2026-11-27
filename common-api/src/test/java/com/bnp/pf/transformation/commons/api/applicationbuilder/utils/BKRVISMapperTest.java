package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.BIRTH_DATE;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.customer;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.date;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCustomers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckVisDto;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.message.input.Application;
// Customer, Organisation, Address, Identification, RiskAssessment: acelasi pachet ca Application (message.input).
import com.bnp.pf.transformation.commons.api.message.input.*;

class BKRVISMapperTest {

    private final BKRVISMapper mapper = new BKRVISMapper();

    private static Customer visCustomer(String key, String applicantType, String document) {
        Customer customer = customer(key, "Jansen", applicantType);
        Identification identification = new Identification();
        identification.setIsoIssuedCountryCode("RO");
        identification.setIdentificationType("I");
        identification.setCode(document);
        customer.setIdentification(identification);
        return customer;
    }

    private static CheckVisDto row(String document, Integer code, boolean withErrors) {
        VisRequestData data = new VisRequestData();
        data.setCountryCode("RO");
        data.setType("VN");
        data.setDocumentNumber(document);
        data.setDateOfBirth(date(BIRTH_DATE));
        VisRequest request = new VisRequest();
        request.setVisRequestData(data);

        VisResponse response = new VisResponse();
        if (code != null) {
            Vis vis = new Vis();
            vis.setCode(code);
            VisResult result = new VisResult();
            result.setVis(vis);
            response.setResult(result);
        }
        CheckVisDto dto = new CheckVisDto();
        dto.setRequest(request);
        dto.setResponse(response);
        dto.setErrors(withErrors ? new ErrorHandler() : null);
        return dto;
    }

    @Test
    void noRows_defaultResultsOnEveryCustomerAndOnLoan() {
        Customer first = visCustomer("C1", "Holder", "D1");
        Customer second = visCustomer("C2", "CoHolder", "D2");
        Application application = withCustomers(first, second);

        mapper.mapVISData(List.of(), application);

        assertThat(first.getRiskAssessments()).singleElement()
                .satisfies(r -> assertThat(r.getClassificationResults()).containsEntry("Result", null));
        assertThat(second.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments())
                .extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
    }

    @Test
    void nullList_sameAsNoRows() {
        Customer customer = visCustomer("C1", "Holder", "D1");
        Application application = withCustomers(customer);

        mapper.mapVISData(null, application);

        assertThat(customer.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments()).hasSize(2);
    }

    @Test
    void matchingCustomer_getsFalseResult_loanGetsVisCode() {
        Customer customer = visCustomer("C1", "Holder", "D1");
        Application application = withCustomers(customer);

        mapper.mapVISData(List.of(row("D1", 3, false)), application);

        assertThat(customer.getRiskAssessments()).singleElement().satisfies(risk -> {
            assertThat(risk.getClassificationInstrumentCode()).isEqualTo("bkr.vis");
            assertThat(risk.getClassificationResults()).containsEntry("Result", "false");
        });
        // identic cu legacy: rezultatul co-holder ("false", nu exista co-holder) e pus pe holder
        assertThat(application.getLoan().getRiskAssessments()).hasSize(2);
        assertThat(application.getLoan().getRiskAssessments().get(0).getClassificationResults())
                .containsEntry("Result", "false");
    }

    @Test
    void coHolderPresent_codeOfTheResponseEndsUpOnLoan() {
        Customer coHolder = visCustomer("C2", "CoHolder", "D2");
        Application application = withCustomers(coHolder);

        mapper.mapVISData(List.of(row("D2", 3, false)), application);

        assertThat(application.getLoan().getRiskAssessments().get(0).getClassificationResults())
                .containsEntry("Result", "3");
    }

    @Test
    void responseWithoutCode_defaultsToOne() {
        Customer coHolder = visCustomer("C2", "CoHolder", "D2");
        Application application = withCustomers(coHolder);

        mapper.mapVISData(List.of(row("D2", null, false)), application);

        assertThat(application.getLoan().getRiskAssessments().get(0).getClassificationResults())
                .containsEntry("Result", "1");
    }

    @Test
    void rowWithErrors_noLoanResults() {
        Customer customer = visCustomer("C1", "Holder", "D1");
        Application application = withCustomers(customer);

        mapper.mapVISData(List.of(row("D1", 3, true)), application);

        assertThat(customer.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }

    @Test
    void severalRows_eachCustomerOnce_loanResultsForEachRow_nullRowsSkipped() {
        Customer first = visCustomer("C1", "Holder", "D1");
        Customer second = visCustomer("C2", "CoHolder", "D2");
        Application application = withCustomers(first, second);

        mapper.mapVISData(Arrays.asList(row("D1", 2, false), null, row("D2", 2, false)), application);

        assertThat(first.getRiskAssessments()).hasSize(1);
        assertThat(second.getRiskAssessments()).hasSize(1);
        assertThat(application.getLoan().getRiskAssessments()).hasSize(4);
    }

    @Test
    void documentFromNl_typeIsPassport_noMatchWithVn() {
        Customer customer = visCustomer("C1", "Holder", "D1");
        customer.getIdentification().setIsoIssuedCountryCode("NL");
        CheckVisDto dto = row("D1", 2, false);
        dto.getRequest().getVisRequestData().setCountryCode("NL");

        mapper.mapVISData(List.of(dto), withCustomers(customer));

        assertThat(customer.getRiskAssessments()).isEmpty();
    }

    @Test
    void noCustomers_nothingHappens() {
        Application application = withCustomers();

        mapper.mapVISData(List.of(), application);

        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }
}
