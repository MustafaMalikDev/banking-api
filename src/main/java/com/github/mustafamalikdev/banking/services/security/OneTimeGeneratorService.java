package com.github.mustafamalikdev.banking.services.security;

import com.github.mustafamalikdev.banking.services.components.OneTimeGeneratorComponent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OneTimeGeneratorService {

    private static final String CACHE_PREFIX = "otp::";

    private final OneTimeGeneratorComponent oneTimeGeneratorComponent;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public OneTimeGeneratorService(
            OneTimeGeneratorComponent oneTimeGeneratorComponent,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.oneTimeGeneratorComponent = oneTimeGeneratorComponent;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String generateSimple() {
        int code = ThreadLocalRandom.current().nextInt(1000, 10000);
        String hashCode = Base64.getEncoder().encodeToString(String.valueOf(code).getBytes(StandardCharsets.UTF_8));

        stringRedisTemplate.opsForValue().set(CACHE_PREFIX + hashCode, hashCode, Duration.ofMinutes(5));
        System.out.println("OTP code is: " + this.decodeSimple(hashCode));

        return hashCode;
    }

    public Optional<String> getSimple(String token) {
        return oneTimeGeneratorComponent.getSimple(token);
    }

    public void discardSimple(int code) {
        String token = Base64.getEncoder()
                .encodeToString(String.valueOf(code).getBytes(StandardCharsets.UTF_8));

        discardSimple(token);
    }

    public void discardSimple(String token) {
        stringRedisTemplate.delete(CACHE_PREFIX + token);
        oneTimeGeneratorComponent.discardSimple(token);
    }

    public int decodeSimple(String base64Token) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Token);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
        return Integer.parseInt(decodedString);
    }

    private List<String> getAllOtpCodes() {
        Set<String> keys = stringRedisTemplate.keys(CACHE_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> rawValues = stringRedisTemplate.opsForValue().multiGet(keys);

        if (rawValues == null) {
            return Collections.emptyList();
        }

        return rawValues.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean otpCodeValid(int code) {
        String targetToken = Base64.getEncoder()
                .encodeToString(String.valueOf(code).getBytes(StandardCharsets.UTF_8));

        Boolean exists = stringRedisTemplate.hasKey(CACHE_PREFIX + targetToken);
        return Boolean.TRUE.equals(exists);
    }
}