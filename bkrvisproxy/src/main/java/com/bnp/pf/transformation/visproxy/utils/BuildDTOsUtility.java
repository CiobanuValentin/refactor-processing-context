package com.bnp.pf.transformation.visproxy.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

// TODO: importurile pentru Customer, Identification si ErrorHandler sunt presupuse - pastreaza-le pe cele actuale.
import com.bnp.pf.transformation.commons.api.error.ErrorHandler;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsDto;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Identification;

import urn.bkr.wsdl.vis.CallRequestDataType;
import urn.bkr.wsdl.vis.DateType;
import urn.bkr.wsdl.vis.DocumentRequestData;
import urn.bkr.wsdl.vis.VisRequest;

// Neschimbat.
@Component
public class BuildDTOsUtility {

    private final String company;

    public BuildDTOsUtility(@Value("${bkrvis.company}") String company) {
        this.company = company;
    }

    public GeneralProxyObsDto createCheckVisDto(String businessKey, String flowId, VisRequest visRequest, String responseRootJson,
                                                ErrorHandler errorHandler, LocalDate localDate, Gson gson) {
        GeneralProxyObsDto checkVisDto = new GeneralProxyObsDto();
        checkVisDto.setBusinessKey(businessKey);
        checkVisDto.setFlowId(flowId);
        checkVisDto.setRequestVisJson(gson.toJson(visRequest));
        checkVisDto.setResponseVisJson(responseRootJson);
        checkVisDto.setErrorHandlerJson(gson.toJson(errorHandler));
        checkVisDto.setCreation(localDate.atTime(LocalTime.now()));
        return checkVisDto;
    }

    public VisRequest createVisRequest(Customer customer) {
        VisRequest visRequest = new VisRequest();
        visRequest.setCallRequestData(createCallRequestData());
        visRequest.setVisRequestData(createDocumentRequestData(customer));
        return visRequest;
    }

    private CallRequestDataType createCallRequestData() {
        CallRequestDataType callRequest = new CallRequestDataType();
        callRequest.setCompany(company);
        callRequest.setChannel(company);
        callRequest.setLabel(company);
        return callRequest;
    }

    private static DocumentRequestData createDocumentRequestData(Customer customer) {
        DocumentRequestData documentRequestData = new DocumentRequestData();
        documentRequestData.setType(getType(customer.getIdentification()));
        documentRequestData.setDocumentNumber(customer.getIdentification().getCode());
        documentRequestData.setCountryCode(customer.getIdentification().getIsoIssuedCountryCode());
        documentRequestData.setDateOfBirth(getDateOfBirth(customer.getBirthDate()));
        return documentRequestData;
    }

    private static String getType(Identification identificationType) {
        return (identificationType.getIdentificationType() != null && identificationType.getIsoIssuedCountryCode() != null
                && identificationType.getIdentificationType().equals("I") && !identificationType.getIsoIssuedCountryCode().equalsIgnoreCase("NL")) ? "VN" : "PP";
    }

    private static DateType getDateOfBirth(LocalDateTime birthDate) {
        DateType dateOfBirth = new DateType();
        dateOfBirth.setDay((short) birthDate.getDayOfMonth());
        dateOfBirth.setMonth((short) birthDate.getMonth().getValue());
        dateOfBirth.setYear((short) birthDate.getYear());
        return dateOfBirth;
    }
}
