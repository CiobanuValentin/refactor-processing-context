// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze.
package com.bnp.pf.transformation.visproxy.controller;

/**
 * Controller used in RAT proxy for fetching data
 */
@RestController
@RequiredArgsConstructor
public class BkrVisController {
    private final VisCheckRecordService visCheckRecordService;

    @GetMapping("/getVisEntities")
    public List<GeneralProxyObsDto> getVisEntities(String businessKey, String flowId) {
        return visCheckRecordService.fetchVisEntity(businessKey, flowId);
    }
}

// --- com.bnp.pf.transformation.visproxy.config.general ---
@Configuration
public class ApplicationServiceConfig {
    @Bean
    public ApplicationService createApplicationService(ModelMapper modelMapper, ApplicationRequestRepository applicationRequestRepository) {
        return new ApplicationService(modelMapper, applicationRequestRepository);
    }
}
