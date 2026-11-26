package com.bnp.pf.transformation.commons.api.applicationbuilder.utils;

// Importurile nu erau vizibile in poze: de corectat din IDE (Optimize Imports).
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.applicationbuilder.dto.CheckDnbCompanyDto;
import com.bnp.pf.transformation.commons.api.message.input.Application;

/**
 * Regenerat din cloud (common-api, branch arval-refactor-last, poze 26.11) si comparat cu legacy (common-api-1.23).
 *
 * Fata de cloud: mapDNBData primeste lista, ca in legacy. Fiecare rand trece prin mapCompanies (corpul din cloud);
 * randurile null sunt sarite, ca in legacy (in cloud, un dto null dadea NPE). Nu se adauga nimic pe loan.
 *
 * Pastrat din cloud, fara echivalent in legacy: setDnbLatestAccount si null-check-ul pe getPersonalDetails().
 */
@Component
@Slf4j
public class DNBMapper {

    private static final String DIRECTOR = "Directeur";
    private static final String TRADE_REGISTRATION = "TradeRegisterNumber";

    public void mapDNBData(List<CheckDnbCompanyDto> matches, Application application) {
        if (matches != null && !matches.isEmpty()) {
            AtomicBoolean processingFlag = new AtomicBoolean(false);

            log.debug(" Mapping DNB Data started for business key {}", application.getBusinessKey());

            for (CheckDnbCompanyDto checkDnbCompanyDto : matches) {
                if (checkDnbCompanyDto != null) {
                    mapCompanies(checkDnbCompanyDto, application, processingFlag);
                }
            }

            if (processingFlag.get()) {
                log.debug(" Mapping DNB Data success for business key {}", application.getBusinessKey());
            } else {
                log.debug(" No matching data was found for DNB, business key {}", application.getBusinessKey());
            }
        }
    }

    private void mapCompanies(CheckDnbCompanyDto checkDnbCompanyDto, Application application, AtomicBoolean processingFlag) {
        List<Organisation> companies = application.getLoan().getCompanies();

        RequestCompany request = checkDnbCompanyDto.getRequest();
        ResponseRoot response = checkDnbCompanyDto.getResponse();

        if (request != null && CollectionUtils.isNotEmpty(companies)) {
            companies.forEach(company -> {
                Company requestCompany = request.getCompany();

                boolean c1 = company.getLegalRegistrationNumber().equals(requestCompany.getCocNumber());
                boolean c2 = company.getCountryOfResidence().equals(requestCompany.getCountryCode());

                if (c1 && c2) {
                    log.info("company before set all the fields is: {}", company);
                    processResponse(company, response);
                    processingFlag.set(true);
                    log.info("company after set all the fields is: {}", company);
                }
            });
        }
    }

    private void processResponse(Organisation company, ResponseRoot response) {
        if (response != null) {
            CompanyResponse companyResponse = response.getCompanyResponse();
            if (companyResponse != null) {
                DUNSResponse dunsresponse = companyResponse.getDunsresponse();
                if (dunsresponse != null) {
                    CCDPResponse ccdpResponse = dunsresponse.getCcdpResponse();
                    if (ccdpResponse != null && CollectionUtils.isNotEmpty(ccdpResponse.getCcdpList())) {
                        CCDP ccdp = ccdpResponse.getCcdpList().get(0);
                        processWithCcdp(company, ccdp, companyResponse, response);
                    }
                }
            }
        }
    }

    private void processWithCcdp(Organisation company, CCDP ccdp, CompanyResponse companyResponse, ResponseRoot response) {
        if (ccdp != null && ccdp.getRecordPayload() != null) {

            var payload = ccdp.getRecordPayload();
            var header = ccdp.getRecordHeader();
            var companySummary = payload.getCompanySummary();

            final Boolean principalsInformationAvailabilityIndicator = header.getRecordStatusNotification().getRequestedSectionsAvailability().isPrincipalsInformationAvailabilityIndicator();

            setDirectorAge(company, principalsInformationAvailabilityIndicator, payload);
            processDnbAssessmentOverview(company, companySummary);
            setNumberEmployees(company, payload);
            setIndustryCode(company, companySummary);
            setLegalForm(company, companySummary);
            setRegistrationNumber(company, companySummary);
            setIndicators(company, companySummary);
            setCompanyAge(company, companySummary);
            setDnbLatestAccount(company, payload);

            //True if there is a response either from cache or from external API call
            company.setCompanyFound(companyResponse != null && response.getErrors() == null);
        }
    }

    private void setDnbLatestAccount(Organisation company, RecordPayload payload) {
        // TODO: conditia iese din ecran in poza (IMG_7753); finalul ".getFinancialStatementHeader() != null" e presupus.
        if (payload.getFinancials() != null && payload.getFinancials().getFinancialInformationEntry() != null
                && CollectionUtils.isNotEmpty(payload.getFinancials().getFinancialInformationEntry())
                && payload.getFinancials().getFinancialInformationEntry().get(0).getFinancialStatementHeader() != null) {
            FinancialStatementHeaderType financialStatementHeader = payload.getFinancials().getFinancialInformationEntry().get(0).getFinancialStatementHeader();
            DNBDate latestAccountFiledDate = financialStatementHeader.getLatestAccountsFiledDate();
            Integer latestAccountFiledDateIntegerValue = null;

            if (latestAccountFiledDate != null) {
                String latestAccountFiledDateValue = latestAccountFiledDate.getValue();

                if (latestAccountFiledDateValue != null) {
                    latestAccountFiledDateIntegerValue = Integer.parseInt(latestAccountFiledDateValue.replace("-", ""));
                }
            }

            company.setDnbLatestAccountFiledDate(latestAccountFiledDateIntegerValue);
            company.setDnbLatestAccountFiledPeriod(financialStatementHeader.getMonthsCovered());
        }
    }

    private void setCompanyAge(Organisation company, CompanySummary companySummary) {
        if (companySummary.getCompanyOverview() != null && companySummary.getCompanyOverview().getBusinessStartDate() != null) {
            company.setCompanyAge(DateUtil.parseToLocalDateTime(companySummary.getCompanyOverview().getBusinessStartDate().getValue(), "yyyy-MM-dd"));
        }
    }

    private void setIndicators(Organisation company, CompanySummary companySummary) {
        PublicFilingsSummaryType publicFilingsOverview = companySummary.getPublicFilingsOverview();
        if (publicFilingsOverview != null) {
            //Business Failure Indicator
            company.setBusinessFailureIndicator(publicFilingsOverview.isBusinessFailureIndicator());

            //Ceased Trading Indicator
            company.setCeasedTradingIndicator(publicFilingsOverview.isCeasedTradingIndicator());

            //Liquidation indicator
            company.setLiquidationIndicator(publicFilingsOverview.isLiquidationIndicator());
        }
    }

    private void setRegistrationNumber(Organisation company, CompanySummary companySummary) {
        var registrationNumber = companySummary.getIdentification().getRegistrationNumbers().stream().filter(this::isTradeRegisterNumber).findFirst().orElse(null);

        if (registrationNumber != null) {
            company.setLegalRegistrationNumber(registrationNumber.getRegistrationNumber().getValue());
        }
    }

    private void setLegalForm(Organisation company, CompanySummary companySummary) {
        if (companySummary.getCompanyOverview() != null && companySummary.getCompanyOverview().getLegalForm() != null) {
            company.setCompanyLegalForm(companySummary.getCompanyOverview().getLegalForm().getValue());
        }
    }

    private void setIndustryCode(Organisation company, CompanySummary companySummary) {
        if (companySummary != null && companySummary.getIdentification() != null) {
            company.setIndustryCode(companySummary.getIdentification().getPrimaryLineofBusinessSIC());
        }
    }

    private void setNumberEmployees(Organisation company, RecordPayload payload) {
        // TODO: conditia iese din ecran in poza (IMG_7754); ultima verificare (getEmployeesInformationEntry() nevida) e presupusa.
        if (payload.getOperationsandHistory() != null && payload.getOperationsandHistory().getSubjectOperationalInformation() != null
                && payload.getOperationsandHistory().getSubjectOperationalInformation().getEmployeesInformation() != null
                && CollectionUtils.isNotEmpty(payload.getOperationsandHistory().getSubjectOperationalInformation().getEmployeesInformation().getEmployeesInformationEntry())) {

            company.setNumberOfEmployees(payload.getOperationsandHistory().getSubjectOperationalInformation().getEmployeesInformation().getEmployeesInformationEntry().get(0).getTotalEmployeesQuantity());
        }
    }

    private void processDnbAssessmentOverview(Organisation company, CompanySummary companySummary) {
        if (companySummary != null && companySummary.getDnbassessmentOverview() != null) {
            AssessmentSummaryType dnbAssessmentOverview = companySummary.getDnbassessmentOverview();

            //Paydex Score
            company.setPaydexScore(dnbAssessmentOverview.getDnbpaydexScore());

            //DNB Failure Score
            company.setDnbFailureScore(dnbAssessmentOverview.getDnbfailureScore());

            //DNB Financial Strength
            company.setDnbFinancialStrength(dnbAssessmentOverview.getDnbfinancialStrength());

            company.setDnbriskIndicator(dnbAssessmentOverview.getDnbriskIndicator());

            company.setScoreOverrideIndicator(dnbAssessmentOverview.isScoreOverrideIndicator());
        }
    }

    private void setDirectorAge(Organisation company, Boolean principalsInformationAvailabilityIndicator, RecordPayload payload) {
        if (principalsInformationAvailabilityIndicator != null && principalsInformationAvailabilityIndicator) {
            //Director's Age
            var directorInfo = payload.getPrincipalsInformation().getCurrentPrincipalsInformation().getCurrentPrincipalsInformationEntry().stream().filter(eachPricipal -> eachPricipal.getPrincipalPositions() != null).filter(this::isDirector).findFirst().orElse(null);

            if (directorInfo != null) {
                log.debug("IsDirector  {}", directorInfo.getPrincipalPositions().getCurrentPrincipalPositionsEntry());
                if (directorInfo.getPersonalDetails() != null && directorInfo.getPersonalDetails().getBirthDate() != null) {
                    company.setDirectorAge(calculateAge(directorInfo.getPersonalDetails().getBirthDate().getValue()));
                }
            }
        }
    }

    /*
      Age must be calculated between years ONLY based on the Old Cloverleaf code,
    */
    public int calculateAge(String birthDate) {
        LocalDate date = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-d"));

        return Period.between(date, LocalDate.now()).getYears();
    }

    private boolean isDirector(PrincipalsInformationEntryType principalInfo) {
        return principalInfo.getPrincipalPositions().getCurrentPrincipalPositionsEntry().stream().filter(entry -> DIRECTOR.equals(entry.getPrincipalPosition().getValue())).findFirst().orElse(null) != null;
    }

    private boolean isTradeRegisterNumber(RegistrationNumbers registration) {
        return registration.getRegistrationNumber().getType().equals(TRADE_REGISTRATION);
    }
}
