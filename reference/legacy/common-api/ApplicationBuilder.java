// REFERINTA - cod existent (legacy, common-api-1.23.jar), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
// IDE-ul afisa "Library source does not match the bytecode for class ApplicationBuilder".
package com.bnp.pf.transformation.commons.api.applicationbuilder.service;

@Service
@Slf4j
@AllArgsConstructor
public class ApplicationBuilder {

    private final PartialDataFetcher fetcher;
    private final ApplicationRepository applicationRepo;
    private final SCALMapper scalMapper;
    private final RVDRMapper rvdrMapper;
    private final DNBMapper dnbMapper;
    private final BKRVISMapper visMapper;
    private final BKREVAMapper evaMapper;
    private final BKRCKIMapper ckiMapper;
    private final EventIndicatorMapper eventIndMapper;

    public Application buildApplication(String businessKey, String flowId) {
        var application = applicationRepo.findById(businessKey).orElse(null);

        if (application == null) {
            log.error("Application for Business Key {} not found", businessKey);
            return null;
        }

        if (application.getLoan() == null) {
            log.error("Application loan is empty for Business Key {}", businessKey);
            return null;
        }

        log.debug("Building application Object started for business key {} and flow ID {}", businessKey, flowId);

        //SCAL Data Mapping
        scalMapper.mapScalPersonData(fetcher.getScalPersonData(businessKey, flowId), application);
        scalMapper.mapScalPersonInitialData(fetcher.getScalPersonInitialData(businessKey, flowId), application);
        scalMapper.mapScalEntityData(fetcher.getScalEntityData(businessKey, flowId), application);

        //DNB Mapping
        dnbMapper.mapDNBData(fetcher.getDNBData(businessKey, flowId), application);

        //RVDR Mapping
        rvdrMapper.mapRVDRData(fetcher.getRVDRData(businessKey, flowId), application);

        //VIS Mapping
        visMapper.mapVISData(fetcher.getVisData(businessKey, flowId), application);

        //EVA Mapping
        evaMapper.mapEVAPersonData(fetcher.getEvaPersonData(businessKey, flowId), application);
        evaMapper.mapEVACompanyData(fetcher.getEvaCompanyData(businessKey, flowId), application);

        //CKI Mapping
        ckiMapper.mapCKIData(fetcher.getCkiData(businessKey, flowId), application);

        //Event Indicator Mapping
        eventIndMapper.mapEventIndicatorData(fetcher.getEventIndicatorData(businessKey, flowId), application);

        log.debug("Application object successfully built for business key {} and flow ID {}", businessKey, flowId);

        return application;
    }
}
