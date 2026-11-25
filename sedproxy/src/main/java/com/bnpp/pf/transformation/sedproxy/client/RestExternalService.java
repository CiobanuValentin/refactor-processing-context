package com.bnpp.pf.transformation.sedproxy.client;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.ARVAL_NL_FLOW_ID_PREFIX;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bnpp.pf.transformation.sedproxy.model.ExpertiseData;
import com.bnpp.pf.transformation.sedproxy.model.ExpertiseReturn;

@Slf4j
@Service
public class RestExternalService {

    private final RestClient restClient;
    private final RestClient arvalRestClient;
    private final ObjectMapper sedObjectMapper;

    @Value("${sed-backend.path}")
    private String uri;

    public RestExternalService(@Qualifier("createRestClientWithOauth2") RestClient restClient,
                               @Qualifier("createArvalRestClientWithOauth2") RestClient arvalRestClient,
                               @Qualifier("sedObjectMapper") ObjectMapper sedObjectMapper) {
        this.restClient = restClient;
        this.arvalRestClient = arvalRestClient;
        this.sedObjectMapper = sedObjectMapper;
    }

    /**
     * @return raspunsul DES; la 4xx un ExpertiseReturn marcat ca eroare, ca pana acum
     * @throws RestClientException daca cererea nu poate fi serializata, la orice alt status non-2xx sau la eroare de retea
     */
    public ExpertiseReturn expertise(ExpertiseData expertiseData, String flowId) {
        String requestBody = toJson(expertiseData);
        boolean arval = flowId != null && flowId.startsWith(ARVAL_NL_FLOW_ID_PREFIX);
        log.info("Calling SED{} for flow id {}", arval ? " (Arval)" : "", flowId);
        log.debug("SED request: {}", requestBody);

        return (arval ? arvalRestClient : restClient).post()
                .uri(uri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .exchange((request, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    int status = response.getStatusCode().value();
                    log.info("SED response status for flow id {}: {}", flowId, status);
                    log.debug("SED response: {}", responseBody);

                    if (response.getStatusCode().is4xxClientError()) {
                        return ExpertiseReturn.builder().error(true)
                                .errorMessage("SED replied with HTTP " + status).build();
                    }
                    // FIX fata de codul actual: un 5xx nu mai e parsat ca raspuns valid.
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new RestClientException("SED replied with HTTP " + status);
                    }
                    return sedObjectMapper.readValue(responseBody, ExpertiseReturn.class);
                });
    }

    /** Raspunde cu ora curenta doar daca SED a raspuns cu succes. */
    public String ping() {
        return restClient.get()
                .uri("/ping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new RestClientException("SED ping replied with HTTP " + response.getStatusCode().value());
                    }
                    return LocalDateTime.now().toString();
                });
    }

    private String toJson(ExpertiseData expertiseData) {
        try {
            return sedObjectMapper.writeValueAsString(expertiseData);
        } catch (JsonProcessingException e) {
            // FIX fata de codul actual: nu se mai trimite un request gol la DES.
            throw new RestClientException("Could not serialize SED request: " + e.getMessage(), e);
        }
    }
}
