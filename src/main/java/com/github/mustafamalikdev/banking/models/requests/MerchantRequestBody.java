package com.github.mustafamalikdev.banking.models.requests;

import com.github.mustafamalikdev.banking.models.Location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantRequestBody(
        @NotBlank String merchantName,
        @NotNull Location location,
        @NotNull Long merchantId,
        @NotBlank String merchantTaxId
) {}