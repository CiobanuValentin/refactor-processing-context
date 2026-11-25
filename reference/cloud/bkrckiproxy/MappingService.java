// REFERINTA - cod existent (cloud, bkrckiproxy), transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ckiproxy.service;

// De observat: aici CollectError e folosit fara context (collectError.addError(code, msg), getErrorMap(),
// removeDuplicateErrors()), deci proxy-ul e pe o versiune de common in care CollectError tine starea in bean.

@Service
@Slf4j
@RequiredArgsConstructor
public class MappingService {

    private final CachingService cachingService;
    private final SoapClient soapClient;
    private final CheckCkiRequestAndResponseRepository ckiRequestRepo;
    private final CollectError collectError;
    private final ModelMapper modelMapper;

    private final Gson gson = GsonBuilder.createGson();


    public void checkCki(Application application, String flowId, String producer, String businessKey, AtomicInteger index, Signal errorMessage, Loan loan) {
        log.info("Checking the Cki for business key {} ", businessKey);

        if (Objects.nonNull(loan) && CollectionUtils.isNotEmpty(loan.getCustomers())) {
            var parameters = application.getCkiParameters();
            var parametersProvided = areParametersProvided(parameters);
            if (isEligibleCustomers(application.getLoan()) && parametersProvided) {
                for (Customer customer : loan.getCustomers()) {
                    if (!COMPANY_PERSON.getApplicant().equals(customer.getApplicantType()) && isEligibleCustomersCoHolder(application.getLoan(), customer)) {
                        SingleBkrRequest ckiRequest = createSingleBrokerRequest(customer, parameters);
                        int ckiRequestHashCode = ckiRequest.hashCode();

                        cachingService.searchInCache(ckiRequestHashCode).ifPresentOrElse(generalProxyObsCacheDto -> {
                                    log.info("Found response in cache for Customer {} and for business key {}, No need to call SingleBkr ", customer.getFirstName(), businessKey);
                                    cachingService.updateModifiedDateAndModifiedBy(producer, ckiRequestHashCode);
                                    createAndSaveCki(businessKey, generalProxyObsCacheDto, flowId);
                                },
                                () -> {
                                    String singleBkrResponseJson = callExternalSoapService(ckiRequest, customer, businessKey, index, errorMessage);
                                    cachingService.saveProxyCache(producer, ckiRequestHashCode, gson.toJson(ckiRequest), singleBkrResponseJson);
                                    createAndSaveCki(businessKey, flowId, singleBkrResponseJson, ckiRequest);
                                });
                    } else {
                        log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}", customer.getApplicantType(), application.getLoan().getBusinessKey());
                    }
                }
            } else {
                areParametersProvided(parametersProvided, businessKey, flowId, index, errorMessage, application);
            }
        }

        log.info("updating the Cki for business key {} ", businessKey);
    }

    private void areParametersProvided(boolean parametersProvided, String businessKey, String flowId, AtomicInteger index, Signal errorMessage, Application application) {
        if (BooleanUtils.isFalse(parametersProvided)) {
            log.error("Parameters for CKI request not provided in the application request for business key {} and flowId {}", businessKey, flowId);
            collectError.addError("400", "CKI Parameters not received");
            collectError.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(),
                    "Parameters for CKI request not provided in the application request for business key " + businessKey + " and flowId " + flowId);
            collectError.addErrorSignal(errorMessage);
        }
        log.info("Not the Eligible customer with credit id {} for business Key {}", application.getLoan().getCreditType(), application.getLoan().getBusinessKey());
    }

    private String callExternalSoapService(SingleBkrRequest ckiRequest, Customer customer, String businessKey, AtomicInteger index, Signal errorMessage) {
        log.info("Invoking the SingleBkrRequest for Customer {} and for business key {} ", customer.getLastName(), businessKey);

        SingleBkrResponse singleBkrResponse = soapClient.checkCkiRequest(ckiRequest, index, errorMessage);
        //SingleBkrResponse singleBkrResponse = SingleBkrResponseBuilder.createSingleBkrResponse();

        return gson.toJson(singleBkrResponse);
    }

    private boolean areParametersProvided(CkiParameters parameters) {
        return parameters != null && !(StringUtil.isNullOrEmpty(parameters.getChannel()) || StringUtil.isNullOrEmpty(parameters.getCompany()) || StringUtil.isNullOrEmpty(parameters.getLabel()));
    }

    public boolean isEligibleCustomers(Loan loan) {
        if (("1".equals(loan.getCreditType()) || "3".equals(loan.getCreditType())) && loan.getCompanies() != null && !loan.getCompanies().isEmpty() && getCompanies(loan)) {
            return true;
        } else {
            return !("1".equals(loan.getCreditType()) || "3".equals(loan.getCreditType()));
        }
    }

    public boolean isEligibleCustomersCoHolder(Loan loan, Customer customer) {
        if (("1".equals(loan.getCreditType()) || "3".equals(loan.getCreditType())) && loan.getCompanies() != null && !loan.getCompanies().isEmpty() && CO_HOLDER.getApplicant().equals(customer.getApplic/* ...taiat */)) {
            return true;
        } else {
            return !("1".equals(loan.getCreditType()) || "3".equals(loan.getCreditType()));
        }
    }

    private boolean getCompanies(Loan loan) {
        return loan.getCompanies().stream().anyMatch(company -> VOF.getValue().equals(company.getLegalStructure()) || EENMANSZAAK.getValue().equals(company.getLegalStructure()) || CV.getValu/* ...taiat */);
    }

    // raspuns gasit in cache
    private void createAndSaveCki(String businessKey, GeneralProxyObsCacheDto generalProxyObsCacheDto, String flowId) {

        ErrorHandler errorHandler = collectError.removeDuplicateErrors();

        CheckCkiRequestAndResponseDto ckiRequestAndResponseDto = new CheckCkiRequestAndResponseDto();
        ckiRequestAndResponseDto.setBusinessKey(businessKey);
        ckiRequestAndResponseDto.setSingleBkrRequestJson(generalProxyObsCacheDto.getRequestText());
        ckiRequestAndResponseDto.setSingleBkrResponseJson(generalProxyObsCacheDto.getResponseText());
        ckiRequestAndResponseDto.setErrorHandlerJson(gson.toJson(errorHandler));
        ckiRequestAndResponseDto.setFlowId(flowId);

        CheckCkiRequestAndResponseEntity checkCkiRequestAndResponseEntity = modelMapper.map(ckiRequestAndResponseDto, CheckCkiRequestAndResponseEntity.class);
        log.info("Saving CheckCkiRequestAndResponseEntity: \n{}", checkCkiRequestAndResponseEntity);
        ckiRequestRepo.save(checkCkiRequestAndResponseEntity);
    }

    // raspuns nou de la serviciul SOAP
    private void createAndSaveCki(String businessKey, String flowId, String singleBkrResponseJson, SingleBkrRequest ckiRequest) {

        ErrorHandler errorHandler = collectError.removeDuplicateErrors();

        CheckCkiRequestAndResponseDto ckiRequestAndResponseDto = new CheckCkiRequestAndResponseDto();
        ckiRequestAndResponseDto.setBusinessKey(businessKey);
        ckiRequestAndResponseDto.setSingleBkrRequestJson(gson.toJson(ckiRequest));
        ckiRequestAndResponseDto.setSingleBkrResponseJson(singleBkrResponseJson);
        ckiRequestAndResponseDto.setErrorHandlerJson(gson.toJson(errorHandler));
        ckiRequestAndResponseDto.setFlowId(flowId);

        CheckCkiRequestAndResponseEntity checkCkiRequestAndResponseEntity = modelMapper.map(ckiRequestAndResponseDto, CheckCkiRequestAndResponseEntity.class);
        log.info("Saving CheckCkiRequestAndResponseEntity: \n{}", checkCkiRequestAndResponseEntity);
        ckiRequestRepo.save(checkCkiRequestAndResponseEntity);
    }

    private SingleBkrRequest createSingleBrokerRequest(Customer customer, CkiParameters parameters) {
        var singleBkrRequest = new SingleBkrRequest();
        singleBkrRequest.setCallRequestData(createCallRequestData(parameters));
        singleBkrRequest.setPerson(createCallPerson(customer));
        singleBkrRequest.setEvaRequest(false);
        return singleBkrRequest;
    }

    private CallRequestDataType createCallRequestData(CkiParameters parameters) {
        var callRequest = new CallRequestDataType();
        callRequest.setCompany(parameters.getCompany());
        callRequest.setChannel(parameters.getChannel());
        callRequest.setLabel(parameters.getLabel());
        return callRequest;
    }

    private SinglePersonRequestData createCallPerson(Customer customer) {
        var callPerson = new SinglePersonRequestData();
        callPerson.setName(customer.getLastName());
        callPerson.setDateOfBirth(getDateType(customer.getBirthDate()));
        callPerson.setInitials((customer.getInitialsName() != null && !customer.getInitialsName().trim().isEmpty()) ? customer.getInitialsName().replace(".", "") : "");
        callPerson.setAddress(getAddressType(customer));
        return callPerson;
    }

    private DateType getDateType(LocalDateTime birthDate) {
        var dateOfBirth = new DateType();
        dateOfBirth.setDay((short) birthDate.getDayOfMonth());
        dateOfBirth.setMonth((short) birthDate.getMonth().getValue());
        dateOfBirth.setYear((short) birthDate.getYear());
        return dateOfBirth;
    }

    private BaseAddressType getAddressType(Customer customer) {
        var addressType = new BaseAddressType();
        addressType.setZipCode(getZipCode(customer.getAddresses().get(0)));
        addressType.setHouseNumber(getHouseNumberType(customer.getAddresses().get(0)));
        return addressType;
    }

    public ZipCodeType getZipCode(Address address) {
        var zipCode = new ZipCodeType();
        if (address.getZipCode() != null && address.getZipCode().length() >= 4) {
            zipCode.setN(Integer.parseInt(address.getZipCode().substring(0, 4)));
        } else {
            zipCode.setN(0);
        }
        if (address.getZipCode() != null && address.getZipCode().length() >= 6) {
            zipCode.setA(address.getZipCode().substring(4));
        } else {
            zipCode.setA("");
        }
        return zipCode;
    }

    public HouseNumberType getHouseNumberType(Address address) {
        var houseNumber = new HouseNumberType();
        houseNumber.setN(address.getHouseNumber());
        if (address.getHouseNumberSuffixCode() != null && !address.getHouseNumberSuffixCode().isEmpty()) {
            houseNumber.setA(address.getHouseNumberSuffixCode());
        }
        return houseNumber;
    }

    //method used in RAT proxy
    public GeneralProxyObsDto fetchCheckCkiRequestAndResponse(String businessKey, String flowId) {
        AtomicReference<GeneralProxyObsDto> ckiRequestAndResponseDtoAtomic = new AtomicReference<>();
        ckiRequestRepo.findTopByBusinessKeyAndFlowIdOrderByCreationDesc(businessKey, flowId)
                .ifPresentOrElse(checkCkiRequestAndResponseEntity -> {
                            log.debug("CheckCkiRequestAndResponseEntity is: {} when calling findTopByBusinessKeyAndFlowIdOrderByCreationDesc()", checkCkiRequestAndResponseEntity);
                            ckiRequestAndResponseDtoAtomic.set(modelMapper.map(checkCkiRequestAndResponseEntity, GeneralProxyObsDto.class));
                        },
                        () -> log.debug("CheckCkiRequestAndResponseEntity not present when calling findTopByBusinessKeyAndFlowIdOrderByCreationDesc()"));

        return ckiRequestAndResponseDtoAtomic.get();
    }
}
