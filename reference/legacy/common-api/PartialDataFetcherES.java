// REFERINTA - cod existent (legacy, common-api-1.23.jar), transcris din poze. Nu se compileaza si poate contine erori
// de transcriere. IDE-ul afisa "Library source does not match the bytecode". Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.applicationbuilder.service;

@Component
@Primary
//Once a Kafka implementation will be available, we need to use profiles or another mechanism to handle the different implementations
@AllArgsConstructor
public class PartialDataFetcherES implements PartialDataFetcher {

    //SCAL Repositories
    private final CheckScalEntityRepository scalEntityRepo;
    private final CheckScalPersonRepository scalPersonRepo;
    private final CheckScalPersonInitialRepository scalPersonInitialRepo;

    //RVDR Repository
    private final CheckRvdrCreditRepository rvdrRepo;

    //DNB Repository
    private final CheckDnbCompanyRepository dnbRepo;

    //VIS Repository
    private final CheckVisRequestAndResponseRepository visRepo;

    //EVA Repositories
    private final CheckEvaRequestAndResponseRepository evaPersonRepo;
    private final CheckEvaCompanyRequestAndResponseRepository evaCompanyRepo;

    //CKI Repository
    private final CheckCkiRequestAndResponseRepository ckiRepo;

    //SED Repository
    private final CheckSedExpertiseRepository sedRepo;

    //RAT Repository
    private final RatSanctionDtoRepository ratSanctionRepo;

    //Event Indicator Repository
    private final CheckEventIndicatorRepository eventIndicatorRepo;

    // Toate cele 12 metode au aceeasi forma: toate randurile pentru businessKey si flowId, sau null daca nu exista.
    @Override
    public List<CheckCkiPersonDto> getCkiData(String businessKey, String flowId) {
        return ckiRepo.findByBusinessKeyAndFlowId(businessKey, flowId).orElse(null);
    }

    // getScalPersonData -> scalPersonRepo, getScalPersonInitialData -> scalPersonInitialRepo,
    // getScalEntityData -> scalEntityRepo, getRVDRData -> rvdrRepo, getDNBData -> dnbRepo, getVisData -> visRepo,
    // getEvaPersonData -> evaPersonRepo, getEvaCompanyData -> evaCompanyRepo, getSedData -> sedRepo,
    // getRATData -> ratSanctionRepo, getEventIndicatorData -> eventIndicatorRepo
}
