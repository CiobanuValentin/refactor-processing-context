// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.builder;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationBuilderPG {

    private final DNBMapper dnbMapper;
    private final SCALMapper scalMapper;
    private final BKRCKIMapper ckiMapper;
    private final RVDRMapper rvdrMapper;
    private final BKREVAMapper bkrevaMapper;
    private final BKRVISMapper bkrvisMapper;
    private final EventIndicatorMapper eventIndMapper;
    private final PartialDataFetcher fetcher;

    // Toate apelurile fetcher.getXxx(...) au aceiasi parametri:
    // (processingContext.getBusinessKey(), processingContext.getFlowId(), processingContext.getErrorMessage(),
    //  processingContext.getIndex(), processingContext) - iar restul liniei era taiat in poze.
    public Application buildApplication(ProcessingContext processingContext) {

        log.debug("Building application Object started for business key {} and flow ID {}", processingContext.getBusinessKey(), processingContext.getFlowId());

        //SCAL Mapping
        scalMapper.mapScalPersonData(fetcher.getScalPersonData(/* ... */), /* ...taiat */);
        scalMapper.mapScalPersonInitialData(fetcher.getScalPersonInitialData(/* ... */), /* ...taiat */);
        scalMapper.mapScalEntityData(fetcher.getScalEntityData(/* ... */), /* ...taiat */);

        //DNB Mapping
        dnbMapper.mapDNBData(fetcher.getDNBData(/* ... */), /* ...taiat */);

        //CKI Mapping
        ckiMapper.mapCKIData(fetcher.getCkiData(/* ... */), /* ...taiat */);

        //RVDR Mapping
        rvdrMapper.mapRVDRData(fetcher.getRVDRData(/* ... */), /* ...taiat */);

        //BKR VIS Mapping
        bkrvisMapper.mapVISData(fetcher.getVisData(/* ... */), /* ...taiat */);

        //BKR EVA Mapping
        bkrevaMapper.mapEVACompanyData(Collections.singletonList(fetcher.getEvaCompanyData(/* ... */)), /* ...taiat */);
        bkrevaMapper.mapEVAPersonData(fetcher.getEvaPersonData(/* ... */), /* ...taiat */);

        //Event Indicator Mapping
        eventIndMapper.mapEventIndicatorData(fetcher.getEventIndicatorData(/* ... */), /* ...taiat */);

        log.debug("Application object successfully built for business key {} and flow ID {}", processingContext.getBusinessKey(), processingContext.getFlowId());

        return processingContext.getApplication();
    }

}
