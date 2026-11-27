package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.BIRTH_DATE;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.customer;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.date;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCode;
import static com.bnp.pf.transformation.commons.api.applicationbuilder.utils.MapperTestData.withCustomers;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckCkiPersonDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.BkrCkiResponse;
import com.bnp.pf.transformation.commons.api.applicationbuilder.model.CreditLoan;
import com.bnp.pf.transformation.commons.api.message.input.Application;

class BKRCKIMapperTest {

    private final BKRCKIMapper mapper = new BKRCKIMapper();

    // ---------------------------------------------------------------- date de test

    private static Customer ckiCustomer(String key, String lastName, String applicantType) {
        Customer customer = customer(key, lastName, applicantType);
        customer.setInitialsName("J.");
        return customer;
    }

    private static CreditLoanType loan(String type, int amount, LocalDateTime first, LocalDateTime plannedLast, String... codes) {
        CreditLoanType loan = new CreditLoanType();
        loan.setAccountNumber("ACC-" + type);
        loan.setTypeOfLoan(type);
        loan.setLoanAmount(amount);
        loan.setFirstPayment(date(first));
        loan.setHypLastPayment(plannedLast == null ? null : date(plannedLast));
        loan.setRegistration(date(first));
        loan.setIndex(1);
        if (codes.length > 0) {
            DetailCodesType detailCodes = new DetailCodesType();
            detailCodes.setDetailCode(Arrays.stream(codes).map(code -> {
                DetailCodeType detail = new DetailCodeType();
                detail.setCode(code);
                return detail;
            }).toList());
            loan.setDetailCodes(detailCodes);
        }
        return loan;
    }

    private static SingleBkrPerson bkrPerson(String id, int score, String outcome, CreditLoanType... loans) {
        BkrScoreResult scoreResult = new BkrScoreResult();
        scoreResult.setCardRiskClass("B");
        scoreResult.setCardRiskPercentage(1.5);
        scoreResult.setCardScoreValue(700);
        BkrScoreType bkrScore = new BkrScoreType();
        bkrScore.setCardName("CARD");
        bkrScore.setResult(scoreResult);
        BkrScoresType scores = new BkrScoresType();
        scores.setBkrScore(new ArrayList<>(List.of(bkrScore)));
        CreditLoansType creditLoans = new CreditLoansType();
        creditLoans.setCreditLoan(new ArrayList<>(List.of(loans)));

        SingleBkrPerson person = new SingleBkrPerson();
        person.setPersonID(id);
        person.setMatchingScore(score);
        person.setMatchingOutcome(outcome);
        person.setBkrScores(scores);
        person.setCreditLoans(creditLoans);
        return person;
    }

    private static CheckCkiPersonDto row(String lastName, SingleBkrPerson... persons) {
        SingleBkrRequestPerson requestPerson = new SingleBkrRequestPerson();
        requestPerson.setName(lastName);
        requestPerson.setInitials("J");
        requestPerson.setDateOfBirth(date(BIRTH_DATE));
        SingleBkrRequest request = new SingleBkrRequest();
        request.setPerson(requestPerson);

        SingleBkrPersons bkrPersons = new SingleBkrPersons();
        bkrPersons.setPerson(new ArrayList<>(List.of(persons)));
        SingleBkrResponseResult result = new SingleBkrResponseResult();
        result.setOverallMatchingOutcome("01");
        result.setPersons(bkrPersons);
        SingleBkrResponse response = new SingleBkrResponse();
        response.setResult(result);

        CheckCkiPersonDto dto = new CheckCkiPersonDto();
        dto.setRequest(request);
        dto.setResponse(response);
        return dto;
    }

    private static CheckCkiPersonDto activeLoanRow(String lastName) {
        LocalDateTime first = LocalDateTime.now().minusMonths(2);
        return row(lastName, bkrPerson("P-" + lastName, 90, "01", loan("AK", 120, first, first.plusMonths(11))));
    }

    // ---------------------------------------------------------------- mapCKIData

    @Test
    void noRows_loanGetsEmptyHolderAndCoHolderResults() {
        Application application = withCustomers(ckiCustomer("C1", "Jansen", "Holder"));

        mapper.mapCKIData(List.of(), application);

        List<RiskAssessment> loan = application.getLoan().getRiskAssessments();
        assertThat(loan).extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
        assertThat(loan).allSatisfy(risk -> {
            assertThat(risk.getClassificationInstrumentCode()).isEqualTo("bkr");
            assertThat(risk.getClassificationResults()).containsEntry("Result", "false").containsEntry("TotalLoanAmount", 0);
        });
    }

    @Test
    void nullList_sameAsNoRows() {
        Application application = withCustomers(ckiCustomer("C1", "Jansen", "Holder"));

        mapper.mapCKIData(null, application);

        assertThat(application.getLoan().getRiskAssessments()).hasSize(2);
    }

    @Test
    void matchingHolder_getsCustomerResultWithLoans_loanGetsHolderAndCoHolder() {
        Customer customer = ckiCustomer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapCKIData(List.of(activeLoanRow("Jansen")), application);

        assertThat(withCode(customer.getRiskAssessments(), "bkr")).singleElement().satisfies(risk -> {
            assertThat(risk.getCustomerBusinessKey()).isEqualTo("C1");
            assertThat(risk.getDescription()).isEqualTo("BKR CKI results for C1");
            assertThat(risk.getClassificationResults())
                    .containsEntry("BkrStatus", "1")
                    .containsEntry("NumberOfActiveLoansTypePersonalLoan", 1)
                    .containsEntry("AmountOfActiveLoansTypePersonalLoan", 12000)
                    .containsEntry("CardName0", "CARD")
                    .containsKey("Loans");
        });
        assertThat(application.getLoan().getRiskAssessments())
                .extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
        assertThat(application.getLoan().getRiskAssessments().get(0).getClassificationResults())
                .containsEntry("NumberOfActiveLoansTypePersonalLoan", 1);
    }

    /** Diferenta fata de varianta cu un singur dto: rezultatele de pe loan se adauga o singura data, dupa bucla. */
    @Test
    void severalRows_eachCustomerGetsItsResult_loanResultsAddedOnce() {
        Customer holder = ckiCustomer("C1", "Jansen", "Holder");
        Customer coHolder = ckiCustomer("C2", "Bakker", "CoHolder");
        Application application = withCustomers(holder, coHolder);

        mapper.mapCKIData(Arrays.asList(activeLoanRow("Jansen"), null, activeLoanRow("Bakker")), application);

        assertThat(withCode(holder.getRiskAssessments(), "bkr")).hasSize(1);
        assertThat(withCode(coHolder.getRiskAssessments(), "bkr")).hasSize(1);
        List<RiskAssessment> loan = application.getLoan().getRiskAssessments();
        assertThat(loan).extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
        assertThat(loan).allSatisfy(risk ->
                assertThat(risk.getClassificationResults()).containsEntry("NumberOfActiveLoansTypePersonalLoan", 1));
    }

    @Test
    void customerAlreadyHasCkiResult_isNotMatchedAgain() {
        Customer customer = ckiCustomer("C1", "Jansen", "Holder");
        Application application = withCustomers(customer);

        mapper.mapCKIData(List.of(activeLoanRow("Jansen")), application);
        mapper.mapCKIData(List.of(activeLoanRow("Jansen")), application);

        assertThat(withCode(customer.getRiskAssessments(), "bkr")).hasSize(1);
    }

    @Test
    void differentInitials_noMatch_loanStillGetsEmptyResults() {
        Customer customer = ckiCustomer("C1", "Jansen", "Holder");
        customer.setInitialsName("P.");
        Application application = withCustomers(customer);

        mapper.mapCKIData(List.of(activeLoanRow("Jansen")), application);

        assertThat(customer.getRiskAssessments()).isEmpty();
        assertThat(application.getLoan().getRiskAssessments()).isEmpty();
    }

    // ---------------------------------------------------------------- collectAllResponses

    @Test
    void collectAllResponses_samePersonWithAliasesLater_creditsAreAddedToTheFirstOne() {
        LocalDateTime first = LocalDateTime.of(2024, 1, 1, 0, 0);
        SingleBkrPerson original = bkrPerson("P1", 90, "01", loan("AK", 100, first, first.plusMonths(12)));
        SingleBkrPerson alias = bkrPerson("P1", 80, "01", loan("RK", 200, first, first.plusMonths(12)));
        alias.setAliases(new Object());
        SingleBkrResponse response = row("Jansen", original, alias).getResponse();

        Map<String, SingleBkrPerson> persons = mapper.collectAllResponses(response);

        assertThat(persons).containsOnlyKeys("P1");
        assertThat(persons.get("P1")).isSameAs(original);
        assertThat(original.getCreditLoans().getCreditLoan()).extracting(CreditLoanType::getTypeOfLoan).containsExactly("AK", "RK");
        assertThat(original.getBkrScores().getBkrScore()).hasSize(2);
    }

    @Test
    void collectAllResponses_firstHasAliases_laterOneReplacesItAndCollectsBoth() {
        LocalDateTime first = LocalDateTime.of(2024, 1, 1, 0, 0);
        SingleBkrPerson withAliases = bkrPerson("P1", 90, "01", loan("AK", 100, first, first.plusMonths(12)));
        withAliases.setAliases(new Object());
        SingleBkrPerson later = bkrPerson("P1", 80, "01", loan("RK", 200, first, first.plusMonths(12)));

        Map<String, SingleBkrPerson> persons = mapper.collectAllResponses(row("Jansen", withAliases, later).getResponse());

        assertThat(persons.get("P1")).isSameAs(later);
        assertThat(later.getCreditLoans().getCreditLoan()).extracting(CreditLoanType::getTypeOfLoan).containsExactly("RK", "AK");
    }

    @Test
    void collectAllResponses_noPersons_emptyMap() {
        assertThat(mapper.collectAllResponses(new SingleBkrResponse())).isEmpty();
    }

    // ---------------------------------------------------------------- createRiskAssessments

    @Test
    void createRiskAssessments_withoutResults_twoEmptyResults() {
        assertThat(mapper.createRiskAssessments(new BkrCkiResponse()))
                .extracting(RiskAssessment::getResultType).containsExactly("Holder", "CoHolder");
    }

    // ---------------------------------------------------------------- calcule pe un credit

    private static CreditLoan creditLoan(String type, Integer amount, LocalDateTime first, LocalDateTime plannedLast,
                                         Boolean active, String... codes) {
        CreditLoan loan = new CreditLoan();
        loan.setTypeOfLoan(type);
        loan.setAmount(amount);
        loan.setFirstPayment(first);
        loan.setPlannedLastPayment(plannedLast);
        loan.setActive(active);
        loan.setBkrCodes(List.of(codes));
        return loan;
    }

    @Test
    void highRisk_byTypeOrByCodes() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(mapper.highRisk(creditLoan("RO", 100, now, now, true))).isTrue();
        assertThat(mapper.highRisk(creditLoan("AK", 100, now, now, true, "A"))).isTrue();
        assertThat(mapper.highRisk(creditLoan("AK", 100, now, now, true, "3"))).isTrue();
        assertThat(mapper.highRisk(creditLoan("AK", 100, now, now, true, "A", "H"))).isFalse();
        assertThat(mapper.highRisk(creditLoan("AK", 100, now, now, true))).isFalse();
    }

    @Test
    void getDuration_inactiveZero_balloonOne_otherwiseMonthsPlusOne() {
        LocalDateTime first = LocalDateTime.of(2024, 1, 1, 0, 0);
        assertThat(mapper.getDuration(creditLoan("AK", 100, first, first.plusMonths(11), false))).isZero();
        assertThat(mapper.getDuration(creditLoan("AK", 100, first, first, true))).isEqualTo(1);
        assertThat(mapper.getDuration(creditLoan("AK", 100, first, null, true))).isNull();
        assertThat(mapper.getDuration(creditLoan("AK", 100, first, first.plusMonths(11), true))).isEqualTo(12);
    }

    @Test
    void getInstallment_rules() {
        LocalDateTime first = LocalDateTime.of(2024, 1, 1, 0, 0);
        assertThat(mapper.getInstallment(creditLoan("AK", 12000, first, first.plusMonths(11), false))).isEqualByComparingTo("0");
        assertThat(mapper.getInstallment(creditLoan("AK", 10000, first, first, true))).isEqualByComparingTo("200");
        assertThat(mapper.getInstallment(creditLoan("RK", 10000, first, null, true))).isEqualByComparingTo("2500");
        assertThat(mapper.getInstallment(creditLoan("AK", 12000, first, first.plusMonths(11), true))).isEqualByComparingTo("1000");
        LocalDateTime oldLease = LocalDateTime.of(2020, 1, 1, 0, 0);
        assertThat(mapper.getInstallment(creditLoan("OA", 6500, oldLease, oldLease.plusMonths(9), true)))
                .isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void getTotalAmount_sumsOnlyActivePositiveLoans() {
        LocalDateTime now = LocalDateTime.now();
        assertThat(mapper.getTotalAmount(List.of(
                creditLoan("AK", 100, now, now, true),
                creditLoan("AK", 50, now, now, false),
                creditLoan("AK", 0, now, now, true)))).isEqualTo(100);
        assertThat(mapper.getTotalAmount(null)).isZero();
    }
}
