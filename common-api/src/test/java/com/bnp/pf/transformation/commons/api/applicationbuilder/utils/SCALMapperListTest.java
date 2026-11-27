package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalEntityDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonDto;
import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckScalPersonInitialDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;
import com.bnp.pf.transformation.commons.api.utils.ApplicationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.bnp.pf.transformation.commons.api.utils.FileUtil.readFileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste pentru varianta cu lista a mapperului. Folosesc aceleasi fisiere JSON si acelasi ApplicationUtil ca SCALMapperTest.
 */
class SCALMapperListTest {

    private static final String BUSINESS_KEY = "990003543191";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SCALMapper scalMapper = new SCALMapper();

    private static Application application() throws Exception {
        return ApplicationUtil.createFullApplication(BUSINESS_KEY);
    }

    private CheckScalPersonDto person() throws Exception {
        return objectMapper.readValue(readFileResource("checkScalPersonDto.json"), CheckScalPersonDto.class);
    }

    private CheckScalPersonInitialDto personInitial() throws Exception {
        return objectMapper.readValue(readFileResource("checkScalPersonInitialDto.json"), CheckScalPersonInitialDto.class);
    }

    private CheckScalEntityDto entity() throws Exception {
        return objectMapper.readValue(readFileResource("checkScalEntityDto.json"), CheckScalEntityDto.class);
    }

    private static int customerRisks(Application application) {
        return application.getLoan().getCustomers().get(0).getRiskAssessments().size();
    }

    private static int companyRisks(Application application) {
        return application.getLoan().getCompanies().get(0).getRiskAssessments().size();
    }

    @Test
    void person_nullOrEmptyList_changesNothing() throws Exception {
        Application application = application();
        scalMapper.mapScalPersonData(null, application);
        scalMapper.mapScalPersonData(List.of(), application);
        assertEquals(0, customerRisks(application));
    }

    @Test
    void person_nullRowsSkipped() throws Exception {
        Application application = application();
        scalMapper.mapScalPersonData(Arrays.asList(null, person()), application);
        assertEquals(1, customerRisks(application));
    }

    /** Ca in legacy: SCAL nu verifica daca rezultatul exista deja, deci fiecare rand adauga unul. */
    @Test
    void person_twoRows_twoResults() throws Exception {
        Application application = application();
        scalMapper.mapScalPersonData(List.of(person(), person()), application);
        assertEquals(2, customerRisks(application));
    }

    @Test
    void personInitial_listWithNullRow() throws Exception {
        Application application = application();
        scalMapper.mapScalPersonInitialData(Arrays.asList(personInitial(), null), application);
        assertEquals(1, customerRisks(application));
    }

    @Test
    void personInitial_emptyList_changesNothing() throws Exception {
        Application application = application();
        scalMapper.mapScalPersonInitialData(List.of(), application);
        assertEquals(0, customerRisks(application));
    }

    @Test
    void entity_listWithNullRow() throws Exception {
        Application application = application();
        scalMapper.mapScalEntityData(Arrays.asList(null, entity()), application);
        assertEquals(1, companyRisks(application));
    }

    @Test
    void entity_nullList_changesNothing() throws Exception {
        Application application = application();
        scalMapper.mapScalEntityData(null, application);
        assertEquals(0, companyRisks(application));
    }
}
