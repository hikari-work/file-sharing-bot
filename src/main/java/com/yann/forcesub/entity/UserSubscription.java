package com.yann.forcesub.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_subscriptions")
public class UserSubscription {

    @Id
    private String id;

    private Long userId;
    private Long channelId;
    private boolean isJoined;
    private Instant updatedAt;
}
