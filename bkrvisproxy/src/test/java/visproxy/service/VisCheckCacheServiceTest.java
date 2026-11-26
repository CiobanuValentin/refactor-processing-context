package visproxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.CachingService;
import com.bnp.pf.transformation.visproxy.service.VisCheckCacheService;

import urn.bkr.wsdl.vis.VisRequest;

// Neschimbat.
@ExtendWith(MockitoExtension.class)
class VisCheckCacheServiceTest {
    private static final String PRODUCER = "producer";
    private static final String RESPONSE_JSON = "{\"response\":\"ok\"}";

    @Mock
    private CachingService cachingService;
    @Mock
    private GeneralProxyObsCacheDto generalProxyObsCacheDto;

    private final Gson gson = new Gson();
    private VisCheckCacheService visCheckCacheService;
    private VisRequest visCheckRequest;
    private String requestJson;

    @BeforeEach
    void setUp() {
        visCheckCacheService = new VisCheckCacheService(cachingService, gson);
        visCheckRequest = new VisRequest();
        requestJson = gson.toJson(visCheckRequest);
    }

    private static List<String> invokedMethods(Object... mocks) {
        return Arrays.stream(mocks)
                .flatMap(mock -> mockingDetails(mock).getInvocations().stream())
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

    @Test
    void find_whenHashMatchesButRequestDiffers_shouldTreatItAsMiss() {
        when(cachingService.searchInCache(requestJson.hashCode())).thenReturn(Optional.of(generalProxyObsCacheDto));
        when(generalProxyObsCacheDto.getRequestText()).thenReturn("{\"customer\":\"another one\"}");

        Optional<GeneralProxyObsCacheDto> result = visCheckCacheService.find(visCheckRequest, PRODUCER);

        assertThat(invokedMethods(cachingService)).containsExactly("searchInCache");
        assertThat(result).isEmpty();
    }

    @Test
    void find_whenSameRequestIsCached_shouldReturnItAndRefreshIt() {
        when(cachingService.searchInCache(requestJson.hashCode())).thenReturn(Optional.of(generalProxyObsCacheDto));
        when(generalProxyObsCacheDto.getRequestText()).thenReturn(requestJson);

        Optional<GeneralProxyObsCacheDto> result = visCheckCacheService.find(visCheckRequest, PRODUCER);

        assertThat(invokedMethods(cachingService)).containsExactly("searchInCache", "updateModifiedDateAndModifiedBy");
        assertThat(result).containsSame(generalProxyObsCacheDto);
    }

    @Test
    void find_whenNothingCached_shouldReturnEmpty() {
        when(cachingService.searchInCache(requestJson.hashCode())).thenReturn(Optional.empty());
        Optional<GeneralProxyObsCacheDto> result = visCheckCacheService.find(visCheckRequest, PRODUCER);

        assertThat(invokedMethods(cachingService)).containsExactly("searchInCache");
        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldStoreRequestJsonUnderItsHash() {
        visCheckCacheService.save(visCheckRequest, RESPONSE_JSON, PRODUCER);

        ArgumentCaptor<String> requestText = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> responseText = ArgumentCaptor.forClass(String.class);

        verify(cachingService).saveProxyCache(eq(PRODUCER), eq(requestJson.hashCode()),
                requestText.capture(), responseText.capture());

        assertThat(requestText.getValue()).isEqualTo(requestJson);
        assertThat(responseText.getValue()).isEqualTo(RESPONSE_JSON);
    }
}
