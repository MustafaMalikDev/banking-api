package com.github.mustafamalikdev.banking.repository;

import com.github.mustafamalikdev.banking.models.entities.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, String> {

    // https://medium.com/@AlexanderObregon/using-spring-boot-with-redis-for-caching-and-data-storage-53f3f8d971fb

    TransactionModel findByRequestId(String requestId);


}
