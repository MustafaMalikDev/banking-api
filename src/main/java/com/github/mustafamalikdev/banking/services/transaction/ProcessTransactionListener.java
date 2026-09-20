package com.github.mustafamalikdev.banking.services.transaction;

import com.github.mustafamalikdev.banking.events.transaction.ProcessTransactionEvent;
import com.github.mustafamalikdev.banking.services.validation.MerchantVerificationService;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ProcessTransactionListener implements ApplicationListener<ProcessTransactionEvent> {

    private final MerchantVerificationService merchantVerificationService;

    @Autowired
    public ProcessTransactionListener(MerchantVerificationService merchantVerificationService) {
        this.merchantVerificationService = merchantVerificationService;
    }

    @Async
    @Override
    public void onApplicationEvent(@NonNull ProcessTransactionEvent event) {
        merchantVerificationService.validate(event.getMessage().paymentId());
    }
}
