package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * URL-urile endpointurilor de rezultate, de exemplu:
 * proxy-results.urls.cki=http://bkrckiproxy/checkCki/results
 * TODO: aliniaza prefixul cu proprietatile folosite acum de PartialDataFetcherPG.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "proxy-results")
public class ProxySourceProperties {

    private Map<ProxySource, String> urls = new EnumMap<>(ProxySource.class);
}
