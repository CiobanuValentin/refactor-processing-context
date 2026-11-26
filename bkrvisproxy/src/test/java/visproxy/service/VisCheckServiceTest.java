package visproxy.service;

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
import java.time.LocalDateTime;
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

import com.google.gson.Gson;

import com.bnp.pf.transformation.commons.api.error.Error;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.GeneralProxyObsCacheDto;
import com.bnp.pf.transformation.commons.api.generalproxyobs.dto.ProxyContext;
import com.bnp.pf.transformation.commons.api.message.input.Loan;
import com.bnp.pf.transformation.commons.api.model.Customer;
import com.bnp.pf.transformation.commons.api.model.Identification;
import com.bnp.pf.transformation.commons.api.utils.LocalDateTimeUtils;
import com.bnp.pf.transformation.visproxy.client.VisSoapClient;
import com.bnp.pf.transformation.visproxy.model.VisCheckResult;
import com.bnp.pf.transformation.visproxy.service.VisCheckCacheService;
import com.bnp.pf.transformation.visproxy.service.VisCheckRecordService;
import com.bnp.pf.transformation.visproxy.service.VisCheckService;
import com.bnp.pf.transformation.visproxy.utils.BuildDTOsUtility;

import urn.bkr.wsdl.vis.VisRequest;
import urn.bkr.wsdl.vis.VisResponse;
import visproxy.builder.ContextBuilder;

/** Aceleasi teste; ProcessingContext devine ProxyContext (producer-ul vine din metadata mesajului). */
@ExtendWith(MockitoExtension.class)
class VisCheckServiceTest {
    private static final String PRODUCER = ContextBuilder.PRODUCER;
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

    @Mock
    private BuildDTOsUtility buildDTOsUtility;
    @Mock
    private VisCheckCacheService visCheckCacheService;
    @Mock
    private VisSoapClient visSoapClient;
    @Mock
    private VisCheckRecordService visCheckRecordService;
    @Spy
    private Gson gson = new Gson();

    @InjectMocks
    private VisCheckService visCheckService;

    @Captor
    private ArgumentCaptor<VisRequest> savedRequest;
    @Captor
    private ArgumentCaptor<String> savedResponse;
    @Captor
    private ArgumentCaptor<List<Error>> savedErrors;
    @Captor
    private ArgumentCaptor<LocalDate> savedDate;

    private ProxyContext processingContext;
    private Loan loan;

    @BeforeEach
    void setUp() {
        processingContext = ContextBuilder.context(ContextBuilder.application("BK-001"));
        loan = new Loan();
    }

    private static Identification identification(String code) {
        Identification identification = mock(Identification.class);
        when(identification.getIsoIssuedCountryCode()).thenReturn("RO");
        when(identification.getIdentificationType()).thenReturn("CI");
        when(identification.getCode()).thenReturn(code);

        return identification;
    }

    private static Customer eligibleCustomer() {
        Customer customer = mock(Customer.class);
        Identification identification = identification("123456");
        when(customer.getApplicantType()).thenReturn("INDIVIDUAL");
        when(customer.getIdentification()).thenReturn(identification);
        when(customer.getBirthDate()).thenReturn(LocalDateTime.of(1990, 1, 1, 0, 0));

        return customer;
    }

    private static Customer companyPerson() {
        Customer customer = new Customer();
        customer.setApplicantType(COMPANY_PERSON.getApplicant());

        return customer;
    }

    private static Customer customerWithoutDocumentCountry() {
        Identification identification = mock(Identification.class);
        when(identification.getIsoIssuedCountryCode()).thenReturn("");
        Customer customer = mock(Customer.class);
        when(customer.getApplicantType()).thenReturn("INDIVIDUAL");
        when(customer.getIdentification()).thenReturn(identification);

        return customer;
    }

    private static Customer customerWithoutBirthDate() {
        Identification identification = identification("123456");
        Customer customer = mock(Customer.class);
        when(customer.getApplicantType()).thenReturn("INDIVIDUAL");
        when(customer.getIdentification()).thenReturn(identification);

        return customer;
    }

    private VisRequest givenRequestFor(Customer customer) {
        VisRequest request = new VisRequest();
        when(buildDTOsUtility.createVisRequest(same(customer))).thenReturn(request);

        return request;
    }

    private void givenSoapResult(VisRequest request, VisCheckResult result) {
        when(visCheckCacheService.find(same(request), eq(PRODUCER))).thenReturn(Optional.empty());
        when(visSoapClient.visCheck(same(processingContext), same(request))).thenReturn(result);
    }

    private void checkVisToday() {
        try (MockedStatic<LocalDateTimeUtils> utils = mockStatic(LocalDateTimeUtils.class)) {
            utils.when(LocalDateTimeUtils::parseLocalDateNow).thenReturn(TODAY);
            visCheckService.checkVis(processingContext, loan);
        }
    }

    private void captureSavedRecords(int expectedRecords) {
        verify(visCheckRecordService, times(expectedRecords)).save(same(processingContext), savedRequest.capture(),
                savedResponse.capture(), savedErrors.capture(), savedDate.capture());
    }

    private static List<String> invokedMethods(Object... mocks) {
        return Arrays.stream(mocks)
                .flatMap(mock -> mockingDetails(mock).getInvocations().stream())
                .map(invocation -> invocation.getMethod().getName())
                .toList();
    }

    @Test
    void checkVis_whenNoCustomers_shouldDoNothing() {
        loan.setCustomers(List.of());
        visCheckService.checkVis(processingContext, loan);

        assertThat(invokedMethods(buildDTOsUtility, visCheckCacheService, visSoapClient, visCheckRecordService)).isEmpty();
    }

    @Test
    void checkVis_whenOnlyCompanyPersons_shouldSkipThem() {
        loan.setCustomers(List.of(companyPerson()));
        visCheckService.checkVis(processingContext, loan);

        assertThat(invokedMethods(buildDTOsUtility, visCheckCacheService, visSoapClient, visCheckRecordService)).isEmpty();
    }

    @Test
    void checkVis_whenDocumentDetailsMissing_shouldSkipCustomer() {
        loan.setCustomers(List.of(customerWithoutDocumentCountry()));
        visCheckService.checkVis(processingContext, loan);

        assertThat(invokedMethods(buildDTOsUtility, visCheckCacheService, visSoapClient, visCheckRecordService)).isEmpty();
    }

    @Test
    void checkVis_whenIdentificationIsNull_shouldSkipCustomerWithoutNpe() {
        Customer customer = mock(Customer.class);
        when(customer.getApplicantType()).thenReturn("INDIVIDUAL");
        loan.setCustomers(List.of(customer));

        visCheckService.checkVis(processingContext, loan);

        assertThat(invokedMethods(buildDTOsUtility, visCheckCacheService, visSoapClient, visCheckRecordService)).isEmpty();
    }

    @Test
    void checkVis_whenBirthDateMissing_shouldSkipCustomerWithoutNpe() {
        loan.setCustomers(List.of(customerWithoutBirthDate()));
        visCheckService.checkVis(processingContext, loan);

        assertThat(invokedMethods(buildDTOsUtility, visCheckCacheService, visSoapClient, visCheckRecordService)).isEmpty();
    }

    @Test
    void checkVis_whenResponseIsCached_shouldSaveCachedResponseWithoutSoapCall() {
        Customer customer = eligibleCustomer();
        loan.setCustomers(List.of(customer));
        VisRequest visRequest = givenRequestFor(customer);
        LocalDate cachedDate = LocalDate.of(2026, 1, 10);
        GeneralProxyObsCacheDto cached = mock(GeneralProxyObsCacheDto.class);
        when(cached.getResponseText()).thenReturn("{\"cached\":true}");
        when(cached.getCreatedDate()).thenReturn(cachedDate);
        when(visCheckCacheService.find(same(visRequest), eq(PRODUCER))).thenReturn(Optional.of(cached));

        visCheckService.checkVis(processingContext, loan);

        captureSavedRecords(1);
        assertThat(invokedMethods(visSoapClient)).isEmpty();
        assertThat(invokedMethods(visCheckCacheService)).containsExactly("find");
        assertThat(savedRequest.getValue()).isSameAs(visRequest);
        assertThat(savedResponse.getValue()).isEqualTo("{\"cached\":true}");
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(cachedDate);
    }

    @Test
    void checkVis_whenSoapCallSucceeds_shouldCacheAndSaveResponse() {
        Customer customer = eligibleCustomer();
        loan.setCustomers(List.of(customer));
        VisRequest visRequest = givenRequestFor(customer);
        VisResponse visResponse = new VisResponse();
        givenSoapResult(visRequest, new VisCheckResult(visResponse, List.of()));

        checkVisToday();

        String responseJson = new Gson().toJson(visResponse);
        verify(visCheckCacheService).save(same(visRequest), eq(responseJson), eq(PRODUCER));
        captureSavedRecords(1);
        assertThat(savedRequest.getValue()).isSameAs(visRequest);
        assertThat(savedResponse.getValue()).isEqualTo(responseJson);
        assertThat(savedErrors.getValue()).isEmpty();
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
    }

    @Test
    void checkVis_whenSoapCallReturnsErrors_shouldSaveErrorsAndNotCache() {
        Customer customer = eligibleCustomer();
        loan.setCustomers(List.of(customer));
        VisRequest visRequest = givenRequestFor(customer);
        Error error = new Error("502", "SOAP Call Error");
        givenSoapResult(visRequest, new VisCheckResult(new VisResponse(), List.of(error)));

        checkVisToday();

        captureSavedRecords(1);
        assertThat(invokedMethods(visCheckCacheService)).containsExactly("find");
        assertThat(savedDate.getValue()).isEqualTo(TODAY);
        assertThat(savedErrors.getValue()).containsExactly(error);
    }

    @Test
    void checkVis_whenSeveralCustomers_shouldSaveEachEligibleOneWithItsOwnErrorsOnly() {
        Customer failing = eligibleCustomer();
        Customer passing = eligibleCustomer();
        loan.setCustomers(List.of(failing, customerWithoutBirthDate(), passing));
        VisRequest failingRequest = givenRequestFor(failing);
        VisRequest passingRequest = givenRequestFor(passing);
        Error error = new Error("502", "SOAP Call Error");
        givenSoapResult(failingRequest, new VisCheckResult(new VisResponse(), List.of(error)));
        givenSoapResult(passingRequest, new VisCheckResult(new VisResponse(), List.of()));

        checkVisToday();

        captureSavedRecords(2);
        assertThat(invokedMethods(visSoapClient)).hasSize(2);
        assertThat(savedRequest.getAllValues()).containsExactly(failingRequest, passingRequest);
        assertThat(savedErrors.getAllValues()).containsExactly(List.of(error), List.of());
    }
}
