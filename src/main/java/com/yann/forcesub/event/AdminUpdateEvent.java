package com.yann.forcesub.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AdminUpdateEvent extends ApplicationEvent {

    private final Long id;
    private final boolean delete;

    public AdminUpdateEvent(Object source, Long id, boolean delete) {
        super(source);
        this.id = id;
        this.delete = delete;
    }
}
