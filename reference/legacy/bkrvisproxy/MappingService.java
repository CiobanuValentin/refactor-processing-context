// REFERINTA - cod existent (legacy, proxyobs master, bkrvisproxy), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnp.pf.transformation.visproxy.service;

@Slf4j
@Service
public class MappingService {

    private final CachingService cachingService;
    private final VisSoapClient visSoapClient;
    private final CheckVisRequestAndResponseRepository visRequestAndResponseRepo;
    @Value("${bkrvis.company}")
    private String company;

    public MappingService(CachingService cachingService, VisSoapClient visSoapClient, CheckVisRequestAndResponseRepository visRequestAndResponseRepo) {
        this.cachingService = cachingService;
        this.visSoapClient = visSoapClient;
        this.visRequestAndResponseRepo = visRequestAndResponseRepo;
    }

    public List<ErrorHandler> checkVis(Application application, String flowId) {
        List<Customer> customers = application.getLoan().getCustomers();
        var errors = new ArrayList<ErrorHandler>();

        if (customers != null && !customers.isEmpty()) {
            for (Customer customer : customers) {
                CheckVisRequestAndResponse response;
                if (isRequiredCallProxy(customer) && !COMPANY_PERSON.getApplicant().equals(customer.getApplicantType())) {
                    var visRequest = createVisRequest(customer);
                    response = cachingService.checkVisRequest(visRequest);
                    if (Objects.isNull(response) || Objects.isNull(response.getResponse()) || (Objects.nonNull(response.getResponse().getErrors()) && !response.getResponse().getErrors().getError().isEm/* ...taiat */)) {
                        var visReqResp = callService(application, customer, visRequest, flowId);
                        if (visReqResp.getErrors() != null) {
                            errors.add(visReqResp.getErrors());
                        }
                    } else {
                        log.info("Found response in cache for Customer {} for business key {}, No need to call Vis, Updating with flowId {} ", customer.getFirstName(), application.getBusinessKey(), flow/* ...taiat */);
                        if (response.getErrors() != null) {
                            errors.add(response.getErrors());
                        }
                        createAndSaveVis(application, response, flowId);
                    }
                }
                if (COMPANY_PERSON.getApplicant().equals(customer.getApplicantType())) {
                    log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}", customer.getApplicantType(), application.getLoan().getBusinessKey());
                } else {
                    log.info("Document number and other details are missing. Not calling proxy for business key {}", application.getLoan().getBusinessKey());
                }
            }
        } else {
            log.info("No Customers found for business key {}", application.getBusinessKey());
        }

        return errors;
    }

    private boolean isRequiredCallProxy(Customer customer) {
        return (!customer.getIdentification().getIsoIssuedCountryCode().isEmpty()
                && !customer.getIdentification().getIdentificationType().isEmpty()
                && !customer.getIdentification().getCode().isEmpty());
    }

    private CheckVisRequestAndResponse callService(Application application, Customer customer, VisRequest visRequest, String flowId) {
        log.info("Invoking the VisRequest for Customer {} for business key {} and correlation ID {}", customer.getFirstName(), application.getBusinessKey(), flowId);
        var visResponse = visSoapClient.checkVisRequest(visRequest);
        var errors = processErrors(visResponse, application.getBusinessKey(), flowId);
        return createAndSaveVis(application, errors, visResponse, visRequest, flowId);
    }

    // raspuns gasit in cache
    private void createAndSaveVis(Application application, CheckVisRequestAndResponse visReqResp, String flowId) {
        var visRequestAndResponse = new CheckVisRequestAndResponse();
        visRequestAndResponse.setFlowId(flowId);
        visRequestAndResponse.setBusinessKey(application.getBusinessKey());
        visRequestAndResponse.setErrors(visReqResp.getErrors());
        visRequestAndResponse.setRequest(visReqResp.getRequest());
        visRequestAndResponse.setResponse(visReqResp.getResponse());
        visRequestAndResponse.setTime(visReqResp.getTime());
        visRequestAndResponseRepo.save(visRequestAndResponse);
    }

    // raspuns nou de la serviciul SOAP
    private CheckVisRequestAndResponse createAndSaveVis(Application application, ErrorHandler errors, VisResponse visResponse, VisRequest visRequest, String flowId) {
        var visRequestAndResponse = new CheckVisRequestAndResponse();
        visRequestAndResponse.setFlowId(flowId);
        visRequestAndResponse.setBusinessKey(application.getBusinessKey());
        visRequestAndResponse.setErrors(errors);
        visRequestAndResponse.setRequest(visRequest);
        visRequestAndResponse.setResponse(visResponse);
        visRequestAndResponse.setTime(LocalDateTime.now());
        visRequestAndResponseRepo.save(visRequestAndResponse);
        return visRequestAndResponse;
    }

    private ErrorHandler processErrors(VisResponse visResponse, String businessKey, String flowId) {
        if (visResponse.getErrors() != null && !visResponse.getErrors().getError().isEmpty()) {
            var errors = new ErrorHandler();

            for (ErrorType errorType : visResponse.getErrors().getError()) {
                log.error("BKR VIS External API error for businessKey {} and flow ID {} -> code: {}, message: {}", businessKey, flowId, errorType.getCode(), errorType.getMessage());
                errors.addError(errorType.getCode(), errorType.getMessage());
            }
            return errors;
        }

        return null;
    }

    private VisRequest createVisRequest(Customer customer) {
        var visRequest = new VisRequest();
        visRequest.setCallRequestData(createCallRequestData());
        visRequest.setVisRequestData(createDocumentRequestData(customer));
        return visRequest;
    }

    private DocumentRequestData createDocumentRequestData(Customer customer) {
        var documentRequestData = new DocumentRequestData();
        documentRequestData.setType(getType(customer.getIdentification()));
        documentRequestData.setDocumentNumber(customer.getIdentification().getCode());
        documentRequestData.setCountryCode(customer.getIdentification().getIsoIssuedCountryCode());
        documentRequestData.setDateOfBirth(getDateOfBirth(customer.getBirthDate()));
        return documentRequestData;
    }

    private String getType(Identification identificationType) {
        return (identificationType.getIdentificationType() != null && identificationType.getIsoIssuedCountryCode() != null
                && identificationType.getIdentificationType().equals("I") && !identificationType.getIsoIssuedCountryCode().equalsIgnoreCase("NL")) ? "VN" : "PP";
    }

    private DateType getDateOfBirth(LocalDateTime birthDate) {
        var dateOfBirth = new DateType();
        dateOfBirth.setDay((short) birthDate.getDayOfMonth());
        dateOfBirth.setMonth((short) birthDate.getMonth().getValue());
        dateOfBirth.setYear((short) birthDate.getYear());
        /* ...restul nu era in poza */
    }
}
