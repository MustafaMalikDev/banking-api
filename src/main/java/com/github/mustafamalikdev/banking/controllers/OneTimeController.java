package com.github.mustafamalikdev.banking.controllers;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;
import com.github.mustafamalikdev.banking.services.broker.RabbitMQProducer;
import com.github.mustafamalikdev.banking.services.security.OneTimeGeneratorService;

import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/authorise")
public class OneTimeController {

    private final OneTimeGeneratorService oneTimeGeneratorService;
    private final PaymentTransactionService paymentTransactionService;
    private final RabbitMQProducer producer;

    @Autowired
    public OneTimeController(
        OneTimeGeneratorService oneTimeGeneratorService,
        PaymentTransactionService paymentTransactionService,
        RabbitMQProducer producer
    ) {
        this.oneTimeGeneratorService = oneTimeGeneratorService;
        this.paymentTransactionService = paymentTransactionService;
        this.producer = producer;
    }

    @GetMapping("/{paymentId}/{otpCode}")
    public ResponseEntity<?> handleOneTimeCode(@PathVariable String paymentId, @PathVariable int otpCode) {
        if (oneTimeGeneratorService.otpCodeValid(otpCode)) {
            oneTimeGeneratorService.discardSimple(otpCode);
            paymentTransactionService.updatePendingVerificationTransaction(paymentId,
                    ErrorCode.PAYMENT_OTP_VERIFIED);
            producer.sendMessage(new RabbitMQMessage(paymentId));

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Thank you for verifying your OTP code. Your transaction is now being processed.");
        }

        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Invalid OTP code or invalid payment ID. This transaction was not completed.");
    }
}
