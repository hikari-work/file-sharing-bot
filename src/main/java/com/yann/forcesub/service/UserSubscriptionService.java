package com.yann.forcesub.service;

import com.yann.forcesub.event.ChannelUpdateEvent;
import com.yann.forcesub.event.UserSubscriptionEvent;
import com.yann.forcesub.service.telegram.TelegramService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private final ChannelService channelService;
    private final TelegramService membershipChecker;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${forcesub.subscription.ttl.minutes:5}")
    private int ttlMinutes;

    private final Set<Long> channelIds = ConcurrentHashMap.newKeySet();
    private final Map<Long, Map<Long, Instant>> userChannelExpiry = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        channelService.findAll()
                .doOnNext(channel -> channelIds.add(channel.getId()))
                .doOnError(e -> log.error("Error loading channels: {}", e.getMessage()))
                .doOnComplete(() -> log.info("Loaded {} channels", channelIds.size()))
                .subscribe();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "subscription-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredSubscriptions,
                1,
                1,
                TimeUnit.MINUTES
        );

        log.info("Subscription TTL set to {} minutes", ttlMinutes);
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void addChannelId(Long id, boolean isAdd) {
        if (isAdd) {
            channelIds.add(id);
        } else {
            channelIds.remove(id);
        }
    }

    public CompletableFuture<Boolean> isUserSubscribedAsync(Long userId, List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        Map<Long, Instant> channels = userChannelExpiry.get(userId);
        Instant now = Instant.now();

        List<Long> channelsToCheck = new ArrayList<>();
        boolean allCached = true;


        for (Long channelId : channelIds) {
            if (channels == null || !channels.containsKey(channelId)) {
                channelsToCheck.add(channelId);
                allCached = false;
            } else {
                Instant expiry = channels.get(channelId);
                if (now.isAfter(expiry)) {
                    channelsToCheck.add(channelId);
                    allCached = false;

                    channels.remove(channelId);
                    if (channels.isEmpty()) {
                        userChannelExpiry.remove(userId);
                    }
                }
            }
        }
        if (allCached) {
            log.debug("All {} channels cached for user {}", channelIds.size(), userId);
            return CompletableFuture.completedFuture(true);
        }

        log.debug("Checking {} channels for user {} via Telegram", channelsToCheck.size(), userId);

        return membershipChecker.checkMultipleChannels(userId, channelsToCheck)
                .thenApply(results -> {
                    boolean allJoined = results.values().stream().allMatch(Boolean::booleanValue);

                    results.forEach((channelId, joined) -> {
                        applicationEventPublisher.publishEvent(
                                new UserSubscriptionEvent(this, userId, channelId, joined)
                        );
                    });

                    log.debug("User {}: checked {} channels, all joined: {}",
                            userId, results.size(), allJoined);

                    return allJoined;
                });
    }

    public void addUserChannelId(Long userId, Long channelId) {
        Instant expiryTime = Instant.now().plusSeconds(ttlMinutes * 60L);

        userChannelExpiry.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(channelId, expiryTime);

        log.debug("Added subscription for user {} to channel {} (expires at {})",
                userId, channelId, expiryTime);
    }

    public void removeUserChannelId(Long userId, Long channelId) {
        Map<Long, Instant> channels = userChannelExpiry.get(userId);
        if (channels != null) {
            channels.remove(channelId);
            if (channels.isEmpty()) {
                userChannelExpiry.remove(userId);
            }
            log.debug("Removed subscription for user {} from channel {}", userId, channelId);
        }
    }

    private void cleanupExpiredSubscriptions() {
        try {
            Instant now = Instant.now();
            int expiredCount = 0;

            for (Map.Entry<Long, Map<Long, Instant>> userEntry : userChannelExpiry.entrySet()) {
                Long userId = userEntry.getKey();
                Map<Long, Instant> channels = userEntry.getValue();

                channels.entrySet().removeIf(entry -> {
                    if (now.isAfter(entry.getValue())) {
                        log.debug("Expired subscription: user {} from channel {}",
                                userId, entry.getKey());
                        return true;
                    }
                    return false;
                });

                if (channels.isEmpty()) {
                    userChannelExpiry.remove(userId);
                    expiredCount++;
                }
            }

            if (expiredCount > 0) {
                log.info("Cleaned up {} expired user subscriptions", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error during subscription cleanup", e);
        }
    }

    @EventListener(ChannelUpdateEvent.class)
    public void updateChannel(ChannelUpdateEvent event) {
        if (event.getChannelId() == null) return;
        addChannelId(event.getChannelId(), event.isActive());
    }

    @EventListener(UserSubscriptionEvent.class)
    public void updateUserSubscription(UserSubscriptionEvent event) {
        if (event.isJoined()) {
            addUserChannelId(event.getUserId(), event.getChannelId());
        } else {
            removeUserChannelId(event.getUserId(), event.getChannelId());
        }
    }
}