package com.bnp.pf.transformation.commons.api.exception;

import java.util.Optional;

import lombok.Getter;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;

/** Inlocuitorul lui ProcessingFailedException pentru proxy-urile trecute pe ProxyContext. */
@Getter
public class ProxyProcessingFailedException extends RuntimeException {

    private final transient ProxyContext proxyContext;

    public ProxyProcessingFailedException(ProxyContext proxyContext, String message, Throwable cause) {
        super(message, cause);
        this.proxyContext = proxyContext;
    }

    public static Optional<ProxyContext> findIn(Throwable throwable) {
        for (Throwable c = throwable; c != null; c = c.getCause()) {
            if (c instanceof ProxyProcessingFailedException) {
                return Optional.ofNullable(((ProxyProcessingFailedException) c).proxyContext);
            }
        }
        return Optional.empty();
    }
}
