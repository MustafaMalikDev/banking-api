package com.github.mustafamalikdev.banking.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    PAYMENT_BEING_PROCESSED(1),
    PAYMENT_REQUIRES_VERIFICATION(2),
    PAYMENT_FRAUD_DETECTED(3),
    PAYMENT_INVALID_INFO(4),
    PAYMENT_UNKNOWN_ERROR(5),
    PAYMENT_OTP_VERIFIED(6),
    PAYMENT_OK(-1);

    private final int error;

    public static String getMessage(int code) {
        switch (code) {
            case 1 -> {
                return "Your transaction is being processed. It will be authorised once the payment information has " +
                        "been verified.";
            }
            case 2 -> {
                return "This transaction requires verification. Please submit OTP code to confirm.";
            }
            case 3 -> {
                return "This transaction was declined. Fraudulent activity was detected.";
            }
            case 4 -> {
                return "This transaction was declined, invalid payment information was provided. Please try again.";
            }
            case 5 -> {
                return "An unknown error occurred and we could not process your transaction.";
            }
            case 6 -> {
                return "OTP is verified. Your transaction is being processed.";
            }
            case -1 -> {
                return "Transaction approved. Thank you for your purchase.";
            }
            default -> {
                return "";
            }
        }
    }
}
