package com.yann.forcesub.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChannelUpdateEvent extends ApplicationEvent {
    private final Long channelId;
    private final String channelLinks;
    private final String placeholder;
    private final boolean isActive;

    public ChannelUpdateEvent(Object source, Long channelId, String channelLinks, String placeholder, boolean isActive) {
        super(source);
        this.channelId = channelId;
        this.channelLinks = channelLinks;
        this.placeholder = placeholder;
        this.isActive = isActive;
    }
}
