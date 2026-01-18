package com.yann.forcesub.handler;

import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.manager.CallbackType;
import com.yann.forcesub.service.ReplyBuilder;
import com.yann.forcesub.service.TextService;
import com.yann.forcesub.service.telegram.MessageTextSender;
import it.tdlight.jni.TdApi;
import org.springframework.stereotype.Component;

@Callback(trigger = "help", type = CallbackType.EXACT)
@Component
public class HelpCallback implements CallbackHandler {
    private final MessageTextSender messageTextSender;
    private final TextService textService;

    public HelpCallback(MessageTextSender messageTextSender, TextService textService) {
        this.messageTextSender = messageTextSender;
        this.textService = textService;
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        long chatId = update.chatId;
        long messageId = update.messageId;
        messageTextSender.edit(textService.get("common.help"), chatId, messageId, reply());
        messageTextSender.answerInlineQuery(update.id);
    }

    @Override
    public String data(TdApi.UpdateNewCallbackQuery update) {
        return CallbackHandler.super.data(update);
    }
    private TdApi.ReplyMarkup reply() {
        return ReplyBuilder.inline()
                .addCallback("Kembali", "start")
                .build();
    }
}
