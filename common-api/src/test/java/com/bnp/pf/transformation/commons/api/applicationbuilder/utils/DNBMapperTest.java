package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.company;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCompanies;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckDnbCompanyDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
// Customer, Organisation, Address, Identification, RiskAssessment: acelasi pachet ca Application (message.input).
import com.bnp.pf.transformation.commons.api.message.input.*;

class DNBMapperTest {

    private static final String DIRECTOR_BIRTH = "1970-05-20";

    private final DNBMapper mapper = new DNBMapper();

    private static CheckDnbCompanyDto row(String coc, String country, RecordPayload payload) {
        Company requestCompany = new Company();
        requestCompany.setCocNumber(coc);
        requestCompany.setCountryCode(country);
        RequestCompany request = new RequestCompany();
        request.setCompany(requestCompany);

        ResponseRoot response = new ResponseRoot();
        if (payload != null) {
            RequestedSectionsAvailability availability = new RequestedSectionsAvailability();
            availability.setPrincipalsInformationAvailabilityIndicator(true);
            RecordStatusNotification notification = new RecordStatusNotification();
            notification.setRequestedSectionsAvailability(availability);
            RecordHeader header = new RecordHeader();
            header.setRecordStatusNotification(notification);
            CCDP ccdp = new CCDP();
            ccdp.setRecordHeader(header);
            ccdp.setRecordPayload(payload);
            CCDPResponse ccdpResponse = new CCDPResponse();
            ccdpResponse.setCcdpList(List.of(ccdp));
            DUNSResponse duns = new DUNSResponse();
            duns.setCcdpResponse(ccdpResponse);
            CompanyResponse companyResponse = new CompanyResponse();
            companyResponse.setDunsresponse(duns);
            response.setCompanyResponse(companyResponse);
        }

        CheckDnbCompanyDto dto = new CheckDnbCompanyDto();
        dto.setRequest(request);
        dto.setResponse(response);
        return dto;
    }

    private static RecordPayload fullPayload() {
        AssessmentSummaryType assessment = new AssessmentSummaryType();
        assessment.setDnbpaydexScore("80");
        assessment.setDnbfailureScore("50");
        assessment.setDnbfinancialStrength("2A");
        assessment.setDnbriskIndicator("2");
        assessment.setScoreOverrideIndicator(true);

        RegistrationNumbers tradeRegister = new RegistrationNumbers();
        tradeRegister.setRegistrationNumber(new ValueType("NL-999", "TradeRegisterNumber"));
        RegistrationNumbers other = new RegistrationNumbers();
        other.setRegistrationNumber(new ValueType("VAT-1", "VAT"));
        IdentificationSummary identification = new IdentificationSummary();
        identification.setRegistrationNumbers(List.of(other, tradeRegister));
        identification.setPrimaryLineofBusinessSIC("7372");

        CompanyOverview overview = new CompanyOverview();
        overview.setBusinessStartDate(new DNBDate("2001-02-03"));
        overview.setLegalForm(new ValueType("BV", null));

        PublicFilingsSummaryType filings = new PublicFilingsSummaryType();
        filings.setBusinessFailureIndicator(true);
        filings.setCeasedTradingIndicator(false);
        filings.setLiquidationIndicator(true);

        CompanySummary summary = new CompanySummary();
        summary.setDnbassessmentOverview(assessment);
        summary.setIdentification(identification);
        summary.setCompanyOverview(overview);
        summary.setPublicFilingsOverview(filings);

        CurrentPrincipalPositionsEntry position = new CurrentPrincipalPositionsEntry();
        position.setPrincipalPosition(new ValueType("Directeur", null));
        PrincipalPositions positions = new PrincipalPositions();
        positions.setCurrentPrincipalPositionsEntry(List.of(position));
        PersonalDetails details = new PersonalDetails();
        details.setBirthDate(new DNBDate(DIRECTOR_BIRTH));
        PrincipalsInformationEntryType director = new PrincipalsInformationEntryType();
        director.setPrincipalPositions(positions);
        director.setPersonalDetails(details);
        CurrentPrincipalsInformation current = new CurrentPrincipalsInformation();
        current.setCurrentPrincipalsInformationEntry(List.of(director));
        PrincipalsInformation principals = new PrincipalsInformation();
        principals.setCurrentPrincipalsInformation(current);

        EmployeesInformationEntry employees = new EmployeesInformationEntry();
        employees.setTotalEmployeesQuantity(42);
        EmployeesInformation employeesInformation = new EmployeesInformation();
        employeesInformation.setEmployeesInformationEntry(List.of(employees));
        SubjectOperationalInformation operational = new SubjectOperationalInformation();
        operational.setEmployeesInformation(employeesInformation);
        OperationsandHistory history = new OperationsandHistory();
        history.setSubjectOperationalInformation(operational);

        FinancialStatementHeaderType header = new FinancialStatementHeaderType();
        header.setLatestAccountsFiledDate(new DNBDate("2024-06-30"));
        header.setMonthsCovered(12);
        FinancialInformationEntry entry = new FinancialInformationEntry();
        entry.setFinancialStatementHeader(header);
        Financials financials = new Financials();
        financials.setFinancialInformationEntry(List.of(entry));

        RecordPayload payload = new RecordPayload();
        payload.setCompanySummary(summary);
        payload.setPrincipalsInformation(principals);
        payload.setOperationsandHistory(history);
        payload.setFinancials(financials);
        return payload;
    }

    @Test
    void matchingCompany_getsAllDnbFields() {
        Organisation company = company("ACME", "123", "NL");

        mapper.mapDNBData(List.of(row("123", "NL", fullPayload())), withCompanies(company));

        int expectedAge = Period.between(LocalDate.parse(DIRECTOR_BIRTH), LocalDate.now()).getYears();
        assertThat(company.getDirectorAge()).isEqualTo(expectedAge);
        assertThat(company.getPaydexScore()).isEqualTo("80");
        assertThat(company.getDnbFailureScore()).isEqualTo("50");
        assertThat(company.getDnbFinancialStrength()).isEqualTo("2A");
        assertThat(company.getDnbriskIndicator()).isEqualTo("2");
        assertThat(company.getScoreOverrideIndicator()).isTrue();
        assertThat(company.getNumberOfEmployees()).isEqualTo(42);
        assertThat(company.getIndustryCode()).isEqualTo("7372");
        assertThat(company.getCompanyLegalForm()).isEqualTo("BV");
        assertThat(company.getLegalRegistrationNumber()).isEqualTo("NL-999");
        assertThat(company.getBusinessFailureIndicator()).isTrue();
        assertThat(company.getCeasedTradingIndicator()).isFalse();
        assertThat(company.getLiquidationIndicator()).isTrue();
        assertThat(company.getCompanyAge()).isEqualTo(LocalDateTime.of(2001, 2, 3, 0, 0));
        assertThat(company.getDnbLatestAccountFiledDate()).isEqualTo(20240630);
        assertThat(company.getDnbLatestAccountFiledPeriod()).isEqualTo(12);
        assertThat(company.getCompanyFound()).isTrue();
    }

    @Test
    void emptyEmployeesEntry_numberOfEmployeesNotSet() {
        Organisation company = company("ACME", "123", "NL");
        RecordPayload payload = fullPayload();
        payload.getOperationsandHistory().getSubjectOperationalInformation().getEmployeesInformation()
                .setEmployeesInformationEntry(null);

        mapper.mapDNBData(List.of(row("123", "NL", payload)), withCompanies(company));

        assertThat(company.getNumberOfEmployees()).isNull();
        assertThat(company.getPaydexScore()).isEqualTo("80");
    }

    @Test
    void responseWithoutCcdp_companyMatchedButNoFieldsSet() {
        Organisation company = company("ACME", "123", "NL");

        mapper.mapDNBData(List.of(row("123", "NL", null)), withCompanies(company));

        assertThat(company.getCompanyFound()).isNull();
        assertThat(company.getPaydexScore()).isNull();
    }

    @Test
    void differentCocNumber_companyUntouched() {
        Organisation company = company("ACME", "123", "NL");

        mapper.mapDNBData(List.of(row("999", "NL", fullPayload())), withCompanies(company));

        assertThat(company.getCompanyFound()).isNull();
        assertThat(company.getLegalRegistrationNumber()).isEqualTo("123");
    }

    @Test
    void severalRows_eachCompanyMatched_nullRowsSkipped() {
        Organisation first = company("ACME", "123", "NL");
        Organisation second = company("Other", "456", "NL");

        mapper.mapDNBData(Arrays.asList(row("123", "NL", fullPayload()), null, row("456", "NL", fullPayload())),
                withCompanies(first, second));

        assertThat(first.getCompanyFound()).isTrue();
        assertThat(second.getCompanyFound()).isTrue();
    }

    @Test
    void nullOrEmptyList_changesNothing() {
        Organisation company = company("ACME", "123", "NL");
        Application application = withCompanies(company);

        mapper.mapDNBData(null, application);
        mapper.mapDNBData(List.of(), application);

        assertThat(company.getCompanyFound()).isNull();
    }

    @Test
    void calculateAge_countsWholeYears() {
        assertThat(mapper.calculateAge(LocalDate.now().minusYears(30).toString())).isEqualTo(30);
    }
}
