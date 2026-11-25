// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

public class PartialDataFetcherPG implements PartialDataFetcher {
    // @Value("${...}") pe fiecare camp:
    //   dnb.root-url, dnb.check-endpoint, dnb.event-endpoint, bkrcki.root-url, bkrcki.endpoint,
    //   scal.root-url, scal.entity-endpoint, scal.person-endpoint, scal.personInitial-endpoint,
    //   rvdr.root-url, rvdr.checkCredit-endpoint, bkrvis.root-url, bkrvis.credit-endpoint,
    //   bkreva.root-url, bkreva.company-endpoint, bkreva.person-endpoint
    // Valori in application-local.yml (bkrckiproxy): /companyCheck, /companyEvent, /checkCki, /checkEntity,
    //   /checkPersonInitial, /checkPerson, /checkCredit, /getEvaCompany, /getEvaPerson, /getVisEntity;
    //   kafka.retries.max.attempts: 1, kafka.retries.interval: 1000
    private final CollectError collectError;
    private final ProxyDtoBuilder proxyDtoBuilder;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Override
    public CheckScalPersonDto getScalPersonData(String businessKey, String flowId, Signal errorMessage, AtomicInteger index, ProcessingContext processingContext) {

        GeneralProxyObsDto generalProxyObsDto = callProxyEndpoint(scalRootUrl, scalPersonEndpoint, businessKey, flowId, errorMessage, index, processingContext);

        log.info("GeneralProxyObsDto after calling ScalProxy /checkPerson endpoint is: {}", generalProxyObsDto);
        return proxyDtoBuilder.buildCheckScalPersonDto(generalProxyObsDto);
    }

    // Toate celelalte metode au aceeasi forma:
    // metoda                    | root url        | endpoint (camp -> cale din log)              | builder
    // getScalPersonData         | scalRootUrl     | scalPersonEndpoint -> /checkPerson            | buildCheckScalPersonDto
    // getScalPersonInitialData  | scalRootUrl     | scalPersonInitialEndpoint -> /checkPersonInitial | buildCheckScalPersonInitialDto
    // getScalEntityData         | scalRootUrl     | scalEntityEndpoint -> /checkEntity            | buildCheckScalEntityDto
    // getRVDRData               | rvdrRootUrl     | rvdrCheckCreditEndpoint -> /checkCredit       | buildCheckRvdrCreditDto
    // getDNBData                | dnbRootUrl      | dnbCompanyCheckEndpoint -> /companyCheck      | buildCheckDnbCompanyDto
    // getEventIndicatorData     | dnbRootUrl      | dnbCompanyEventEndpoint -> /companyEvent      | buildCheckEventIndicatorDto
    // getCkiData                | ckiRootUrl      | ckiEndpoint -> /checkCki                      | buildCheckCkiPersonDto
    // getVisData                | bkrVisRootUrl   | bkrVisEndpoint -> /getVisEntity               | buildCheckVisDto
    // getEvaPersonData          | bkrEvaRootUrl   | bkrEvaPersonEndpoint -> /checkEvaPerson       | buildCheckEvaPersonDto
    // getEvaCompanyData         | bkrEvaRootUrl   | bkrEvaCompanyEndpoint -> /checkEvaCompany     | buildCheckEvaCompanyDto

    public GeneralProxyObsDto callProxyEndpoint(String proxyRootUrl, String specificProxyEndpoint, String businessKey, String flowId,
                                                Signal errorMessage, AtomicInteger index, ProcessingContext processingContext) {

        String url = UriComponentsBuilder
                .fromUriString(proxyRootUrl + specificProxyEndpoint)
                .queryParam("businessKey", businessKey)
                .queryParam("flowId", flowId)
                .build()
                .toUriString();

        AtomicReference<GeneralProxyObsDto> generalProxyObsDto = new AtomicReference<>(new GeneralProxyObsDto());
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange((request, response) -> {
                    String responseBody = new String(response.getBody().readAllBytes());
                    if (StringUtils.isEmpty(responseBody) || responseBody.equals("{}")) {
                        log.info("response body after the call to {} endpoint is empty and for that reason we will return GeneralProxyObsDto with null fields", specificProxyEndpoint);
                    } else {
                        handleException(request, response, collectError, index, errorMessage, processingContext);
                        generalProxyObsDto.set(objectMapper.readValue(responseBody, GeneralProxyObsDto.class));
                    }
                    return generalProxyObsDto.get();
                });
    }

    public void handleException(HttpRequest request, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
                                CollectError collectError, AtomicInteger index, Signal errorMessage, ProcessingContext processingContext) throws IOException {

        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            String message = new String(response.getBody().readAllBytes()) + ". ErrorCode is: " + response.getStatusCode() + ". External call url is: " + request.getURI();
            log.error(message);
            collectError.addError(processingContext, String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
            processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
            collectError.addErrorSignal(processingContext, errorMessage);
            throw new RestCallException(HttpStatus.BAD_REQUEST, message, new Throwable(message));
        }
    }
}
