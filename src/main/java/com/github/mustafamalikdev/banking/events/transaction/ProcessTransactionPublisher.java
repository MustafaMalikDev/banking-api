package com.github.mustafamalikdev.banking.events.transaction;

import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ProcessTransactionPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public ProcessTransactionPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(final RabbitMQMessage message) {
        ProcessTransactionEvent processTransactionEvent =
                new ProcessTransactionEvent(this, message);
        applicationEventPublisher.publishEvent(processTransactionEvent);
    }
}
