package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.core.env.Environment;

import lombok.Getter;

/**
 * URL-urile endpointurilor de rezultate, citite din aceleasi proprietati ca in PartialDataFetcherPG
 * (de exemplu bkrcki.root-url + bkrcki.endpoint). Configurarea din medii nu se schimba ca structura:
 * cand un proxy agregator trece pe liste, valoarea endpointului devine calea endpointului de lista
 * (de exemplu bkrcki.endpoint: "/checkCki/results").
 *
 * Nu e adnotata ca bean: proxy-ul o declara in config, ca pe celelalte clase din common-api.
 */
@Getter
public class ProxySourceProperties {

    private final Map<ProxySource, String> urls = new EnumMap<>(ProxySource.class);

    public ProxySourceProperties() {
    }

    public ProxySourceProperties(Environment environment) {
        for (ProxySource source : ProxySource.values()) {
            String rootUrl = environment.getProperty(source.rootUrlProperty());
            String endpoint = environment.getProperty(source.endpointProperty());
            if (rootUrl != null && endpoint != null) {
                urls.put(source, rootUrl + endpoint);
            }
        }
    }
}
