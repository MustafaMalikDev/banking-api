package com.github.mustafamalikdev.banking.models.rabbitmq;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record RabbitMQMessage(
    @JsonProperty("paymentId") String paymentId
) implements Serializable {
}
