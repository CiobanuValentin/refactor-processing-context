package com.bnp.pf.transformation.visproxy.config.general;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bnp.pf.transformation.commons.api.generalproxyobs.repo.ApplicationRequestRepository;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;

// Neschimbat: ApplicationService din common-api nou are acelasi constructor (ModelMapper, ApplicationRequestRepository).
@Configuration
public class ApplicationServiceConfig {
    @Bean
    public ApplicationService createApplicationService(ModelMapper modelMapper, ApplicationRequestRepository applicationRequestRepository) {
        return new ApplicationService(modelMapper, applicationRequestRepository);
    }
}
