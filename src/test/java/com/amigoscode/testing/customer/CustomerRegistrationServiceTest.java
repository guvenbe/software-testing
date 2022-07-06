package com.amigoscode.testing.customer;

import com.amigoscode.testing.utils.PhoneNumberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CustomerRegistrationServiceTest {

    private CustomerRegistrationService underTest;
    @Mock
    private CustomerRepository mockCustomerRepository;

    @Mock
    private PhoneNumberValidator phoneNumberValidator;

    @Captor
    private ArgumentCaptor<Customer> customerArgumentCaptor;

    //private CustomerRepository mockCustomerRepository = mock(CustomerRepository.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new CustomerRegistrationService(mockCustomerRepository, phoneNumberValidator);
    }

    @Test
    void itShouldSaveNewCustomer() {
        //Given phone number and customer
        String phoneNumber = "000099";
        Customer customer = new Customer(UUID.randomUUID(),"Maryam", phoneNumber);
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);
        // ... No Customer with phone number passed
        given(mockCustomerRepository
                .selectCustomerByPhoneNumber(phoneNumber))
                .willReturn(Optional.empty());
        // ... Valid phone number
        given(phoneNumberValidator.test(phoneNumber)).willReturn(true);

        //When
        underTest.registerNewCustomer(request);
        //Then
        then(mockCustomerRepository).should().save(customerArgumentCaptor.capture());
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();
        assertThat(customerArgumentCaptorValue).isEqualTo(customer);

    }


    @Test
    void itShouldNotSaveCustomerWhenCustomerExists() {
        //Given phone number and customer
        String phoneNumber = "000099";
        Customer customer = new Customer(UUID.randomUUID(),"Maryam", phoneNumber);
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);
        // ... an existing customer is returned
        given(mockCustomerRepository
                .selectCustomerByPhoneNumber(phoneNumber))
                .willReturn(Optional.of(customer));
        // ... Valid phone number
        given(phoneNumberValidator.test(phoneNumber)).willReturn(true);
        //When
        underTest.registerNewCustomer(request);

        //Then
        //then(mockCustomerRepository).should(never()).save(any());
        then(mockCustomerRepository).should().selectCustomerByPhoneNumber(phoneNumber);
        then(mockCustomerRepository).shouldHaveNoMoreInteractions();


    }

    @Test
    void itShouldThrowWhenPhoneNumberIsTaken() {
        //Given phone number and customer
        String phoneNumber = "000099";
        Customer customer = new Customer(UUID.randomUUID(),"Maryam", phoneNumber);
        Customer customerTwo = new Customer(UUID.randomUUID(),"John", phoneNumber);
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);
        // ... an existing customer is returned
        given(mockCustomerRepository
                .selectCustomerByPhoneNumber(phoneNumber))
                .willReturn(Optional.of(customerTwo));
        // ... Valid phone number
        given(phoneNumberValidator.test(phoneNumber)).willReturn(true);
        //When

        //Then
        assertThatThrownBy(() ->underTest.registerNewCustomer(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format("phone number [%s] is taken",phoneNumber));

        //Finally
        then(mockCustomerRepository).should(never()).save(any(Customer.class));
    }

    @Test
    void itShouldSaveNewCustomerWhenIdIsNull() {
        //Given phone number and customer
        String phoneNumber = "000099";
        Customer customer = new Customer(null,"Maryam", phoneNumber);
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);
        // ... No Customer with phone number passed
        given(mockCustomerRepository
                .selectCustomerByPhoneNumber(phoneNumber))
                .willReturn(Optional.empty());
        // ... Valid phone number
        given(phoneNumberValidator.test(phoneNumber)).willReturn(true);
        //When
        underTest.registerNewCustomer(request);
        //Then
        then(mockCustomerRepository).should().save(customerArgumentCaptor.capture());
        Customer customerArgumentCaptorValue = customerArgumentCaptor.getValue();
        assertThat(customerArgumentCaptorValue)
                .isEqualToIgnoringGivenFields(customer, "id");
        assertThat(customerArgumentCaptorValue.getId()).isNotNull();

    }

    @Test
    void itShouldNotSaveNewCustomerWhenPhoneNumberIsNotValid() {
        //Given phone number and customer
        String phoneNumber = "000099";
        Customer customer = new Customer(UUID.randomUUID(),"Maryam", phoneNumber);
        // ... a request
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(customer);

        // ... Valid phone number
        given(phoneNumberValidator.test(phoneNumber)).willReturn(false);

        //When
        assertThatThrownBy(()->underTest.registerNewCustomer(request))
                .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Phone Number " + phoneNumber + " is not valid");
        //Then
        then(mockCustomerRepository).shouldHaveNoMoreInteractions();

    }
}