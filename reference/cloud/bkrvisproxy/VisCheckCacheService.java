// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.service;

@Service
@RequiredArgsConstructor
public class VisCheckCacheService {
    private final CachingService cachingService;
    private final Gson gson;

    public Optional<GeneralProxyObsCacheDto> find(VisRequest visRequest, String producer) {
        String requestJson = gson.toJson(visRequest);
        Optional<GeneralProxyObsCacheDto> hit = cachingService.searchInCache(requestJson.hashCode())
                .filter(cached ->
                        requestJson.equals(cached.getRequestText()));

        hit.ifPresent(cached ->
                cachingService.updateModifiedDateAndModifiedBy(producer, cached.getRequestHash()));

        return hit;
    }

    public void save(VisRequest visRequest, String responseJson, String producer) {
        String requestJson = gson.toJson(visRequest);
        cachingService.saveProxyCache(producer, requestJson.hashCode(), requestJson, responseJson);
    }
}
