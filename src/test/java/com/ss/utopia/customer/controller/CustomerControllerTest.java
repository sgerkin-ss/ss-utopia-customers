package com.ss.utopia.customer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.utopia.customer.dto.CreateCustomerDto;
import com.ss.utopia.customer.dto.UpdateCustomerDto;
import com.ss.utopia.customer.dto.PaymentMethodDto;
import com.ss.utopia.customer.entity.Address;
import com.ss.utopia.customer.entity.Customer;
import com.ss.utopia.customer.entity.PaymentMethod;
import com.ss.utopia.customer.exception.DuplicateEmailException;
import com.ss.utopia.customer.exception.ExceptionControllerAdvisor;
import com.ss.utopia.customer.exception.NoSuchCustomerException;
import com.ss.utopia.customer.exception.NoSuchPaymentMethod;
import com.ss.utopia.customer.service.CustomerService;
import com.ss.utopia.customer.service.DeleteAccountService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Profile("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CustomerControllerTest {

  public static final UUID validCustomerId = UUID.randomUUID();

  public static final String CUSTOMER_ENDPOINT = EndpointConstants.API_V_0_1_CUSTOMERS;
  public static final String DEFAULT_PAYMENT_ENDPOINT =
      CUSTOMER_ENDPOINT + "/" + validCustomerId + "/payment-method";

  private final CustomerService customerService = Mockito.mock(CustomerService.class);
  private final DeleteAccountService deleteAccountService = Mockito.mock(DeleteAccountService.class);
  private final CustomerController controller = new CustomerController(customerService, deleteAccountService);
  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
      .setControllerAdvice(new ExceptionControllerAdvisor())
      .build();

  private Customer validCustomer;
  private UpdateCustomerDto validDto;
  private CreateCustomerDto validCreateCustomerDto;
  private PaymentMethod validPaymentMethod;
  private PaymentMethodDto validPaymentMethodDto;

  @BeforeEach
  void beforeEach() {
    Mockito.reset(customerService);

    // setup Customer objs
    validCustomer = new Customer();

    validCustomer.setId(validCustomerId);
    validCustomer.setFirstName("John");

    validCustomer.setLastName("Doe");
    validCustomer.setEmail("test@test.com");
    validCustomer.setPhoneNumber("999-999-9999");
    validCustomer.setTicketEmails(true);
    validCustomer.setFlightEmails(true);

    //SSUTO-13 - View their Loyalty Points
    validCustomer.setLoyaltyPoints(3);

    // setup Address
    Address validAddress = new Address();
    validAddress.setCardinality(1);
    validAddress.setId(1L);
    validAddress.setLine1("123 Main St.");
    validAddress.setLine2("Apt #1");
    validAddress.setCity("Las Vegas");
    validAddress.setState("NV");
    validAddress.setZipcode("12345");

    // add addr and empty payment methods
    validCustomer.setAddresses(Set.of(validAddress));

    validPaymentMethod = PaymentMethod.builder()
        .id(1L)
        .ownerId(validCustomer.getId())
        .accountNum("12345")
        .notes("payment notes")
        .build();

    validCustomer.setPaymentMethods(Set.of(validPaymentMethod));

    // setup DTOs
    validDto = UpdateCustomerDto.builder()
        .firstName(validCustomer.getFirstName())
        .lastName(validCustomer.getLastName())
        .email(validCustomer.getEmail())
        .addrLine1(validAddress.getLine1())
        .addrLine2(validAddress.getLine2())
        .city(validAddress.getCity())
        .state(validAddress.getState())
        .zipcode(validAddress.getZipcode())
        .ticketEmails(validCustomer.getTicketEmails())
        .flightEmails(validCustomer.getFlightEmails())
        .build();

    validPaymentMethodDto = PaymentMethodDto.builder()
        .accountNum(validPaymentMethod.getAccountNum())
        .notes(validPaymentMethod.getNotes())
        .build();

    validCreateCustomerDto = CreateCustomerDto.builder()
        .firstName(validCustomer.getFirstName())
        .lastName(validCustomer.getLastName())
        .email(validCustomer.getEmail())
        .password("abCD1234!@")
        .phoneNumber(validCustomer.getPhoneNumber())
        .addrLine1(validAddress.getLine1())
        .addrLine2(validAddress.getLine2())
        .city(validAddress.getCity())
        .state(validAddress.getState())
        .zipcode(validAddress.getZipcode())
        .build();
  }

  @Test
  void test_getAllCustomers_ReturnsListWith200StatusCode() throws Exception {
    when(customerService.getAllCustomers()).thenReturn(List.of(validCustomer));

    var result = mvc
        .perform(get(CUSTOMER_ENDPOINT))
        .andExpect(status().is(200))
        .andReturn();

    var response = Arrays
        .stream(jsonMapper.readValue(result.getResponse().getContentAsString(), Customer[].class))
        .collect(Collectors.toList());

    assertEquals(List.of(validCustomer), response);
  }

  @Test
  void test_getAllCustomers_ReturnsEmptyListWith204StatusCodeIfNoCustomers() throws Exception {
    when(customerService.getAllCustomers()).thenReturn(Collections.emptyList());

    mvc
        .perform(
            get(CUSTOMER_ENDPOINT))
        .andExpect(status().is(204));
  }

  @Test
  void test_getCustomerById_ReturnsValidCustomerWith200StatusCode() throws Exception {
    when(customerService.getCustomerById(validCustomer.getId())).thenReturn(validCustomer);

    var result = mvc
        .perform(
            get(CUSTOMER_ENDPOINT + "/" + validCustomer.getId()))
        .andExpect(status().is(200))
        .andReturn();

    var response = jsonMapper
        .readValue(result.getResponse().getContentAsString(), Customer.class);

    assertEquals(validCustomer, response);
  }

  @Test
  void test_getCustomerById_Returns404OnInvalidId() throws Exception {
    var randomId = UUID.randomUUID();
    when(customerService.getCustomerById(randomId)).thenThrow(new NoSuchCustomerException(randomId));

    mvc
        .perform(
            get(CUSTOMER_ENDPOINT + "/" + randomId))
        .andExpect(status().is(404));
  }

  //SSUTO-13
  @Test
  void test_getCustomerLoyaltyPoints_ReturnsValidCustomerLoyaltyPointsWith200StatusCode()
      throws Exception {
    when(customerService.getCustomerLoyaltyPoints(validCustomer.getId())).thenReturn(validCustomer.getLoyaltyPoints());

    var result = mvc
        .perform(
            get(CUSTOMER_ENDPOINT + "/loyalty/" + validCustomer.getId()))
        .andExpect(status().is(200))
        .andReturn();

    var response = jsonMapper
        .readValue(result.getResponse().getContentAsString(), Integer.class);

    assertEquals(validCustomer.getLoyaltyPoints(), response);
  }

  @Test
  void test_getCustomerLoyaltyPoints_Returns404StatusCode() throws Exception {
    var randomId = UUID.randomUUID();
    when(customerService.getCustomerLoyaltyPoints(randomId))
        .thenThrow(new NoSuchCustomerException(randomId));

    mvc
        .perform(
            get(CUSTOMER_ENDPOINT + "/loyalty/" + randomId ))
        .andExpect(status().is(404));
  }

  @Test
  void test_createNewCustomer_ReturnsCreatedIdAnd201StatusCodeOnValidDto() throws Exception {
    when(customerService.createNewCustomer(validCreateCustomerDto))
        .thenReturn(validCustomer);

    var headerName = "Location";
    var headerVal = CUSTOMER_ENDPOINT + "/" + validCustomer.getId();

    mvc
        .perform(
            post(CUSTOMER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validCreateCustomerDto)))
        .andExpect(status().is(201))
        .andExpect(header().string(headerName, headerVal));
  }

  //util
  boolean noValidationViolations(UpdateCustomerDto updateCustomerDto) {
    return Validation.buildDefaultValidatorFactory()
        .getValidator()
        .validate(updateCustomerDto)
        .isEmpty();
  }

  boolean noValidationViolations(PaymentMethodDto paymentMethodDto) {
    return Validation.buildDefaultValidatorFactory()
        .getValidator()
        .validate(paymentMethodDto)
        .isEmpty();
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidFirstName() {
    validDto.setFirstName(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setFirstName("");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidLastName() {
    validDto.setLastName(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setLastName("");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidEmail() {
    validDto.setEmail(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setEmail("");
    assertFalse(noValidationViolations(validDto));

    validDto.setEmail("asdfasdf");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidAddrLine1() {
    validDto.setAddrLine1(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setAddrLine1("");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidCity() {
    validDto.setCity(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setCity("");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidState() {
    validDto.setState(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setState("");
    assertFalse(noValidationViolations(validDto));

    validDto.setState("a");
    assertFalse(noValidationViolations(validDto));

    validDto.setState("aaa");
    assertFalse(noValidationViolations(validDto));
  }

  @Test
  void test_createNewCustomer_DoesNotAllowInvalidZipcode() {
    validDto.setZipcode(null);
    assertFalse(noValidationViolations(validDto));

    validDto.setZipcode("");
    assertFalse(noValidationViolations(validDto));

    validDto.setZipcode("asdfd-asdf");
    assertFalse(noValidationViolations(validDto));

    // test valid zipcodes as well
    validDto.setZipcode("12345-1234");
    assertTrue(noValidationViolations(validDto));

    validDto.setZipcode("12345");
    assertTrue(noValidationViolations(validDto));
  }

  @Test
  void test_updateExistingCustomer_Returns405OnMissingId() throws Exception {
    mvc
        .perform(
            put(CUSTOMER_ENDPOINT))
        .andExpect(status().is(405));
  }

  @Test
  void test_updateExistingCustomer_Returns404OnNonExistentCustomer() throws Exception {
    var randomId = UUID.randomUUID();
    when(customerService.updateCustomer(any(UUID.class), any(UpdateCustomerDto.class)))
        .thenThrow(new NoSuchCustomerException(randomId));

    mvc
        .perform(
            put(CUSTOMER_ENDPOINT + "/" + randomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validDto)))
        .andExpect(status().is(404));
  }

  @Test
  void test_updateExistingCustomer_Returns409OnDuplicateEmail() throws Exception {
    when(customerService.updateCustomer(validCustomer.getId(), validDto))
        .thenThrow(new DuplicateEmailException(validDto.getEmail()));

    mvc
        .perform(
            put(CUSTOMER_ENDPOINT + "/" + validCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validDto)))
        .andExpect(status().is(409));
  }


  @Test
  void test_updateExistingCustomer_Returns200StatusCodeOnSuccess() throws Exception {
    when(customerService.updateCustomer(any(UUID.class), any(UpdateCustomerDto.class))).thenReturn(validCustomer);

    mvc
        .perform(
            put(CUSTOMER_ENDPOINT + "/" + validCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validDto)))
        .andExpect(status().is(204));
  }

  @Test
  void test_deleteCustomer_Returns204StatusCode() throws Exception {
    mvc
        .perform(
            delete(CUSTOMER_ENDPOINT + "/" + validCustomerId))
        .andExpect(status().is(204));
  }

  // util for converting from JSON to obj from result
  PaymentMethod paymentMap(MvcResult result) throws Exception {
    return jsonMapper.readValue(result.getResponse()
                                    .getContentAsString(),
                                PaymentMethod.class);
  }

  @Test
  void test_getPaymentMethod_Returns200AndExpectedResultOnValidInput() throws Exception {
    when(customerService.getPaymentMethod(validCustomer.getId(), validPaymentMethod.getId()))
        .thenReturn(validPaymentMethod);

    var result = mvc
        .perform(
            get(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId()))
        .andExpect(status().is(200))
        .andReturn();
    var response = paymentMap(result);

    assertEquals(validPaymentMethod, response);
  }

  @Test
  void test_getPaymentMethod_Returns404OnNoSuchCustomerException() throws Exception {
    var randomId = UUID.randomUUID();
    when(customerService.getPaymentMethod(any(UUID.class), anyLong()))
        .thenThrow(new NoSuchCustomerException(randomId));

    mvc
        .perform(
            get(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId()))
        .andExpect(status().is(404));
  }

  @Test
  void test_getPaymentMethod_Returns404OnNoSuchPaymentMethodException() throws Exception {
    when(customerService.getPaymentMethod(validCustomer.getId(), -1L))
        .thenThrow(new NoSuchPaymentMethod(validCustomer.getId(), -1L));

    mvc
        .perform(
            get(DEFAULT_PAYMENT_ENDPOINT + "/-1"))
        .andExpect(status().is(404));
  }

  @Test
  void test_addPaymentMethod_Returns201AndURIOnValidDto() throws Exception {
    when(customerService.addPaymentMethod(validCustomer.getId(), validPaymentMethodDto))
        .thenReturn(validPaymentMethod.getId());

    var headerName = "Location";
    var headerVal = DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId();

    var result = mvc
        .perform(
            post(DEFAULT_PAYMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validPaymentMethodDto)))
        .andExpect(status().is(201))
        .andExpect(header().string(headerName, headerVal))
        .andReturn();

    assertTrue(result.getResponse().getContentAsString().isEmpty());
  }

  @Test
  void test_addPaymentMethod_Returns404OnNoSuchCustomerException() throws Exception {
    var randomId = UUID.randomUUID();
    when(customerService.addPaymentMethod(any(UUID.class), any(PaymentMethodDto.class)))
        .thenThrow(new NoSuchCustomerException(randomId));

    var result = mvc
        .perform(
            post(DEFAULT_PAYMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validPaymentMethodDto)))
        .andExpect(status().is(404))
        .andReturn();

    // should have a message
    assertFalse(result.getResponse().getContentAsString().isBlank());
  }

  @Test
  void test_addPaymentMethod_DoesNotAllowInvalidDto() {
    validPaymentMethodDto.setAccountNum(null);
    assertFalse(noValidationViolations(validPaymentMethodDto));

    validPaymentMethodDto.setAccountNum("");
    assertFalse(noValidationViolations(validPaymentMethodDto));
  }

  @Test
  void test_updatePaymentMethod_Returns204AndNoBodyOnValidDto() throws Exception {

    var result = mvc
        .perform(
            put(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validPaymentMethodDto)))
        .andExpect(status().is(204))
        .andReturn();

    assertTrue(result.getResponse().getContentAsString().isEmpty());
  }

  @Test
  void test_updatePaymentMethod_Returns404OnNoSuchCustomerException() throws Exception {
    doThrow(new NoSuchCustomerException(validCustomerId))
        .when(customerService)
        .updatePaymentMethod(any(UUID.class), anyLong(), any(PaymentMethodDto.class));

    var result = mvc
        .perform(
            put(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validPaymentMethodDto)))
        .andExpect(status().is(404))
        .andReturn();

    assertFalse(result.getResponse().getContentAsString().isBlank());
  }

  @Test
  void test_updatePaymentMethod_Returns404OnNoSuchPaymentMethodException() throws Exception {
    doThrow(new NoSuchPaymentMethod(validCustomerId, 1L))
        .when(customerService)
        .updatePaymentMethod(any(UUID.class), anyLong(), any(PaymentMethodDto.class));

    var result = mvc
        .perform(
            put(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validPaymentMethodDto)))
        .andExpect(status().is(404))
        .andReturn();

    assertFalse(result.getResponse().getContentAsString().isBlank());
  }

  @Test
  void test_removePaymentMethod_Returns204OnSuccess() throws Exception {
    var result = mvc
        .perform(
            delete(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId()))
        .andExpect(status().is(204))
        .andReturn();

    assertTrue(result.getResponse().getContentAsString().isEmpty());
  }

  @Test
  void test_removePaymentMethod_Returns404OnNoSuchCustomerException() throws Exception {
    doThrow(new NoSuchCustomerException(validCustomerId))
        .when(customerService)
        .removePaymentMethod(any(UUID.class), anyLong());

    var result = mvc
        .perform(
            delete(DEFAULT_PAYMENT_ENDPOINT + "/" + validPaymentMethod.getId()))
        .andExpect(status().is(404))
        .andReturn();

    assertFalse(result.getResponse().getContentAsString().isBlank());
  }
}