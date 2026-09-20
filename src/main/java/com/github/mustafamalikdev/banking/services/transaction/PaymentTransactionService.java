package com.github.mustafamalikdev.banking.services.transaction;

import com.github.mustafamalikdev.banking.models.ErrorCode;
import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.models.requests.PaymentRequestBody;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;
import com.github.mustafamalikdev.banking.services.broker.RabbitMQProducer;

import com.github.mustafamalikdev.banking.services.components.PendingTransactionComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionService {

    private final TransactionRepository transactionRepository;
    private final PendingTransactionComponent pendingTransactionComponent;
    private final RabbitMQProducer producer;

    @Autowired
    public PaymentTransactionService(
       TransactionRepository transactionRepository,
       PendingTransactionComponent pendingTransactionComponent,
       RabbitMQProducer producer
    ) {
        this.transactionRepository = transactionRepository;
        this.pendingTransactionComponent = pendingTransactionComponent;
        this.producer = producer;
    }

    public TransactionModel getPendingTransaction(String id) {
        return pendingTransactionComponent.getPendingTransaction(id);
    }

    @CachePut(value = "idempotency", key = "#paymentRequestBody.requestId", unless = "#result == null")
    public TransactionModel storePendingTransaction(PaymentRequestBody paymentRequestBody) {
        TransactionModel model = getPendingTransaction(paymentRequestBody.getRequestId());

        if (model != null) {
            return null;
        }

        return transactionRepository.save(new TransactionModel()
            .setModelFromPaymentRequest(paymentRequestBody)
            .setErrorCode(ErrorCode.PAYMENT_BEING_PROCESSED)
        );
    }

    @Transactional("transactionManager")
    public void updatePendingVerificationTransaction(String paymentId, ErrorCode errorCode) {
        TransactionModel model = this.getPendingTransaction(paymentId);

        if (model != null) {
            model.setErrorCode(errorCode);
            updatePendingTransaction(model);
        }
    }

    public void updatePendingTransactionFraud(TransactionModel model) {
        transactionRepository.save(model);
        pendingTransactionComponent.removeFromCache(model.getRequestId());
    }

    public void updatePendingTransaction(TransactionModel model) {
        if (model == null) {
            return;
        }

        transactionRepository.save(model);
        pendingTransactionComponent.removeFromCache(model.getRequestId());
    }
}
