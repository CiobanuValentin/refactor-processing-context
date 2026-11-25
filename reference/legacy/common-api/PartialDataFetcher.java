// REFERINTA - cod existent (legacy, common-api-1.23.jar), transcris din poze. Nu se compileaza si poate contine erori
// de transcriere. IDE-ul afisa "Library source does not match the bytecode". Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.service;

public interface PartialDataFetcher {
    List<CheckScalPersonDto> getScalPersonData(String businessKey, String flowId);

    List<CheckScalPersonInitialDto> getScalPersonInitialData(String businessKey, String flowId);

    List<CheckScalEntityDto> getScalEntityData(String businessKey, String flowId);

    List<CheckRvdrCreditDto> getRVDRData(String businessKey, String flowId);

    List<CheckDnbCompanyDto> getDNBData(String businessKey, String flowId);

    List<CheckVisDto> getVisData(String businessKey, String flowId);

    List<CheckEvaPersonDto> getEvaPersonData(String businessKey, String flowId);

    List<CheckEvaCompanyDto> getEvaCompanyData(String businessKey, String flowId);

    List<CheckCkiPersonDto> getCkiData(String businessKey, String flowId);

    List<CheckSedExpertiseDto> getSedData(String businessKey, String flowId);

    List<RatSanctionDto> getRATData(String businessKey, String flowId);

    List<CheckEventIndicatorDto> getEventIndicatorData(String businessKey, String flowId);
}
