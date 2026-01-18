package com.yann.forcesub.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    private Long channelId;

    private List<Long> messageIds;

    @Builder.Default
    private boolean contentRestricted = true;

    @Builder.Default
    private Integer viewCount = 0;

    @CreatedDate
    private Instant createdAt;
}
