// REFERINTA - test existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
// Folderul src/test/java/com/bnp/pf/transformation/rvdrproxy/: builder/ApplicationBuilder, client/SoapClientTest,
// config/MappingConfigTest, controller/{ProxyListenerTest, RvdrControllerTest},
// service/{CreditCheckCacheServiceTest, CreditCheckRecordServiceTest, CreditCheckServiceTest, IntegrityCheckServiceTest}.
package com.bnp.pf.transformation.rvdrproxy.service;

@ExtendWith(MockitoExtension.class)
class CreditCheckRecordServiceTest {
    private static final String RESPONSE_JSON = "{\"response\":\"ok\"}";
    private static final String FLOW_ID = "FLOW-1";
    private static final String BUSINESS_KEY = "BK-001";
    private static final LocalDate CHECK_DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private CreditCheckRepository creditCheckRepository;
    @Mock
    private ModelMapper modelMapper;
    @Spy
    private Gson gson = new Gson();

    @InjectMocks
    private CreditCheckRecordService creditCheckRecordService;

    private ProcessingContext processingContext;
    private CreditCheckRequest creditCheckRequest;

    @BeforeEach
    void setUp() {
        processingContext = new ProcessingContext();
        processingContext.setBusinessKey(BUSINESS_KEY);
        processingContext.setFlowId(FLOW_ID);
        creditCheckRequest = new CreditCheckRequest();
    }

    @Test
    void save_shouldPersistEntityBuiltWithTheGivenErrors() {
        List<Error> errors = List.of(new Error("502", "SOAP Call Error"));
        GeneralProxyObsDto dto = new GeneralProxyObsDto();
        CreditCheckEntity entity = new CreditCheckEntity();
        when(modelMapper.map(dto, CreditCheckEntity.class)).thenReturn(entity);

        ArgumentCaptor<ErrorHandler> errorHandler = ArgumentCaptor.forClass(ErrorHandler.class);
        try (MockedStatic<BuildDTOsUtility> utility = mockStatic(BuildDTOsUtility.class)) {
            utility.when(() -> BuildDTOsUtility.buildCreditCheckDto(eq(BUSINESS_KEY), eq(FLOW_ID),
                    same(creditCheckRequest), eq(RESPONSE_JSON), any(ErrorHandler.class),
                    same(gson), eq(CHECK_DATE))).thenReturn(dto);

            creditCheckRecordService.save(processingContext, creditCheckRequest, RESPONSE_JSON, errors, CHECK_DATE);

            utility.verify(() -> BuildDTOsUtility.buildCreditCheckDto(any(), any(), any(), any(),
                    errorHandler.capture(), any(), any()));
        }

        verify(creditCheckRepository).save(same(entity));
        assertThat(errorHandler.getValue().getErrors()).containsExactlyElementsOf(errors);
    }

    @Test
    void save_whenDtoCannotBeBuilt_shouldPersistEntityFromEmptyDto() {
        CreditCheckEntity entity = new CreditCheckEntity();
        when(modelMapper.map(any(GeneralProxyObsDto.class), eq(CreditCheckEntity.class))).thenReturn(entity);

        try (MockedStatic<BuildDTOsUtility> utility = mockStatic(BuildDTOsUtility.class)) {
            utility.when(() -> BuildDTOsUtility.buildCreditCheckDto(any(), any(), any(), any(), any(),
                    any(), any())).thenReturn(null);

            creditCheckRecordService.save(processingContext, creditCheckRequest, RESPONSE_JSON, List.of(), CHECK_DATE);
        }

        ArgumentCaptor<GeneralProxyObsDto> mappedDto = ArgumentCaptor.forClass(GeneralProxyObsDto.class);
        verify(modelMapper).map(mappedDto.capture(), eq(CreditCheckEntity.class));
        verify(creditCheckRepository).save(same(entity));
        assertThat(mappedDto.getValue()).usingRecursiveComparison().isEqualTo(new GeneralProxyObsDto());
    }

    @Test
    void fetchCheckCredit_shouldReturnDto_whenEntityFound() {
        CreditCheckEntity creditCheckEntity = new CreditCheckEntity();
        GeneralProxyObsDto creditCheckDto = new GeneralProxyObsDto();

        when(creditCheckRepository.findTopByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(Optional.of(creditCheckEntity));
        when(modelMapper.map(creditCheckEntity, GeneralProxyObsDto.class)).thenReturn(creditCheckDto);

        GeneralProxyObsDto result = creditCheckRecordService.fetchCheckCredit(BUSINESS_KEY, FLOW_ID);

        assertNotNull(result);
        assertThat(result).isSameAs(creditCheckDto);
    }

    @Test
    void fetchCheckCredit_shouldReturnEmptyDto_whenEntityNotFound() {
        when(creditCheckRepository.findTopByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(Optional.empty());

        GeneralProxyObsDto result = creditCheckRecordService.fetchCheckCredit(BUSINESS_KEY, FLOW_ID);

        assertNotNull(result);
        assertThat(mockingDetails(modelMapper).getInvocations()).isEmpty();
        assertThat(result).usingRecursiveComparison().isEqualTo(new GeneralProxyObsDto());
    }
}
