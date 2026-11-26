package com.bnp.pf.transformation.visproxy.config;

// Neschimbat. Importurile nu erau vizibile in poze - pastreaza-le pe cele actuale
// (PfCxfSoapClientBuildHelper, ClientProperties, HttpConduitProperties vin din biblioteca pfspring).
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import urn.bkr.wsdl.vis.VISPort;

@Configuration
@Slf4j
public class SoapClientConfig {
    @Value("${pfspring.soap.clients.bkrvis-backend-endpoint.root-uri}")
    private String bkrVisBackendEndpointRootUri;

    @Value("${pfspring.soap.clients.bkrvis-backend-endpoint.http-conduit.proxy-context}")
    private String bizProxy;

    @Value("${pfspring.soap.clients.bkrvis-backend-endpoint.http-conduit.ssl-bundle}")
    private String sslbundle;

    @Bean
    public VISPort jaxWsClientVis(final PfCxfSoapClientBuildHelper pfCxfSoapClientBuildHelper) {
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.getFeatures().add(loggingFeature());

        ClientProperties clientProperties = new ClientProperties();
        clientProperties.getHealthcheck().setDisabled(true);
        clientProperties.setRootUri(bkrVisBackendEndpointRootUri);

        HttpConduitProperties httpConduitProperties = clientProperties.getHttpConduit();
        httpConduitProperties.setProxyContext(bizProxy);
        httpConduitProperties.setSslBundle(sslbundle);

        return pfCxfSoapClientBuildHelper.configureBuildAndRegister(
                VISPort.class, proxyFactory, "bkrvis-backend-endpoint", clientProperties);
    }

    private LoggingFeature loggingFeature() {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        return loggingFeature;
    }
}
