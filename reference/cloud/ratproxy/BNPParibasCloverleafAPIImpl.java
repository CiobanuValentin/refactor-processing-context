// REFERINTA - cod existent (cloud, ratproxy, branch arval), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.datasource;

@Component
@Slf4j
public class BNPParibasCloverleafAPIImpl implements BNPParibasCloverLeafAPI {

    private static final String CALLING_EXTERNAL_SERVICE = "calling external service...";
    private static final String REQUEST = "external service will be called using the request: {}";
    private static final String RESPONSE = "response received from external service is: {}";

    private final RestClient restClient;
    private final PartialDataFetcher fetcher;
    private final ObjectMapper objectMapper;

    public BNPParibasCloverleafAPIImpl(@Qualifier("createRestClientWithOauth2") RestClient restClient, PartialDataFetcher fetcher, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
    }

    /**
     * Get risk assessments for application
     */
    @Override
    public Risk getScoreWithHttpMessages(String applicationId, CollectError collectError, AtomicInteger index,
                                         Signal errorMessage, ProcessingContext processingContext) {
        applicationId = applicationId.trim();
        log.info(CALLING_EXTERNAL_SERVICE);

        return restClient.get()
                .uri("/score/{applicationId}", applicationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange((request, response) -> {
                    fetcher.handleException(request, response, collectError, index, errorMessage, processingContext);
                    String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.info(RESPONSE, responseBody.replaceAll("\\R", " "));
                    return objectMapper.readValue(responseBody, Risk.class);
                });
    }

    /**
     * Add or update an application request for the sanction phase
     */
    @Override
    public Boolean putSanctionWithHttpMessages(String applicationId, Loan loan, CollectError collectError,
                                               AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        final String loanJson = toJson(loan, collectError, index, errorMessage, processingContext);

        log.info("Process PUT Sanction : {}", loanJson);
        log.info(CALLING_EXTERNAL_SERVICE);
        log.info(REQUEST, loanJson.replaceAll("\\R", " "));

        return restClient.put()
                .uri("/sanction/{applicationId}", applicationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(loanJson)
                .exchange((request, response) -> {
                    fetcher.handleException(request, response, collectError, index, errorMessage, processingContext);
                    return true;
                });
    }

    /**
     * Add or update an application request for the compliance phase for companies
     */
    // putComplianceCompanyWithHttpMessages: la fel, cu applicationId.trim() la inceput si "/compliance/company/{applicationId}"

    /**
     * Add or update an application request for the compliance phase
     */
    // putComplianceWithHttpMessages: la fel, cu loan.validate(collectError, index, errorMessage, processingContext)
    // la inceput, logul "Process PUT Compliance : {}" si "/compliance/{applicationId}"

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

    private String toJson(Loan loan, CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) {
        try {
            return objectMapper.writeValueAsString(loan);
        } catch (JsonProcessingException e) {
            String message = "Exception while converting Loan object to json : " + e.getMessage();
            log.error(message);
            collectError.addError(processingContext, e);
            processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, errorMessage);
        }
        return StringUtils.EMPTY;
    }
}
