package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

/** Sursele din care un proxy agregator (rat, sed) isi ia rezultatele celorlalte proxy-uri. */
public enum ProxySource {
    SCAL_PERSON,
    SCAL_PERSON_INITIAL,
    SCAL_ENTITY,
    DNB,
    CKI,
    RVDR,
    VIS,
    EVA_COMPANY,
    EVA_PERSON,
    EVENT_INDICATOR,
    RAT_COMPLIANCE("rat.compliance");

    // TODO: completeaza si pentru celelalte surse, daca au un skip command
    private final String skipCommand;

    ProxySource() {
        this(null);
    }

    ProxySource(String skipCommand) {
        this.skipCommand = skipCommand;
    }

    /** Valoarea din Application.skipCommand care scoate sursa din agregare; null daca nu exista. */
    public String skipCommand() {
        return skipCommand;
    }
}
