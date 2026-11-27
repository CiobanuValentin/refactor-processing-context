package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.company;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCompanies;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEventIndicatorDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
// Customer, Organisation, Address, Identification, RiskAssessment: acelasi pachet ca Application (message.input).
import com.bnp.pf.transformation.commons.api.message.input.*;

class EventIndicatorMapperTest {

    private final EventIndicatorMapper mapper = new EventIndicatorMapper();

    private static CheckEventIndicatorDto row(String coc, String country, EventTriggerResult result) {
        Company requestCompany = new Company();
        requestCompany.setCocNumber(coc);
        requestCompany.setCountryCode(country);
        RequestCompany request = new RequestCompany();
        request.setCompany(requestCompany);

        EventTriggerResponse trigger = new EventTriggerResponse();
        trigger.setEventTriggerResult(result);
        DUNSResponse duns = new DUNSResponse();
        duns.setEventTriggerResponse(trigger);
        CompanyResponse companyResponse = new CompanyResponse();
        companyResponse.setDunsresponse(duns);
        ResponseRoot response = new ResponseRoot();
        response.setCompanyResponse(companyResponse);

        CheckEventIndicatorDto dto = new CheckEventIndicatorDto();
        dto.setRequest(request);
        dto.setResponse(response);
        return dto;
    }

    private static EventTriggerResult result(String resultStatus) {
        EventTriggerResult result = new EventTriggerResult();
        result.setOutput01("true");
        result.setOutput02("false");
        result.setOutput03(" ");
        result.setOutput25("TRUE");
        result.setAverageCount("3");
        result.setMinCount("1");
        result.setMaxCount("5");
        result.setNumberRows("10");
        result.setIsActive(true);
        result.setAddDate(0L);
        result.setResultStatus(resultStatus);
        return result;
    }

    @Test
    void matchingCompany_getsOutputsAndCounters() {
        Organisation company = company("ACME", "123", "NL");

        mapper.mapEventIndicatorData(List.of(row("123", "NL", result("OK"))), withCompanies(company));

        assertThat(company.getOutput01()).isTrue();
        assertThat(company.getOutput02()).isFalse();
        assertThat(company.getOutput03()).isFalse();
        assertThat(company.getOutput04()).isFalse();
        assertThat(company.getOutput25()).isTrue();
        assertThat(company.getAverageCount()).isEqualTo("3");
        assertThat(company.getMinCount()).isEqualTo("1");
        assertThat(company.getMaxCount()).isEqualTo("5");
        assertThat(company.getNumberRows()).isEqualTo("10");
        assertThat(company.getActive()).isTrue();
        assertThat(company.getAddDate()).isNotNull();
        assertThat(company.getResultStatus()).isEqualTo("OK");
    }

    @Test
    void activeMissing_isFalse_addDateMissing_staysNull() {
        Organisation company = company("ACME", "123", "NL");
        EventTriggerResult result = result("OK");
        result.setIsActive(null);
        result.setAddDate(null);

        mapper.mapEventIndicatorData(List.of(row("123", "NL", result)), withCompanies(company));

        assertThat(company.getActive()).isFalse();
        assertThat(company.getAddDate()).isNull();
    }

    @Test
    void differentCountry_orNoTriggerResult_companyUntouched() {
        Organisation company = company("ACME", "123", "NL");

        mapper.mapEventIndicatorData(List.of(row("123", "BE", result("OK")), row("123", "NL", null)), withCompanies(company));

        assertThat(company.getResultStatus()).isNull();
        assertThat(company.getOutput01()).isNull();
    }

    @Test
    void severalRows_eachCompanyMatched_nullRowsSkipped() {
        Organisation first = company("ACME", "123", "NL");
        Organisation second = company("Other", "456", "NL");

        mapper.mapEventIndicatorData(Arrays.asList(row("123", "NL", result("A")), null, row("456", "NL", result("B"))),
                withCompanies(first, second));

        assertThat(first.getResultStatus()).isEqualTo("A");
        assertThat(second.getResultStatus()).isEqualTo("B");
    }

    @Test
    void nullOrEmptyList_changesNothing() {
        Organisation company = company("ACME", "123", "NL");
        Application application = withCompanies(company);

        mapper.mapEventIndicatorData(null, application);
        mapper.mapEventIndicatorData(List.of(), application);

        assertThat(company.getResultStatus()).isNull();
    }
}
