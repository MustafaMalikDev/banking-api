package com.github.mustafamalikdev.banking.controllers;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.requests.OtpBody;
import com.github.mustafamalikdev.banking.models.requests.PaymentRequestBody;
import com.github.mustafamalikdev.banking.services.security.OneTimeGeneratorService;
import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import com.github.mustafamalikdev.banking.services.validation.AccountVerificationService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/request")
public class PaymentRequestController {

    private final PaymentTransactionService paymentTransactionService;
    private final AccountVerificationService accountVerificationService;
    private final OneTimeGeneratorService oneTimeGeneratorService;

    @Autowired
    public PaymentRequestController(
        PaymentTransactionService paymentTransactionService,
        AccountVerificationService accountVerificationService,
        OneTimeGeneratorService oneTimeGeneratorService
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.accountVerificationService = accountVerificationService;
        this.oneTimeGeneratorService = oneTimeGeneratorService;
    }

    @PostMapping
    public ResponseEntity<String> requestPayment(@RequestBody PaymentRequestBody paymentRequestBody) {
        TransactionModel model = paymentTransactionService.getPendingTransaction(paymentRequestBody
                .getRequestId());

        if (model != null) {
            return ResponseEntity.status(
                    model.getErrorCode() ==
                        ErrorCode.PAYMENT_OK.getError() ? HttpStatusCode.valueOf(HttpServletResponse.SC_OK) :
                        HttpStatusCode.valueOf(HttpServletResponse.SC_BAD_REQUEST)
                    )
                 .contentType(MediaType.TEXT_PLAIN)
                 .body(ErrorCode.getMessage(model.getErrorCode()));
        } else {
            if (paymentTransactionService.storePendingTransaction(paymentRequestBody) != null) {
                ErrorCode processResult = accountVerificationService.validate(
                    paymentRequestBody.getRequestId());

                if (processResult == ErrorCode.PAYMENT_BEING_PROCESSED) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Your transaction is pending. It will be processed and authorised once the merchant " +
                                    "has been verified.");
                }

                if (processResult == ErrorCode.PAYMENT_REQUIRES_VERIFICATION) {
                    String otpToken = oneTimeGeneratorService.generateSimple();

                    return ResponseEntity.status(HttpStatusCode.valueOf(HttpServletResponse.SC_ACCEPTED))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new OtpBody(otpToken, ErrorCode.getMessage(processResult.getError())).toString());
                }

                if (processResult == ErrorCode.PAYMENT_OK) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(ErrorCode.getMessage(processResult.getError()));
                }
            }
        }

        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body("We could not process your transaction. Please try again later.");
    }
}
