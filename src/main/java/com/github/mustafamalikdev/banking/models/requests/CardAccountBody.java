package com.github.mustafamalikdev.banking.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CardAccountBody(@NotNull Long accountNumber,
                              @NotBlank @Pattern(regexp = "\\d{6}") String sortCode) {
}
