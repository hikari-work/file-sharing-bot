package com.yann.forcesub.handler;

import com.yann.forcesub.manager.Callback;
import com.yann.forcesub.manager.CallbackHandler;
import com.yann.forcesub.manager.CallbackType;
import com.yann.forcesub.service.ReplyBuilder;
import com.yann.forcesub.service.TextService;
import com.yann.forcesub.service.telegram.MessageTextSender;
import it.tdlight.jni.TdApi;
import org.springframework.stereotype.Component;

@Callback(trigger = "about", type = CallbackType.EXACT)
@Component
public class AboutCallback implements CallbackHandler {
    private final TextService textService;
    private final MessageTextSender messageTextSender;

    public AboutCallback(TextService textService, MessageTextSender messageTextSender) {
        this.textService = textService;
        this.messageTextSender = messageTextSender;
    }

    @Override
    public void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data) {
        String text = textService.get("common.about");
        messageTextSender.edit(text, update.chatId, update.messageId, aboutMarkup());
    }

    public TdApi.ReplyMarkup aboutMarkup() {
        return ReplyBuilder.inline()
                .addUrl("Dev", "https://t.me/b_yannnn")
                .addCallback("Kembali", "start")
                .build();
    }
}
