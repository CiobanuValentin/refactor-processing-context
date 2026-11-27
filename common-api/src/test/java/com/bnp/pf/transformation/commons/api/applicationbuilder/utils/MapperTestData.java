package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importuri: Customer, Organisation, Address, Identification, RiskAssessment si tipurile WSDL (DateType, ...) sunt
// in aceleasi pachete ca in mappere - copiaza importurile din mapperul testat.
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

/** Date comune pentru testele mapperelor. */
final class MapperTestData {
    static final String BUSINESS_KEY = "BK-001";
    static final LocalDateTime BIRTH_DATE = LocalDateTime.of(1990, 1, 15, 0, 0);

    private MapperTestData() {
    }

    static Application application(List<Customer> customers, List<Organisation> companies) {
        Loan loan = new Loan();
        loan.setBusinessKey(BUSINESS_KEY);
        loan.setCustomers(new ArrayList<>(customers));
        loan.setCompanies(new ArrayList<>(companies));
        loan.setRiskAssessments(new ArrayList<>());
        Application application = new Application();
        application.setBusinessKey(BUSINESS_KEY);
        application.setLoan(loan);
        return application;
    }

    static Application withCustomers(Customer... customers) {
        return application(List.of(customers), List.of());
    }

    static Application withCompanies(Organisation... companies) {
        return application(List.of(), List.of(companies));
    }

    static Customer customer(String businessKey, String lastName, String applicantType) {
        Customer customer = new Customer();
        customer.setBusinessKey(businessKey);
        customer.setLastName(lastName);
        customer.setApplicantType(applicantType);
        customer.setBirthDate(BIRTH_DATE);
        customer.setRiskAssessments(new ArrayList<>());
        return customer;
    }

    static Organisation company(String name, String cocNumber, String country) {
        Organisation company = new Organisation();
        company.setStatutoryName(name);
        company.setLegalRegistrationNumber(cocNumber);
        company.setCountryOfResidence(country);
        company.setRiskAssessments(new ArrayList<>());
        return company;
    }

    static DateType date(LocalDateTime dateTime) {
        DateType date = new DateType();
        date.setYear(dateTime.getYear());
        date.setMonth(dateTime.getMonthValue());
        date.setDay(dateTime.getDayOfMonth());
        return date;
    }

    static List<RiskAssessment> withCode(List<RiskAssessment> riskAssessments, String code) {
        return riskAssessments.stream().filter(r -> code.equals(r.getClassificationInstrumentCode())).toList();
    }
}
