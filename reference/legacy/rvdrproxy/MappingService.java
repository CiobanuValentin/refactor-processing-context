// REFERINTA - cod existent (legacy, proxyobs master, rvdrproxy), transcris din pozele din 27.11.
// Nu se compileaza si poate contine erori de transcriere. Liniile taiate in poza sunt marcate cu /* ...taiat */.
package com.bnp.pf.transformation.rvdrproxy.service;

public class MappingService {
    /* ...campuri si constructor: modelMapper, cachingService, soapClient, creditCheckRepo */

    public List<ErrorHandler> checkCredit(Application application, String flowId) {

        var errors = new ArrayList<>();

        List<Customer> customers = application.getLoan().getCustomers();

        for (Customer customer : customers) {
            if (!COMPANY_PERSON.getApplicant().equals(customer.getApplicantType())) {
                var request = modelMapper.map(customer, CreditCheckRequest.class);

                var requestAndResponse = cachingService.creditCheck(request);

                if (Objects.isNull(requestAndResponse) || Objects.isNull(requestAndResponse.getResponse()) || (Objects.nonNull(requestAndResponse /* ...taiat */) {
                    requestAndResponse = new CreditCheckDto();
                    requestAndResponse.setRequest(request);
                    requestAndResponse.setBusinessKey(application.getBusinessKey());
                    requestAndResponse.setTime(LocalDateTime.now());
                    requestAndResponse.setErrors(new ErrorHandler());
                    requestAndResponse.setFlowId(flowId);

                    try {
                        var response = soapClient.checkPerson(request);

                        if (response != null) {
                            if (response.getResult() != null) {
                                requestAndResponse.setResponse(response);
                            } else if (response.getErrors() != null) {
                                for (ErrorType error : response.getErrors().getError()) {
                                    log.error("RVDR External API error for customer {} -> code: {}, message: {}", customer.getLastName(), error.g/* ...taiat */);
                                    requestAndResponse.getErrors().addError(error.getCode(), error.getMessage());
                                }
                                errors.add(requestAndResponse.getErrors());
                            }
                        }
                    } catch (ConnectException_Exception e) {
                        requestAndResponse.getErrors().addError(e);
                        errors.add(requestAndResponse.getErrors());
                        log.error("Check credit request failed for {}, error message: {}", request.getCreditCheckRequestData().getPerson().getNam/* ...taiat */);
                    } catch (SOAPFaultException e) {
                        requestAndResponse.getErrors().addError("502", "External API request triggered a SOAP Fault Exception (Check API gateway)" /* ...taiat */);
                        errors.add(requestAndResponse.getErrors());
                        log.error("Check credit SOAP call failed for {}, error message: {}", request.getCreditCheckRequestData().getPerson().getN/* ...taiat */);
                    }

                    creditCheckRepo.save(requestAndResponse);
                } else {
                    var auditCopy = new CreditCheckDto();
                    auditCopy.setBusinessKey(application.getBusinessKey());
                    auditCopy.setFlowId(flowId);
                    auditCopy.setErrors(requestAndResponse.getErrors());
                    auditCopy.setRequest(requestAndResponse.getRequest());
                    auditCopy.setResponse(requestAndResponse.getResponse());
                    auditCopy.setTime(requestAndResponse.getTime());
                    creditCheckRepo.save(auditCopy);
                }
            } else {
                log.info("Party Type is U0x, So not processing for CKI with Applicant Type {} business Key {}", customer.getApplicantType(), appl/* ...taiat */);
            }
        }

        return errors;
    }
}
