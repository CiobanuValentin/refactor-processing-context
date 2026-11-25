package com.bnpp.pf.transformation.ratproxy.service;

import static com.bnp.pf.transformation.commons.api.generalproxyobs.util.ProxyObsConstant.LOAN_EMPTY;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ApplicationAggregator;
import com.bnp.pf.transformation.commons.api.generalproxyobs.aggregation.ProxySource;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.generalproxyobs.service.ApplicationService;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.message.input.Loan;

/**
 * Pasii rat dupa validare: agregare, verificarea loan-ului, sanction, compliance, score, salvare.
 * Inlocuieste RatNullOrEmptyCheckService si ApplicationBuilderPG din rat. Nu are stare:
 * aplicatia sta in context, nu intr-un camp al bean-ului.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatProcessingService {

    private static final String BAD_REQUEST = "400";

    /** Aceleasi surse ca in cloud, in ordinea din legacy (ApplicationBuilder, common-api-1.23.jar). */
    static final List<ProxySource> SOURCES = List.of(
            ProxySource.SCAL_PERSON, ProxySource.SCAL_PERSON_INITIAL, ProxySource.SCAL_ENTITY,
            ProxySource.DNB, ProxySource.RVDR, ProxySource.VIS,
            ProxySource.EVA_PERSON, ProxySource.EVA_COMPANY, ProxySource.CKI, ProxySource.EVENT_INDICATOR);

    private final ApplicationAggregator applicationAggregator;
    private final RatMappingService ratMappingService;
    private final ApplicationService applicationService;

    public void process(ProxyContext ctx) {
        // TODO (Arval NL): aceleasi intrebari ca in SedProcessingService - daca rezultatele vin deja
        // in JSON-ul aplicatiei pentru flow-urile Arval NL, agregarea trebuie sarita pentru ele.
        if (!applicationAggregator.enrich(ctx, SOURCES)) {
            return;
        }
        Application application = ctx.getApplication();
        Loan loan = application.getLoan();
        if (loan == null) {
            log.error(LOAN_EMPTY);
            ctx.addError(BAD_REQUEST, LOAN_EMPTY);
            return;
        }
        if (CollectionUtils.isEmpty(loan.getCustomers()) && CollectionUtils.isEmpty(loan.getCompanies())) {
            String message = "Unable to add RAT data. No valid Customers or Companies found in Application with businessKey: "
                    + ctx.getBusinessKey();
            log.error(message);
            ctx.addError(BAD_REQUEST, message);
            return;
        }

        // Un pas esuat opreste pasii urmatori; eroarea e deja in context si raspunsul va fi KO (ca in legacy).
        boolean completed = ratMappingService.processSanction(ctx);
        // TODO: de confirmat cu echipa. In legacy (RATProxyListener) pasii erau doar sanction si score:
        // processRatComplianceDataAsync nu era apelat, iar skipCommand nu exista. Compliance si conditia de skip
        // au aparut in cloud; sunt pastrate aici pana se confirma ca au fost adaugate intentionat.
        if (completed && !isComplianceSkipped(application)) {
            completed = ratMappingService.processCompliance(ctx) && ratMappingService.applyScore(ctx);
        }

        // FIX fata de codul actual: aplicatia se salveaza si cand rat.compliance e sarit (inainte nu se salva deloc).
        if (completed) {
            applicationService.saveApplicationRequest(ctx);
        }
    }

    private static boolean isComplianceSkipped(Application application) {
        List<String> skipCommands = application.getSkipCommand();
        return skipCommands != null && skipCommands.contains(RatMappingService.RAT_COMPLIANCE);
    }
}
