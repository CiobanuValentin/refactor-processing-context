// REFERINTA - test existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@ExtendWith(MockitoExtension.class)
class CreditCheckServiceTest {
    private static final String BUSINESS_KEY = "BK-001";
    private static final String PRODUCER = "producer";
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

    @Mock
    private ModelMapper modelMapper;
    @Mock
    private CreditCheckCacheService creditCheckCacheService;
    @Mock
    private SoapClient soapClient;
    @Mock
    private CreditCheckRecordService creditCheckRecordService;
    @Spy
    private Gson gson = new Gson();

    @InjectMocks
    private CreditCheckService creditCheckService;

    @Captor
    private ArgumentCaptor<CreditCheckRequest> savedRequest;
    @Captor
    private ArgumentCaptor<String> savedResponse;
    @Captor
    private ArgumentCaptor<List<Error>> savedErrors;
    @Captor
    private ArgumentCaptor<LocalDate> savedDate;

    private ProcessingContext processingContext;
    private Loan loan;

    @BeforeEach
    void setUp() {
        processingContext = new ProcessingContext();
        processingContext.setBusinessKey(BUSINESS_KEY);
        processingContext.setProducer(PRODUCER);
        loan = new Loan();
    }

    private static Customer person() {
        return new Customer();
    }

    private static Customer companyPerson() {
        Customer customer = new Customer();
        customer.setApplicantType(COMPANY_PERSON.getApplicant());

        return customer;
    }

    private CreditCheckRequest givenRequestFor(Customer customer) {
        CreditCheckRequest request = new CreditCheckRequest();
        when(modelMapper.map(same(customer), eq(CreditCheckRequest.class))).thenReturn(request);

        return request;
    }

    private void givenSoapResult(CreditCheckRequest request, CreditCheckResult result) {
        when(creditCheckCacheService.find(same(request), eq(PRODUCER))).thenReturn(Optional.empty());
        when(soapClient.creditCheckRequest(same(processingContext), same(request))).thenReturn(result);
    }

    private void checkCreditToday() {
        try (MockedStatic<LocalDateTimeUtils> utils = mockStatic(LocalDateTimeUtils.class)) {
            utils.when(LocalDateTimeUtils::parseLocalDateNow).thenReturn(TODAY);
            creditCheckService.checkCredit(processingContext, loan);
        }
    }

    private void captureSavedRecords(int expectedRecords) {
        verify(creditCheckRecordService, times(expectedRecords)).save(same(processingContext), savedRequest.capture(),
                savedResponse.capture(), savedErrors.capture(), savedDate.capture());
    }

    private static List<String> invokedMethods(Object... mocks) { /* ...ca in SoapClientTest */ }

    @Test
    void checkCredit_whenNoCustomers_shouldDoNothing() {
        loan.setCustomers(List.of());

        creditCheckService.checkCredit(processingContext, loan);
        assertThat(invokedMethods(modelMapper, creditCheckCacheService, soapClient, creditCheckRecordService)).isEmpty();
    }

    @Test
    void checkCredit_whenOnlyCompanyPersons_shouldSkipThem() {
        loan.setCustomers(List.of(companyPerson()));

        creditCheckService.checkCredit(processingContext, loan);
        assertThat(invokedMethods(modelMapper, creditCheckCacheService, soapClient, creditCheckRecordService)).isEmpty();
    }

    @Test
    void checkCredit_whenResponseIsCached_shouldSaveCachedResponseWithoutSoapCall() {
        Customer customer = person();
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        LocalDate cachedDate = LocalDate.of(2026, 1, 10);
        GeneralProxyObsCacheDto cached = mock(GeneralProxyObsCacheDto.class);
        when(cached.getResponseText()).thenReturn("{\"cached\":true}");
        when(cached.getCreatedDate()).thenReturn(cachedDate);
        when(creditCheckCacheService.find(same(request), eq(PRODUCER))).thenReturn(Optional.of(cached));

        creditCheckService.checkCredit(processingContext, loan);

        captureSavedRecords(1);
        assertThat(invokedMethods(soapClient)).isEmpty();
        assertThat(invokedMethods(creditCheckCacheService)).containsExactly("find");
        assertThat(savedRequest.getValue()).isSameAs(request);
        assertThat(savedResponse.getValue()).isEqualTo("{\"cached\":true}");
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(cachedDate);
    }

    @Test
    void checkCredit_whenSoapCallSucceeds_shouldCacheAndSaveResponse() {
        Customer customer = person();
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        CreditCheckResponse response = new CreditCheckResponse();
        givenSoapResult(request, new CreditCheckResult(response, List.of()));

        checkCreditToday();

        String responseJson = new Gson().toJson(response);
        verify(creditCheckCacheService).save(same(request), eq(responseJson), eq(PRODUCER));
        captureSavedRecords(1);
        assertThat(savedRequest.getValue()).isSameAs(request);
        assertThat(savedResponse.getValue()).isEqualTo(responseJson);
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
    }

    @Test
    void checkCredit_whenSoapCallReturnsErrors_shouldSaveErrorsAndNotCache() {
        Customer customer = person();
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        Error error = new Error("502", "SOAP Call Error");
        givenSoapResult(request, new CreditCheckResult(new CreditCheckResponse(), List.of(error)));

        checkCreditToday();

        captureSavedRecords(1);
        assertThat(invokedMethods(creditCheckCacheService)).containsExactly("find");
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
        assertThat(savedErrors.getValue()).containsExactly(error);
    }

    @Test
    void checkCredit_whenSeveralCustomers_shouldSaveEachWithItsOwnErrorsOnly() {
        Customer failing = person();
        Customer passing = person();
        Customer company = companyPerson();
        loan.setCustomers(List.of(failing, passing, company));
        CreditCheckRequest failingRequest = givenRequestFor(failing);
        CreditCheckRequest passingRequest = givenRequestFor(passing);
        Error error = new Error("502", "SOAP Call Error");

        givenSoapResult(failingRequest, new CreditCheckResult(new CreditCheckResponse(), List.of(error)));
        givenSoapResult(passingRequest, new CreditCheckResult(new CreditCheckResponse(), List.of()));

        checkCreditToday();

        captureSavedRecords(2);
        assertThat(invokedMethods(soapClient)).hasSize(2);
        assertThat(savedErrors.getAllValues()).containsExactly(List.of(error), List.of());
    }
}
