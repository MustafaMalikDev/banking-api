package com.github.mustafamalikdev.banking.models.requests;

import com.github.mustafamalikdev.banking.models.Location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public final class PaymentRequestBody {

    private MerchantRequestBody merchant;
    private CardAccountBody customer;

    private String requestId;
    private Location location;

    private double amount;

    @Override
    public String toString() {
        return String.format("MERCHANT INFO:\n%s\nCUSTOMER INFO:\n%s\nREQUEST ID: %s\nLOCATION: %s\n",
            merchant, customer, requestId, location
        );
    }
}
