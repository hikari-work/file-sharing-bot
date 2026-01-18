package com.yann.forcesub.service.telegram;

import com.yann.forcesub.event.UserSubscriptionEvent;
import com.yann.forcesub.service.ConfigService;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final SimpleTelegramClient client;
    private final ConfigService configService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public CompletableFuture<TdApi.Chat> getChat(long chatId) {
        return client.send(new TdApi.GetChat(chatId));
    }
    public CompletableFuture<TdApi.User> getMe() {
        return client.send(new TdApi.GetMe());
    }
    public void answerCallbackQuery(long callbackQueryId, String text, boolean showAlert) {
        client.send(new TdApi.AnswerCallbackQuery(callbackQueryId, text, showAlert, null, 0));
    }

    public CompletableFuture<Map<Permission, Boolean>> getUserPermissions(long channelId) {
        CompletableFuture<Map<Permission, Boolean>> future = new CompletableFuture<>();

        long supergroupId = convertToSupergroupId(channelId);

        client.send(
                new TdApi.GetSupergroup(supergroupId),
                supergroupResult -> {
                    if (supergroupResult.isError()) {
                        future.completeExceptionally(
                                new RuntimeException("Error getting supergroup: " + supergroupResult.getError().message)
                        );
                        return;
                    }

                    client.send(
                            new TdApi.GetSupergroupFullInfo(supergroupId),
                            fullInfoResult -> {
                                if (fullInfoResult.isError()) {
                                    future.completeExceptionally(
                                            new RuntimeException("Error getting supergroup full info: " + fullInfoResult.getError().message)
                                    );
                                    return;
                                }

                                processSupergroupPermissions(
                                        supergroupResult.get(),
                                        future
                                );
                            }
                    );
                }
        );

        return future;
    }

    private void processSupergroupPermissions(
            TdApi.Supergroup supergroup,
            CompletableFuture<Map<Permission, Boolean>> future
    ) {
        try {
            Map<Permission, Boolean> result = new ConcurrentHashMap<>();

            TdApi.ChatMemberStatus status = supergroup.status;

            if (status instanceof TdApi.ChatMemberStatusAdministrator admin) {

                result.put(Permission.CAN_INVITE_USERS, admin.rights.canInviteUsers);
                result.put(Permission.CAN_CHANGE_INFO, admin.rights.canChangeInfo);
                result.put(Permission.CAN_DELETE_MESSAGES, admin.rights.canDeleteMessages);

                future.complete(result);
            } else if (status instanceof TdApi.ChatMemberStatusCreator) {
                result.put(Permission.CAN_INVITE_USERS, true);
                result.put(Permission.CAN_CHANGE_INFO, true);
                result.put(Permission.CAN_DELETE_MESSAGES, true);

                future.complete(result);
            } else {
                future.completeExceptionally(
                        new RuntimeException("Bot is not an administrator in this channel")
                );
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private long convertToSupergroupId(long channelId) {
        String channelIdStr = String.valueOf(Math.abs(channelId));

        if (channelIdStr.startsWith("100")) {
            String supergroupIdStr = channelIdStr.substring(3);
            return Long.parseLong(supergroupIdStr);
        }

        return Math.abs(channelId);
    }

    public CompletableFuture<TdApi.ChatInviteLink> getChannelLinks(long channelId) {
        CompletableFuture<TdApi.ChatInviteLink> future = new CompletableFuture<>();
        TdApi.CreateChatInviteLink chatInviteLink = new TdApi.CreateChatInviteLink();
        chatInviteLink.chatId = channelId;
        chatInviteLink.name = "FORCECUB";
        client.send(
                chatInviteLink, result -> {
                    if (result.isError()) future.completeExceptionally(new RuntimeException(result.getError().message));
                    else future.complete(result.get());
                }
        );
        return future;
    }
    public CompletableFuture<Boolean> isUserInChat(long chatId, long userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        client.send(
                new TdApi.GetChatMember(chatId, new TdApi.MessageSenderUser(userId)), result -> {
                    if (result.isError()) future.complete(false);
                    TdApi.ChatMember member = result.get();
                    log.info("User {} is {} in chat {}", userId, member.status, chatId);
                    boolean isJoined = isActiveMember(member.status);
                    applicationEventPublisher.publishEvent(new UserSubscriptionEvent(this, userId, chatId, isJoined));
                    future.complete(isJoined);
                }
        );
        return future;
    }
    private boolean isActiveMember(TdApi.ChatMemberStatus member) {
        return switch (member) {
            case TdApi.ChatMemberStatusAdministrator ignored -> true;
            case TdApi.ChatMemberStatusCreator ignored -> true;
            case TdApi.ChatMemberStatusMember ignored -> true;
            case TdApi.ChatMemberStatusRestricted ignored -> true;
            default -> false;
        };
    }

    public CompletableFuture<Map<Long, Boolean>> checkMultipleChannels(long userId, List<Long> channelIds) {
        Map<Long, Boolean> results = new ConcurrentHashMap<>();
        CompletableFuture<Void> allChecks = CompletableFuture.allOf(
                channelIds.stream()
                        .map(channelId ->
                                isUserInChat(channelId, userId)
                                        .thenAccept(joined -> results.put(channelId, joined))
                        )
                        .toArray(CompletableFuture[]::new)
        );

        return allChecks.thenApply(v -> results);
    }
}
