package com.yann.forcesub.handler;

import com.yann.forcesub.entity.Channel;
import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.service.*;
import com.yann.forcesub.service.telegram.MessageTextSender;
import com.yann.forcesub.service.telegram.TelegramService;
import it.tdlight.client.CommandHandler;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@Callback(trigger = "start")
public class StartHandler implements CommandHandler, CallbackHandler {

    private final TextService textService;
    private final MessageTextSender messageTextSender;
    private final TelegramService telegramService;
    private final MessageService messageService;
    private final ConfigService configService;
    private final AdminService adminService;
    private final UserSubscriptionService userSubscriptionService;
    private final ChannelService channelService;

    private static final String KEY_FORCE_SUB_ENABLED = "FORCE SUB ENABLED";

    private Mono<String> botUsernameMono;

    @PostConstruct
    public void init() {
        botUsernameMono = Mono.fromFuture(telegramService.getMe())
                .map(user -> java.util.Arrays.stream(user.usernames.activeUsernames)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Bot username not found"))
                )
                .cache()
                .doOnSuccess(username -> log.info("Bot username loaded: {}", username))
                .doOnError(error -> log.error("Failed to load bot username", error));
    }

    @Override
    public void onCommand(TdApi.Chat chat, TdApi.MessageSender messageSender, String argument) {
        if (messageSender instanceof TdApi.MessageSenderUser senderUser) {
            log.info("Start command received from user {}", senderUser.userId);
        }

        if (argument.isEmpty()) {
            handleWelcomeMessage(chat);
        } else {
            handleDeepLink(chat, argument);
        }
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        log.info("Start callback received from chat {}", update.chatId);

        if (!data.isEmpty()) {
            Mono<TdApi.Chat> chatMono = Mono.fromFuture(telegramService.getChat(update.chatId));
            Mono<Boolean> adminMono = adminService.isAdmin(update.chatId);

            Mono.zip(chatMono, adminMono)
                    .doOnNext(tuple -> {
                        TdApi.Chat chatData = tuple.getT1();
                        boolean isAdmin = tuple.getT2();

                        String welcomeText = textService.get("common.welcome", chatData.title);
                        TdApi.ReplyMarkup markup = createFeatureMarkup(isAdmin);

                        messageTextSender.edit(welcomeText, chatData.id, update.messageId, markup);
                    })
                    .doOnError(ex -> log.error("Error handling start callback", ex))
                    .subscribe();
        }
    }

    private void handleWelcomeMessage(TdApi.Chat chat) {
        adminService.isAdmin(chat.id)
                .subscribe(isAdmin -> {
                    String welcomeText = textService.get("common.welcome", chat.title) +
                            "\n\n" +
                            textService.get("common.feature", chat.title);

                    messageTextSender.send(welcomeText, chat.id, createFeatureMarkup(isAdmin));
                }, error -> log.error("Error checking admin status", error));
    }

    private void handleDeepLink(TdApi.Chat chat, String linkCode) {
        if (!isForceSubEnabled()) {
            forwardMessage(linkCode, chat.id);
            return;
        }

        log.info("Force sub enabled, checking subscriptions for chat {}", chat.id);

        channelService.findAllActive(true)
                .collectList()
                .flatMap(channels -> checkSubscriptionAndForward(chat, linkCode, channels))
                .subscribe(
                        result -> log.debug("Deep link handled successfully"),
                        error -> log.error("Error handling deep link", error)
                );
    }

    private Mono<Void> checkSubscriptionAndForward(TdApi.Chat chat, String linkCode, List<Channel> channels) {
        if (channels.isEmpty()) {
            log.info("No active channels, forwarding message directly");
            forwardMessage(linkCode, chat.id);
            return Mono.empty();
        }

        List<Long> channelIds = channels.stream()
                .map(Channel::getId)
                .toList();

        return Mono.fromFuture(userSubscriptionService.isUserSubscribedAsync(chat.id, channelIds))
                .flatMap(isSubscribed -> {
                    if (isSubscribed) {
                        log.info("User {} subscribed to all channels", chat.id);
                        forwardMessage(linkCode, chat.id);
                    } else {
                        log.warn("User {} not subscribed to all required channels", chat.id);
                        return sendSubscriptionRequired(chat, channels, linkCode);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> sendSubscriptionRequired(TdApi.Chat chat, List<Channel> channels, String linkCode) {
        return botUsernameMono
                .doOnNext(username -> {
                    TdApi.ReplyMarkup markup = createSubscriptionMarkup(channels, linkCode, username);
                    String message = textService.get("common.notSubscribed", chat.title);
                    messageTextSender.send(message, chat.id, markup);
                })
                .then();
    }

    private void forwardMessage(String token, long chatId) {
        log.info("Forwarding message with token {} to chat {}", token, chatId);

        messageService.findByToken(token)
                .flatMap(messageData -> forwardMessages(messageData, chatId))
                .subscribe(
                        result -> log.info("Messages forwarded successfully to chat {}", chatId),
                        error -> log.error("Error forwarding message", error)
                );
    }

    private Mono<Void> forwardMessages(com.yann.forcesub.entity.Message messageData, long chatId) {
        log.info("Forwarding {} messages from channel {} to chat {}",
                messageData.getMessageIds().size(), messageData.getChannelId(), chatId);

        List<Long> messageIds = messageData.getMessageIds();
        boolean isRestricted = configService.isContentRestricted();

        if (messageIds.size() == 1) {
            return forwardSingleMessage(messageData.getChannelId(), messageIds.getFirst(), chatId, isRestricted);
        }

        return forwardMultipleMessages(messageData.getChannelId(), messageIds, chatId, isRestricted);
    }

    private Mono<Void> forwardSingleMessage(long channelId, long messageId, long chatId, boolean isRestricted) {
        return Mono.fromFuture(
                        messageTextSender.copyMessage(channelId, messageId, chatId, isRestricted)
                                .exceptionally(ex -> {
                                    log.error("Failed to copy message {}", messageId, ex);
                                    return null;
                                })
                ).doOnSuccess(result -> log.info("Message {} copied successfully to chat {}", messageId, chatId))
                .then();
    }

    private Mono<Void> forwardMultipleMessages(long channelId, List<Long> messageIds, long chatId, boolean isRestricted) {
        List<CompletableFuture<Void>> futures = messageIds.stream()
                .map(messageId ->
                        messageTextSender.copyMessage(channelId, messageId, chatId, isRestricted)
                                .thenAccept(result ->
                                        log.debug("Message {} copied successfully", messageId)
                                )
                                .exceptionally(ex -> {
                                    log.error("Failed to copy message {}", messageId, ex);
                                    return null;
                                })
                )
                .toList();

        return Mono.fromFuture(
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                ).doOnSuccess(v -> log.info("All {} messages copied successfully to chat {}", messageIds.size(), chatId))
                .then();
    }

    private boolean isForceSubEnabled() {
        return configService.getConfig(KEY_FORCE_SUB_ENABLED).equalsIgnoreCase("true");
    }

    private TdApi.ReplyMarkup createFeatureMarkup(boolean isAdmin) {
        ReplyBuilder.InlineKeyboardBuilder builder = ReplyBuilder.inline()
                .row(2)
                .addCallback("Help", "help")
                .addCallback("Ping", "ping")
                .addCallback("About", "about")
                .addUrl("Dev", "https://t.me/b_yannnn");

        if (isAdmin) {
            builder
                    .addCallback("Cek Vars", "vars")
                    .addCallback("Force Sub", "channel")
                    .addCallback("Admins", "admins");
        }

        return builder.build();
    }

    private TdApi.ReplyMarkup createSubscriptionMarkup(List<Channel> channels, String linkCode, String botUsername) {
        ReplyBuilder.InlineKeyboardBuilder builder = ReplyBuilder.inline();

        // Add channel subscription buttons
        for (Channel channel : channels) {
            builder.addUrl(channel.getPlaceholder(), channel.getChannelLinks());
        }

        // Add retry button
        builder.row(2);
        String retryLink = String.format("https://t.me/%s?start=%s", botUsername, linkCode);
        builder.addUrl("Coba Lagi", retryLink);

        return builder.build();
    }
}