package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckRvdrCreditDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca RVDRMapperTest.
 */
class RVDRMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RVDRMapper rvdrMapper = new RVDRMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckRvdrCreditDto row() throws Exception {
        return objectMapper.readValue(readFileResource("CheckRvdrRequestAndResponse.json"), CheckRvdrCreditDto.class);
    }

    private static int customerRisks(Application application) {
        return application.getLoan().getCustomers().get(0).getRiskAssessments().size();
    }

    @Test
    void nullOrEmptyList_changesNothing() throws Exception {
        Application application = application();
        rvdrMapper.mapRVDRData(null, application);
        rvdrMapper.mapRVDRData(List.of(), application);
        assertEquals(0, customerRisks(application));
    }

    @Test
    void oneRow_sameResultAsBefore() throws Exception {
        Application application = application();
        rvdrMapper.mapRVDRData(List.of(row()), application);
        assertEquals(1, customerRisks(application));
    }

    @Test
    void nullRowsAreSkipped() throws Exception {
        Application application = application();
        rvdrMapper.mapRVDRData(Arrays.asList(null, row(), null), application);
        assertEquals(1, customerRisks(application));
    }

    /** Ca in legacy: fiecare rand care se potriveste adauga un rezultat (fara deduplicare). */
    @Test
    void twoMatchingRows_twoResults() throws Exception {
        Application application = application();
        rvdrMapper.mapRVDRData(List.of(row(), row()), application);
        assertEquals(2, customerRisks(application));
    }

    @Test
    void customersNull_noException() throws Exception {
        Application application = application();
        application.getLoan().setCustomers(null);
        rvdrMapper.mapRVDRData(List.of(row()), application);
        assertNull(application.getLoan().getCustomers());
    }
}
