package com.bnp.pf.transformation.commons.api.generalproxyobs.service;

import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.APPLICATION;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND;
import static com.bnp.pf.transformation.commons.api.util.ProxyObsConstant.CORRELATION_ID_NOT_PROVIDED;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.model.Application;

/**
 * Validarea de inceput, comuna tuturor proxy-urilor. Fail-fast ca in legacy:
 * prima verificare picata adauga o singura eroare si opreste validarea.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyContextValidator {

    // TODO: aliniaza textele cu CORR_ID_EMPTY / ORCH_MSG_EMPTY / BUSS_KEY_EMPTY / FLOW_ID_EMPTY din legacy
    public static final String ORCH_MSG_EMPTY = "Orchestration message is empty";
    public static final String BUSS_KEY_EMPTY = "Business key is empty";
    public static final String FLOW_ID_EMPTY = "Flow ID is empty";

    private static final String BAD_REQUEST = "400";

    private final Gson gson;

    /** @return true daca mesajul poate fi procesat; la false, eroarea e deja in context. */
    public boolean validate(ProxyContext ctx) {
        if (StringUtils.isEmpty(ctx.getCorrelation())) {
            return fail(ctx, CORRELATION_ID_NOT_PROVIDED);
        }
        if (!ctx.isSignalPresent()) {
            return fail(ctx, ORCH_MSG_EMPTY);
        }
        ctx.setApplication(parseApplication(ctx));
        if (ctx.getApplication() == null) {
            return fail(ctx, APPLICATION_WITH_BUSINESS_KEY_NOT_FOUND);
        }
        if (StringUtils.isEmpty(ctx.getBusinessKey())) {
            return fail(ctx, BUSS_KEY_EMPTY);
        }
        if (StringUtils.isEmpty(ctx.getFlowId())) {
            return fail(ctx, FLOW_ID_EMPTY);
        }
        return true;
    }

    private Application parseApplication(ProxyContext ctx) {
        String json = ctx.property(APPLICATION);
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return gson.fromJson(json, Application.class);
        } catch (JsonSyntaxException e) {
            log.error("Invalid application JSON for flow id {}: {}", ctx.getFlowId(), e.getMessage());
            return null;
        }
    }

    private boolean fail(ProxyContext ctx, String message) {
        log.error("{} - flow id {}", message, ctx.getFlowId());
        ctx.addError(BAD_REQUEST, message);
        return false;
    }
}
