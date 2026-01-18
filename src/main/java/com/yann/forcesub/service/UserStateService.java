package com.yann.forcesub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserStateService {

    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    public void setState(Long userId, String state) {
        if (userId == null || state == null || state.isEmpty()) {
            log.warn("Invalid state or userId: userId={}, state={}", userId, state);
            return;
        }
        userStates.put(userId, state);
        log.debug("User {} state set to: {}", userId, state);
    }

    public String getState(Long userId) {
        return userId != null ? userStates.get(userId) : null;
    }

    public void clearState(Long userId) {
        if (userId != null) {
            userStates.remove(userId);
            log.debug("User {} state cleared", userId);
        }
    }

}