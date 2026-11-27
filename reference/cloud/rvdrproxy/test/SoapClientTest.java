// REFERINTA - test existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
// Folderul src/test/java/com/bnp/pf/transformation/rvdrproxy/: builder/ApplicationBuilder, client/SoapClientTest,
// config/MappingConfigTest, controller/{ProxyListenerTest, RvdrControllerTest},
// service/{CreditCheckCacheServiceTest, CreditCheckRecordServiceTest, CreditCheckServiceTest, IntegrityCheckServiceTest}.
package com.bnp.pf.transformation.rvdrproxy.client;

@ExtendWith(MockitoExtension.class)
class SoapClientTest {
    @Mock
    private CreditCheckPort creditCheckPort;
    @Mock
    private CollectError collectError;

    @InjectMocks
    private SoapClient soapClient;

    private ProcessingContext processingContext;
    private CreditCheckRequest request;
    private int initialIndex;

    @BeforeEach
    void setUp() {
        processingContext = new ProcessingContext();
        request = new CreditCheckRequest();
        initialIndex = processingContext.getIndex().get();
    }

    private static ErrorType errorType(String code, String message) {
        ErrorType errorType = new ErrorType();
        errorType.setCode(code);
        errorType.setMessage(message);

        return errorType;
    }

    private static List<String> invokedMethods(Object... mocks) {
        return Arrays.stream(mocks)
                .flatMap(mock -> mockingDetails(mock).getInvocations().stream())
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

    private String registeredErrorMessage(String expectedCode) {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(collectError).addError(same(processingContext), eq(expectedCode), message.capture());
        verify(collectError).addErrorSignal(same(processingContext), any());

        return message.getValue();
    }

    @Test
    void creditCheckRequest_WhenResponseHasNoErrors_shouldReturnItAndRegisterNothing() throws ConnectException_Exception {
        CreditCheckResponse response = new CreditCheckResponse();
        when(creditCheckPort.creditCheck(request)).thenReturn(response);

        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, request);

        assertThat(invokedMethods(collectError)).isEmpty();
        assertThat(processingContext.getErrorMap()).isEmpty();
        assertThat(result.getResponse()).isSameAs(response);
    }

    @Test
    void creditCheckRequest_WhenResponseHasBusinessErrors_shouldRegisterEachErrorAndOneSignal() throws ConnectException_Exception {
        ErrorsType errors = new ErrorsType();
        errors.getError().add(errorType("E1", "Invalid client"));
        errors.getError().add(errorType("E2", "Limit exceeded"));

        CreditCheckResponse response = new CreditCheckResponse();
        response.setErrors(errors);
        when(creditCheckPort.creditCheck(request)).thenReturn(response);

        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, request);

        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(collectError, times(2)).addError(same(processingContext), codes.capture(), messages.capture());
        verify(collectError).addErrorSignal(same(processingContext), any());

        assertThat(result.getResponse()).isSameAs(response);
        assertThat(codes.getAllValues()).containsExactly("E1", "E2");
        assertThat(messages.getAllValues()).containsExactly("Invalid client", "Limit exceeded");
        assertThat(processingContext.getErrorMap()).containsOnly(
                entry(ERROR_MESSAGE + initialIndex, "CreditCheck returned an error: E1:Invalid client"),
                entry(ERROR_MESSAGE + (initialIndex + 1), "CreditCheck returned an error: E2:Limit exceeded"));
    }

    @Test
    void creditCheckResuest_whenSoapFault_shouldReturn502AndRegisterError() throws ConnectException_Exception {
        SOAPFault fault = mock(SOAPFault.class);
        when(fault.getFaultString()).thenReturn("Service unavailable");
        SOAPFaultException soapFaultException = new SOAPFaultException(fault);
        when(creditCheckPort.creditCheck(request)).thenThrow(soapFaultException);

        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, request);

        String message = registeredErrorMessage("502");
        assertThat(message).contains("Service unavailable");
        assertThat(processingContext.getErrorMap()).containsOnlyKeys(ERROR_MESSAGE + initialIndex);
        assertThat(result.getResponse().getErrors().getError())
                .extracting(ErrorType::getCode, ErrorType::getMessage)
                .containsExactly(tuple("502", message));
        assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Error("502", message));
    }

    @Test
    void creditCheckRequest_whenUnexpectedException_shouldReturn555AndRegisterError() throws ConnectException_Exception {
        when(creditCheckPort.creditCheck(request)).thenThrow(new RuntimeException("Read timed out"));
        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, request);

        String message = registeredErrorMessage("555");
        assertThat(message).contains("Read timed out");
        assertThat(processingContext.getErrorMap()).containsOnlyKeys(ERROR_MESSAGE + initialIndex);
        /* ...aceleasi verificari pe result ca mai sus, cu 555 */
    }

    @Test
    void creditCheckRequest_whenResponseIsNull_shouldReturn555AndRegisterError() throws ConnectException_Exception {
        when(creditCheckPort.creditCheck(request)).thenReturn(null);
        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, request);

        String message = registeredErrorMessage("555");
        assertThat(message).isEqualTo("Empty response from CreditCheck");
        assertThat(processingContext.getErrorMap()).containsOnlyKeys(ERROR_MESSAGE + initialIndex);
        /* ...aceleasi verificari pe result ca mai sus, cu 555 */
    }
}
