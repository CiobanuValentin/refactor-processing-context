package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;

/**
 * Citeste rezultatele unui alt proxy pentru aplicatia din context. Inlocuieste cele 10 metode din PartialDataFetcherPG.
 * Nu arunca: orice esec devine o eroare in context si o lista goala, iar apelantul raspunde KO.
 */
@Slf4j
@Component
public class ProxyResultsFetcher {

    private static final String SERVICE_UNAVAILABLE = "503";
    private static final int MAX_BODY_IN_MESSAGE = 300;

    private final RestClient restClient;
    private final ProxySourceProperties properties;
    private final Gson gson = GsonInstance.createGson();

    // TODO: pune @Qualifier-ul RestClient-ului folosit acum de PartialDataFetcherPG
    public ProxyResultsFetcher(RestClient restClient, ProxySourceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<ProxyResultDto> fetch(ProxyContext ctx, ProxySource source) {
        String url = properties.getUrls().get(source);
        if (StringUtils.isBlank(url)) {
            return fail(ctx, source, "no URL configured");
        }
        try {
            return restClient.get()
                    .uri(url + "?businessKey={businessKey}&flowId={flowId}", ctx.getBusinessKey(), ctx.getFlowId())
                    .exchange((request, response) -> read(ctx, source, response.getStatusCode().value(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (RestClientException e) {
            return fail(ctx, source, e.getMessage());
        }
    }

    /** Statusul se verifica inaintea continutului; body-ul a fost deja citit o singura data. */
    List<ProxyResultDto> read(ProxyContext ctx, ProxySource source, int status, String body) {
        if (status < 200 || status >= 300) {
            String detail = StringUtils.isBlank(body) ? "" : ": " + StringUtils.abbreviate(body, MAX_BODY_IN_MESSAGE);
            return fail(ctx, source, "HTTP " + status + detail);
        }
        if (StringUtils.isBlank(body)) {
            return List.of();
        }
        try {
            ProxyResultsDto dto = gson.fromJson(body, ProxyResultsDto.class);
            if (dto == null || dto.getResults() == null) {
                return List.of();
            }
            List<ProxyResultDto> results = new ArrayList<>(dto.getResults());
            results.removeIf(Objects::isNull);
            log.info("{} returned {} result(s) for flow id {}", source, results.size(), ctx.getFlowId());
            return results;
        } catch (JsonSyntaxException e) {
            return fail(ctx, source, "invalid response: " + e.getMessage());
        }
    }

    private List<ProxyResultDto> fail(ProxyContext ctx, ProxySource source, String reason) {
        String message = "Unable to get " + source + " results for business key " + ctx.getBusinessKey()
                + " and flow ID " + ctx.getFlowId() + ": " + reason;
        log.error(message);
        ctx.addError(SERVICE_UNAVAILABLE, message);
        return List.of();
    }
}
