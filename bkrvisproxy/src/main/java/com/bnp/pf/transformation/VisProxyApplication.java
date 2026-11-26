package com.bnp.pf.transformation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Neschimbat. Clasele din common-api nu sunt scanate (doar repo/entity); bean-urile lor sunt in config/general.
@SpringBootApplication(scanBasePackages = {"org.springframework.util", "com.bnp.pf.transformation.visproxy"})
@EnableJpaAuditing
@EnableJpaRepositories({"com.bnp.pf.transformation.visproxy.repository",
        "com.bnp.pf.transformation.commons.api.generalproxyobs.repo"})
@EntityScan({"com.bnp.pf.transformation.visproxy.entity",
        "com.bnp.pf.transformation.commons.api.generalproxyobs.entity"})
public class VisProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisProxyApplication.class, args);
    }
}
