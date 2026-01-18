package com.yann.forcesub.repository;

import com.yann.forcesub.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, Long> {

    Mono<User> findById(Long id);
    Mono<Void> deleteById(Long id);
    Mono<Long> count();
    Mono<User> insert(User user);
    Mono<Boolean> existsUserById(Long id);
}
