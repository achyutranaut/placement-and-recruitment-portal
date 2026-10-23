package com.placement.portal.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login attempt tracker to mitigate brute-force and credential stuffing attacks.
 * Tracks failed attempts within a 15-minute sliding window without requiring external dependencies like Redis.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MILLIS = 15 * 60 * 1000L; // 15 minutes

    private static class AttemptInfo {
        int attempts;
        Instant firstAttempt;
        Instant lockedUntil;

        AttemptInfo(Instant now) {
            this.attempts = 1;
            this.firstAttempt = now;
        }
    }

    private final ConcurrentHashMap<String, AttemptInfo> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null && !key.isBlank()) {
            attemptsCache.remove(key.toLowerCase().trim());
        }
    }

    public void loginFailed(String key) {
        if (key == null || key.isBlank()) return;
        String cleanKey = key.toLowerCase().trim();
        Instant now = Instant.now();

        attemptsCache.compute(cleanKey, (k, info) -> {
            if (info == null || (now.toEpochMilli() - info.firstAttempt.toEpochMilli() > LOCK_DURATION_MILLIS)) {
                return new AttemptInfo(now);
            }
            info.attempts++;
            if (info.attempts >= MAX_ATTEMPTS) {
                info.lockedUntil = now.plusMillis(LOCK_DURATION_MILLIS);
            }
            return info;
        });
    }

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) return false;
        String cleanKey = key.toLowerCase().trim();
        AttemptInfo info = attemptsCache.get(cleanKey);
        if (info == null) return false;

        Instant now = Instant.now();
        if (info.lockedUntil != null) {
            if (now.isBefore(info.lockedUntil)) {
                return true;
            } else {
                attemptsCache.remove(cleanKey);
                return false;
            }
        }
        if (now.toEpochMilli() - info.firstAttempt.toEpochMilli() > LOCK_DURATION_MILLIS) {
            attemptsCache.remove(cleanKey);
            return false;
        }
        return false;
    }

    public void reset() {
        attemptsCache.clear();
    }
}
