package com.yann.forcesub.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VariableUpdateEvent extends ApplicationEvent {

    private final String key;
    private final String value;
    private final boolean deleted;

    public VariableUpdateEvent(Object source, String key, String value, boolean deleted) {
        super(source);
        this.key = key;
        this.value = value;
        this.deleted = deleted;
    }
}
