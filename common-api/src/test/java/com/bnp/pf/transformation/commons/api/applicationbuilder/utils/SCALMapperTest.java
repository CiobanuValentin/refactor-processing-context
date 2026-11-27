package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.BUSINESS_KEY;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.company;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.customer;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCompanies;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCustomers;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalEntityDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonInitialDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

class SCALMapperTest {

    private static final Calendar BIRTH = new GregorianCalendar(1990, Calendar.JANUARY, 15);

    private final SCALMapper mapper = new SCALMapper();

    private static Customer person(String firstName, String lastName, String initials) {
        Customer customer = customer("C-" + lastName, lastName, "Holder");
        customer.setFirstName(firstName);
        customer.setPrefixLastName("");
        customer.setInitialsName(initials);
        return customer;
    }

    private static CheckResult result(Boolean sanction, Boolean pep) {
        CheckResult result = new CheckResult();
        if (sanction != null) {
            result.setPerson1(new CheckPersonResult(sanction, pep));
            result.setEntity(new CheckPersonResult(sanction, pep));
        }
        return result;
    }

    private static CheckScalPersonDto personRow(String firstName, String name, CheckResult result) {
        ScalPersonRequest request = new ScalPersonRequest();
        request.setFirstName(firstName);
        request.setName(name);
        request.setDate(BIRTH);
        CheckScalPersonDto dto = new CheckScalPersonDto();
        dto.setRequest(request);
        dto.setResponse(result);
        return dto;
    }

    @Test
    void person_matchingCustomer_getsSanctionPepAndRiskAssessment() {
        Customer customer = person("Jan", "Jansen", "J.");

        mapper.mapScalPersonData(List.of(personRow("Jan", "Jansen", result(true, false))), withCustomers(customer));

        assertThat(customer.getHasSanction()).isTrue();
        assertThat(customer.getIsPep()).isFalse();
        assertThat(customer.getRiskAssessments()).singleElement().satisfies(risk -> {
            assertThat(risk.getClassificationInstrumentCode()).isEqualTo("SCAL");
            assertThat(risk.getLoanBusinessKey()).isEqualTo(BUSINESS_KEY);
            assertThat(risk.getDescription()).isEqualTo("SCAL Sanction and PEP Checks");
            assertThat(risk.getRawResult()).isNotBlank();
        });
    }

    @Test
    void person_responseWithoutPerson1_leavesSanctionAndPepNull() {
        Customer customer = person("Jan", "Jansen", "J.");
        customer.setHasSanction(true);

        mapper.mapScalPersonData(List.of(personRow("Jan", "Jansen", result(null, null))), withCustomers(customer));

        assertThat(customer.getHasSanction()).isNull();
        assertThat(customer.getIsPep()).isNull();
        assertThat(customer.getRiskAssessments()).hasSize(1);
    }

    @Test
    void person_prefixIsPartOfTheName() {
        Customer customer = person("Jan", "Berg", "J.");
        customer.setPrefixLastName("van den");

        mapper.mapScalPersonData(List.of(personRow("Jan", "Berg", result(true, true))), withCustomers(customer));
        assertThat(customer.getRiskAssessments()).isEmpty();

        mapper.mapScalPersonData(List.of(personRow("Jan", "van den Berg", result(true, true))), withCustomers(customer));
        assertThat(customer.getRiskAssessments()).hasSize(1);
    }

    @Test
    void person_severalRows_eachCustomerMatched_nullRowsSkipped() {
        Customer first = person("Jan", "Jansen", "J.");
        Customer second = person("Piet", "Bakker", "P.");

        mapper.mapScalPersonData(Arrays.asList(personRow("Jan", "Jansen", result(true, false)), null,
                personRow("Piet", "Bakker", result(false, true))), withCustomers(first, second));

        assertThat(first.getHasSanction()).isTrue();
        assertThat(second.getIsPep()).isTrue();
        assertThat(first.getRiskAssessments()).hasSize(1);
        assertThat(second.getRiskAssessments()).hasSize(1);
    }

    @Test
    void person_nullOrEmptyList_changesNothing() {
        Customer customer = person("Jan", "Jansen", "J.");
        Application application = withCustomers(customer);

        mapper.mapScalPersonData(null, application);
        mapper.mapScalPersonData(List.of(), application);

        assertThat(customer.getRiskAssessments()).isEmpty();
    }

    @Test
    void personInitial_matchesOnInitialsWithoutDotsAndSpaces() {
        Customer customer = person("Jan", "Jansen", " J. P. ");
        ScalPersonInitialRequest request = new ScalPersonInitialRequest();
        request.setInitial("JP");
        request.setName("Jansen");
        request.setDate(BIRTH);
        CheckScalPersonInitialDto dto = new CheckScalPersonInitialDto();
        dto.setRequest(request);
        dto.setResponse(result(false, true));

        mapper.mapScalPersonInitialData(List.of(dto), withCustomers(customer));

        assertThat(customer.getIsPep()).isTrue();
        assertThat(customer.getRiskAssessments()).hasSize(1);
    }

    @Test
    void entity_matchingCompany_getsSanctionPepAndRiskAssessment() {
        Organisation company = company("ACME BV", "123", "NL");
        company.setEstablishmentDate(LocalDateTime.of(1990, 1, 15, 0, 0));
        ScalEntityRequest request = new ScalEntityRequest();
        request.setName("ACME BV");
        request.setDate(BIRTH);
        CheckScalEntityDto dto = new CheckScalEntityDto();
        dto.setRequest(request);
        dto.setResponse(result(true, true));

        mapper.mapScalEntityData(List.of(dto), withCompanies(company));

        assertThat(company.getHasSanction()).isTrue();
        assertThat(company.getIsPep()).isTrue();
        assertThat(company.getRiskAssessments()).hasSize(1);
    }
}
