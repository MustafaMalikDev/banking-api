package com.github.mustafamalikdev.banking;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.Location;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;
import com.github.mustafamalikdev.banking.models.requests.CardAccountBody;
import com.github.mustafamalikdev.banking.models.requests.MerchantRequestBody;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;
import com.github.mustafamalikdev.banking.services.broker.RabbitMQProducer;
import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import com.github.mustafamalikdev.banking.services.validation.AccountVerificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private RabbitMQProducer producer;

    @InjectMocks
    private AccountVerificationService accountVerificationService;

    private MerchantRequestBody dummyMerchant;

    @BeforeEach
    void setUp() {
        CardAccountBody validCard = new CardAccountBody(10482937L, "204511");
        ReflectionTestUtils.setField(accountVerificationService, "accountData", List.of(validCard));
        dummyMerchant = new MerchantRequestBody("LVMH Moët Hennessy",  Location.LOC_EUROPE, 884102L, "LVMH_MH_884102_EUROPE");
    }

    @Test
    void validate_WhenAccountValidAndAmountUnder150_ShouldReturnProcessing() {
        String requestId = "REQ-101";
        CardAccountBody customer = new CardAccountBody(10482937L, "204511");
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(customer);
        model.setMerchant(dummyMerchant);
        model.setAmount(100.0f);
        model.setErrorCode(ErrorCode.PAYMENT_UNKNOWN_ERROR);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = accountVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_BEING_PROCESSED, result);
        assertEquals(ErrorCode.PAYMENT_BEING_PROCESSED.getError(), model.getErrorCode());
        verify(paymentTransactionService).updatePendingTransaction(model);
        verify(producer).sendMessage(any(RabbitMQMessage.class));
    }

    @Test
    void validate_WhenAccountValidAndAmountOver150_ShouldReturnRequiresVerification() {
        String requestId = "REQ-102";
        CardAccountBody customer = new CardAccountBody(10482937L, "204511");
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(customer);
        model.setMerchant(dummyMerchant);
        model.setAmount(200.0f);
        model.setErrorCode(ErrorCode.PAYMENT_UNKNOWN_ERROR);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = accountVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_REQUIRES_VERIFICATION, result);
        assertEquals(ErrorCode.PAYMENT_REQUIRES_VERIFICATION.getError(), model.getErrorCode());
        verify(paymentTransactionService).updatePendingTransaction(model);
        verify(producer, never()).sendMessage(any(RabbitMQMessage.class));
    }

    @Test
    void validate_WhenAccountInvalid_ShouldReturnFraudDetected() {
        String requestId = "REQ-103";
        CardAccountBody customer = new CardAccountBody(99999999L, "000000");
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(customer);
        model.setMerchant(dummyMerchant);
        model.setAmount(50.0f);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = accountVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_FRAUD_DETECTED, result);
        verify(paymentTransactionService).updatePendingTransaction(model);
        verify(producer, never()).sendMessage(any(RabbitMQMessage.class));
    }
}