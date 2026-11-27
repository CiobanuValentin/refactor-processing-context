package com.bnp.pf.transformation.rvdrproxy.service;

import static com.bnp.pf.transformation.commons.api.model.ApplicantType.COMPANY_PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.google.gson.Gson;

// TODO: scris dupa modelul VisCheckServiceTest, inainte de a vedea testul existent din rvdr; importurile pentru
// ApplicantType, Customer, Error, GeneralProxyObsCacheDto si clasele WSDL (CreditCheckRequest, CreditCheckResponse)
// sunt presupuse sau lipsesc - pastreaza-le pe cele din testul actual.
import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;
import com.bnp.pf.transformation.rvdrproxy.builder.ContextBuilder;
import com.bnp.pf.transformation.rvdrproxy.client.SoapClient;
import com.bnp.pf.transformation.rvdrproxy.model.CreditCheckResult;

@ExtendWith(MockitoExtension.class)
class CreditCheckServiceTest {
    private static final String PRODUCER = ContextBuilder.PRODUCER;
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

    @Mock
    private ModelMapper modelMapper;
    @Mock
    private CreditCheckCacheService creditCheckCacheService;
    @Mock
    private SoapClient soapClient;
    @Mock
    private CreditCheckRecordService creditCheckRecordService;
    @Spy
    private Gson gson = new Gson();

    @InjectMocks
    private CreditCheckService creditCheckService;

    @Captor
    private ArgumentCaptor<CreditCheckRequest> savedRequest;
    @Captor
    private ArgumentCaptor<String> savedResponse;
    @Captor
    private ArgumentCaptor<List<Error>> savedErrors;
    @Captor
    private ArgumentCaptor<LocalDate> savedDate;

    private ProxyContext ctx;
    private Loan loan;

    @BeforeEach
    void setUp() {
        ctx = ContextBuilder.context(ContextBuilder.application("BK-001"));
        loan = new Loan();
    }

    private static Customer customer(String applicantType) {
        Customer customer = new Customer();
        customer.setApplicantType(applicantType);

        return customer;
    }

    private CreditCheckRequest givenRequestFor(Customer customer) {
        CreditCheckRequest request = new CreditCheckRequest();
        when(modelMapper.map(same(customer), eq(CreditCheckRequest.class))).thenReturn(request);

        return request;
    }

    private void givenSoapResult(CreditCheckRequest request, CreditCheckResult result) {
        when(creditCheckCacheService.find(same(request), eq(PRODUCER))).thenReturn(Optional.empty());
        when(soapClient.creditCheckRequest(same(ctx), same(request))).thenReturn(result);
    }

    private void checkCreditToday() {
        try (MockedStatic<LocalDateTimeUtils> utils = mockStatic(LocalDateTimeUtils.class)) {
            utils.when(LocalDateTimeUtils::parseLocalDateNow).thenReturn(TODAY);
            creditCheckService.checkCredit(ctx, loan);
        }
    }

    private void captureSavedRecords(int expectedRecords) {
        verify(creditCheckRecordService, times(expectedRecords)).save(same(ctx), savedRequest.capture(),
                savedResponse.capture(), savedErrors.capture(), savedDate.capture());
    }

    private static List<String> invokedMethods(Object... mocks) {
        return Arrays.stream(mocks)
                .flatMap(mock -> mockingDetails(mock).getInvocations().stream())
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

    @Test
    void checkCredit_whenNoCustomers_shouldDoNothing() {
        loan.setCustomers(List.of());

        creditCheckService.checkCredit(ctx, loan);

        assertThat(invokedMethods(modelMapper, creditCheckCacheService, soapClient, creditCheckRecordService)).isEmpty();
    }

    @Test
    void checkCredit_whenCustomersAreNull_shouldDoNothing() {
        loan.setCustomers(null);

        creditCheckService.checkCredit(ctx, loan);

        assertThat(invokedMethods(modelMapper, creditCheckCacheService, soapClient, creditCheckRecordService)).isEmpty();
    }

    @Test
    void checkCredit_whenOnlyCompanyPersons_shouldSkipThem() {
        loan.setCustomers(List.of(customer(COMPANY_PERSON.getApplicant())));

        creditCheckService.checkCredit(ctx, loan);

        assertThat(invokedMethods(modelMapper, creditCheckCacheService, soapClient, creditCheckRecordService)).isEmpty();
    }

    @Test
    void checkCredit_whenResponseIsCached_shouldSaveCachedResponseWithoutSoapCall() {
        Customer customer = customer("INDIVIDUAL");
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        LocalDate cachedDate = LocalDate.of(2026, 1, 10);
        GeneralProxyObsCacheDto cached = mock(GeneralProxyObsCacheDto.class);
        when(cached.getResponseText()).thenReturn("{\"cached\":true}");
        when(cached.getCreatedDate()).thenReturn(cachedDate);
        when(creditCheckCacheService.find(same(request), eq(PRODUCER))).thenReturn(Optional.of(cached));

        creditCheckService.checkCredit(ctx, loan);

        captureSavedRecords(1);
        assertThat(invokedMethods(soapClient)).isEmpty();
        assertThat(invokedMethods(creditCheckCacheService)).containsExactly("find");
        assertThat(savedRequest.getValue()).isSameAs(request);
        assertThat(savedResponse.getValue()).isEqualTo("{\"cached\":true}");
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(cachedDate);
    }

    @Test
    void checkCredit_whenSoapCallSucceeds_shouldCacheAndSaveResponse() {
        Customer customer = customer("INDIVIDUAL");
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        CreditCheckResponse response = new CreditCheckResponse();
        givenSoapResult(request, new CreditCheckResult(response, List.of()));

        checkCreditToday();

        String responseJson = new Gson().toJson(response);
        verify(creditCheckCacheService).save(same(request), eq(responseJson), eq(PRODUCER));
        captureSavedRecords(1);
        assertThat(savedRequest.getValue()).isSameAs(request);
        assertThat(savedResponse.getValue()).isEqualTo(responseJson);
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
    }

    @Test
    void checkCredit_whenSoapCallReturnsErrors_shouldSaveErrorsAndNotCache() {
        Customer customer = customer("INDIVIDUAL");
        loan.setCustomers(List.of(customer));
        CreditCheckRequest request = givenRequestFor(customer);
        Error error = new Error("502", "SOAP Call Error");
        givenSoapResult(request, new CreditCheckResult(new CreditCheckResponse(), List.of(error)));

        checkCreditToday();

        captureSavedRecords(1);
        assertThat(invokedMethods(creditCheckCacheService)).containsExactly("find");
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
        assertThat(savedErrors.getValue()).containsExactly(error);
    }

    @Test
    void checkCredit_whenSeveralCustomers_shouldSaveEachEligibleOneWithItsOwnErrorsOnly() {
        Customer failing = customer("INDIVIDUAL");
        Customer passing = customer("INDIVIDUAL");
        loan.setCustomers(List.of(failing, customer(COMPANY_PERSON.getApplicant()), passing));
        CreditCheckRequest failingRequest = givenRequestFor(failing);
        CreditCheckRequest passingRequest = givenRequestFor(passing);
        Error error = new Error("502", "SOAP Call Error");
        givenSoapResult(failingRequest, new CreditCheckResult(new CreditCheckResponse(), List.of(error)));
        givenSoapResult(passingRequest, new CreditCheckResult(new CreditCheckResponse(), List.of()));

        checkCreditToday();

        captureSavedRecords(2);
        assertThat(invokedMethods(soapClient)).hasSize(2);
        assertThat(savedRequest.getAllValues()).containsExactly(failingRequest, passingRequest);
        assertThat(savedErrors.getAllValues()).containsExactly(List.of(error), List.of());
    }
}
