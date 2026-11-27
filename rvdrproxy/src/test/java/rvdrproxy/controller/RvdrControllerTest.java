package rvdrproxy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.bnp.pf.transformation.rvdrproxy.controller.RvdrController;
import com.bnp.pf.transformation.rvdrproxy.service.CreditCheckRecordService;

@ExtendWith(MockitoExtension.class)
class RvdrControllerTest {
    private static final String BUSINESS_KEY = "BK-123";
    private static final String FLOW_ID = "FL-456";

    @Mock
    private CreditCheckRecordService creditCheckRecordService;
    @InjectMocks
    private RvdrController rvdrController;

    @Test
    void getCreditChecks_shouldReturnAllRows() {
        List<GeneralProxyObsDto> rows = List.of(new GeneralProxyObsDto(), new GeneralProxyObsDto());
        when(creditCheckRecordService.fetchCheckCredits(BUSINESS_KEY, FLOW_ID)).thenReturn(rows);

        assertEquals(rows, rvdrController.getCreditChecks(BUSINESS_KEY, FLOW_ID));
        verify(creditCheckRecordService, times(1)).fetchCheckCredits(BUSINESS_KEY, FLOW_ID);
    }

    @Test
    void getCreditChecks_shouldReturnEmptyList_whenNothingSaved() {
        when(creditCheckRecordService.fetchCheckCredits(BUSINESS_KEY, FLOW_ID)).thenReturn(List.of());

        assertEquals(List.of(), rvdrController.getCreditChecks(BUSINESS_KEY, FLOW_ID));
    }
}
