// REFERINTA - cod existent (cloud, rvdrproxy, branch feature/proxy-refactorization-2.0), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Importurile nu erau vizibile.
package com.bnp.pf.transformation.rvdrproxy.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCheckService {
    private final ModelMapper modelMapper;
    private final CreditCheckCacheService creditCheckCacheService;
    private final SoapClient soapClient;
    private final CreditCheckRecordService creditCheckRecordService;
    private final Gson gson;

    public void checkCredit(ProcessingContext processingContext, Loan loan) {
        List<Customer> customers = loan.getCustomers();

        if (CollectionUtils.isEmpty(customers)) {
            return;
        }

        for (Customer customer : customers) {
            if (isCompanyPerson(customer)) {
                log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}",
                        customer.getApplicantType(), processingContext.getBusinessKey());
                continue;
            }
            checkCustomer(processingContext, customer);
        }
    }

    private void checkCustomer(ProcessingContext processingContext, Customer customer) {
        log.info("Entering into checkCredit step for businessKey {}", processingContext.getBusinessKey());
        CreditCheckRequest creditCheckRequest = modelMapper.map(customer, CreditCheckRequest.class);
        String creditCheckRequestJson = gson.toJson(creditCheckRequest);

        Optional<GeneralProxyObsCacheDto> cached = creditCheckCacheService.find(creditCheckRequest, processingContext.getProducer());

        if (cached.isPresent()) {
            GeneralProxyObsCacheDto cacheDto = cached.get();
            log.info("Credit check information found in cache for businessKey {}", processingContext.getBusinessKey());
            creditCheckRecordService.save(processingContext, creditCheckRequest,
                    cacheDto.getResponseText(), List.of(), cacheDto.getCreatedDate());

            return;
        }

        log.info("Making the SOAP call to the RVDR external service with the request: {}",
                creditCheckRequestJson.replaceAll("\\R", " "));

        CreditCheckResult result = soapClient.creditCheckRequest(processingContext, creditCheckRequest);
        String responseJson = gson.toJson(result.getResponse());

        log.info("Response received after performing the RVDR external SOAP call is: {} ",
                responseJson.replaceAll("\\R", " "));

        if (!result.hasErrors()) {
            creditCheckCacheService.save(creditCheckRequest, responseJson, processingContext.getProducer());
        }

        creditCheckRecordService.save(processingContext, creditCheckRequest, responseJson,
                result.getErrors(), LocalDateTimeUtils.parseLocalDateNow());
    }

    private static boolean isCompanyPerson(Customer customer) {
        return COMPANY_PERSON.getApplicant().equals(customer.getApplicantType());
    }
}
