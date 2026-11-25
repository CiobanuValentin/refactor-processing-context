package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProxySourcePropertiesTest {

    @Test
    void urlIsRootUrlPlusEndpoint_fromTheExistingProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("bkrcki.root-url", "http://localhost:14006")
                .withProperty("bkrcki.endpoint", "/checkCki/results")
                .withProperty("dnb.root-url", "http://localhost:14008")
                .withProperty("dnb.check-endpoint", "/companyCheck")
                .withProperty("dnb.event-endpoint", "/companyEvent");

        ProxySourceProperties properties = new ProxySourceProperties(environment);

        assertEquals("http://localhost:14006/checkCki/results", properties.getUrls().get(ProxySource.CKI));
        assertEquals("http://localhost:14008/companyCheck", properties.getUrls().get(ProxySource.DNB));
        assertEquals("http://localhost:14008/companyEvent", properties.getUrls().get(ProxySource.EVENT_INDICATOR));
    }

    @Test
    void sourceWithoutConfiguration_hasNoUrl() {
        ProxySourceProperties properties = new ProxySourceProperties(new MockEnvironment());

        assertFalse(properties.getUrls().containsKey(ProxySource.VIS));
    }
}
