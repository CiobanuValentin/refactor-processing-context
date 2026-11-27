package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importuri: aici sunt doar cele sigure. Pastreaza blocul de importuri din fisierul existent (inclusiv importurile
// statice pentru isSameDateOfBirth, RESULT, RESULT_TRUE, HOLDER, LOW, ...) si inlocuieste doar corpul clasei.
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bnp.pf.transformation.commons.api.applicationbuilder.swagger.model.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckEventIndicatorDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: mapEventIndicatorData primeste lista, ca in legacy. Fiecare rand trece prin mapCompanies (corpul din
 * cloud); randurile null sunt sarite, ca in legacy (in cloud, un dto null dadea NPE). Nu se adauga nimic pe loan.
 */
@Component
@Slf4j
public class EventIndicatorMapper {

    public void mapEventIndicatorData(List<CheckEventIndicatorDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            AtomicBoolean processingFlag = new AtomicBoolean(false);

            log.debug("Mapping EventIndicator Data started for business key {}", application.getBusinessKey());

            for (CheckEventIndicatorDto checkEventIndicatorDto : matches) {
                if (checkEventIndicatorDto != null) {
                    mapCompanies(checkEventIndicatorDto, application, processingFlag);
                }
            }

            if (processingFlag.get()) {
                log.debug(" Mapping EventIndicator Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for EventIndicator, business key {}", application.getBusinessKey());
            }
        }
    }

    private void mapCompanies(CheckEventIndicatorDto checkEventIndicatorDto, Application application, AtomicBoolean processingFlag) {
        List<Organisation> companies = application.getLoan().getCompanies();
        var request = checkEventIndicatorDto.getRequest();
        var response = checkEventIndicatorDto.getResponse();

        if (request != null && CollectionUtils.isNotEmpty(companies)) {
            companies.forEach(company -> {

                if (company.getLegalRegistrationNumber().equals(request.getCompany().getCocNumber())
                        && company.getCountryOfResidence().equals(request.getCompany().getCountryCode())) {
                    processResponce(company, response, processingFlag);
                }
            });
        }
    }

    private void processResponce(Organisation company, ResponseRoot response, AtomicBoolean processingFlag) {
        if (response != null) {
            CompanyResponse companyResponse = response.getCompanyResponse();
            if (companyResponse != null) {
                DUNSResponse dunsresponse = companyResponse.getDunsresponse();
                if (dunsresponse != null) {
                    EventTriggerResponse eventTriggerResponse = dunsresponse.getEventTriggerResponse();
                    setOutput(company, eventTriggerResponse, processingFlag);
                }
            }
        }
    }

    private void setOutput(Organisation company, EventTriggerResponse eventTriggerResponse, AtomicBoolean processingFlag) {
        if (eventTriggerResponse != null
                && eventTriggerResponse.getEventTriggerResult() != null) {
            log.info("company before set all the fields is: {}", company);
            var triggerResult = eventTriggerResponse.getEventTriggerResult();

            company.setOutput01(getBooleanVal(triggerResult.getOutput01()));
            company.setOutput02(getBooleanVal(triggerResult.getOutput02()));
            company.setOutput03(getBooleanVal(triggerResult.getOutput03()));
            company.setOutput04(getBooleanVal(triggerResult.getOutput04()));
            company.setOutput05(getBooleanVal(triggerResult.getOutput05()));
            company.setOutput06(getBooleanVal(triggerResult.getOutput06()));
            company.setOutput07(getBooleanVal(triggerResult.getOutput07()));
            company.setOutput08(getBooleanVal(triggerResult.getOutput08()));
            company.setOutput09(getBooleanVal(triggerResult.getOutput09()));
            company.setOutput10(getBooleanVal(triggerResult.getOutput10()));
            company.setOutput11(getBooleanVal(triggerResult.getOutput11()));
            company.setOutput12(getBooleanVal(triggerResult.getOutput12()));
            company.setOutput13(getBooleanVal(triggerResult.getOutput13()));
            company.setOutput14(getBooleanVal(triggerResult.getOutput14()));
            company.setOutput15(getBooleanVal(triggerResult.getOutput15()));
            company.setOutput16(getBooleanVal(triggerResult.getOutput16()));
            company.setOutput17(getBooleanVal(triggerResult.getOutput17()));
            company.setOutput18(getBooleanVal(triggerResult.getOutput18()));
            company.setOutput19(getBooleanVal(triggerResult.getOutput19()));
            company.setOutput20(getBooleanVal(triggerResult.getOutput20()));
            company.setOutput21(getBooleanVal(triggerResult.getOutput21()));
            company.setOutput22(getBooleanVal(triggerResult.getOutput22()));
            company.setOutput23(getBooleanVal(triggerResult.getOutput23()));
            company.setOutput24(getBooleanVal(triggerResult.getOutput24()));
            company.setOutput25(getBooleanVal(triggerResult.getOutput25()));
            company.setAverageCount(triggerResult.getAverageCount());
            company.setStandardDeviation(triggerResult.getStandardDeviation());
            company.setMinCount(triggerResult.getMinCount());
            company.setMaxCount(triggerResult.getMaxCount());
            company.setNumberRows(triggerResult.getNumberRows());
            company.setActive(triggerResult.isIsActive() != null && Boolean.TRUE.equals(triggerResult.isIsActive()));
            if (triggerResult.getAddDate() != null) {
                company.setAddDate(DateUtil.longToLocalDateTime(triggerResult.getAddDate()));
            }
            company.setResultStatus(triggerResult.getResultStatus());

            processingFlag.set(true);
            log.info("company after set all the fields is: {}", company);

        }
    }

    private boolean getBooleanVal(String output) {
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(output);
    }

    public LocalDateTime getLocalDateTime(XMLGregorianCalendar calendar) {
        return LocalDateTime.of(getVal(calendar.getYear()), getVal(calendar.getMonth()), getVal(calendar.getDay()), getVal(calendar.getHour()), getVal(calendar.getMinute()), getVal(calendar.getSecond()));
    }

    private Integer getVal(Integer time) {
        return time != null ? time : 0;
    }
}
