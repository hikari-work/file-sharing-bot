package com.yann.forcesub.repository;

import com.yann.forcesub.entity.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

    Mono<Message> findById(String id);
    Mono<Void> deleteById(String id);
    Mono<Long> count();
    Flux<Message> findTop10ByOrderByViewCountDesc();
}
