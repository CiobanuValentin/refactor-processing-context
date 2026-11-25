// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.builder;

@Component
@Slf4j
public class ProxyDtoBuilder {

    private final Gson gson = GsonInstance.createGson();

    public CheckScalEntityDto buildCheckScalEntityDto(GeneralProxyObsDto generalProxyObsDto) {
        CheckScalEntityDto checkScalEntityDto = new CheckScalEntityDto();
        checkScalEntityDto.setRequest(gson.fromJson(generalProxyObsDto.getRequestEntityJson(), Entity.class));
        String checkScalEntityDtoResponse = generalProxyObsDto.getResponseEntityJson();
        log.debug("checkScalEntityDto response is: {}", checkScalEntityDtoResponse);
        if (Objects.isNull(checkScalEntityDtoResponse) || checkScalEntityDtoResponse.equals("{}") || checkScalEntityDtoResponse.equals(CURLY_BRACES)) {
            checkScalEntityDto.setResponse(new CheckResult());
        } else {
            checkScalEntityDto.setResponse(gson.fromJson(checkScalEntityDtoResponse, CheckResult.class));
        }

        checkScalEntityDto.setErrors(gson.fromJson(generalProxyObsDto.getErrorHandlerJson(), ErrorHandler.class));
        checkScalEntityDto.setBusinessKey(generalProxyObsDto.getBusinessKey());
        checkScalEntityDto.setRequestId(String.valueOf(generalProxyObsDto.getId()));
        checkScalEntityDto.setFlowId(generalProxyObsDto.getFlowId());
        checkScalEntityDto.setCreation(generalProxyObsDto.getCreation());

        return checkScalEntityDto;
    }

    // Toate celelalte metode au exact aceeasi forma (request, raspuns gol -> obiect nou, errors, businessKey,
    // requestId, flowId, creation). Difera doar campurile citite si tipurile:
    //
    // metoda                          | request (getter -> tip)                              | response (getter -> tip)
    // buildCheckScalEntityDto         | getRequestEntityJson -> Entity                        | getResponseEntityJson -> CheckResult
    // buildCheckScalPersonInitialDto  | getRequestPersonInitialJson -> PersonInitial          | getResponsePersonInitialJson -> CheckResult
    // buildCheckScalPersonDto         | getRequestPersonJson -> Person                        | getResponsePersonJson -> CheckResult
    // buildCheckRvdrCreditDto         | getRequestCreditCheckEntityJson -> CreditCheckRequest | getResponseCreditCheckJson -> CreditCheckResponse
    // buildCheckDnbCompanyDto         | getRequestCompanyJson -> RequestCompany               | getResponseRootJson -> ResponseRoot
    // buildCheckEventIndicatorDto     | getRequestCompanyJson -> RequestCompany               | getResponseRootJson -> ResponseRoot
    // buildCheckCkiPersonDto          | getSingleBkrRequestJson -> SingleBkrRequest           | getSingleBkrResponseJson -> SingleBkrResponse
    // buildCheckVisDto                | getRequestVisJson -> VisRequest                       | getResponseVisJson -> VisResponse
    // buildCheckEvaPersonDto          | getRequestCheckEvaJson -> EvaRequest                  | getResponsePersonJson -> EvaResponse
    // buildCheckEvaCompanyDto         | getRequestCheckEvaCompanyJson -> EvaCompanyRequest    | getResponseCheckEvaCompanyJson -> EvaResponse
    //
    // De observat: buildCheckEvaPersonDto citeste raspunsul din getResponsePersonJson(), acelasi camp ca SCAL person.
}
