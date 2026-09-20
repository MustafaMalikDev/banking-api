package com.github.mustafamalikdev.banking.services.components;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class OneTimeGeneratorComponent {

    @Cacheable(value = "otp", key = "#token", unless = "#token == null")
    public Optional<String> getSimple(String token) {
        return Optional.empty();
    }

    @CacheEvict(value = "otp", key = "#token")
    public void discardSimple(String token) {
        System.out.println("Discarded OTP code from Redis In-Memory Database");
    }
}
