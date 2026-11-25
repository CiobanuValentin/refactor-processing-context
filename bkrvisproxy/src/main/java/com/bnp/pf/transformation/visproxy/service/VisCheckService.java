package com.bnp.pf.transformation.visproxy.service;

// TODO: importurile pentru ApplicantType, Customer, Identification, GeneralProxyObsCacheDto, VisRequest,
// VisCheckResult si BuildDTOsUtility sunt presupuse - pastreaza-le pe cele din clasa actuala.
import static com.bnp.pf.transformation.commons.api.model.ApplicantType.COMPANY_PERSON;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Identification;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;
import com.bnp.pf.transformation.visproxy.client.VisSoapClient;
import com.bnp.pf.transformation.visproxy.model.VisCheckResult;
import com.bnp.pf.transformation.visproxy.utils.BuildDTOsUtility;

import urn.bkr.wsdl.vis.VisRequest;

/** Logica neschimbata; singura diferenta e ProxyContext in loc de ProcessingContext. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisCheckService {
    private final BuildDTOsUtility buildDTOsUtility;
    private final VisCheckCacheService visCheckCacheService;
    private final VisSoapClient visSoapClient;
    private final VisCheckRecordService visCheckRecordService;
    private final Gson gson;

    public void checkVis(ProxyContext ctx, Loan loan) {
        List<Customer> customers = loan.getCustomers();
        if (CollectionUtils.isEmpty(customers)) {
            log.info("No Customers found for businessKey {}", ctx.getBusinessKey());
            return;
        }

        for (Customer customer : customers) {
            if (isCompanyPerson(customer)) {
                log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} businessKey {}",
                        customer.getApplicantType(), ctx.getBusinessKey());
            } else if (!hasRequiredDetails(customer)) {
                log.info("Document number, birth date or other details are missing. Not calling proxy for businessKey {}",
                        ctx.getBusinessKey());
            } else {
                checkCustomer(ctx, customer);
            }
        }
    }

    public void checkCustomer(ProxyContext ctx, Customer customer) {
        log.info("Entering into checkVis step for businessKey {}", ctx.getBusinessKey());
        VisRequest visRequest = buildDTOsUtility.createVisRequest(customer);
        String visRequestJson = gson.toJson(visRequest);

        Optional<GeneralProxyObsCacheDto> cached = visCheckCacheService.find(visRequest, ctx.getProducer());
        if (cached.isPresent()) {
            GeneralProxyObsCacheDto cacheDto = cached.get();
            log.info("Information found in cache for customer with businessKey {}, extracting response from cache",
                    ctx.getBusinessKey());
            visCheckRecordService.save(ctx, visRequest, cacheDto.getResponseText(),
                    List.of(), cacheDto.getCreatedDate());

            return;
        }

        // Cererea si raspunsul contin datele clientului, deci se logheaza la debug (inainte: la info).
        log.debug("VisRequest used to perform the external VIS Call is: {}", visRequestJson.replaceAll("\\R", " "));

        VisCheckResult result = visSoapClient.visCheck(ctx, visRequest);
        String responseJson = gson.toJson(result.getResponse());

        log.debug("Response received after performing the external SOAP call to BkrVis is: {}", responseJson.replaceAll("\\R", " "));

        if (!result.hasErrors()) {
            visCheckCacheService.save(visRequest, responseJson, ctx.getProducer());
        }

        visCheckRecordService.save(ctx, visRequest, responseJson,
                result.getErrors(), LocalDateTimeUtils.parseLocalDateNow());
    }

    private static boolean isCompanyPerson(Customer customer) {
        return COMPANY_PERSON.getApplicant().equals(customer.getApplicantType());
    }

    private static boolean hasRequiredDetails(Customer customer) {
        Identification identification = customer.getIdentification();
        return identification != null
                && StringUtils.hasLength(identification.getIsoIssuedCountryCode())
                && StringUtils.hasLength(identification.getIdentificationType())
                && StringUtils.hasLength(identification.getCode())
                && customer.getBirthDate() != null;
    }
}
