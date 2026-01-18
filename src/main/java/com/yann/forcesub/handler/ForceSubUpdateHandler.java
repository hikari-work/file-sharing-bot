package com.yann.forcesub.handler;

import com.yann.forcesub.entity.Channel;
import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.manager.CallbackType;
import com.yann.forcesub.service.ChannelService;
import com.yann.forcesub.service.ReplyBuilder;
import com.yann.forcesub.service.UserStateService;
import com.yann.forcesub.service.telegram.MessageTextSender;
import com.yann.forcesub.service.telegram.TelegramService;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Callback(trigger = "channel", type = CallbackType.STARTS_WITH)
@Component
public class ForceSubUpdateHandler implements CallbackHandler {

    private final ChannelService channelService;
    private final TelegramService telegramService;
    private final MessageTextSender messageTextSender;
    private final UserStateService userStateService;

    private static final String STATE_ADD_CHANNEL = "ADD_CHANNEL";
    private static final String STATE_EDIT_CHANNEL = "EDIT_CHANNEL";

    public ForceSubUpdateHandler(ChannelService channelService,
                                 TelegramService telegramService,
                                 MessageTextSender messageTextSender,
                                 UserStateService userStateService) {
        this.channelService = channelService;
        this.telegramService = telegramService;
        this.messageTextSender = messageTextSender;
        this.userStateService = userStateService;
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        String callbackPayload = data(update);
        long chatId = update.chatId;
        long messageId = update.messageId;
        long userId = update.senderUserId;

        if (callbackPayload.equals("channel")) {
            showChannelMenu(chatId, messageId);
            telegramService.answerCallbackQuery(update.id, null, false);
            return;
        }

        if (callbackPayload.equals("channel_add")) {
            userStateService.setState(userId, STATE_ADD_CHANNEL);
            String message = """
                    ➕ <b>Tambahkan Channel Baru</b>
                    
                    📝 Silahkan kirim Channel ID
                    Format: <code>-1001234567890</code>
                    
                    Ketik /cancel untuk membatalkan.
                    """;
            messageTextSender.edit(message, chatId, messageId, null);
            telegramService.answerCallbackQuery(update.id, null, false);
            return;
        }

        String[] parts = callbackPayload.split("_");

        if (parts.length == 3 && parts[1].equals("view")) {
            Long channelId = parseChannelId(parts[2]);
            if (channelId != null) {
                showChannelDetail(chatId, messageId, channelId);
            }
            telegramService.answerCallbackQuery(update.id, null, false);
            return;
        }

        if (parts.length == 3 && parts[1].equals("edit")) {
            Long channelId = parseChannelId(parts[2]);
            if (channelId != null) {
                handleEditChannel(chatId, messageId, userId, channelId);
            }
            telegramService.answerCallbackQuery(update.id, null, false);
            return;
        }

        if (parts.length == 3 && parts[1].equals("delete")) {
            Long channelId = parseChannelId(parts[2]);
            if (channelId != null) {
                showDeleteConfirmation(chatId, messageId, channelId);
            }
            telegramService.answerCallbackQuery(update.id, null, false);
            return;
        }

        if (parts.length == 4 && parts[1].equals("delete") && parts[2].equals("confirm")) {
            Long channelId = parseChannelId(parts[3]);
            if (channelId != null) {
                handleDeleteChannel(chatId, messageId, channelId);
                telegramService.answerCallbackQuery(update.id, "Channel dihapus!", false);
                messageTextSender.edit("Channel berhasil dihapus!", chatId, messageId, null);
                return;
            }
        }

        telegramService.answerCallbackQuery(update.id, null, false);
    }

    private void showChannelMenu(long chatId, long messageId) {
        channelService.findAllActive(true)
                .collectList()
                .subscribe(channels -> {
                    ReplyBuilder.InlineKeyboardBuilder inline = ReplyBuilder.inline();

                    if (channels.isEmpty()) {
                        String text = """
                                📋 <b>Daftar Channel Force Subscribe</b>
                                
                                ℹ️ Belum ada channel yang ditambahkan.
                                Silahkan tambahkan channel terlebih dahulu.
                                """;
                        inline.addCallback("➕ Tambah Channel", "channel_add");
                        inline.newRow();
                        inline.addCallback("🔙 Kembali", "start");
                        messageTextSender.edit(text, chatId, messageId, inline.build());
                    } else {
                        StringBuilder textBuilder = new StringBuilder();
                        textBuilder.append(" <b>Daftar Channel Force Subscribe</b>\n\n");

                        for (int i = 0; i < channels.size(); i++) {
                            Channel channel = channels.get(i);
                            textBuilder.append(String.format("%d. <b>%s</b>\n", i + 1, channel.getPlaceholder()));
                            textBuilder.append(String.format("   ID: <code>%d</code>\n", channel.getId()));
                            textBuilder.append(String.format("   Link: %s\n\n", channel.getChannelLinks()));

                            inline.addCallback("📌 " + channel.getPlaceholder(), "channel_view_" + channel.getId());
                        }

                        inline.row(2);
                        inline.newRow();
                        inline.addCallback("➕ Tambah Channel", "channel_add");
                        inline.newRow();
                        inline.addCallback("🔙 Kembali", "start");

                        messageTextSender.edit(textBuilder.toString(), chatId, messageId, inline.build());
                    }
                }, error -> {
                    log.error("Error fetching channels", error);
                    messageTextSender.send("❌ Error memuat daftar channel: " + error.getMessage(), chatId);
                });
    }

    private void showChannelDetail(long chatId, long messageId, long channelId) {
        channelService.findById(channelId)
                .subscribe(channel -> {
                    String text = String.format("""
                            📌 <b>Detail Channel</b>
                            
                            🏷 <b>Nama:</b> %s
                            🆔 <b>Channel ID:</b> <code>%d</code>
                            🔗 <b>Link:</b> %s
                            📊 <b>Placeholder:</b> %s
                            ✅ <b>Status:</b> %s
                            """,
                            channel.getName() != null ? channel.getName() : "N/A",
                            channel.getId(),
                            channel.getChannelLinks() != null ? channel.getChannelLinks() : "N/A",
                            channel.getPlaceholder(),
                            channel.isActive() ? "Aktif" : "Tidak Aktif"
                    );

                    ReplyBuilder.InlineKeyboardBuilder inline = ReplyBuilder.inline();
                    inline.addCallback("✏️ Edit", "channel_edit_" + channelId);
                    inline.addCallback("🗑 Hapus", "channel_delete_" + channelId);
                    inline.newRow();
                    inline.addCallback("🔙 Kembali", "channel");

                    messageTextSender.edit(text, chatId, messageId, inline.build());
                }, error -> {
                    log.error("Error fetching channel", error);
                    messageTextSender.send("❌ Channel tidak ditemukan", chatId);
                });
    }

    private void handleEditChannel(long chatId, long messageId, long userId, long channelId) {
        channelService.findById(channelId)
                .subscribe(channel -> {
                    userStateService.setState(userId, STATE_EDIT_CHANNEL + channelId);

                    String text = String.format("""
                            ✏️ <b>Edit Channel</b>
                            
                            📌 <b>Channel:</b> %s
                            🆔 <b>ID:</b> <code>%d</code>
                            🔗 <b>Current Placeholder:</b> %s
                            
                            📝 Silahkan kirim placeholder baru untuk channel ini.
                            
                            Ketik /cancel untuk membatalkan.
                            """,
                            channel.getName() != null ? channel.getName() : "N/A",
                            channel.getId(),
                            channel.getPlaceholder()
                    );

                    messageTextSender.edit(text, chatId, messageId, null);
                }, error -> {
                    log.error("Error fetching channel", error);
                    messageTextSender.send("❌ Channel tidak ditemukan", chatId);
                });
    }

    private void showDeleteConfirmation(long chatId, long messageId, long channelId) {
        channelService.findById(channelId)
                .subscribe(channel -> {
                    String text = String.format("""
                            ⚠️ <b>Konfirmasi Hapus Channel</b>
                            
                            Apakah anda yakin ingin menghapus channel ini?
                            
                            📌 <b>Nama:</b> %s
                            🆔 <b>ID:</b> <code>%d</code>
                            
                            ⚠️ <b>Tindakan ini tidak dapat dibatalkan!</b>
                            """,
                            channel.getPlaceholder(),
                            channel.getId()
                    );

                    ReplyBuilder.InlineKeyboardBuilder inline = ReplyBuilder.inline();
                    inline.addCallback("✅ Ya, Hapus", "channel_delete_confirm_" + channelId);
                    inline.addCallback("❌ Batal", "channel_view_" + channelId);

                    messageTextSender.edit(text, chatId, messageId, inline.build());
                }, error -> {
                    log.error("Error fetching channel", error);
                    messageTextSender.send("❌ Channel tidak ditemukan", chatId);
                });
    }


    public void handleDeleteChannel(long chatId, long messageId, long channelId) {
        channelService.deleteById(channelId)
                .then()
                .subscribe(
                        unused -> {
                            String text = "✅ Channel berhasil dihapus!";
                            ReplyBuilder.InlineKeyboardBuilder inline = ReplyBuilder.inline();
                            inline.addCallback("🔙 Kembali ke Daftar", "channel");

                            messageTextSender.edit(text, chatId, messageId, inline.build());
                        },
                        error -> {
                            log.error("Error deleting channel", error);
                            messageTextSender.send("❌ Error menghapus channel: " + error.getMessage(), chatId);
                        }
                );
    }


    public void handleTextMessage(TdApi.Message message, long userId) {
        String state = userStateService.getState(userId);
        if (state == null) return;

        String text = null;
        if (message.content instanceof TdApi.MessageText textMessage) {
            text = textMessage.text.text;
        }

        if ("/cancel".equals(text)) {
            userStateService.clearState(userId);
            messageTextSender.send("❌ Operasi dibatalkan.", message.chatId);
            return;
        }

        if (STATE_ADD_CHANNEL.equals(state)) {
            handleAddChannel(message.chatId, userId, text);
            return;
        }

        if (state.startsWith(STATE_EDIT_CHANNEL)) {
            long channelId = Long.parseLong(state.replace(STATE_EDIT_CHANNEL, ""));
            handleUpdateChannelPlaceholder(message.chatId, userId, channelId, text);
        }
    }

    public void handleAddChannel(long chatId, long userId, String input) {
        if (input == null || input.trim().isEmpty()) {
            messageTextSender.send("⚠️ Input tidak boleh kosong. Ketik /cancel untuk membatalkan.", chatId);
            return;
        }

        Long channelId = parseChannelId(input);

        if (channelId == null) {
            messageTextSender.send("""
                    ❌ <b>Format tidak valid!</b>
                    
                    Gunakan format Channel ID: <code>-1001234567890</code>
                    
                    Ketik /cancel untuk membatalkan.
                    """, chatId);
            return;
        }

        checkBotPermissionAndSave(chatId, userId, channelId);
    }


    private void handleUpdateChannelPlaceholder(long chatId, long userId, long channelId, String newPlaceholder) {
        if (newPlaceholder == null || newPlaceholder.trim().isEmpty()) {
            messageTextSender.send("⚠️ Placeholder tidak boleh kosong. Ketik /cancel untuk membatalkan.", chatId);
            return;
        }

        channelService.findById(channelId)
                .flatMap(channel -> {
                    channel.setPlaceholder(newPlaceholder.trim());
                    return channelService.saveChannel(channel);
                })
                .subscribe(updated -> {
                    userStateService.clearState(userId);

                    String text = String.format("""
                            ✅ <b>Channel berhasil diupdate!</b>
                            
                            📌 <b>Placeholder Baru:</b> %s
                            🆔 <b>ID:</b> <code>%d</code>
                            """,
                            updated.getPlaceholder(),
                            updated.getId()
                    );

                    messageTextSender.send(
                            text,
                            chatId,
                            ReplyBuilder.inline()
                                    .addCallback("🔙 Kembali ke Channel", "channel_view_" + channelId)
                                    .build()
                    );
                }, error -> {
                    log.error("Error updating channel", error);
                    messageTextSender.send("❌ Error mengupdate channel: " + error.getMessage(), chatId);
                });
    }

    private void checkBotPermissionAndSave(long chatId, long userId, long channelId) {
        messageTextSender.send("🔄 Memeriksa permissions bot di channel...", chatId);

        telegramService.getUserPermissions(channelId)
                .thenAccept(permission -> {
                    boolean isPermissionOK = permission.values()
                            .stream()
                            .allMatch(Boolean::booleanValue);

                    if (!isPermissionOK) {
                        StringBuilder missingPermission = new StringBuilder();
                        missingPermission.append("❌ <b>Bot tidak memiliki permissions lengkap!</b>\n\n");
                        missingPermission.append("Missing permissions:\n");

                        permission.forEach((perm, granted) -> {
                            if (!granted) {
                                missingPermission.append("• ").append(perm).append("\n");
                            }
                        });

                        missingPermission.append("\n⚠️ Pastikan bot adalah admin dengan semua permissions yang diperlukan.");

                        userStateService.clearState(userId);
                        messageTextSender.send(missingPermission.toString(), chatId);
                    } else {
                        telegramService.getChat(channelId)
                                .thenAccept(channelChat -> {
                                    String title = channelChat.title;

                                    telegramService.getChannelLinks(channelId)
                                            .thenAccept(chatInviteLink -> {
                                                Channel channel = new Channel();
                                                channel.setId(channelId);
                                                channel.setPlaceholder("Join Now");
                                                channel.setName(title);
                                                channel.setChannelLinks(chatInviteLink.inviteLink);
                                                channel.setActive(true);

                                                channelService.saveChannel(channel)
                                                        .subscribe(saved -> {
                                                            userStateService.clearState(userId);

                                                            String text = String.format("""
                                                                    ✅ <b>Channel berhasil ditambahkan!</b>
                                                                    
                                                                    📌 <b>Nama:</b> %s
                                                                    🆔 <b>ID:</b> <code>%d</code>
                                                                    🔗 <b>Link:</b> %s
                                                                    ✅ <b>Status:</b> Aktif
                                                                    """,
                                                                    saved.getName(),
                                                                    saved.getId(),
                                                                    saved.getChannelLinks()
                                                            );

                                                            messageTextSender.send(
                                                                    text,
                                                                    chatId,
                                                                    ReplyBuilder.inline()
                                                                            .addCallback("🔙 Kembali ke Daftar", "channel")
                                                                            .build()
                                                            );
                                                        }, error -> {
                                                            log.error("Error saving channel", error);
                                                            userStateService.clearState(userId);
                                                            messageTextSender.send("❌ Error menyimpan channel: " + error.getMessage(), chatId);
                                                        });
                                            })
                                            .exceptionally(ex -> {
                                                log.error("Error getting channel invite link", ex);
                                                userStateService.clearState(userId);
                                                messageTextSender.send("❌ Error mendapatkan invite link. Pastikan bot bisa membuat invite link di channel.", chatId);
                                                return null;
                                            });
                                })
                                .exceptionally(ex -> {
                                    log.error("Error getting chat info", ex);
                                    userStateService.clearState(userId);
                                    messageTextSender.send("❌ Error mendapatkan info channel. Pastikan Channel ID benar dan bot adalah admin.", chatId);
                                    return null;
                                });
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error checking permissions", ex);
                    userStateService.clearState(userId);
                    messageTextSender.send("❌ Error memeriksa permissions. Pastikan bot adalah admin di channel tersebut.", chatId);
                    return null;
                });
    }


    private Long parseChannelId(String input) {
        try {
            if (input.startsWith("@")) {
                return null;
            }
            return Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}