package com.github.mustafamalikdev.banking.services;

import com.github.mustafamalikdev.banking.models.ErrorCode;

public abstract class PaymentValidation implements Verification {

    public PaymentValidation() { }

    @Override
    public ErrorCode validate(String requestId) {
        reconcileData();
        return ErrorCode.PAYMENT_BEING_PROCESSED;
    }
}
