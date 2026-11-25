// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.dto;

// Toate dto-urile Check* au aceeasi forma: extind RequestAndResponseBase si au request + response tipizate.

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckDnbCompanyDto extends RequestAndResponseBase {
    private RequestCompany request;
    private ResponseRoot response;
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRvdrCreditDto extends RequestAndResponseBase {
    private CreditCheckRequest request;
    private CreditCheckResponse response;
}

// RequestCompany (generat din OpenAPI, applicationbuilder.swagger.model): callReference, company, registers.
// CreditCheckRequest (generat din WSDL, urn.cc.wsdl.creditcheck): conversationID, callReferenceID, sessionID,
// callRequestData, performCheck, creditCheckRequestData.
// Lista din IDE: CheckCkiPersonDto, CheckDnbCompanyDto, CheckEvaCompanyDto, CheckEvaPersonDto, CheckEventIndicatorDto,
// CheckRvdrCreditDto, CheckScalEntityDto, CheckScalPersonDto, CheckScalPersonInitialDto, CheckSedExpertiseDto,
// CheckVisDto, ProcessingErrorsListDto, RatSanctionDto, RequestAndResponseBase.
