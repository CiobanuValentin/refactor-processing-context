package visproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.visproxy.service.IntegrityCheckService;
import com.bnp.pf.transformation.visproxy.service.VisCheckService;

import visproxy.builder.ApplicationBuilder;
import visproxy.builder.ContextBuilder;

/**
 * Rescris: fara GeneralNullOrEmptyCheckService si CollectError; erorile se citesc din ProxyContext.
 * Aceleasi scenarii ca in cloud. JSON-ul aplicatiei il construieste acum ApplicationService (saveApplicationRequest(ctx)).
 */
@ExtendWith(MockitoExtension.class)
class IntegrityCheckServiceTest {
    private static final String BUSINESS_KEY = "BK-001";

    @Mock
    private ApplicationService applicationService;
    @Mock
    private VisCheckService visCheckService;

    @InjectMocks
    private IntegrityCheckService integrityCheckService;

    private Application application;
    private ProxyContext processingContext;

    @BeforeEach
    void init() {
        application = ApplicationBuilder.createApplication();
        application.setBusinessKey(BUSINESS_KEY);
        processingContext = ContextBuilder.context(application);
    }

    private static List<String> invokedMethods(Object... mocks) {
        return Arrays.stream(mocks)
                .flatMap(mock -> mockingDetails(mock).getInvocations().stream())
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

    @Test
    void loanCheckProcess_whenCustomersPresent_shouldSaveApplicationThenCheckVis() {
        integrityCheckService.loanCheckProcess(processingContext);

        InOrder inOrder = Mockito.inOrder(applicationService, visCheckService);
        inOrder.verify(applicationService).saveApplicationRequest(same(processingContext));
        inOrder.verify(visCheckService).checkVis(same(processingContext), same(application.getLoan()));

        assertThat(processingContext.getErrors()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenCustomersAndCompaniesEmpty_shouldRegisterErrorInsteadOfThrowing() {
        application.getLoan().setCompanies(List.of());
        application.getLoan().setCustomers(List.of());
        integrityCheckService.loanCheckProcess(processingContext);

        assertThat(invokedMethods(applicationService)).containsExactly("saveApplicationRequest");
        assertThat(invokedMethods(visCheckService)).isEmpty();
        assertThat(processingContext.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("400", "Unable to process VIS data. "
                        + "No valid Customers or Companies found in Application with businessKey: " + BUSINESS_KEY));
    }

    @Test
    void loanCheckProcess_whenLoanIsNull_shouldSaveApplicationAndReportLoanNotPresent() {
        application.setLoan(null);
        integrityCheckService.loanCheckProcess(processingContext);

        assertThat(invokedMethods(applicationService)).containsExactly("saveApplicationRequest");
        assertThat(invokedMethods(visCheckService)).isEmpty();
        assertThat(processingContext.getErrors())
                .extracting(Error::getErrorCode, Error::getErrorMessage)
                .containsExactly(tuple("400", LOAN_EMPTY));
    }

    @Test
    void loanCheckProcess_whenApplicationIsNull_shouldDoNothing() {
        processingContext.setApplication(null);
        integrityCheckService.loanCheckProcess(processingContext);

        assertThat(invokedMethods(applicationService, visCheckService)).isEmpty();
        assertThat(processingContext.getErrors()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenSaveFails_shouldRegisterErrorAndSkipVisCheck() {
        doThrow(new RuntimeException("Database down"))
                .when(applicationService).saveApplicationRequest(any());

        integrityCheckService.loanCheckProcess(processingContext);

        assertThat(invokedMethods(visCheckService)).isEmpty();
        assertThat(processingContext.getErrors()).hasSize(1);
        assertThat(processingContext.getErrors().get(0).getErrorCode()).isEqualTo("500");
        assertThat(processingContext.getErrors().get(0).getErrorMessage()).contains("Database down", BUSINESS_KEY);
    }
}
