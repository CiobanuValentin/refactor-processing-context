package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/** Pune rezultatele unei surse pe aplicatie. Leaga agregatorul de mapperele existente; lista poate fi goala. */
public interface ProxyResultMapper {

    ProxySource source();

    void apply(List<GeneralProxyObsDto> results, Application application, ProxyContext ctx);
}
