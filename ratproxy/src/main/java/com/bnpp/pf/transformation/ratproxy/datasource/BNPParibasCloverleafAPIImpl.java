package com.bnpp.pf.transformation.ratproxy.datasource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.exception.RestCallException;
import com.bnpp.pf.transformation.ratproxy.datasource.model.Loan;
import com.bnpp.pf.transformation.ratproxy.datasource.model.Risk;

@Component
@Slf4j
public class BNPParibasCloverleafAPIImpl implements BNPParibasCloverLeafAPI {

    private static final String CALLING_EXTERNAL_SERVICE = "calling external service...";
    private static final String REQUEST = "external service will be called using the request: {}";
    private static final String RESPONSE = "response received from external service is: {}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BNPParibasCloverleafAPIImpl(@Qualifier("createRestClientWithOauth2") RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Get risk assessments for application
     */
    @Override
    public Risk getScoreWithHttpMessages(String applicationId) {
        String id = applicationId.trim();
        log.info(CALLING_EXTERNAL_SERVICE);

        return restClient.get()
                .uri("/score/{applicationId}", id)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange((request, response) -> {
                    failOnErrorStatus(request, response);
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.debug(RESPONSE, responseBody.replaceAll("\\R", " "));
                    return objectMapper.readValue(responseBody, Risk.class);
                });
    }

    /**
     * Add or update an application request for the sanction phase
     */
    @Override
    public Boolean putSanctionWithHttpMessages(String applicationId, Loan loan) {
        return put("/sanction/{applicationId}", applicationId, loan);
    }

    /**
     * Add or update an application request for the compliance phase for companies
     */
    @Override
    public Boolean putComplianceCompanyWithHttpMessages(String applicationId, Loan loan) {
        return put("/compliance/company/{applicationId}", applicationId.trim(), loan);
    }

    /**
     * Add or update an application request for the compliance phase
     */
    @Override
    public Boolean putComplianceWithHttpMessages(String applicationId, Loan loan) {
        // TODO: Loan.validate() nu mai primeste nimic si doar arunca - vezi snippets/ratproxy/LoanMapping-changes.java
        loan.validate();
        return put("/compliance/{applicationId}", applicationId, loan);
    }

    /**
     * test rest client connection
     */
    @Override
    public String ping() {
        return restClient.get()
                .uri("/ping")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange((request, response) -> LocalDateTime.now().toString());
    }

    private Boolean put(String path, String applicationId, Loan loan) {
        final String loanJson = toJson(loan);
        log.info(CALLING_EXTERNAL_SERVICE);
        // Cererea contine datele clientilor, deci se logheaza doar la debug (inainte: la info).
        log.debug(REQUEST, loanJson.replaceAll("\\R", " "));

        return restClient.put()
                .uri(path, applicationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(loanJson)
                .exchange((request, response) -> {
                    failOnErrorStatus(request, response);
                    return true;
                });
    }

    /** Aceeasi verificare si acelasi mesaj ca in PartialDataFetcherPG.handleException, fara scrierea in context. */
    private void failOnErrorStatus(HttpRequest request,
                                   RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            String message = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8) + ". ErrorCode is: "
                    + response.getStatusCode() + ". External call url is: " + request.getURI();
            log.error(message);
            throw new RestCallException(HttpStatus.BAD_REQUEST, message, new Throwable(message));
        }
    }

    private String toJson(Loan loan) {
        try {
            return objectMapper.writeValueAsString(loan);
        } catch (JsonProcessingException e) {
            // FIX fata de codul actual: inainte se inregistra eroarea si se trimitea totusi cererea, cu body gol.
            throw new IllegalStateException("Exception while converting Loan object to json : " + e.getMessage(), e);
        }
    }
}
