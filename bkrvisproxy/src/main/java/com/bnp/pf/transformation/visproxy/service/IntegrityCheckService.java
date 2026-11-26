package com.bnp.pf.transformation.visproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

/** Aceiasi pasi ca in cloud: verificarea aplicatiei, salvarea ei, verificarea loan-ului si a partilor, apoi verificarea VIS. */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrityCheckService {
    private static final String BAD_REQUEST = "400";
    private static final String INTERNAL_ERROR = "500";

    private final ApplicationService applicationService;
    private final VisCheckService visCheckService;

    /** Se apeleaza dupa ProxyContextValidator.validate(ctx) == true; verificarea aplicatiei ramane, ca in cloud. */
    public void loanCheckProcess(ProxyContext ctx) {
        Application application = ctx.getApplication();
        if (application == null) {
            log.error("No application in context for businessKey {}, skipping loan check", ctx.getBusinessKey());
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

        if (CollectionUtils.isEmpty(loan.getCustomers()) && CollectionUtils.isEmpty(loan.getCompanies())) {
            String errorMessage = "Unable to process VIS data. "
                    + "No valid Customers or Companies found in Application with businessKey: " + ctx.getBusinessKey();
            log.error(errorMessage);
            ctx.addError(BAD_REQUEST, errorMessage);
            return;
        }

        visCheckService.checkVis(ctx, loan);
    }

    private boolean saveApplication(ProxyContext ctx) {
        try {
            applicationService.saveApplicationRequest(ctx);
            return true;
        } catch (Exception e) {
            String errorMessage = "Error occurred when trying to save application to database. Error message for businessKey "
                    + ctx.getBusinessKey() + " and flowId " + ctx.getFlowId() + ": message: " + e.getMessage();
            log.error(errorMessage, e);
            ctx.addError(INTERNAL_ERROR, errorMessage);
            return false;
        }
    }
}
