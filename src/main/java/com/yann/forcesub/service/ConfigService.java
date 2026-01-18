package com.yann.forcesub.service;

import com.yann.forcesub.entity.AppSetting;
import com.yann.forcesub.event.ContentRestrictedEvent;
import com.yann.forcesub.event.VariableUpdateEvent;
import com.yann.forcesub.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final AppSettingRepository appSettingRepository;
    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    public static final String KEY_CONTENT_RESTRICTED = "CONTENT RESTRICTED";
    public static final String KEY_FORCE_SUB_ENABLED = "FORCE SUB ENABLED";



    @PostConstruct
    public void init() {
        log.info("Initializing ConfigService...");
        appSettingRepository.findAll()
                .doOnNext(setting -> {
                    localCache.put(setting.getKey(), setting.getValue());
                    log.debug("Loaded config: {} = {}", setting.getKey(), setting.getValue());
                })
                .doOnComplete(() -> log.info("Loaded {} configs from database", localCache.size()))
                .then(initializeDefaultConfigs())
                .doOnSuccess(v -> log.info("ConfigService initialized successfully"))
                .doOnError(e -> log.error("Error initializing ConfigService", e))
                .subscribe();
    }

    private Mono<Void> initializeDefaultConfigs() {
        Map<String, String> defaultConfigs = getDefaultConfigs();

        return Flux.fromIterable(defaultConfigs.entrySet())
                .filter(entry -> !localCache.containsKey(entry.getKey()))
                .flatMap(entry -> {
                    log.info("Creating default config: {} = {}", entry.getKey(), entry.getValue());
                    AppSetting setting = new AppSetting(entry.getKey(), entry.getValue());
                    return appSettingRepository.save(setting)
                            .doOnSuccess(saved -> localCache.put(saved.getKey(), saved.getValue()));
                })
                .then();
    }

    private Map<String, String> getDefaultConfigs() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put(KEY_CONTENT_RESTRICTED, "true");
        defaults.put(KEY_FORCE_SUB_ENABLED, "true");

        return defaults;
    }

    public boolean isContentRestricted() {
        String value = localCache.getOrDefault(KEY_CONTENT_RESTRICTED, "false");
        return Boolean.parseBoolean(value);
    }

    public Mono<AppSetting> setContentRestricted(boolean status) {
        return setConfig(KEY_CONTENT_RESTRICTED, String.valueOf(status));
    }


    public String getConfig(String key) {
        return localCache.get(key);
    }

    public Mono<AppSetting> setConfig(String key, String value) {
        AppSetting setting = new AppSetting(key, value);
        return appSettingRepository.save(setting)
                .doOnSuccess(saved -> {
                    localCache.put(key, value);
                    log.info("Config updated: {} = {}", key, value);
                })
                .doOnError(e -> log.error("Error updating config {}: {}", key, e.getMessage()));
    }

    public Flux<AppSetting> findAll() {
        return appSettingRepository.findAll();
    }


    @EventListener(ContentRestrictedEvent.class)
    public void event(ContentRestrictedEvent event) {
        setContentRestricted(event.isRestricted())
                .doOnError(e -> log.error("Error updating config: {}", e.getMessage()))
                .subscribe();
    }
    @EventListener(VariableUpdateEvent.class)
    public void eventEditVariable(VariableUpdateEvent event) {
        setConfig(event.getKey(), event.getValue())
                .subscribe();
    }
}