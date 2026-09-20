package com.github.mustafamalikdev.banking.services;

import com.github.mustafamalikdev.banking.models.ErrorCode;

public interface Verification {

    void reconcileData();

    ErrorCode validate(String requestId);

}
