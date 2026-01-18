package com.yann.forcesub.service;

import com.yann.forcesub.entity.User;
import com.yann.forcesub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void saveUser(Long id) {
        existsUserById(id)
        .flatMap(exists -> exists ? Mono.empty() : userRepository.insert(new User(id)))
        .subscribe();
    }
    public Mono<User> findById(Long id) {
        return userRepository.findById(id);
    }
    public Mono<Void> deleteById(Long id) {
        return userRepository.deleteById(id);
    }
    public Mono<Long> count() {
        return userRepository.count();
    }
    public Mono<Boolean> existsUserById(Long id) {
        return userRepository.existsUserById(id);
    }

}
