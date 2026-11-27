package com.bnp.pf.transformation.rvdrproxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.google.gson.Gson;

// TODO: importurile pentru Error, ErrorHandler si CreditCheckRequest (WSDL) - pastreaza-le pe cele din testul actual.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.rvdrproxy.builder.ContextBuilder;
import com.bnp.pf.transformation.rvdrproxy.entity.CreditCheckEntity;
import com.bnp.pf.transformation.rvdrproxy.repository.CreditCheckRepository;
import com.bnp.pf.transformation.rvdrproxy.utils.BuildDTOsUtility;

/**
 * Testele de save sunt cele existente, cu ProxyContext in loc de ProcessingContext (business key din aplicatie,
 * flowId din mesaj). Testele de fetch sunt rescrise pentru lista.
 */
@ExtendWith(MockitoExtension.class)
class CreditCheckRecordServiceTest {
    private static final String RESPONSE_JSON = "{\"response\":\"ok\"}";
    private static final String FLOW_ID = ContextBuilder.FLOW_ID;
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

    private ProxyContext ctx;
    private CreditCheckRequest creditCheckRequest;

    @BeforeEach
    void setUp() {
        ctx = ContextBuilder.context(ContextBuilder.application(BUSINESS_KEY));
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

            creditCheckRecordService.save(ctx, creditCheckRequest, RESPONSE_JSON, errors, CHECK_DATE);

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

            creditCheckRecordService.save(ctx, creditCheckRequest, RESPONSE_JSON, List.of(), CHECK_DATE);
        }

        ArgumentCaptor<GeneralProxyObsDto> mappedDto = ArgumentCaptor.forClass(GeneralProxyObsDto.class);
        verify(modelMapper).map(mappedDto.capture(), eq(CreditCheckEntity.class));
        verify(creditCheckRepository).save(same(entity));
        assertThat(mappedDto.getValue()).usingRecursiveComparison().isEqualTo(new GeneralProxyObsDto());
    }

    @Test
    void fetchCheckCredits_shouldReturnOneDtoPerEntity_newestFirst() {
        CreditCheckEntity latest = new CreditCheckEntity();
        CreditCheckEntity older = new CreditCheckEntity();
        GeneralProxyObsDto latestDto = new GeneralProxyObsDto();
        GeneralProxyObsDto olderDto = new GeneralProxyObsDto();
        when(creditCheckRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(List.of(latest, older));
        when(modelMapper.map(same(latest), eq(GeneralProxyObsDto.class))).thenReturn(latestDto);
        when(modelMapper.map(same(older), eq(GeneralProxyObsDto.class))).thenReturn(olderDto);

        List<GeneralProxyObsDto> result = creditCheckRecordService.fetchCheckCredits(BUSINESS_KEY, FLOW_ID);

        assertThat(result)
                .usingElementComparator((a, b) -> a == b ? 0 : 1)
                .containsExactly(latestDto, olderDto);
    }

    @Test
    void fetchCheckCredits_shouldReturnEmptyList_whenNoEntityFound() {
        when(creditCheckRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(List.of());

        List<GeneralProxyObsDto> result = creditCheckRecordService.fetchCheckCredits(BUSINESS_KEY, FLOW_ID);

        assertThat(result).isEmpty();
        assertThat(mockingDetails(modelMapper).getInvocations()).isEmpty();
    }
}
