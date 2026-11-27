package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckVisDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca BKRVISMapperTest.
 */
class BKRVISMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BKRVISMapper bkrvisMapper = new BKRVISMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckVisDto row() throws Exception {
        return objectMapper.readValue(readFileResource("CheckVisRequestAndResponse.json"), CheckVisDto.class);
    }

    private static int customerRisks(Application application) {
        return application.getLoan().getCustomers().get(0).getRiskAssessments().size();
    }

    private static int loanRisks(Application application) {
        return application.getLoan().getRiskAssessments().size();
    }

    /** Ca in legacy: fara randuri, fiecare client primeste un rezultat implicit, iar loan-ul doua (holder, co-holder). */
    @Test
    void emptyList_defaultResults() throws Exception {
        Application application = application();
        int loanBefore = loanRisks(application);
        bkrvisMapper.mapVISData(List.of(), application);
        assertEquals(1, customerRisks(application));
        assertEquals(loanBefore + 2, loanRisks(application));
    }

    @Test
    void nullList_sameAsEmptyList() throws Exception {
        Application application = application();
        int loanBefore = loanRisks(application);
        bkrvisMapper.mapVISData(null, application);
        assertEquals(1, customerRisks(application));
        assertEquals(loanBefore + 2, loanRisks(application));
    }

    @Test
    void oneRow_sameResultAsBefore() throws Exception {
        Application application = application();
        bkrvisMapper.mapVISData(List.of(row()), application);
        assertEquals(1, customerRisks(application));
    }

    /** Clientul primeste VIS o singura data (isVisExists); loan-ul primeste rezultatele pentru fiecare rand fara erori. */
    @Test
    void twoRows_customerOnce_loanResultsPerRow() throws Exception {
        Application one = application();
        int oneBefore = loanRisks(one);
        bkrvisMapper.mapVISData(List.of(row()), one);
        int perRow = loanRisks(one) - oneBefore;

        Application two = application();
        int twoBefore = loanRisks(two);
        bkrvisMapper.mapVISData(Arrays.asList(row(), null, row()), two);

        assertEquals(1, customerRisks(two));
        assertEquals(2 * perRow, loanRisks(two) - twoBefore);
    }
}
