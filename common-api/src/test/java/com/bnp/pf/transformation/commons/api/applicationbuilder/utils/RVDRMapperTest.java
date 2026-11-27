package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.BIRTH_DATE;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.customer;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.date;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCustomers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckRvdrCreditDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
// Customer, Organisation, Address, Identification, RiskAssessment: acelasi pachet ca Application (message.input).
import com.bnp.pf.transformation.commons.api.message.input.*;

class RVDRMapperTest {

    private final RVDRMapper mapper = new RVDRMapper();

    private static Customer customerAt(String lastName, int houseNumber) {
        Customer customer = customer("C-" + lastName, lastName, "Holder");
        Address address = new Address();
        address.setHouseNumber(houseNumber);
        customer.setAddresses(new ArrayList<>(List.of(address)));
        return customer;
    }

    private static CheckRvdrCreditDto row(String name, int houseNumber, String outcome) {
        HouseNumber number = new HouseNumber();
        number.setN(houseNumber);
        CreditCheckAddress address = new CreditCheckAddress();
        address.setHouseNumber(number);
        CreditCheckPerson person = new CreditCheckPerson();
        person.setName(name);
        person.setDateOfBirth(date(BIRTH_DATE));
        person.setAddress(address);
        CreditCheckRequestDataType data = new CreditCheckRequestDataType();
        data.setPerson(person);
        CreditCheckRequest request = new CreditCheckRequest();
        request.setCreditCheckRequestData(data);

        CheckRvdrCreditDto dto = new CheckRvdrCreditDto();
        dto.setRequest(request);
        if (outcome != null) {
            CreditCheckResult result = new CreditCheckResult();
            result.setOverallOutcomeDescription(outcome);
            CreditCheckResponse response = new CreditCheckResponse();
            response.setResult(result);
            dto.setResponse(response);
        }
        return dto;
    }

    @Test
    void nullOrEmptyList_changesNothing() {
        Customer customer = customerAt("Jansen", 10);
        Application application = withCustomers(customer);

        mapper.mapRVDRData(null, application);
        mapper.mapRVDRData(List.of(), application);

        assertThat(customer.getRiskAssessments()).isEmpty();
        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }

    @Test
    void matchingCustomer_getsCirAndCcbrChecksFromOutcome() {
        Customer customer = customerAt("Jansen", 10);

        mapper.mapRVDRData(List.of(row("Jansen", 10, "cir ok, ccbr nok")), withCustomers(customer));

        assertThat(customer.getRiskAssessments()).singleElement().satisfies(risk -> {
            assertThat(risk.getClassificationInstrumentCode()).isEqualTo("bkr.circcbr");
            assertThat(risk.getClassificationInstrumentName()).isEqualTo("BKR cir & Ccbr");
            assertThat(risk.getDescription()).isEqualTo("BKR Cir & Ccbr");
            assertThat(risk.getClassificationResults()).containsEntry("CirCheck", "true").containsEntry("CcbrCheck", "false");
        });
    }

    @Test
    void responseMissing_bothChecksAreFalse() {
        Customer customer = customerAt("Jansen", 10);

        mapper.mapRVDRData(List.of(row("Jansen", 10, null)), withCustomers(customer));

        assertThat(customer.getRiskAssessments().get(0).getClassificationResults())
                .containsEntry("CirCheck", "false").containsEntry("CcbrCheck", "false");
    }

    @Test
    void differentHouseNumber_noMatch() {
        Customer customer = customerAt("Jansen", 10);

        mapper.mapRVDRData(List.of(row("Jansen", 11, "CIR OK")), withCustomers(customer));

        assertThat(customer.getRiskAssessments()).isEmpty();
    }

    @Test
    void severalRows_eachMatchesItsCustomer_nullRowsSkipped() {
        Customer first = customerAt("Jansen", 10);
        Customer second = customerAt("Bakker", 20);

        mapper.mapRVDRData(Arrays.asList(row("Jansen", 10, "CIR OK"), null, row("Bakker", 20, "CCBR OK")),
                withCustomers(first, second));

        assertThat(first.getRiskAssessments()).singleElement()
                .satisfies(r -> assertThat(r.getClassificationResults()).containsEntry("CirCheck", "true"));
        assertThat(second.getRiskAssessments()).singleElement()
                .satisfies(r -> assertThat(r.getClassificationResults()).containsEntry("CcbrCheck", "true"));
    }
}
