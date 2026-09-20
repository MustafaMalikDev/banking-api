package com.github.mustafamalikdev.banking.events.transaction;

import com.github.mustafamalikdev.banking.models.rabbitmq.RabbitMQMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProcessTransactionEvent extends ApplicationEvent {

    private final RabbitMQMessage message;

    public ProcessTransactionEvent(Object source, RabbitMQMessage message) {
        super(source);
        this.message = message;
    }

}