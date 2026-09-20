package com.github.mustafamalikdev.banking.services.broker;

import com.github.mustafamalikdev.banking.config.RabbitMQConfig;
import com.github.mustafamalikdev.banking.events.transaction.ProcessTransactionPublisher;
import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQConsumer {

    private final ProcessTransactionPublisher processTransactionPublisher;

    @Autowired
    public RabbitMQConsumer(ProcessTransactionPublisher processTransactionPublisher) {
        this.processTransactionPublisher = processTransactionPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeMessage(final RabbitMQMessage message) {
        processTransactionPublisher.publish(message);
    }

}
