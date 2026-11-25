package com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation;

import java.util.List;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Pune rezultatele unei surse pe aplicatie. Mapperele existente (SCALMapper, BKRCKIMapper...) il implementeaza
 * pe rand; lista poate fi goala. Problemele se raporteaza cu ctx.addError, nu cu rezultate fabricate.
 */
public interface ProxyResultMapper {

    ProxySource source();

    void apply(List<ProxyResultDto> results, Application application, ProxyContext ctx);
}
