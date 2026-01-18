package com.yann.forcesub.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ContentRestrictedEvent extends ApplicationEvent {
    private final boolean isRestricted;
    public ContentRestrictedEvent(Object source, boolean isRestricted) {
        super(source);
        this.isRestricted = isRestricted;
    }
}
