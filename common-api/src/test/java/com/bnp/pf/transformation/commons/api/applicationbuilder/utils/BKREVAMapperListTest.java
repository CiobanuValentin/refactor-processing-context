package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaCompanyDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEvaPersonDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca BKREVAMapperTest.
 */
class BKREVAMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BKREVAMapper bkrevaMapper = new BKREVAMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckEvaPersonDto person() throws Exception {
        return objectMapper.readValue(readFileResource("CheckEvaRequestAndResponse.json"), CheckEvaPersonDto.class);
    }

    private CheckEvaCompanyDto company() throws Exception {
        return objectMapper.readValue(readFileResource("CheckEvaCompanyRequestAndResponse.json"), CheckEvaCompanyDto.class);
    }

    private static int customerRisks(Application application) {
        return application.getLoan().getCustomers().get(0).getRiskAssessments().size();
    }

    private static int loanRisks(Application application) {
        return application.getLoan().getRiskAssessments().size();
    }

    @Test
    void person_oneRow_sameResultAsBefore() throws Exception {
        Application application = application();
        bkrevaMapper.mapEVAPersonData(List.of(person()), application);
        assertEquals(1, customerRisks(application));
        assertEquals("bkr.eva", application.getLoan().getCustomers().get(0).getRiskAssessments().get(0).getClassificationInstrumentCode());
    }

    @Test
    void person_nullOrEmptyList_changesNothing() throws Exception {
        Application application = application();
        int loanBefore = loanRisks(application);
        bkrevaMapper.mapEVAPersonData(null, application);
        bkrevaMapper.mapEVAPersonData(List.of(), application);
        assertEquals(0, customerRisks(application));
        assertEquals(loanBefore, loanRisks(application));
    }

    /** Clientul primeste EVA o singura data (isEvaExists); loan-ul primeste rezultatele pentru fiecare rand fara erori. */
    @Test
    void person_twoRows_customerOnce_loanResultsPerRow() throws Exception {
        Application one = application();
        int oneBefore = loanRisks(one);
        bkrevaMapper.mapEVAPersonData(List.of(person()), one);
        int perRow = loanRisks(one) - oneBefore;

        Application two = application();
        int twoBefore = loanRisks(two);
        bkrevaMapper.mapEVAPersonData(Arrays.asList(person(), null, person()), two);

        assertEquals(1, customerRisks(two));
        assertEquals(2 * perRow, loanRisks(two) - twoBefore);
    }

    @Test
    void company_twoRows_loanResultsPerRow() throws Exception {
        Application one = application();
        int oneBefore = loanRisks(one);
        bkrevaMapper.mapEVACompanyData(List.of(company()), one);
        int perRow = loanRisks(one) - oneBefore;

        Application two = application();
        int twoBefore = loanRisks(two);
        bkrevaMapper.mapEVACompanyData(Arrays.asList(company(), null, company()), two);

        assertEquals(2 * perRow, loanRisks(two) - twoBefore);
    }
}
