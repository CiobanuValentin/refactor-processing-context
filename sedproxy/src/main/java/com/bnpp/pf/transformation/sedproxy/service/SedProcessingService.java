package com.bnpp.pf.transformation.sedproxy.service;

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
import com.bnp.pf.transformation.commons.api.model.Loan;

/**
 * Pasii sed dupa validare: agregare, salvare, verificarea loan-ului, apelul catre DES.
 * Inlocuieste SedNullOrEmptyCheckService si ApplicationBuilderPG din sed. Nu are stare.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SedProcessingService {

    private static final String BAD_REQUEST = "400";

    /** Aceeasi ordine ca in ApplicationBuilderPG, plus rezultatul rat, care lipsea. */
    static final List<ProxySource> SOURCES = List.of(
            ProxySource.SCAL_PERSON, ProxySource.SCAL_PERSON_INITIAL, ProxySource.SCAL_ENTITY,
            ProxySource.DNB, ProxySource.CKI, ProxySource.RVDR, ProxySource.VIS,
            ProxySource.EVA_COMPANY, ProxySource.EVA_PERSON, ProxySource.EVENT_INDICATOR,
            ProxySource.RAT_COMPLIANCE);

    private final ApplicationAggregator applicationAggregator;
    private final ApplicationService applicationService;
    private final SedProcessorService sedProcessorService;

    public void process(ProxyContext ctx) {
        /*
         * TODO (Arval NL) - de clarificat cu echipa Arval / arval-input-api:
         * Codul de pana acum sarea agregarea pentru flow-urile cu prefixul Arval NL, iar legacy o facea
         * tocmai pentru ele (applicationBuilder.buildApplication). Aici agregarea ruleaza pentru toate flow-urile.
         * De intrebat:
         *  1. Proxy-urile sursa (cki, vis, eva, rvdr, scal, dnb, event indicator, rat) ruleaza si pe flow-urile
         *     Arval NL si isi salveaza rezultatele cu acelasi businessKey si flowId pe care le primeste sed?
         *  2. Daca nu, de unde vin risk assessment-urile pentru Arval: le pune arval-input-api direct in
         *     JSON-ul aplicatiei?
         *  3. Daca vin deja in JSON, agregarea nu trebuie sa ruleze pentru Arval, altfel rezultatele se dubleaza:
         *     se revine la "if (!ctx.isArvalNlFlow())" in jurul apelului de mai jos.
         */
        boolean aggregated = applicationAggregator.enrich(ctx, SOURCES);

        // Se salveaza aplicatia cu rezultatele agregate: snippets/common-api/ApplicationService-changes.java
        applicationService.saveApplicationRequest(ctx);

        if (!aggregated) {
            return;
        }
        Loan loan = ctx.getApplication().getLoan();
        if (loan == null) {
            log.error(LOAN_EMPTY);
            ctx.addError(BAD_REQUEST, LOAN_EMPTY);
            return;
        }
        if (CollectionUtils.isEmpty(loan.getCustomers()) && CollectionUtils.isEmpty(loan.getCompanies())) {
            String message = "Unable to add SED data. There are no customers or companies in the application with businessKey: "
                    + ctx.getBusinessKey();
            log.error(message);
            ctx.addError(BAD_REQUEST, message);
            return;
        }
        sedProcessorService.process(ctx);
    }
}
