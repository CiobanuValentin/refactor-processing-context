package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un rezultat al unui proxy pentru o singura persoana sau companie din aplicatie. */
@Getter
@Setter
@NoArgsConstructor
public class ProxyResultDto {

    /** Business key-ul persoanei sau companiei; null cand rezultatul e pe toata aplicatia. */
    private String partyBusinessKey;
    private String requestJson;
    private String responseJson;
    private String errorHandlerJson;
}
