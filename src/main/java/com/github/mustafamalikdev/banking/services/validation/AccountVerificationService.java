package com.github.mustafamalikdev.banking.services.validation;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;
import com.github.mustafamalikdev.banking.models.requests.CardAccountBody;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;
import com.github.mustafamalikdev.banking.services.PaymentValidation;
import com.github.mustafamalikdev.banking.services.broker.RabbitMQProducer;
import com.github.mustafamalikdev.banking.services.transaction.PaymentTransactionService;
import com.github.mustafamalikdev.banking.util.FileHelper;
import com.github.mustafamalikdev.banking.util.JsonFileHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AccountVerificationService extends PaymentValidation {

    private List<CardAccountBody> accountData;

    @Value("classpath:models/customers/customers.json")
    private Resource resourceJson;
    private final TransactionRepository transactionRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final RabbitMQProducer producer;

    @Autowired
    public AccountVerificationService(
        TransactionRepository transactionRepository,
        RabbitMQProducer producer,
        PaymentTransactionService paymentTransactionService
    ) {
        this.transactionRepository = transactionRepository;
        this.producer = producer;
        this.paymentTransactionService = paymentTransactionService;
    }

    private boolean isAccountValid(final CardAccountBody cardInfo) {
        if (cardInfo == null || accountData.isEmpty()) {
            return false;
        }

        return accountData.stream()
                .anyMatch(card -> (
                    Objects.equals(card.accountNumber(), cardInfo.accountNumber()) &&
                    card.sortCode().equalsIgnoreCase(cardInfo.sortCode())
                ));
    }

    private void updateModelData(TransactionModel model) {
        paymentTransactionService.updatePendingTransaction(model);
        FileHelper.writeTransactionLog(model);
    }

    @Override
    public void reconcileData() {
        if (accountData == null) {
            try {
                accountData = JsonFileHelper.getList(resourceJson, CardAccountBody.class);
                System.out.println(Arrays.toString(accountData.toArray()));
            } catch (Exception ignored) { }
        }
    }

    @Override
    public ErrorCode validate(String requestId) {
        super.validate(requestId);

        ErrorCode errorCode;
        TransactionModel model = transactionRepository.findByRequestId(requestId);

        if (model.getErrorCode() == ErrorCode.PAYMENT_OTP_VERIFIED.getError()) {
            producer.sendMessage(new RabbitMQMessage(requestId));
        }

        if (isAccountValid(model.getCustomer())) {
            if (model.getErrorCode() != ErrorCode.PAYMENT_OTP_VERIFIED.getError() && model.getAmount() >= 150.0f) {
                errorCode = ErrorCode.PAYMENT_REQUIRES_VERIFICATION;
            } else {
                errorCode = ErrorCode.PAYMENT_BEING_PROCESSED;
            }

            model.setErrorCode(errorCode);
        } else {
            errorCode = ErrorCode.PAYMENT_FRAUD_DETECTED;
            model.setErrorCode(ErrorCode.PAYMENT_FRAUD_DETECTED);
        }

        updateModelData(model);

        if (errorCode == ErrorCode.PAYMENT_BEING_PROCESSED) {
            producer.sendMessage(new RabbitMQMessage(requestId));
        }

        return errorCode;
    }
}
