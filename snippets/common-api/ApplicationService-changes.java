// Metode noi in ApplicationService (com.bnp.pf.transformation.commons.api.generalproxyobs.service).
// Cele vechi (saveApplicationRequest cu ProcessingContext, arvalSaveApplicationRequest) raman pana migreaza
// toate proxy-urile. updateApplicationStatus ramane neschimbata.

private final Gson gson = GsonInstance.createGson();

/**
 * Aceeasi logica precum saveApplicationRequest / arvalSaveApplicationRequest, cu datele luate din context:
 * JSON-ul este aplicatia din context serializata, deci contine si rezultatele agregate.
 * Ca pana acum, daca exista deja un rand pentru business key, se actualizeaza doar modifiedDate / modifiedBy;
 * JSON-ul si statusul NU se rescriu.
 */
@Transactional
public void saveApplicationRequest(ProxyContext ctx) {
    Application application = ctx.getApplication();
    if (Objects.isNull(application) || StringUtils.isBlank(application.getBusinessKey())) {
        return;
    }
    Long businessKey = Long.valueOf(application.getBusinessKey());
    applicationRequestRepo.findByBusinessKey(businessKey).ifPresentOrElse(existing -> {
        log.warn(APPLICATION_WITH_BUSINESS_KEY + "{}" + ALREADY_PROCESSED, application.getBusinessKey());
        existing.setModifiedDate(LocalDateTimeUtils.parseLocalDateNow());
        existing.setModifiedBy(String.valueOf(ctx.getProducer()));
        applicationRequestRepo.save(existing);
    }, () -> {
        ApplicationRequestDto created = new ApplicationRequestDto();
        created.setStatus(ctx.hasErrors() ? KO : OK);
        created.setBusinessKey(businessKey);
        created.setApplicationJson(gson.toJson(application).replace("\n", ""));
        created.setCreatedDate(ctx.getEventTime().toLocalDate());
        created.setCreatedBy(String.valueOf(ctx.getProducer()));
        applicationRequestRepo.save(modelMapper.map(created, ApplicationRequestEntity.class));
    });
}

/**
 * Folosita de GET-ul din rat (RatResultsService). Tabela e cheiata doar pe business key, deci flowId nu filtreaza.
 * TODO: getApplicationJson() pe entitate este presupus.
 */
@Transactional(readOnly = true)
public Optional<Application> findSavedApplication(String businessKey, String flowId) {
    if (StringUtils.isBlank(businessKey)) {
        return Optional.empty();
    }
    return applicationRequestRepo.findByBusinessKey(Long.valueOf(businessKey))
            .map(entity -> gson.fromJson(entity.getApplicationJson(), Application.class));
}
