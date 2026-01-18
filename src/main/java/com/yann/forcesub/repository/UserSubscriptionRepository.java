package com.yann.forcesub.repository;

import com.yann.forcesub.entity.UserSubscription;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends ReactiveMongoRepository<UserSubscription, String> {
}
