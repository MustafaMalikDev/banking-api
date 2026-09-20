package com.github.mustafamalikdev.banking.services.components;

import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import com.github.mustafamalikdev.banking.repository.TransactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class PendingTransactionComponent {
    private final TransactionRepository transactionRepository;

    @Autowired
    public PendingTransactionComponent(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Cacheable(value = "idempotency", key = "#id", unless = "#result == null")
    public TransactionModel getPendingTransaction(String id) {
        return transactionRepository.findByRequestId(id);
    }

    @CacheEvict(value = "idempotency", key = "#id")
    public void removeFromCache(String id) {
    }
}
