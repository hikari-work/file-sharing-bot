package com.yann.forcesub.handler;

import com.yann.forcesub.entity.AppSetting;
import com.yann.forcesub.event.VariableUpdateEvent;
import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.manager.CallbackType;
import com.yann.forcesub.service.ConfigService;
import com.yann.forcesub.service.ReplyBuilder;
import com.yann.forcesub.service.TextService;
import com.yann.forcesub.service.UserStateService;
import com.yann.forcesub.service.telegram.MessageTextSender;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Callback(trigger = "vars", type = CallbackType.STARTS_WITH)
public class VarsHandler implements CallbackHandler {
    private final ConfigService configService;
    private final MessageTextSender messageTextSender;
    private final TextService textService;
    private final UserStateService userStateService;
    private final ApplicationEventPublisher publisher;

    private static final String STATE_EDIT_VAR = "EDIT_VAR:";

    public VarsHandler(ConfigService configService, MessageTextSender messageTextSender,
                       TextService textService, UserStateService userStateService,
                       ApplicationEventPublisher publisher) {
        this.configService = configService;
        this.messageTextSender = messageTextSender;
        this.textService = textService;
        this.userStateService = userStateService;
        this.publisher = publisher;
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        log.info("Vars callback received {}", data);
        long userId = update.senderUserId;

        if (data(update).equals("vars")) {
            showVarsMenu(update, "view");
            return;
        }

        String[] dataSplit = data(update).split("_");

        if (dataSplit.length == 3 && dataSplit[1].equals("mode")) {
            String mode = dataSplit[2];
            showVarsMenu(update, mode);
            messageTextSender.answerInlineQuery(update.id);
            return;
        }

        if (dataSplit.length == 3) {
            String operation = dataSplit[1];
            String keyVars = dataSplit[2];

            if (operation.equals("view")) {
                handleViewVar(update, keyVars, userId);
            } else if (operation.equals("edit")) {
                handleEditVar(update, keyVars, userId);
            }

            messageTextSender.answerInlineQuery(update.id);
        }
    }


    private void showVarsMenu(TdApi.UpdateNewCallbackQuery update, String mode) {
        varsReply(mode)
                .subscribe(
                        replyMarkup -> {
                            log.info("Reply markup created for mode: {}", mode);
                            String message = mode.equals("view")
                                    ? "📋 Silahkan pilih variable yang ingin anda lihat"
                                    : "✏️ Silahkan pilih variable yang ingin anda edit";

                            messageTextSender.edit(
                                    message,
                                    update.chatId,
                                    update.messageId,
                                    replyMarkup
                            );
                        },
                        error -> log.error("Error creating reply markup", error)
                );
    }


    private void handleViewVar(TdApi.UpdateNewCallbackQuery update, String keyVars, long userId) {
        String config = configService.getConfig(keyVars);
        String value = config != null ? config : "null";

        String message = String.format(
                "📌 <b>Variable: %s</b>\n\n" +
                        "💬 <b>Value:</b>\n<code>%s</code>",
                keyVars, value
        );

        messageTextSender.edit(
                message,
                update.chatId,
                update.messageId,
                ReplyBuilder.inline()
                        .addCallback("🔙 Kembali", "vars")
                        .build()
        );
    }


    private void handleEditVar(TdApi.UpdateNewCallbackQuery update, String keyVars, long userId) {
        String currentConfig = configService.getConfig(keyVars);
        String currentValue = currentConfig != null ? currentConfig : "null";

        userStateService.setState(userId, STATE_EDIT_VAR + keyVars);

        String message = String.format(
                "✏️ <b>Edit Variable: %s</b>\n\n" +
                        "💬 <b>Current Value:</b>\n<code>%s</code>\n\n" +
                        "📝 Silahkan kirim value baru untuk variable ini.\n" +
                        "Ketik /cancel untuk membatalkan.",
                keyVars, currentValue
        );

        messageTextSender.edit(
                message,
                update.chatId,
                update.messageId,
                null
        );
    }


    public void handleTextMessage(TdApi.Chat chat, TdApi.Message message, long userId) {
        String state = userStateService.getState(userId);

        if (state != null && state.startsWith(STATE_EDIT_VAR)) {
            String keyVars = state.replace(STATE_EDIT_VAR, "");

            String newValue = null;
            if (message.content instanceof TdApi.MessageText) {
                newValue = ((TdApi.MessageText) message.content).text.text;
            }

            if ("/cancel".equals(newValue)) {
                userStateService.clearState(userId);
                messageTextSender.send("❌ Edit dibatalkan.", message.chatId);
                return;
            }

            if (newValue == null || newValue.trim().isEmpty()) {
                messageTextSender.send("⚠️ Value tidak boleh kosong. Ketik /cancel untuk membatalkan.", chat.id);
                return;
            }
            final String finalValue = newValue;
            publisher.publishEvent(new VariableUpdateEvent(this, keyVars, finalValue, false));
            userStateService.clearState(userId);
            String successMessage = String.format(
                    "✅ <b>Variable berhasil diupdate!</b>\n\n" +
                            "📌 <b>Variable:</b> %s\n" +
                            "💬 <b>New Value:</b>\n<code>%s</code>",
                    keyVars, finalValue
            );
            messageTextSender.send(
                    successMessage,
                    message.chatId,
                    ReplyBuilder.inline()
                            .addCallback("🔙 Kembali ke Variables", "vars")
                            .build()
            );
        }
    }

    @Override
    public String data(TdApi.UpdateNewCallbackQuery update) {
        return CallbackHandler.super.data(update);
    }


    private Mono<TdApi.ReplyMarkup> varsReply(String operation) {
        return configService.findAll()
                .collectList()
                .map(configList -> {
                    ReplyBuilder.InlineKeyboardBuilder inline = ReplyBuilder.inline();
                    inline.row(2);

                    for (AppSetting setting : configList) {
                        String emoji = operation.equals("view") ? "👁" : "✏️";
                        inline.addCallback(
                                emoji + " " + setting.getKey(),
                                "vars_" + operation + "_" + setting.getKey()
                        );
                    }

                    inline.row(2);
                    inline.newRow();

                    if (operation.equals("view")) {
                        inline.addCallback("✏️ Mode Edit", "vars_mode_edit");
                    } else {
                        inline.addCallback("👁 Mode View", "vars_mode_view");
                    }

                    inline.newRow();
                    inline.addCallback("🔙 Kembali", "start");

                    return inline.build();
                });
    }
}