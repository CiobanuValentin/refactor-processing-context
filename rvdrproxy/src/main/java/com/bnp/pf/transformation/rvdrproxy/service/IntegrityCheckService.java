package com.bnp.pf.transformation.rvdrproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru OrchestrationMessage, Application, Loan, ApplicationService si GsonInstance
// sunt presupuse - pastreaza-le pe cele din fisierul actual.
import com.bnp.pf.transformation.avro.OrchestrationMessage;
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.GsonInstance;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.model.Application;
import com.bnp.pf.transformation.commons.api.model.Loan;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheckService {

    private static final String BAD_REQUEST = "400";
    private static final String INTERNAL_ERROR = "500";

    private final ApplicationService applicationService;
    private final CreditCheckService creditCheckService;
    private final Gson gson = GsonInstance.createGson();

    /** Se apeleaza doar dupa ProxyContextValidator.validate(ctx) == true, deci aplicatia exista. */
    public void loanCheckProcess(ProxyContext ctx, OrchestrationMessage orchestrationMessage) {
        Application application = ctx.getApplication();

        if (!saveApplication(application, ctx, orchestrationMessage)) {
            return;
        }

        Loan loan = application.getLoan();
        if (loan == null) {
            log.error(LOAN_EMPTY);
            ctx.addError(BAD_REQUEST, LOAN_EMPTY);
            return;
        }

        // Fara clienti nu avem ce verifica: se iese cu OK, ca sa nu blocam procesul de scoring.
        if (CollectionUtils.isEmpty(loan.getCustomers())) {
            log.warn("No valid Customers found in Application with businessKey: {}", ctx.getBusinessKey());
            return;
        }

        // TODO: CreditCheckService.checkCredit trebuie sa primeasca ProxyContext si sa foloseasca ctx.addError(...)
        creditCheckService.checkCredit(ctx, loan);
    }

    private boolean saveApplication(Application application, ProxyContext ctx, OrchestrationMessage orchestrationMessage) {
        try {
            // TODO: ApplicationService.saveApplicationRequest are nevoie de o varianta cu ProxyContext
            applicationService.saveApplicationRequest(application, orchestrationMessage, gson.toJson(application), ctx);
            return true;
        } catch (Exception e) {
            String errorMessage = "Error occurred when trying to save application to database. Error message for Business Key "
                    + ctx.getBusinessKey() + " and flow ID " + ctx.getFlowId() + ": message: " + e.getMessage();
            log.error(errorMessage, e);
            ctx.addError(INTERNAL_ERROR, errorMessage);
            return false;
        }
    }
}
