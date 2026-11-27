package com.bnp.pf.transformation.rvdrproxy.service;

// TODO: importurile pentru ApplicantType, Customer, GeneralProxyObsCacheDto, CreditCheckRequest, CreditCheckResult
// si SoapClient sunt presupuse - pastreaza-le pe cele din clasa actuala.
import static com.bnp.pf.transformation.commons.api.model.ApplicantType.COMPANY_PERSON;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;
import com.bnp.pf.transformation.rvdrproxy.client.SoapClient;
import com.bnp.pf.transformation.rvdrproxy.model.CreditCheckResult;

/** Logica neschimbata; singura diferenta e ProxyContext in loc de ProcessingContext. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCheckService {
    private final ModelMapper modelMapper;
    private final CreditCheckCacheService creditCheckCacheService;
    private final SoapClient soapClient;
    private final CreditCheckRecordService creditCheckRecordService;
    private final Gson gson;

    public void checkCredit(ProxyContext ctx, Loan loan) {
        List<Customer> customers = loan.getCustomers();

        if (CollectionUtils.isEmpty(customers)) {
            return;
        }

        for (Customer customer : customers) {
            if (isCompanyPerson(customer)) {
                log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}",
                        customer.getApplicantType(), ctx.getBusinessKey());
                continue;
            }
            checkCustomer(ctx, customer);
        }
    }

    private void checkCustomer(ProxyContext ctx, Customer customer) {
        log.info("Entering into checkCredit step for businessKey {}", ctx.getBusinessKey());
        CreditCheckRequest creditCheckRequest = modelMapper.map(customer, CreditCheckRequest.class);
        String creditCheckRequestJson = gson.toJson(creditCheckRequest);

        Optional<GeneralProxyObsCacheDto> cached = creditCheckCacheService.find(creditCheckRequest, ctx.getProducer());

        if (cached.isPresent()) {
            GeneralProxyObsCacheDto cacheDto = cached.get();
            log.info("Credit check information found in cache for businessKey {}", ctx.getBusinessKey());
            creditCheckRecordService.save(ctx, creditCheckRequest,
                    cacheDto.getResponseText(), List.of(), cacheDto.getCreatedDate());

            return;
        }

        // Cererea si raspunsul contin datele clientului, deci se logheaza la debug (inainte: la info), ca in vis.
        log.debug("Making the SOAP call to the RVDR external service with the request: {}",
                creditCheckRequestJson.replaceAll("\\R", " "));

        CreditCheckResult result = soapClient.creditCheckRequest(ctx, creditCheckRequest);
        String responseJson = gson.toJson(result.getResponse());

        log.debug("Response received after performing the RVDR external SOAP call is: {} ",
                responseJson.replaceAll("\\R", " "));

        if (!result.hasErrors()) {
            creditCheckCacheService.save(creditCheckRequest, responseJson, ctx.getProducer());
        }

        creditCheckRecordService.save(ctx, creditCheckRequest, responseJson,
                result.getErrors(), LocalDateTimeUtils.parseLocalDateNow());
    }

    private static boolean isCompanyPerson(Customer customer) {
        return COMPANY_PERSON.getApplicant().equals(customer.getApplicantType());
    }
}
