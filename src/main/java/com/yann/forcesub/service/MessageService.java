package com.yann.forcesub.service;

import com.yann.forcesub.entity.Message;
import com.yann.forcesub.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.LongStream;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public Mono<String> createBatchLink(long channelId, long startMessageId, long endMessageId, boolean contentRestricted) {
        List<Long> ids = LongStream.rangeClosed(startMessageId, endMessageId)
                .boxed()
                .toList();
        Message message = Message.builder()
                .channelId(channelId)
                .messageIds(ids)
                .contentRestricted(contentRestricted)
                .viewCount(0)
                .build();
        return messageRepository.save(message)
                .map(Message::getId);
    }
    public Mono<String> createSingleLink(long channelId, long messageId, boolean contentRestricted) {
        return createBatchLink(channelId, messageId, messageId, contentRestricted);
    }
    public Mono<Message> findByToken(String token) {
        return messageRepository.findById(token)
                .flatMap(msg -> {
                    msg.setViewCount(msg.getViewCount() + 1);
                    return messageRepository.save(msg);
                });
    }
}
