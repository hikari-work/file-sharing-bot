package com.yann.forcesub.repository;

import com.yann.forcesub.entity.Channel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChannelRepository extends ReactiveMongoRepository<Channel, Long> {
    Flux<Channel> findAllByIsActiveIs(boolean isActive);
}
