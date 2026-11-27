package com.bnp.pf.transformation.rvdrproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: importurile pentru Application, Loan si ApplicationService
// sunt presupuse - pastreaza-le pe cele din fisierul actual.
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheckService {

    private static final String BAD_REQUEST = "400";
    private static final String INTERNAL_ERROR = "500";

    private final ApplicationService applicationService;
    private final CreditCheckService creditCheckService;

    /** Se apeleaza dupa ProxyContextValidator.validate(ctx) == true; verificarea aplicatiei ramane, ca in cloud si ca in vis. */
    public void loanCheckProcess(ProxyContext ctx) {
        Application application = ctx.getApplication();
        if (application == null) {
            log.warn("No application in context for businessKey {}, skipping loan check", ctx.getBusinessKey());
            return;
        }

        if (!saveApplication(ctx)) {
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

        creditCheckService.checkCredit(ctx, loan);
    }

    private boolean saveApplication(ProxyContext ctx) {
        try {
            applicationService.saveApplicationRequest(ctx);
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
