package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.KO;
import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;

import com.bnp.pf.transformation.avro.Metadata;
import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.avro.Signal;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.entity.ApplicationRequestEntity;
import com.bnp.pf.transformation.commons.api.generalproxyobs.repo.ApplicationRequestRepository;
import com.bnp.pf.transformation.commons.api.message.input.Application;

class ApplicationServiceTest {

    private final ApplicationRequestRepository repository = mock(ApplicationRequestRepository.class);
    private final ApplicationService service = new ApplicationService(new ModelMapper(), repository);

    @Test
    void newApplicationWithoutErrors_isSavedWithStatusOk() {
        when(repository.findByBusinessKey(42L)).thenReturn(Optional.empty());

        service.saveApplicationRequest(context("42"));

        ApplicationRequestEntity saved = savedEntity();
        assertEquals(Long.valueOf(42L), saved.getBusinessKey());
        assertEquals(OK, saved.getStatus());
        assertEquals("granting-api", saved.getCreatedBy());
        assertNotNull(saved.getCreatedDate());
        assertTrue(saved.getApplicationJson().contains("\"businessKey\":\"42\""));
        assertFalse(saved.getApplicationJson().contains("\n"));
        assertNull(saved.getModifiedBy());
    }

    @Test
    void newApplicationWithErrors_isSavedWithStatusKo() {
        when(repository.findByBusinessKey(42L)).thenReturn(Optional.empty());
        ProxyContext ctx = context("42");
        ctx.addError("400", "boom");

        service.saveApplicationRequest(ctx);

        assertEquals(KO, savedEntity().getStatus());
    }

    @Test
    void existingApplication_onlyModifiedFieldsChange() {
        ApplicationRequestEntity existing = new ApplicationRequestEntity();
        existing.setBusinessKey(42L);
        existing.setStatus(OK);
        existing.setApplicationJson("{\"old\":true}");
        when(repository.findByBusinessKey(42L)).thenReturn(Optional.of(existing));
        ProxyContext ctx = context("42");
        ctx.addError("400", "boom");

        service.saveApplicationRequest(ctx);

        ApplicationRequestEntity saved = savedEntity();
        assertEquals("{\"old\":true}", saved.getApplicationJson());
        assertEquals(OK, saved.getStatus());
        assertEquals("granting-api", saved.getModifiedBy());
        assertNotNull(saved.getModifiedDate());
    }

    @Test
    void noApplicationOrBlankBusinessKey_nothingIsSaved() {
        ProxyContext withoutApplication = context("42");
        withoutApplication.setApplication(null);

        service.saveApplicationRequest(withoutApplication);
        service.saveApplicationRequest(context(""));

        verifyNoInteractions(repository);
    }

    @Test
    void findSavedApplication_parsesTheStoredJson() {
        ApplicationRequestEntity existing = new ApplicationRequestEntity();
        existing.setApplicationJson("{\"businessKey\":\"42\"}");
        when(repository.findByBusinessKey(42L)).thenReturn(Optional.of(existing));

        assertEquals("42", service.findSavedApplication("42").orElseThrow().getBusinessKey());
    }

    @Test
    void findSavedApplication_missingOrBlankKey_isEmpty() {
        when(repository.findByBusinessKey(7L)).thenReturn(Optional.empty());

        assertTrue(service.findSavedApplication("7").isEmpty());
        assertTrue(service.findSavedApplication(" ").isEmpty());
    }

    @Test
    void updateApplicationStatus_updatesByNumericBusinessKey() {
        service.updateApplicationStatus("42", KO);

        verify(repository).setApplicationRequestByStatus(KO, 42L);
    }

    @Test
    void updateApplicationStatus_blankBusinessKey_doesNothing() {
        service.updateApplicationStatus("", OK);
        service.updateApplicationStatus(null, OK);

        verifyNoInteractions(repository);
    }

    private ApplicationRequestEntity savedEntity() {
        ArgumentCaptor<ApplicationRequestEntity> captor = ArgumentCaptor.forClass(ApplicationRequestEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // TODO: Metadata si setterele ei sunt presupuse, ca in ProxyContextTest.
    private static ProxyContext context(String businessKey) {
        Metadata metadata = new Metadata();
        metadata.setProducer("granting-api");
        Signal signal = new Signal();
        signal.setFlowId("F1");
        OrchestrationMessage message = new OrchestrationMessage();
        message.setData(signal);
        message.setMetadata(metadata);
        ProxyContext ctx = ProxyContext.of(message, "corr", "visproxy");
        Application application = new Application();
        application.setBusinessKey(businessKey);
        ctx.setApplication(application);
        return ctx;
    }
}
