package visproxy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.visproxy.controller.BkrVisController;
import com.bnp.pf.transformation.visproxy.service.VisCheckRecordService;

// Neschimbat.
@ExtendWith(MockitoExtension.class)
class BkrVisControllerTest {
    @Mock
    private VisCheckRecordService visCheckRecordService;
    @InjectMocks
    private BkrVisController bkrVisController;

    @Test
    void getVis_shouldReturnDto_whenCalledWithValidParams() {
        String businessKey = "BK-123";
        String flowId = "FL-456";
        List<GeneralProxyObsDto> checkVisDto = List.of(new GeneralProxyObsDto());

        when(visCheckRecordService.fetchVisEntity(businessKey, flowId)).thenReturn(checkVisDto);

        List<GeneralProxyObsDto> result = bkrVisController.getVisEntities(businessKey, flowId);

        assertNotNull(result);
        assertEquals(checkVisDto, result);
        verify(visCheckRecordService, times(1)).fetchVisEntity(businessKey, flowId);
    }

    @Test
    void getVis_shouldReturnNull_whenServiceReturnsNull() {
        when(visCheckRecordService.fetchVisEntity(any(), any())).thenReturn(null);

        List<GeneralProxyObsDto> result = bkrVisController.getVisEntities(any(), any());
        assertNull(result);
        verify(visCheckRecordService, times(1)).fetchVisEntity(any(), any());
    }
}
