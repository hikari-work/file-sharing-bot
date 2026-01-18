package com.yann.forcesub.handler;

import com.yann.forcesub.service.ConfigService;
import com.yann.forcesub.service.MessageService;
import com.yann.forcesub.service.ReplyBuilder;
import com.yann.forcesub.service.telegram.MessageTextSender;
import com.yann.forcesub.service.telegram.TelegramService;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandleMessageGenerator {

    private final MessageService messageService;
    private final ConfigService configService;
    private final TelegramService telegramService;
    @Value("${default.database.id}")
    private long defaultDatabaseId;

    private static final long TDLIB_MESSAGE_ID_MULTIPLIER = 1048576L;

    private final MessageTextSender messageTextSender;

    public void handleMessage(TdApi.Message message) {
        log.info("Message received: {}", toBotApiMessageId(message.id));
        messageReplyMarkup(toBotApiMessageId(message.id))
                .subscribe(reply -> {
                    String text = buildResponseText(message);
                    messageTextSender.send(text, message.chatId, null, reply);
                });
    }
    private Mono<TdApi.ReplyMarkup> messageReplyMarkup(long messageId) {

        return messageService.createSingleLink(defaultDatabaseId, messageId, configService.isContentRestricted())
                .flatMap(this::getLink)
                .map(shareUrl -> ReplyBuilder.inline()
                        .addUrl("Share", "https://telegram.me/share/url?url=" + shareUrl)
                        .addCopy("Copy Link", shareUrl)
                        .build());
    }

    private Mono<String> getLink(String linkCode) {
        return Mono.fromFuture(telegramService.getMe())
                .map(user -> {
                    String username = Arrays.stream(user.usernames.activeUsernames)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Bot username not found"));

                    return String.format("https://t.me/%s?start=%s", username, linkCode);
                });
    }

    private String buildResponseText(TdApi.Message message) {
        StringBuilder text = new StringBuilder();
        text.append("<b>Link berhasil dibuat!</b>\n\n");
        text.append("<b>Detail:</b>\n");
        text.append("├ Message ID: <code>").append(message.id).append("</code>\n");
        text.append("├ Database ID: <code>").append(defaultDatabaseId).append("</code>\n");

        if (configService.isContentRestricted()) {
            text.append("└ Status: 🔒 <i>Content Restricted</i>\n\n");
        } else {
            text.append("└ Status: 🔓 <i>Public Access</i>\n\n");
        }
        text.append("📤 Klik tombol di bawah untuk share link!");

        return text.toString();
    }
    public static Long toBotApiMessageId(long tdLibMessageId) {
        if (tdLibMessageId % TDLIB_MESSAGE_ID_MULTIPLIER == 0) {
            return tdLibMessageId / TDLIB_MESSAGE_ID_MULTIPLIER;
        }
        return tdLibMessageId;
    }
}
