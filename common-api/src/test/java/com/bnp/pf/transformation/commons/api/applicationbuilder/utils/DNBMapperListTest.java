package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckDnbCompanyDto;
import com.bnp.pf.transformation.commons.api.message.input.Organisation;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca DNBMapperTest.
 */
class DNBMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DNBMapper dnbMapper = new DNBMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckDnbCompanyDto row() throws Exception {
        return objectMapper.readValue(readFileResource("CheckDNBRequestAndResponse.json"), CheckDnbCompanyDto.class);
    }

    private CheckDnbCompanyDto principals() throws Exception {
        return objectMapper.readValue(readFileResource("CheckDNBRequestAndResponsePrinciples.json"), CheckDnbCompanyDto.class);
    }

    @Test
    void oneRow_sameResultAsBefore() throws Exception {
        Application application = application();
        dnbMapper.mapDNBData(List.of(row()), application);

        Organisation organisation = application.getLoan().getCompanies().get(0);
        assertEquals("72432101", organisation.getLegalRegistrationNumber());
        assertTrue(organisation.isCompanyFound());
        assertEquals(20201231, organisation.getDnbLatestAccountFiledDate());
        assertEquals(12, organisation.getDnbLatestAccountFiledPeriod());
    }

    @Test
    void nullRowsSkipped() throws Exception {
        Application application = application();
        dnbMapper.mapDNBData(Arrays.asList(null, row(), null), application);
        assertTrue(application.getLoan().getCompanies().get(0).isCompanyFound());
    }

    @Test
    void multiplePrincipals_inList() throws Exception {
        Application application = application();
        dnbMapper.mapDNBData(List.of(principals()), application);
        assertEquals("01043556", application.getLoan().getCompanies().get(0).getLegalRegistrationNumber());
        assertTrue(application.getLoan().getCompanies().get(0).isCompanyFound());
    }

    @Test
    void nullOrEmptyList_companyUnchanged() throws Exception {
        Application application = application();
        String before = application.getLoan().getCompanies().get(0).getLegalRegistrationNumber();
        dnbMapper.mapDNBData(null, application);
        dnbMapper.mapDNBData(List.of(), application);
        assertEquals(before, application.getLoan().getCompanies().get(0).getLegalRegistrationNumber());
    }
}
