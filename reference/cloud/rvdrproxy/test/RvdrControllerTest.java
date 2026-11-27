// REFERINTA - test existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
// Folderul src/test/java/com/bnp/pf/transformation/rvdrproxy/: builder/ApplicationBuilder, client/SoapClientTest,
// config/MappingConfigTest, controller/{ProxyListenerTest, RvdrControllerTest},
// service/{CreditCheckCacheServiceTest, CreditCheckRecordServiceTest, CreditCheckServiceTest, IntegrityCheckServiceTest}.
package com.bnp.pf.transformation.rvdrproxy.controller;

@ExtendWith(MockitoExtension.class)
class RvdrControllerTest {
    @Mock
    private CreditCheckRecordService creditCheckRecordService;
    @InjectMocks
    private RvdrController rvdrController;

    @Test
    void getCreditCheck_shouldReturnDto_whenCalledWithValidParams() {
        String businessKey = "BK-123";
        String flowId = "FL-456";
        GeneralProxyObsDto creditCheckDto = new GeneralProxyObsDto();

        when(creditCheckRecordService.fetchCheckCredit(businessKey, flowId)).thenReturn(creditCheckDto);

        GeneralProxyObsDto result = rvdrController.getCreditCheck(businessKey, flowId);

        assertNotNull(result);
        assertEquals(creditCheckDto, result);
        verify(creditCheckRecordService, times(1)).fetchCheckCredit(businessKey, flowId);
    }

    @Test
    void getCreditCheck_shouldReturnNull_whenServiceReturnsNull() {
        when(creditCheckRecordService.fetchCheckCredit(any(), any())).thenReturn(null);

        GeneralProxyObsDto result = rvdrController.getCreditCheck(any(), any());

        assertNull(result);
        verify(creditCheckRecordService, times(1)).fetchCheckCredit(any(), any());
    }
}
