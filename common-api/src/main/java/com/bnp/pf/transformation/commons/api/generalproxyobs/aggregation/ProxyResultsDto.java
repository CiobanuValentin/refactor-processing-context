package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Raspunsul unui endpoint de rezultate: cheile aplicatiei o singura data si cate un rezultat per party. */
@Getter
@Setter
@NoArgsConstructor
public class ProxyResultsDto {

    private String businessKey;
    private String flowId;
    private List<ProxyResultDto> results = new ArrayList<>();
}
