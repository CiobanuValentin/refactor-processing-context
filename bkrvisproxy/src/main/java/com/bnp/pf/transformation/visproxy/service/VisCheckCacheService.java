package com.bnp.pf.transformation.visproxy.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

// TODO: importul pentru CachingService este presupus - pastreaza-l pe cel actual.
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.CachingService;

import urn.bkr.wsdl.vis.VisRequest;

// Neschimbat.
@Service
@RequiredArgsConstructor
public class VisCheckCacheService {
    private final CachingService cachingService;
    private final Gson gson;

    public Optional<GeneralProxyObsCacheDto> find(VisRequest visRequest, String producer) {
        String requestJson = gson.toJson(visRequest);
        Optional<GeneralProxyObsCacheDto> hit = cachingService.searchInCache(requestJson.hashCode())
                .filter(cached -> requestJson.equals(cached.getRequestText()));

        hit.ifPresent(cached -> cachingService.updateModifiedDateAndModifiedBy(producer, cached.getRequestHash()));

        return hit;
    }

    public void save(VisRequest visRequest, String responseJson, String producer) {
        String requestJson = gson.toJson(visRequest);
        cachingService.saveProxyCache(producer, requestJson.hashCode(), requestJson, responseJson);
    }
}
