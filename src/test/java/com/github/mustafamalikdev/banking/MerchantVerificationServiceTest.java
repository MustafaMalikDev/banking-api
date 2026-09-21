package com.github.mustafamalikdev.banking;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.Location;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.requests.CardAccountBody;
import com.github.mustafamalikdev.banking.models.requests.MerchantRequestBody;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;
import com.github.mustafamalikdev.banking.services.security.OneTimeGeneratorService;
import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import com.github.mustafamalikdev.banking.services.validation.MerchantVerificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantVerificationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OneTimeGeneratorService oneTimeGeneratorService;

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @InjectMocks
    private MerchantVerificationService merchantVerificationService;

    private MerchantRequestBody validMerchant;
    private CardAccountBody dummyCustomer;

    @BeforeEach
    void setUp() {
        validMerchant = new MerchantRequestBody("LVMH Moët Hennessy", Location.LOC_EUROPE, 884102L, "LVMH_MH_884102_EUROPE");
        dummyCustomer = new CardAccountBody(10482937L, "204511");
        ReflectionTestUtils.setField(merchantVerificationService, "merchantData", List.of(validMerchant));
    }

    @Test
    void validate_WhenMerchantValidAndLocationMismatch_ShouldReturnRequiresVerification() {
        String requestId = "REQ-201";
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(dummyCustomer);
        model.setMerchant(validMerchant);
        model.setLocation(Location.LOC_NORTH_AMERICA);
        model.setErrorCode(ErrorCode.PAYMENT_UNKNOWN_ERROR);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = merchantVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_REQUIRES_VERIFICATION, result);
        assertEquals(ErrorCode.PAYMENT_REQUIRES_VERIFICATION.getError(), model.getErrorCode());
        verify(paymentTransactionService).updatePendingTransaction(model);
        verify(oneTimeGeneratorService).generateSimple();
    }

    @Test
    void validate_WhenMerchantValidAndOtpVerified_ShouldReturnPaymentOk() {
        String requestId = "REQ-202";
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(dummyCustomer);
        model.setMerchant(validMerchant);
        model.setLocation(Location.LOC_EUROPE);
        model.setErrorCode(ErrorCode.PAYMENT_OTP_VERIFIED);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = merchantVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_OK, result);
        assertEquals(ErrorCode.PAYMENT_OK.getError(), model.getErrorCode());
        verify(paymentTransactionService).updatePendingTransaction(model);
    }

    @Test
    void validate_WhenMerchantInvalid_ShouldReturnFraudDetected() {
        String requestId = "REQ-203";
        MerchantRequestBody invalidMerchant = new MerchantRequestBody("Unknown Corp", Location.LOC_EUROPE, 999999L, "UNKNOWN_TAX");
        TransactionModel model = new TransactionModel();
        model.setRequestId(requestId);
        model.setCustomer(dummyCustomer);
        model.setMerchant(invalidMerchant);

        when(transactionRepository.findByRequestId(requestId)).thenReturn(model);

        ErrorCode result = merchantVerificationService.validate(requestId);

        assertEquals(ErrorCode.PAYMENT_FRAUD_DETECTED, result);
        assertEquals(ErrorCode.PAYMENT_FRAUD_DETECTED.getError(), model.getErrorCode());
        verify(paymentTransactionService).updatePendingTransactionFraud(model);
    }
}
