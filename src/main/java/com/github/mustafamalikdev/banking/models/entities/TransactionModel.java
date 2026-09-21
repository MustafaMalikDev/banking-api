package com.github.mustafamalikdev.banking.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.Location;
import com.github.mustafamalikdev.banking.models.requests.CardAccountBody;
import com.github.mustafamalikdev.banking.models.requests.MerchantRequestBody;
import com.github.mustafamalikdev.banking.models.requests.PaymentRequestBody;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.persistence.Transient;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@JsonIgnoreProperties({"isNew"})
@Table(name = "idempotency")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionModel implements Persistable<String> {
    @Id
    private String requestId;

    @Embedded
    private CardAccountBody customer;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "location", column = @Column(name = "merchant_location"))
    })
    private MerchantRequestBody merchant;

    @CreatedDate
    private LocalDateTime executedAt;

    private Location location;

    @Setter(AccessLevel.NONE)
    @Column(name = "error_code")
    private int errorCode;

    private double amount;

    @Transient
    @Getter(AccessLevel.NONE)
    private Boolean isNew = true;

    public TransactionModel setModelFromPaymentRequest(PaymentRequestBody body) {
        requestId = body.getRequestId();
        customer = body.getCustomer();
        merchant = body.getMerchant();
        location = body.getLocation();
        amount = body.getAmount();

        return this;
    }

    public TransactionModel setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode.getError();

        return this;
    }

    @Override
    public @Nullable String getId() {
        return requestId;
    }

    @PostLoad
    @PrePersist
    void markPersisted() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return Boolean.TRUE.equals(this.isNew);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s", requestId, customer, merchant, executedAt, location);
    }
}
