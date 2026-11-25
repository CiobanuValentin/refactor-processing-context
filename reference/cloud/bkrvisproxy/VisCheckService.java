// REFERINTA - cod existent (cloud, bkrvisproxy, branch feature/proxy-refactorization-2.0), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.service;

@Service
@RequiredArgsConstructor
public class VisCheckService {
    private final BuildDTOsUtility buildDTOsUtility;
    private final VisCheckCacheService visCheckCacheService;
    private final VisSoapClient visSoapClient;
    private final VisCheckRecordService visCheckRecordService;
    private final Gson gson;

    public void checkVis(ProcessingContext processingContext, Loan loan) {
        List<Customer> customers = loan.getCustomers();
        if (CollectionUtils.isEmpty(customers)) {
            log.info("No Customers found for businessKey {}", processingContext.getBusinessKey());
            return;
        }

        for (Customer customer : customers) {
            if (isCompanyPerson(customer)) {
                log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} businessKey {}",
                        customer.getApplicantType(), processingContext.getBusinessKey());
            } else if (!hasRequiredDetails(customer)) {
                log.info("Document number, birth date or other details are missing. Not calling proxy for businessKey {}",
                        processingContext.getBusinessKey());
            } else {
                checkCustomer(processingContext, customer);
            }
        }
    }

    public void checkCustomer(ProcessingContext processingContext, Customer customer) {
        log.info("Entering into checkVis step for businessKey {}", processingContext.getBusinessKey());
        VisRequest visRequest = buildDTOsUtility.createVisRequest(customer);
        String visRequestJson = gson.toJson(visRequest);

        Optional<GeneralProxyObsCacheDto> cached = visCheckCacheService.find(visRequest, processingContext.getProducer());
        if (cached.isPresent()) {
            GeneralProxyObsCacheDto cacheDto = cached.get();
            log.info("Information found in cache for customer with businessKey {}, extracting response from cache",
                    processingContext.getBusinessKey());
            visCheckRecordService.save(processingContext, visRequest, cacheDto.getResponseText(),
                    List.of(), cacheDto.getCreatedDate());

            return;
        }

        log.info("VisRequest used to perform the external VIS Call is: {}", visRequestJson.replaceAll("\\R", " "));

        VisCheckResult result = visSoapClient.visCheck(processingContext, visRequest);
        String responseJson = gson.toJson(result.getResponse());

        log.info("Response received after performing the external SOAP call to BkrVis is: {}", responseJson.replaceAll("\\R", " "));

        if (!result.hasErrors()) {
            visCheckCacheService.save(visRequest, responseJson, processingContext.getProducer());
        }

        visCheckRecordService.save(processingContext, visRequest, responseJson,
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
