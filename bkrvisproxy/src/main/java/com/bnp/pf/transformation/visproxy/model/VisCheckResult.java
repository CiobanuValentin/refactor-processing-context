package com.bnp.pf.transformation.visproxy.model;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import com.bnp.pf.transformation.commons.api.error.Error;

import urn.bkr.wsdl.vis.VisResponse;

// Neschimbat.
@Data
@RequiredArgsConstructor
public class VisCheckResult {
    private final VisResponse response;
    private final List<Error> errors;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
