// REFERINTA - cod existent (cloud, common-api), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.util;

/**
 * Class with constants used in all the proxies. DO NOT DELETE CONSTANTS BECAUSE YOU WILL BREAK PROXY PROJECTS
 */
@UtilityClass
public class ProxyObsConstant {
    public static final String CORRELATION_HEADER = "X-Correlation";
    public static final String APPLICATION = "application";
    public static final String STATUS = "STATUS";
    public static final String DNB = "dnb";
    public static final String EVENTINDICATOR = "eventindicator";
    public static final String DNB_EVENTINDICATOR = "dnb_eventindicator";
    public static final String EVENTINDICATOR_DNB = "eventindicator_dnb";
    public static final String OK = "OK";
    public static final String KO = "KO";
    public static final String ERROR_MESSAGE = "Error_message_";
    public static final String SPECIAL_CHAR = ".";
    public static final String Z = "Z";
    public static final String LOAN_EMPTY = "Application Loan is empty";
    public static final String ALREADY_PROCESSED = " already processed";
    public static final String APPLICATION_WITH_BUSINESS_KEY = "application with business key ";
    public static final String APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND = "Application with Business key \"\" not found";
    public static final String MEDIUM_RISK_OR_ALERTS_OR_ON_HOLD = "MediumRiskOrAlertsOrOnHold";
    public static final String BACKSLASH = "/";
    public static final String RESPONSE_ROOT_JSON = "response_root_json";
    public static final String ERROR_HANDLER_JSON = "error_handler_json";
    public static final String ID = "id";
    public static final String FLOW_ID = "flow_id";
    public static final String CREATION = "creation";
    public static final String BUSINESS_KEY = "business_key";
    public static final String REQUEST_ENTITY_JSON = "request_entity_json";
    public static final String REQUEST_PERSON_INITIAL_JSON = "request_person_initial_json";
    public static final String REQUEST_CREDIT_CHECK_ENTITY_JSON = "request_credit_check_entity_json";
    public static final String REQUEST_PERSON_JSON = "request_person_json";
    public static final String REQUEST_COMPANY_JSON = "request_company_json";
    public static final String BUSINESS_KEY_ID = "123456";
    public static final String HOLDER_PARTNER_ID = "HolderPartner_" + BUSINESS_KEY + "_1";
    public static final String HOLDER_ID = "Holder_" + BUSINESS_KEY + "_0";
    public static final String ID_FLOW = "AZEAZEA-23424324-234234234";
    public static final String SINGLE_BKR_REQUEST_JSON = "single_bkr_request_json";
    public static final String SINGLE_BKR_RESPONSE_JSON = "single_bkr_response_json";
    public static final String AND_FOR = " and for";
    public static final String FOR_COMMAND = " for command ";
    public static final String SOAP_ERROR_MESSAGE = "External API request triggered a SOAP Fault Exception (Check API gateway)";
    public static final String ARVAL_NL_FLOW_ID_PREFIX = "arvl";
    public static final String STANDARD_PATTERN = "yyyyMMdd";
    public static final String YYYYY_MM_DD_HH_SS = "yyyyMMddHHmmss";
    public static final String HOLDER = "Holder";
    public static final String EXTRA_INCOME = "ExtraIncome";
    public static final String ONE = "1";
    public static final String TWO = "2";
    public static final String ZERO = "0";
    public static final String EMPTY = "";
    public static final String RESULT = "Result";
    public static final String TOTAL_LOAN_AMOUNT = "TotalLoanAmount";
    public static final String FINANCIAL_STRENGTH_KEY = "DB_FINANCIAL_STRENGTH";
    public static final String SCORE_OVERRIDE_KEY = "DB_RISK_INDICATOR_OVERRIDE";
    public static final String RISK_INDICATOR_KEY = "DB_RISK_INDICATOR";
    public static final String LATEST_ACCOUNTS_FILED_DATE_KEY = "DB_LATESTACCOUNTSFILEDDATE";
    public static final String LATEST_ACCOUNTS_FILED_PERIOD_KEY = "DB_LATESTACCOUNTSFILEDPERIOD";
    public static final String ARVAL_FLOW_ID = "arvl-468840a1";
    public static final String PF_FLOW_ID = "pf-nl-468840a1";
    public static final String BKR_EVA = "bkr.eva";
    public static final String BKR_EVA_NAME = "Bkr Eva";
    public static final String BKR_EVA_RESULT_DESC = "BKR EVA results";
    public static final String BKR_EVA_RESULT_HOLDER_DESC = "BKR EVA results for loan holders";
    public static final String BKR_EVA_RESULT_CO_HOLDER_DESC = "BKR EVA results for loan co-holders";
    public static final String BKR_CKI = "bkr";
    public static final String BKR_CKI_NAME = "Bkr Cki";
    public static final String BKR_CKI_RESULT_DESC = "BKR CKI results";
    public static final String BKR_VIS = "bkr.vis";
    public static final String BKR_VIS_NAME = "Bkr Vis";
    public static final String BKR_VIS_RESULT_DESC = "BKR VIS results";
    public static final String BKR_VIS_RESULT_HOLDER_DESC = "BKR VIS results for loan holders";
    public static final String BKR_VIS_RESULT_CO_HOLDER_DESC = "BKR VIS results for loan co-holders";
    public static final String RESULT_HOLDER = "Holder";
    public static final String RESULT_CO_HOLDER = "CoHolder";
    public static final String APP_NAME_SCAL = "SCAL";
    public static final String AMSTERDAM = "AMSTERDAM";
    public static final String ALMERE = "ALMERE";
    public static final String CURLY_BRACES = "\"{}\"";
    //arval flow
    public static final String SCOPE_KEY = "scope";
    public static final String BKR_CKI_DATA_SCOPE = "bkr_cki_data";
    public static final String SED_DATA_SCOPE = "sed_data";
    public static final String APPLICATION_DATA_SCOPE = "application_data";

    public static final String CORRELATION_ID_NOT_PROVIDED = "Correlation ID not provided";
    public static final String BUSINESS_KEY_WITH_SPACE = "business key: ";
    public static final String IS_EMPTY = " is empty";
    public static final String OR_FLOW_ID = " or flow id: ";
    public static final String SAVING_APPLICATION_REQUEST_ENTITY = "Saving ApplicationRequestEntity in table application_request: {}";
}
