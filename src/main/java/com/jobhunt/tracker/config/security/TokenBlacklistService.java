package com.jobhunt.tracker.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    public void blacklist(String jti, long ttlMillis) {
        if (ttlMillis <= 0) {
            log.debug("Token already expired, skipping blacklist. jti={}", jti);
            return;
        }

        String key = BLACKLIST_PREFIX + jti;

        redisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);

        log.debug("Access token blacklisted. jti={}, ttl={}ms", jti, ttlMillis);
    }

    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }
}