// REFERINTA - cod existent (cloud, common-api), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.dto;

/**
 * class used in all proxies as a general DTO class
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GeneralProxyObsDto {
    private Integer id;
    @JsonProperty("flow_id")
    private String flowId;
    private LocalDateTime creation;
    @JsonProperty("business_key")
    private String businessKey;
    @JsonProperty("error_handler_json")
    private String errorHandlerJson;

    // DNB
    @JsonProperty("request_company_json")
    private String requestCompanyJson;
    @JsonProperty("response_root_json")
    private String responseRootJson;

    // RAT
    @JsonProperty("request_loan_json")
    private String requestLoanJson;
    @JsonProperty("rat_response")
    private Boolean ratResponse;

    // CKI
    @JsonProperty("single_bkr_request_json")
    private String singleBkrRequestJson;
    @JsonProperty("single_bkr_response_json")
    private String singleBkrResponseJson;

    // SCAL
    @JsonProperty("request_entity_json")
    private String requestEntityJson;
    @JsonProperty("response_entity_json")
    private String responseEntityJson;
    @JsonProperty("request_person_json")
    private String requestPersonJson;
    @JsonProperty("response_person_json")
    private String responsePersonJson;
    @JsonProperty("request_person_initial_json")
    private String requestPersonInitialJson;
    @JsonProperty("response_person_initial_json")
    private String responsePersonInitialJson;

    // RVDR
    @JsonProperty("request_credit_check_entity_json")
    private String requestCreditCheckEntityJson;
    @JsonProperty("response_credit_check_json")
    private String responseCreditCheckJson;

    // BKR EVA
    @JsonProperty("request_check_eva_company_json")
    private String requestCheckEvaCompanyJson;
    @JsonProperty("response_check_eva_company_json")
    private String responseCheckEvaCompanyJson;
    @JsonProperty("request_check_eva_json")
    private String requestCheckEvaJson;
    @JsonProperty("response_check_eva_json")
    private String responseCheckEvaJson;

    // BKR VIS
    @JsonProperty("request_vis_json")
    private String requestVisJson;
    @JsonProperty("response_vis_json")
    private String responseVisJson;

    // SED
    @JsonProperty("expertise_request_json")
    private String expertiseRequestJson;
    @JsonProperty("expertise_return_json")
    private String expertiseReturnJson;
}
