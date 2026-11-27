// REFERINTA - test existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@ExtendWith(MockitoExtension.class)
class IntegrityCheckServiceTest {
    private static final String BUSINESS_KEY = "BK-001";

    @Mock
    private GeneralNullOrEmptyCheckService generalNullOrEmptyCheckService;
    @Mock
    private CollectError collectError;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private CreditCheckService creditCheckService;
    @Mock
    private OrchestrationMessage orchestrationMessage;

    @InjectMocks
    private IntegrityCheckService integrityCheckService;

    private Application application;
    private ProcessingContext processingContext;
    private int initialIndex;

    @BeforeEach
    void init() {
        application = ApplicationBuilder.createApplication();

        processingContext = new ProcessingContext();
        processingContext.setApplication(application);
        processingContext.setBusinessKey(BUSINESS_KEY);

        initialIndex = processingContext.getIndex().get();
    }

    private static List<String> invokedMethods(Object... mocks) { /* ...ca in SoapClientTest */ }

    @Test
    void loanCheckProcess_whenCustomersPresent_shouldSaveApplicationThenCheckCredit() {
        integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);

        ArgumentCaptor<String> applicationJson = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = Mockito.inOrder(applicationService, creditCheckService);
        inOrder.verify(applicationService).saveApplicationRequest(same(application), same(orchestrationMessage),
                applicationJson.capture(), same(processingContext));
        inOrder.verify(creditCheckService).checkCredit(same(processingContext), same(application.getLoan()));

        assertThat(invokedMethods(collectError, generalNullOrEmptyCheckService)).isEmpty();
        assertThat(processingContext.getErrorMap()).isEmpty();
        assertThat(applicationJson.getValue()).isEqualTo(GsonInstance.createGson().toJson(application));
    }

    @Test
    void loanCheckProcess_whenCustomers_shouldRegisterErrorInsteadOfThrowing() {
        application.getLoan().setCustomers(List.of());
        integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);

        assertThat(invokedMethods(applicationService)).containsExactly("saveApplicationRequest");
        assertThat(invokedMethods(creditCheckService)).isEmpty();
        assertThat(processingContext.getErrorMap()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenLoanIsNull_shouldSaveApplicationAndReportLoanNotPresent() {
        application.setLoan(null);
        integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);
        verify(generalNullOrEmptyCheckService).loanNotPresent(same(processingContext));

        assertThat(invokedMethods(applicationService)).containsExactly("saveApplicationRequest");
        assertThat(invokedMethods(creditCheckService, collectError)).isEmpty();
    }

    @Test
    void loanCheckProcess_whenApplicationIsNull_shouldDoNothing() {
        processingContext.setApplication(null);
        integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);

        assertThat(invokedMethods(applicationService, creditCheckService, collectError, generalNullOrEmptyCheckService)).isEmpty();
        assertThat(processingContext.getErrorMap()).isEmpty();
    }

    @Test
    void loanCheckProcess_whenSaveFails_shouldRegisterErrorAndSkipCreditCheck() {
        doThrow(new RuntimeException("Database down"))
                .when(applicationService).saveApplicationRequest(any(), any(), anyString(), any());

        integrityCheckService.loanCheckProcess(processingContext, orchestrationMessage);

        verify(collectError).addError(same(processingContext), any(RuntimeException.class));
        verify(collectError).addErrorSignal(same(processingContext), any());

        assertThat(invokedMethods(creditCheckService, generalNullOrEmptyCheckService)).isEmpty();
        assertThat(processingContext.getErrorMap()).containsOnlyKeys(ERROR_MESSAGE + initialIndex);
        assertThat(processingContext.getErrorMap()
                .get(ERROR_MESSAGE + initialIndex)).contains("Database down", BUSINESS_KEY);
    }
}
