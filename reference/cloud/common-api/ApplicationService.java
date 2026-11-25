// REFERINTA - cod existent (cloud, common-api, branch arval-refactor-last), transcris din poze. Nu se compileaza si poate
// contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationService {

    private final ModelMapper modelMapper;

    private final ApplicationRequestRepository applicationRequestRepo;


    @Transactional
    public void saveApplicationRequest(Application application, OrchestrationMessage messageToSend, String applicationJson, ProcessingContext pc) {

        if (Objects.nonNull(application) && StringUtils.isNotBlank(application.getBusinessKey())) {
            Optional<ApplicationRequestEntity> applicationRequestEntityOptional = applicationRequestRepo.findByBusinessKey(Long.valueOf(application.g/* ...taiat */));
            log.debug("applicationRequestEntity is present ? {}", applicationRequestEntityOptional.isPresent());
            applicationRequestEntityOptional.ifPresentOrElse(applicationRequestPresent -> {
                        log.warn(APPLICATION_WITH_BUSINESS_KEY + "{}" + ALREADY_PROCESSED, application.getBusinessKey());
                        log.debug("applicationRequestEntity before saving is: {}", applicationRequestPresent);

                        LocalDate now = LocalDateTimeUtils.parseLocalDateNow();
                        applicationRequestPresent.setModifiedDate(now);
                        applicationRequestPresent.setModifiedBy(String.valueOf(messageToSend.getMetadata().getProducer()));
                        applicationRequestRepo.save(applicationRequestPresent);
                        log.info(SAVING_APPLICATION_REQUEST_ENTITY, applicationRequestPresent);
                    },
                    () -> {
                        LocalDate newLocalDate = LocalDateTimeUtils.convertToNewLocalDate(messageToSend);
                        Map<String, Signal> errorSignalMap = pc.getErrorSignalMap();
                        List<Error> errors = pc.getErrorHandler().getErrors();
                        ApplicationRequestDto applicationRequestNotPresent = new ApplicationRequestDto();

                        //set default status
                        if (CollectionUtils.isEmpty(errors) && MapUtils.isEmpty(errorSignalMap)) {
                            applicationRequestNotPresent.setStatus(OK);
                        } else {
                            applicationRequestNotPresent.setStatus(KO);
                        }

                        applicationRequestNotPresent.setBusinessKey(Long.valueOf(application.getBusinessKey()));
                        applicationRequestNotPresent.setApplicationJson(applicationJson.replace("\n", ""));
                        applicationRequestNotPresent.setCreatedDate(newLocalDate);
                        applicationRequestNotPresent.setCreatedBy(String.valueOf(messageToSend.getMetadata().getProducer()));
                        applicationRequestRepo.save(modelMapper.map(applicationRequestNotPresent, ApplicationRequestEntity.class));
                        log.info(SAVING_APPLICATION_REQUEST_ENTITY, applicationRequestNotPresent);
                    });
        }
    }

    // arvalSaveApplicationRequest(Application, ArvalOrchestrationMessage messageToSend, String applicationJson,
    //     ArvalProcessingContext apc): identica, cu trei diferente:
    //   - LocalDateTimeUtils.arvalConvertToNewLocalDate(messageToSend) si apc.getArvalErrorSignalMap()
    //   - setApplicationJson(applicationJson), fara replace("\n", "")
    //   - logurile cu SAVING_APPLICATION_REQUEST_ENTITY sunt la debug
    @Transactional
    public void arvalSaveApplicationRequest(/* ... */) { /* ... */ }

    public void updateApplicationStatus(String businessKey, String status) {
        if (StringUtils.isNotBlank(businessKey)) {
            applicationRequestRepo.setApplicationRequestByStatus(status, Long.valueOf(businessKey));
            log.info("Set status with value '{}' of ApplicationRequestEntity with businessKey: {}", status, businessKey);
        }
    }
}
