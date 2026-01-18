package com.yann.forcesub.service.telegram;

import com.yann.forcesub.exceptions.NotValidFormatException;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTextSender {

    private final SimpleTelegramClient client;


    public CompletableFuture<TdApi.FormattedText> format(String text) {
        return format(text, new TdApi.TextParseModeHTML());
    }


    public CompletableFuture<TdApi.FormattedText> format(String text, TdApi.TextParseMode parseMode) {
        CompletableFuture<TdApi.FormattedText> future = new CompletableFuture<>();
        client.send(new TdApi.ParseTextEntities(text, parseMode), result -> {
            if (result.isError()) {
                log.error("Failed to format text: {}", result.getError().message);
                future.completeExceptionally(new NotValidFormatException(result.getError().message));
            } else {
                future.complete(result.get());
            }
        });
        return future;
    }


    public void send(String text, long chatId) {
        send(text, chatId, null, null);
    }


    public void send(String text, long chatId, TdApi.ReplyMarkup replyMarkup) {
        send(text, chatId, null, replyMarkup);
    }

    public void send(String text, long chatId, TdApi.TextParseMode parseMode, TdApi.ReplyMarkup replyMarkup) {
        CompletableFuture<TdApi.Message> future = new CompletableFuture<>();

        inputText(text, parseMode)
                .thenAccept(input -> {
                    TdApi.SendMessage request = new TdApi.SendMessage(
                            chatId,
                            0,
                            null,
                            null,
                            replyMarkup,
                            input
                    );

                    client.send(request, result -> {
                        if (result.isError()) {
                            log.error("Failed to send message to {}: {}", chatId, result.getError().message);
                            future.completeExceptionally(new RuntimeException(result.getError().message));
                        } else {
                            future.complete(result.get());
                        }
                    });
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });

    }

    public CompletableFuture<TdApi.InputMessageContent> inputText(String text, TdApi.TextParseMode parseMode) {
        CompletableFuture<TdApi.InputMessageContent> future = new CompletableFuture<>();

        TdApi.TextParseMode mode = parseMode != null ? parseMode : new TdApi.TextParseModeHTML();

        format(text, mode)
                .thenAccept(formatted -> {
                    TdApi.InputMessageText inputMessageContent = new TdApi.InputMessageText();
                    inputMessageContent.text = formatted;
                    inputMessageContent.clearDraft = true;
                    inputMessageContent.linkPreviewOptions = new TdApi.LinkPreviewOptions();
                    future.complete(inputMessageContent);
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });

        return future;
    }
    public void edit(String text, long chatId, long messageId, TdApi.ReplyMarkup replyMarkup) {
        edit(text, chatId, messageId, null, replyMarkup);
    }

    private CompletableFuture<TdApi.Message> edit(String text, long chatId, long messageId, TdApi.TextParseMode parseMode, TdApi.ReplyMarkup replyMarkup) {
        CompletableFuture<TdApi.Message> future = new CompletableFuture<>();
        TdApi.EditMessageText edit = new TdApi.EditMessageText();
        edit.chatId = chatId;
        edit.messageId = messageId;
        inputText(text, parseMode).thenAccept(input -> client.send(
                new TdApi.EditMessageText(chatId, messageId, replyMarkup, input), result -> {
                    if (result.isError()) {
                        log.error("Failed to edit message: {}", result.getError().message);
                        future.completeExceptionally(new RuntimeException(result.getError().message));
                    } else {
                        future.complete(result.get());
                    }
                }
        ));
        return future;
    }
    public void answerInlineQuery(long inlineId) {
        client.send(
                new TdApi.AnswerCallbackQuery(inlineId, null, false, null, 0)
        );
    }
    public CompletableFuture<TdApi.Messages> copyMessage(long fromChatId, long urlMessageId, long toChatId, boolean protectContent) {
        long tdLibMessageId = urlMessageId << 20;

        CompletableFuture<TdApi.Messages> future = new CompletableFuture<>();

        client.send(new TdApi.GetMessage(fromChatId, tdLibMessageId), msgResult -> {
            if (msgResult.isError()) {
                log.error("Message {} (TDLib ID: {}) not found in chat {}: {}",
                        urlMessageId, tdLibMessageId, fromChatId, msgResult.getError().message);
                future.completeExceptionally(
                        new RuntimeException("Message not found: " + msgResult.getError().message)
                );
                return;
            }

            TdApi.Message originalMessage = msgResult.get();
            log.info("Found message: TDLib ID={}, Content={}",
                    originalMessage.id,
                    originalMessage.content.getClass().getSimpleName());

            TdApi.MessageSendOptions options = messageSendOptions(protectContent);

            client.send(
                    new TdApi.ForwardMessages(
                            toChatId,
                            0,
                            fromChatId,
                            new long[]{tdLibMessageId},
                            options,
                            true,
                            false
                    ),
                    forwardResult -> {
                        if (forwardResult.isError()) {
                            log.error("Forward error: {}", forwardResult.getError().message);
                            future.completeExceptionally(
                                    new RuntimeException(forwardResult.getError().message)
                            );
                            return;
                        }

                        TdApi.Messages messages = forwardResult.get();
                        log.info("Successfully copied {} messages", messages.totalCount);
                        future.complete(messages);
                    }
            );
        });
        return future;
    }
    public TdApi.MessageSendOptions messageSendOptions(boolean protectContent) {
        TdApi.MessageSendOptions options = new TdApi.MessageSendOptions();
        options.disableNotification = false;
        options.protectContent = protectContent;
        return options;
    }

}