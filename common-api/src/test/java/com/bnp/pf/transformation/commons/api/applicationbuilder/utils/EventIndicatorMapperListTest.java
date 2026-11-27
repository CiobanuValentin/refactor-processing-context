package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEventIndicatorDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca EventIndicatorMapperTest.
 */
class EventIndicatorMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EventIndicatorMapper eventIndicatorMapper = new EventIndicatorMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckEventIndicatorDto row() throws Exception {
        return objectMapper.readValue(readFileResource("EventTriggerResponse.json"), CheckEventIndicatorDto.class);
    }

    @Test
    void oneRow_sameResultAsBefore() throws Exception {
        Application application = application();
        eventIndicatorMapper.mapEventIndicatorData(List.of(row()), application);
        assertEquals("30165126", application.getLoan().getCompanies().get(0).getLegalRegistrationNumber());
    }

    @Test
    void nullRowsSkipped() throws Exception {
        Application application = application();
        eventIndicatorMapper.mapEventIndicatorData(Arrays.asList(null, row()), application);
        assertEquals("30165126", application.getLoan().getCompanies().get(0).getLegalRegistrationNumber());
    }

    @Test
    void nullOrEmptyList_companyUnchanged() throws Exception {
        Application application = application();
        Object before = application.getLoan().getCompanies().get(0).getResultStatus();
        eventIndicatorMapper.mapEventIndicatorData(null, application);
        eventIndicatorMapper.mapEventIndicatorData(List.of(), application);
        assertEquals(before, application.getLoan().getCompanies().get(0).getResultStatus());
    }
}
