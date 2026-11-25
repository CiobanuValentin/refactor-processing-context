package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.ALREADY_PROCESSED;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.APPLICATION_WITH_BUSINESS_KEY;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.OK;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: pachetele pentru ApplicationRequestDto, ApplicationRequestEntity, ApplicationRequestRepository si Application
// sunt presupuse - pastreaza importurile din clasa actuala.
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ApplicationRequestDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.entity.ApplicationRequestEntity;
import com.bnp.pf.transformation.commons.api.generalproxyobs.repository.ApplicationRequestRepository;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ModelMapper modelMapper;
    private final ApplicationRequestRepository applicationRequestRepo;

    private final Gson gson = GsonInstance.createGson();

    /**
     * Salveaza aplicatia din context. Inlocuieste saveApplicationRequest(application, message, json, context)
     * si arvalSaveApplicationRequest(...): contextul e acelasi pentru ambele tipuri de mesaj.
     *
     * Ca pana acum: daca exista deja un rand pentru business key, se actualizeaza doar modifiedDate si modifiedBy;
     * JSON-ul si statusul nu se rescriu. Altfel se creeaza randul, cu status OK sau KO dupa erorile din context.
     */
    @Transactional
    public void saveApplicationRequest(ProxyContext ctx) {
        Application application = ctx.getApplication();
        if (Objects.isNull(application) || StringUtils.isBlank(application.getBusinessKey())) {
            return;
        }
        Long businessKey = Long.valueOf(application.getBusinessKey());
        Optional<ApplicationRequestEntity> existing = applicationRequestRepo.findByBusinessKey(businessKey);

        if (existing.isPresent()) {
            log.warn(APPLICATION_WITH_BUSINESS_KEY + "{}" + ALREADY_PROCESSED, application.getBusinessKey());
            ApplicationRequestEntity entity = existing.get();
            entity.setModifiedDate(LocalDateTimeUtils.parseLocalDateNow());
            entity.setModifiedBy(String.valueOf(ctx.getProducer()));
            applicationRequestRepo.save(entity);
            return;
        }

        ApplicationRequestDto created = new ApplicationRequestDto();
        created.setStatus(ctx.hasErrors() ? KO : OK);
        created.setBusinessKey(businessKey);
        created.setApplicationJson(gson.toJson(application).replace("\n", ""));
        created.setCreatedDate(ctx.getEventTime().toLocalDate());
        created.setCreatedBy(String.valueOf(ctx.getProducer()));
        applicationRequestRepo.save(modelMapper.map(created, ApplicationRequestEntity.class));
        log.info("Saved application request for business key {}", businessKey);
    }

    /**
     * Aplicatia salvata de saveApplicationRequest; folosita de GET-ul de rezultate din rat.
     * Tabela e cheiata doar pe business key.
     * TODO: getApplicationJson() pe entitate este presupus.
     */
    @Transactional(readOnly = true)
    public Optional<Application> findSavedApplication(String businessKey) {
        if (StringUtils.isBlank(businessKey)) {
            return Optional.empty();
        }
        return applicationRequestRepo.findByBusinessKey(Long.valueOf(businessKey))
                .map(entity -> gson.fromJson(entity.getApplicationJson(), Application.class));
    }

    /** Neschimbata. */
    public void updateApplicationStatus(String businessKey, String status) {
        if (StringUtils.isNotBlank(businessKey)) {
            applicationRequestRepo.setApplicationRequestByStatus(status, Long.valueOf(businessKey));
            log.info("Set status with value '{}' of ApplicationRequestEntity with businessKey: {}", status, businessKey);
        }
    }
}
