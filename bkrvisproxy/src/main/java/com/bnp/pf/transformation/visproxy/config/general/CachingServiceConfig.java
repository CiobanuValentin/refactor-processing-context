package com.bnp.pf.transformation.visproxy.config.general;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO: importurile pentru CachingService si GeneralProxyObsCacheRepository sunt presupuse - pastreaza-le pe cele actuale.
import com.bnp.pf.transformation.commons.api.generalproxyobs.repo.GeneralProxyObsCacheRepository;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.CachingService;

// Neschimbat.
@Configuration
public class CachingServiceConfig {
    @Bean
    public CachingService createCachingService(ModelMapper modelMapper, GeneralProxyObsCacheRepository generalProxyObsCacheRepository) {
        return new CachingService(modelMapper, generalProxyObsCacheRepository);
    }
}
