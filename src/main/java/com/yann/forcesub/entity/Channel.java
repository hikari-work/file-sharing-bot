package com.yann.forcesub.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "channels")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Channel {

    @Id
    private Long id;

    private String channelLinks;
    private String placeholder;
    private boolean isActive;
    private String name;
}
