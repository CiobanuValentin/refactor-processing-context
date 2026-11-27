// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@Service
@RequiredArgsConstructor
public class CreditCheckCacheService {
    private final CachingService cachingService;
    private final Gson gson;

    public Optional<GeneralProxyObsCacheDto> find(CreditCheckRequest creditCheckRequest, String producer) {
        String requestJson = gson.toJson(creditCheckRequest);
        Optional<GeneralProxyObsCacheDto> hit = cachingService.searchInCache(requestJson.hashCode())
                .filter(cached -> requestJson.equals(cached.getRequestText()));

        hit.ifPresent(cached ->
                cachingService.updateModifiedDateAndModifiedBy(producer, cached.getRequestHash()));

        return hit;
    }

    public void save(CreditCheckRequest request, String responseJson, String producer) {
        String requestJson = gson.toJson(request);
        cachingService.saveProxyCache(producer, requestJson.hashCode(), requestJson, responseJson);
    }
}
