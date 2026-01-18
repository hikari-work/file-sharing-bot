package com.yann.forcesub.handler;

import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.manager.CallbackType;
import com.yann.forcesub.service.telegram.MessageTextSender;
import com.yann.forcesub.service.telegram.TelegramService;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Callback(trigger = "ping", type = CallbackType.EXACT)
@Component
@Slf4j
public class PingCallback implements CallbackHandler {
    private final TelegramService telegramService;
    private final MessageTextSender messageTextSender;

    public PingCallback(TelegramService telegramService, MessageTextSender messageTextSender) {
        this.telegramService = telegramService;
        this.messageTextSender = messageTextSender;
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        long startTime = System.nanoTime();

        telegramService.getMe()
                .thenAccept(me -> {
                    long endTime = System.nanoTime();
                    long durationNanos = endTime - startTime;

                    double milliseconds = durationNanos / 1_000_000.0;
                    String message = getString(durationNanos);

                    messageTextSender.send(message, update.chatId, null, null);

                    String callbackText = String.format("%.2f ms", milliseconds);
                    telegramService.answerCallbackQuery(update.id, "⚡ " + callbackText, false);
                })
                .exceptionally(ex -> {
                    long endTime = System.nanoTime();
                    long durationNanos = endTime - startTime;

                    log.error("Ping failed after {} ns", durationNanos, ex);
                    telegramService.answerCallbackQuery(update.id, "❌ Error!", true);
                    return null;
                });
    }

    private static @NonNull String getString(long durationNanos) {
        long milliseconds = durationNanos / 1_000_000;
        long remainingAfterMs = durationNanos % 1_000_000;

        long microseconds = remainingAfterMs / 1_000;
        long nanoseconds = remainingAfterMs % 1_000;

        return String.format(
                """
                        🏓 Pong!
                        
                        ⏱️ Response Time:
                        • %d ms %d μs %d ns""",
                milliseconds,
                microseconds,
                nanoseconds
        );
    }
}
