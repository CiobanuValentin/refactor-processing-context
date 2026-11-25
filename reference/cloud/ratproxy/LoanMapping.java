// REFERINTA - cod existent, transcris din poze. Nu se compileaza si poate contine erori de transcriere.
// Liniile taiate in poza sunt marcate cu /* ...taiat */. Importurile nu erau vizibile.
package com.bnpp.pf.transformation.ratproxy.utils.mapper;

@Slf4j
public class LoanMapping {

    private LoanMapping() {
    }

    public static String genderTransform(String input, CollectError collectError, Signal errorMessage, AtomicInteger index, ProcessingContext processingContext) {
        if (input != null) {
            return input.equals(GenderType.MALE.getGenderCode()) ? "M" : "V";
        }

        String message = "Error mapping data. Input is not of type GenderType.";
        log.error(message);
        collectError.addError(processingContext, String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
        processingContext.getErrorMap().put(ERROR_MESSAGE + index.getAndIncrement(), message);
        collectError.addErrorSignal(processingContext, errorMessage);

        return StringUtils.EMPTY;
    }

    public static Loan map(Application application, Boolean isSanction, CollectError collectError, Signal errorMessage, AtomicInteger index, ProcessingContext processingContext) {
        setRelatedPersons(application);

        final com.bnp.pf.transformation.commons.api.message.input.Loan applicationLoan = application.getLoan();
        final Customer customer = applicationLoan.getCustomers().stream().findFirst().orElse(null);

        return Loan.builder()
                .companies(getCompanies(applicationLoan))
                .applicants(getApplicants(isSanction, applicationLoan, collectError, errorMessage, index, processingContext))
                .amount(applicationLoan.getAmount())
                .productType(applicationLoan.getProductCommercialName())
                .duration(applicationLoan.getDurationInMonths())
                .salesChannel(applicationLoan.getBroker().getBusinessKey())
                .dealerCoc(applicationLoan.getBroker().getLegalRegistrationNumber())
                .salesChannelSubbroker(applicationLoan.getSubBroker() != null ? applicationLoan.getSubBroker().getBusinessKey() : StringUtils.EMPTY)
                .totalAmountCurrency(applicationLoan.getCurrency())
                .supplier(application.getSupplier())
                .scalAvailability(customer != null && customer.getHasSanction() != null ? "ok" : "scal_unavailable")
                .build();
    }

    private static List<Company> getCompanies(com.bnp.pf.transformation.commons.api.message.input.Loan applicationLoan) {
        final List<Organisation> companies = applicationLoan.getCompanies();
        List<Company> companyList = new ArrayList<>();
        if (companies != null) {
            companies.forEach(organisation -> {
                Company company = new Company();
                company.setHasVatLiability(true);
                company.setApplicantType(organisation.getApplicantType());
                final List<Email> emails = organisation.getEmails();
                if (CollectionUtils.isNotEmpty(emails)) {
                    company.setEmail(emails.get(0).getAddress());
                }
                company.setCompanyId(organisation.getBusinessKey());
                company.setBranchSbi(organisation.getSbiCode());
                company.setLegalStructure(organisation.getLegalStructure());
                company.setLegalRegistrationNumber(organisation.getLegalRegistrationNumber());
                company.setStatutoryName(organisation.getStatutoryName());
                company.setEstablishmentDate(getDate(organisation.getEstablishmentDate()));
                company.setCommunicationHalted(organisation.getCommunicationHalted());
                company.setSectorOfActivity(organisation.getSectorOfActivity());
                company.setIban(getValue(organisation.getIban()));
                company.setCommunicationPreference(organisation.getPreferedCommunicationType());
                company.setCountryOfEstablishment(organisation.getCountryOfResidence());
                final List<TelecomDevice> telecomDevices = organisation.getTelecomDevices();
                checkAndSetPhoneNumber(telecomDevices, company);
                company.setHasVatLiability(organisation.getHasVatLiability());

                final List<Address> addresses = organisation.getAddresses();
                List<com.bnpp.pf.transformation.ratproxy.datasource.model.Address> addressList = new ArrayList<>();
                addresses.forEach(address -> {
                    com.bnpp.pf.transformation.ratproxy.datasource.model.Address address1 = new com.bnpp.pf.transformation.ratproxy.datasource.model.Address();
                    address1.setAddress1(address.getAddressLine1());
                    address1.setAddress2(getValue(address.getAddressLine2()));
                    address1.setCity(address.getCity());
                    address1.setZipCode(address.getZipCode());
                    address1.setCountry(address.getIsoCountryCode());
                    address1.setType(address.getAddressType());
                    addressList.add(address1);
                });

                company.setAddresses(addressList);
                companyList.add(company);
            });
        }
        return companyList;
    }

    private static void checkAndSetPhoneNumber(List<TelecomDevice> telecomDevices, Company company) {
        if (CollectionUtils.isNotEmpty(telecomDevices)) {
            company.setPhoneNumber1(telecomDevices.get(0) != null ? telecomDevices.get(0).getDeviceNumber() : null);
            if (telecomDevices.size() > 1) {
                company.setPhoneNumber2(telecomDevices.get(1) != null ? telecomDevices.get(1).getDeviceNumber() : null);
            }
        }
    }

    public static LocalDate getDate(LocalDateTime date) { return date != null ? date.toLocalDate() : null; }

    public static LocalDate getDateForEmploymentEndDate(LocalDateTime date) {
        return date != null ? date.toLocalDate() : LocalDate.of(1, 1, 1);
    }

    private static List<Applicant> getApplicants(Boolean isSanction, com.bnp.pf.transformation.commons.api.message.input.Loan applicationLoan,
                                                 CollectError collectError, Signal errorMessage, AtomicInteger index, ProcessingContext processingContext) {
        final List<Customer> customers = applicationLoan.getCustomers();
        List<Applicant> applicants = new ArrayList<>();
        if (customers != null) {
            customers.forEach(customer -> {
                final Email email = customer.getEmails()
                        .stream()
                        .findFirst()
                        .orElse(Email.builder()
                                .address(StringUtils.EMPTY)
                                .build());

                Applicant applicant = ApplicantBuilder.createApplicant(customer, email, isSanction, collectError, errorMessage, index, processingContext);

                final List<TelecomDevice> telecomDevices = customer.getTelecomDevices();
                checkAndSetApplicantPhoneNumber(telecomDevices, applicant);

                applicant.setAddresses(customer.getAddresses().stream().map(address ->
                                com.bnpp.pf.transformation.ratproxy.datasource.model.Address.builder()
                                        .address1(address.getAddressLine1())
                                        .address2(getValue(address.getAddressLine2()))
                                        .city(address.getCity())
                                        .zipCode(address.getZipCode())
                                        .country(address.getIsoCountryCode())
                                        .type(address.getAddressType())
                                        .build())
                        .toList());

                applicant.setProfessions(customer.getEmployments()
                        .stream()
                        .map(employment ->
                                Profession.builder()
                                        .employerZipCode(employment.getEmployerZipCode())
                                        .contractType(employment.getContractType())
                                        .nettIncome(employment.getNetIncomeAmount())
                                        //.sbiCode(getValue(employment.getSbiCode()))
                                        .grossIncome(employment.getGrossIncomeAmount())
                                        .employerCountry(employment.getIsoEmployerCountryCode())
                                        .isIndependentProfessional(employment.isFreelance())
                                        .startDate(getDate(employment.getEmploymentStartDate()))
                                        .endDate(getDateForEmploymentEndDate(employment.getEmploymentEndDate()))
                                        .isDirector(employment.isDirector())
                                        .isSmo(employment.isSmo())
                                        .isUbo(employment.isUbo())
                                        .build())
                        .toList());

                applicant.setHasProofOfAddress(true);
                applicant.setHasSufficientFinancialAssets(applicationLoan.isHasSufficientFinancialAssets());
                applicants.add(applicant);
            });
        }
        return applicants;
    }

    private static void checkAndSetApplicantPhoneNumber(List<TelecomDevice> telecomDevices, Applicant applicant) {
        telecomDevices.forEach(telecomDevice -> {
            if (applicant.getPhoneNumber1() == null) {
                applicant.setPhoneNumber1(telecomDevice.getDeviceNumber());
            } else if (applicant.getPhoneNumber2() == null) {
                applicant.setPhoneNumber2((telecomDevice.getDeviceNumber()));
            }
        });
    }

    public static String getValue(String value) {
        return value != null ? value : StringUtils.EMPTY;
    }

    private static void setRelatedPersons(Application application) {
        var companies = application.getLoan().getCompanies();

        if (CollectionUtils.isNotEmpty(companies)) {
            var customers = application.getLoan().getCustomers();
            var mainApp = companies.stream()
                    .findFirst()
                    .filter(organisation -> ApplicantType.HOLDER.getApplicant().equals(organisation.getApplicantType()))
                    .orElse(Organisation.builder()
                            .legalStructure(StringUtils.EMPTY)
                            .build());

            if (StringUtils.isNotEmpty(mainApp.getLegalStructure())) {
                setDirectorOnCustomer(customers, mainApp);

                var relatedCompanies = companies.stream()
                        .filter(organisation -> !ApplicantType.HOLDER.getApplicant().equals(organisation.getApplicantType())).toList();
                if (LegalStructureType.EENMANSZAAK.getValue().equals(mainApp.getLegalStructure())
                        || LegalStructureType.VOF.getValue().equals(mainApp.getLegalStructure())
                        || LegalStructureType.COOPERATIE.getValue().equals(mainApp.getLegalStructure())) {
                    relatedCompanies.forEach(relatedCompany ->
                            relatedCompany.setApplicantType(ApplicantType.CO_HOLDER.getApplicant()));
                    customers.forEach(customer ->
                            customer.setApplicantType(ApplicantType.CO_HOLDER.getApplicant()));
                } else {
                    relatedCompanies.forEach(relatedCompany ->
                            relatedCompany.setApplicantType(ApplicantType.UNKNOWN.getApplicant()));
                    customers.forEach(customer ->
                            customer.setApplicantType(ApplicantType.UNKNOWN.getApplicant()));
                }
            }
        }
    }

    private static void setDirectorOnCustomer(List<Customer> customers, Organisation mainApp) {
        if (!(LegalStructureType.EENMANSZAAK.getValue().equals(mainApp.getLegalStructure()) || LegalStructureType.VOF.getValue().equals(mainApp.getLegalStructure()))) {
            customers.forEach(customer -> {
                if (ApplicantType.CO_HOLDER.getApplicant().equals(customer.getApplicantType()) && !customer.getEmployments().isEmpty()) {
                    customer.getEmployments().get(0).setDirector(true);
                }
            });
        }
    }
}
