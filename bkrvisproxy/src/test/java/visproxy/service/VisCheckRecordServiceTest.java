package visproxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.google.gson.Gson;

import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.visproxy.entity.CheckVisEntity;
import com.bnp.pf.transformation.visproxy.repository.CheckVisEntityRepository;
import com.bnp.pf.transformation.visproxy.service.VisCheckRecordService;
import com.bnp.pf.transformation.visproxy.utils.BuildDTOsUtility;

import urn.bkr.wsdl.vis.VisRequest;
import visproxy.builder.ContextBuilder;

/** Aceleasi teste; doar ProcessingContext devine ProxyContext (business key din aplicatie, flowId din mesaj). */
@ExtendWith(MockitoExtension.class)
class VisCheckRecordServiceTest {
    private static final String RESPONSE_JSON = "{\"response\":\"ok\"}";
    private static final String FLOW_ID = ContextBuilder.FLOW_ID;
    private static final String BUSINESS_KEY = "BK-001";
    private static final LocalDate CHECK_DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private CheckVisEntityRepository checkVisEntityRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private BuildDTOsUtility buildDTOsUtility;
    @Spy
    private Gson gson = new Gson();

    @InjectMocks
    private VisCheckRecordService visCheckRecordService;

    private ProxyContext processingContext;
    private VisRequest visCheckRequest;

    @BeforeEach
    void setUp() {
        processingContext = ContextBuilder.context(ContextBuilder.application(BUSINESS_KEY));
        visCheckRequest = new VisRequest();
    }

    @Test
    void save_shouldPersistEntityBuiltWithTheGivenErrors() {
        List<Error> errors = List.of(new Error("502", "SOAP Call Error"));
        GeneralProxyObsDto dto = new GeneralProxyObsDto();
        CheckVisEntity entity = new CheckVisEntity();
        when(buildDTOsUtility.createCheckVisDto(any(), any(), any(), any(), any(), any(), any())).thenReturn(dto);
        when(modelMapper.map(dto, CheckVisEntity.class)).thenReturn(entity);

        visCheckRecordService.save(processingContext, visCheckRequest, RESPONSE_JSON, errors, CHECK_DATE);

        ArgumentCaptor<ErrorHandler> errorHandler = ArgumentCaptor.forClass(ErrorHandler.class);
        verify(buildDTOsUtility).createCheckVisDto(eq(BUSINESS_KEY), eq(FLOW_ID), same(visCheckRequest),
                eq(RESPONSE_JSON), errorHandler.capture(), eq(CHECK_DATE), same(gson));
        verify(checkVisEntityRepository).save(same(entity));

        assertThat(errorHandler.getValue().getErrors()).containsExactlyElementsOf(errors);
    }

    @Test
    void save_whenDtoCannotBeBuilt_shouldPersistEntityFromEmptyDto() {
        CheckVisEntity entity = new CheckVisEntity();
        when(buildDTOsUtility.createCheckVisDto(any(), any(), any(), any(), any(), any(), any())).thenReturn(null);
        when(modelMapper.map(any(GeneralProxyObsDto.class), eq(CheckVisEntity.class))).thenReturn(entity);

        visCheckRecordService.save(processingContext, visCheckRequest, RESPONSE_JSON, List.of(), CHECK_DATE);

        ArgumentCaptor<GeneralProxyObsDto> mappedDto = ArgumentCaptor.forClass(GeneralProxyObsDto.class);
        verify(modelMapper).map(mappedDto.capture(), eq(CheckVisEntity.class));
        verify(checkVisEntityRepository).save(entity);

        assertThat(mappedDto.getValue()).usingRecursiveComparison().isEqualTo(new GeneralProxyObsDto());
    }

    @Test
    void fetchCheckVis_shouldReturnDto_whenEntityFound() {
        CheckVisEntity latest = new CheckVisEntity();
        CheckVisEntity older = new CheckVisEntity();
        GeneralProxyObsDto latestDto = new GeneralProxyObsDto();
        GeneralProxyObsDto olderDto = new GeneralProxyObsDto();
        when(checkVisEntityRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(List.of(latest, older));
        when(modelMapper.map(same(latest), eq(GeneralProxyObsDto.class))).thenReturn(latestDto);
        when(modelMapper.map(same(older), eq(GeneralProxyObsDto.class))).thenReturn(olderDto);

        List<GeneralProxyObsDto> result = visCheckRecordService.fetchVisEntity(BUSINESS_KEY, FLOW_ID);

        assertThat(result)
                .usingElementComparator((a, b) -> a == b ? 0 : 1)
                .containsExactly(latestDto, olderDto);
    }

    @Test
    void fetchCheckVis_shouldReturnEmptyDto_whenEntityNotFound() {
        when(checkVisEntityRepository.findAllByBusinessKeyAndFlowIdOrderByCreatedDateDesc(BUSINESS_KEY, FLOW_ID))
                .thenReturn(List.of());

        List<GeneralProxyObsDto> result = visCheckRecordService.fetchVisEntity(BUSINESS_KEY, FLOW_ID);

        assertThat(result).isNotNull();
        assertThat(mockingDetails(modelMapper).getInvocations()).isEmpty();
        assertThat(result).isEmpty();
    }
}
