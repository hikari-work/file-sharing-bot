package com.yann.forcesub.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserSubscriptionEvent extends ApplicationEvent {
    private final Long userId;
    private final Long channelId;
    private final boolean isJoined;

    public UserSubscriptionEvent(Object source, Long userId, Long channelId, boolean isJoined) {
        super(source);
        this.userId = userId;
        this.channelId = channelId;
        this.isJoined = isJoined;
    }
}
