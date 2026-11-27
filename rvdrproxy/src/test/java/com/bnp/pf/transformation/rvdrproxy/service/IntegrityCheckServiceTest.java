package com.bnp.pf.transformation.rvdrproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO: importurile pentru Error si Customer sunt presupuse.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Customer;

import com.bnp.pf.transformation.rvdrproxy.builder.ContextBuilder;

/** Dupa modelul din vis: erorile se citesc din ProxyContext; aceleasi scenarii ca in cloud. */
@ExtendWith(MockitoExtension.class)
class IntegrityCheckServiceTest {
    private static final String BUSINESS_KEY = "BK-001";

    @Mock
    private ApplicationService applicationService;
    @Mock
    private CreditCheckService creditCheckService;

    @InjectMocks
    private IntegrityCheckService integrityCheckService;

    private Application application;
    private ProxyContext ctx;

    @BeforeEach
    void init() {
        Loan loan = new Loan();
        loan.setCustomers(List.of(new Customer()));
        application = ContextBuilder.application(BUSINESS_KEY);
        application.setLoan(loan);
        ctx = ContextBuilder.context(application);
    }

    @Test
    void loanCheckProcess_whenCustomersPresent_shouldSaveApplicationThenCheckCredit() {
        integrityCheckService.loanCheckProcess(ctx);

        InOrder inOrder = Mockito.inOrder(applicationService, creditCheckService);
        inOrder.verify(applicationService).saveApplicationRequest(same(ctx));
        inOrder.verify(creditCheckService).checkCredit(same(ctx), same(application.getLoan()));
        assertThat(ctx.getErrors()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenNoCustomers_shouldSkipCreditCheckWithoutError() {
        application.getLoan().setCustomers(List.of());

        integrityCheckService.loanCheckProcess(ctx);

        Mockito.verify(applicationService).saveApplicationRequest(same(ctx));
        verifyNoInteractions(creditCheckService);
        assertThat(ctx.getErrors()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenLoanIsNull_shouldReportLoanNotPresent() {
        application.setLoan(null);

        integrityCheckService.loanCheckProcess(ctx);

        Mockito.verify(applicationService).saveApplicationRequest(same(ctx));
        verifyNoInteractions(creditCheckService);
        assertThat(ctx.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("400", LOAN_EMPTY));
    }

    @Test
    void loanCheckProcess_whenApplicationIsNull_shouldDoNothing() {
        ctx.setApplication(null);

        integrityCheckService.loanCheckProcess(ctx);

        verifyNoInteractions(applicationService, creditCheckService);
        assertThat(ctx.getErrors()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenSaveFails_shouldRegisterErrorAndSkipCreditCheck() {
        doThrow(new RuntimeException("Database down")).when(applicationService).saveApplicationRequest(any());

        integrityCheckService.loanCheckProcess(ctx);

        verifyNoInteractions(creditCheckService);
        assertThat(ctx.getErrors()).hasSize(1);
        assertThat(ctx.getErrors().get(0).getErrorCode()).isEqualTo("500");
        assertThat(ctx.getErrors().get(0).getErrorMessage()).contains("Database down", BUSINESS_KEY);
    }
}
