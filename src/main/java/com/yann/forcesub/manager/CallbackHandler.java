package com.yann.forcesub.manager;

import it.tdlight.jni.TdApi;

import java.nio.charset.StandardCharsets;

public interface CallbackHandler {

    void handle(TdApi.Chat chat, TdApi.UpdateNewCallbackQuery update, String data);
    default String data(TdApi.UpdateNewCallbackQuery update) {
     String payloadData;
     if (update.payload instanceof TdApi.CallbackQueryPayloadData data) {
         payloadData = new String(data.data, StandardCharsets.UTF_8);
         if (payloadData.isEmpty()) return null;
         return payloadData;
     } else return null;
    }
}
