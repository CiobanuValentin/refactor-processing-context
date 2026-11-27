package com.bnp.pf.transformation.rvdrproxy.model;

import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

// TODO: importul pentru CreditCheckResponse (clasa generata din WSDL) lipseste - pastreaza-l pe cel actual.
import com.bnp.pf.transformation.commons.api.error.Error;

// Neschimbat.
@Data
@RequiredArgsConstructor
public class CreditCheckResult {
    private final CreditCheckResponse response;
    private final List<Error> errors;

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
