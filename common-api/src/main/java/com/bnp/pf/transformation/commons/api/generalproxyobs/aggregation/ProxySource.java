package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

/**
 * Sursele din care un proxy agregator (rat, sed) isi ia rezultatele: aceleasi zece apeluri din PartialDataFetcherPG,
 * cu aceleasi proprietati de configurare (root url + endpoint).
 */
public enum ProxySource {
    SCAL_PERSON("scal.root-url", "scal.person-endpoint"),
    SCAL_PERSON_INITIAL("scal.root-url", "scal.personInitial-endpoint"),
    SCAL_ENTITY("scal.root-url", "scal.entity-endpoint"),
    DNB("dnb.root-url", "dnb.check-endpoint"),
    CKI("bkrcki.root-url", "bkrcki.endpoint"),
    RVDR("rvdr.root-url", "rvdr.checkCredit-endpoint"),
    VIS("bkrvis.root-url", "bkrvis.credit-endpoint"),
    EVA_COMPANY("bkreva.root-url", "bkreva.company-endpoint"),
    EVA_PERSON("bkreva.root-url", "bkreva.person-endpoint"),
    EVENT_INDICATOR("dnb.root-url", "dnb.event-endpoint");

    private final String rootUrlProperty;
    private final String endpointProperty;

    ProxySource(String rootUrlProperty, String endpointProperty) {
        this.rootUrlProperty = rootUrlProperty;
        this.endpointProperty = endpointProperty;
    }

    public String rootUrlProperty() {
        return rootUrlProperty;
    }

    public String endpointProperty() {
        return endpointProperty;
    }
}
