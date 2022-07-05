package com.amigoscode.testing.payment;

import com.amigoscode.testing.customer.Customer;
import com.amigoscode.testing.customer.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class PaymentServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CardPaymentCharger cardPaymentCharger;

    private PaymentService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new PaymentService(
                customerRepository,
                paymentRepository,
                cardPaymentCharger);
    }

    @Test
    void itShouldChargeCardSuccessfully() {
        //Given
        UUID customerId = UUID.randomUUID();

        // ...Customer exists
        given(customerRepository
                .findById(customerId))
                .willReturn(Optional.of(mock(Customer.class)));

        // ... Payment request
        PaymentRequest paymentRequest = new PaymentRequest(
                new Payment(
                    null,
                    null,
                        new BigDecimal("100.00"),
                        Currency.USD,
                    "card123",
                    "Donation"
                )
        );

        // ... Card is charged successfully
        given(cardPaymentCharger.chargeCard(
                paymentRequest.getPayment().getSource(),
                paymentRequest.getPayment().getAmount(),
                paymentRequest.getPayment().getCurrency(),
                paymentRequest.getPayment().getDescription()
        )).willReturn( new CardPaymentCharge(true));

        //When
        underTest.chargeCard(customerId,paymentRequest);
        //Then

        ArgumentCaptor<Payment> paymentArgumentCaptor = ArgumentCaptor.forClass(Payment.class);

        then(paymentRepository).should().save(paymentArgumentCaptor.capture());
        Payment paymentArgumentCaptorValue = paymentArgumentCaptor.getValue();
        assertThat(paymentArgumentCaptorValue)
                .isEqualToIgnoringGivenFields(
                        paymentRequest.getPayment(), "customerId");
        assertThat(paymentArgumentCaptorValue.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void itShouldThrowWhenCardIsNotCharged() {

        //Given
        UUID customerId = UUID.randomUUID();

        // ...Customer exists
        given(customerRepository
                .findById(customerId))
                .willReturn(Optional.of(mock(Customer.class)));

        // ... Payment request
        PaymentRequest paymentRequest = new PaymentRequest(
                new Payment(
                        null,
                        null,
                        new BigDecimal("100.00"),
                        Currency.USD,
                        "card123",
                        "Donation"
                )
        );

        // ... Card is not charged successfully
        given(cardPaymentCharger.chargeCard(
                paymentRequest.getPayment().getSource(),
                paymentRequest.getPayment().getAmount(),
                paymentRequest.getPayment().getCurrency(),
                paymentRequest.getPayment().getDescription()
        )).willReturn( new CardPaymentCharge(false));

        //When
        //Then

        assertThatThrownBy(
                ()->underTest.chargeCard(customerId,paymentRequest))
                .hasMessageContaining("Card is not debited for customer " + customerId)
                .isInstanceOf(IllegalStateException.class);

        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    void itShouldNotChargeCardAndThrowWhenCurrencyNotSupported() {
        //Given
        UUID customerId = UUID.randomUUID();

        // ...Customer exists
        given(customerRepository
                .findById(customerId))
                .willReturn(Optional.of(mock(Customer.class)));

        // ... EUROS not supported
        Currency currency = Currency.EUR;
        // ... Payment request
        PaymentRequest paymentRequest = new PaymentRequest(
                new Payment(
                        null,
                        null,
                        new BigDecimal("100.00"),
                        currency,
                        "card123",
                        "Donation"
                )
        );
       //When
        assertThatThrownBy(
                ()->underTest.chargeCard(customerId,paymentRequest))
                .hasMessageContaining("Currency [" + currency + "] not supported")
                .isInstanceOf(IllegalStateException.class);

        //Then
        // ... No interaction with cardPaymentCharger
        then(cardPaymentCharger).shouldHaveNoInteractions();

        then(paymentRepository).shouldHaveNoMoreInteractions();

    }

    @Test
    void itShouldNotChargeAndThrowWhenCustomerNotFound() {
        //Given
        UUID customerId = UUID.randomUUID();
        // Customer not found in DB
        given(customerRepository.findById(customerId)).willReturn(Optional.empty());
        //When
        assertThatThrownBy(
                ()->underTest.chargeCard(customerId, any()))
                .hasMessageContaining("Customer with id [" + customerId + "] not found")
                .isInstanceOf(IllegalStateException.class);

        //Then
        // ... No interaction with cardPaymentCharger
        then(cardPaymentCharger).shouldHaveNoInteractions();

        then(paymentRepository).shouldHaveNoMoreInteractions();
    }
}