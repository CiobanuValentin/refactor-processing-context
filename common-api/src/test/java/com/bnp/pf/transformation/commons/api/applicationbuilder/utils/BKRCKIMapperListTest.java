package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckCkiPersonDto;
import com.bnp.pf.transformation.commons.api.message.RiskAssessment;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca BKRCKIMapperTest.
 */
class BKRCKIMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BKRCKIMapper bkrckiMapper = new BKRCKIMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckCkiPersonDto row() throws Exception {
        return objectMapper.readValue(readFileResource("CheckCkiPersonRequestAndResponse.json"), CheckCkiPersonDto.class);
    }

    private static long customerCki(Application application) {
        return application.getLoan().getCustomers().get(0).getRiskAssessments().stream()
                .filter(r -> BKRCKIMapper.CLASS_INSTR_CD.equals(r.getClassificationInstrumentCode())).count();
    }

    private static List<RiskAssessment> loanRisks(Application application) {
        return application.getLoan().getRiskAssessments();
    }

    /** Ca in legacy: fara randuri, loan-ul primeste cele doua rezultate goale (holder, co-holder). */
    @Test
    void emptyList_twoEmptyLoanResults() throws Exception {
        Application application = application();
        int before = loanRisks(application).size();
        bkrckiMapper.mapCKIData(List.of(), application);
        assertEquals(before + 2, loanRisks(application).size());
        assertEquals(BKRCommons.RESULT_HOLDER, loanRisks(application).get(before).getResultType());
        assertEquals(BKRCommons.RESULT_CO_HOLDER, loanRisks(application).get(before + 1).getResultType());
    }

    @Test
    void nullList_sameAsEmptyList() throws Exception {
        Application application = application();
        int before = loanRisks(application).size();
        bkrckiMapper.mapCKIData(null, application);
        assertEquals(before + 2, loanRisks(application).size());
    }

    /**
     * Diferenta fata de varianta cu un dto: rezultatele de pe loan se adauga O SINGURA DATA, dupa bucla,
     * oricate randuri ar fi. Clientul primeste CKI o singura data (isCkiExists).
     */
    @Test
    void severalRows_loanResultsAddedOnce() throws Exception {
        Application one = application();
        int oneBefore = loanRisks(one).size();
        bkrckiMapper.mapCKIData(List.of(row()), one);
        int oneAdded = loanRisks(one).size() - oneBefore;

        Application three = application();
        int threeBefore = loanRisks(three).size();
        bkrckiMapper.mapCKIData(Arrays.asList(row(), null, row(), row()), three);

        assertEquals(oneAdded, loanRisks(three).size() - threeBefore);
        assertEquals(customerCki(one), customerCki(three));
    }
}
