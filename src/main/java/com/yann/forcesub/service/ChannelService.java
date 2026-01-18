package com.yann.forcesub.service;

import com.yann.forcesub.entity.Channel;
import com.yann.forcesub.event.ChannelUpdateEvent;
import com.yann.forcesub.repository.ChannelRepository;
import com.yann.forcesub.service.telegram.TelegramService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final TelegramService telegramService;

    @Value("${default.database.id}")
    private long defaultDatabaseId;

    public Mono<Channel> saveChannel(Long id, String channelLinks, boolean isActive) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setChannelLinks(channelLinks);
        channel.setActive(isActive);
        return channelRepository.save(channel);
    }
    public Mono<Channel> saveChannel(Channel channel) {
        return channelRepository.save(channel);
    }
    public Mono<Channel> findById(Long id) {
        return channelRepository.findById(id);
    }
    public Mono<Void> deleteById(Long id) {
        return channelRepository.deleteById(id);
    }
    public Mono<Long> count() {
        return channelRepository.count();
    }

    public Mono<Boolean> existsChannelById(Long id) {
        return channelRepository.existsById(id);
    }
    public Flux<Channel> findAll() {
        return channelRepository.findAll();
    }
    public Flux<Channel> findAllActive(boolean isActive) {
        return channelRepository.findAllByIsActiveIs(isActive);
    }
    public Mono<Channel> updateChannel(Long id, String channelLinks, boolean isActive) {
        return channelRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Channel not found")))
                .flatMap(channel -> {
                    channel.setChannelLinks(channelLinks);
                    channel.setActive(isActive);
                    return channelRepository.save(channel);
                });
    }
    @EventListener(ChannelUpdateEvent.class)
    public void event(ChannelUpdateEvent event) {
        if (event.getChannelId() == null) return;
        if (event.isActive()) {
            updateChannel(event.getChannelId(), event.getChannelLinks(), true)
                    .doOnError(e -> log.error("Error updating channel: {}", e.getMessage()))
                    .subscribe();
        } else {
            deleteById(event.getChannelId())
                    .doOnError(e -> log.error("Error deleting channel: {}", e.getMessage()))
                    .subscribe();
        }
    }

    @PostConstruct
    public void init() {
        telegramService.getUserPermissions(defaultDatabaseId)
                .exceptionally(throwable -> {
                    log.error("Error getting default database permissions: {}", throwable.getMessage());
                    System.err.println("Error getting default database permissions: " + throwable.getMessage());
                    System.exit(-1);
                    return null;
                });
    }
}
