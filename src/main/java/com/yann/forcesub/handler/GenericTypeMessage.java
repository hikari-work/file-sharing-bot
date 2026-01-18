package com.yann.forcesub.handler;

import com.yann.forcesub.service.UserStateService;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericTypeMessage {

    private final HandleMessageGenerator handleMessageGenerator;
    @Value("${default.database.id}")
    private long defaultDatabaseId;

    private final UserStateService userStateService;
    private final VarsHandler varsHandler;
    private final ForceSubUpdateHandler forceSubUpdateHandler;

    public void handle(TdApi.UpdateNewMessage message) {
        String state = userStateService.getState(message.message.chatId);
        if (state != null) {
            if (state.startsWith("EDIT_VAR")) {
                varsHandler.handleTextMessage(null, message.message, message.message.chatId);
            }
            if (state.startsWith("EDIT_CHANNEL") || state.startsWith("ADD_CHANNEL")) {
                forceSubUpdateHandler.handleTextMessage(message.message, message.message.chatId);
            }
        }
        String messageText = extractMessageText(message.message.content);
        if (messageText == null || messageText.startsWith("/")) {
            log.info("Processing non-command message: {}", messageText);
        }
        if (message.message.chatId == defaultDatabaseId) {
            processMessage(message);
        }

    }

    private String extractMessageText(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText text) {
            return text.text.text;
        } else if (content instanceof TdApi.MessageAnimation animation) {
            return animation.caption != null ? animation.caption.text : null;
        } else if (content instanceof TdApi.MessagePhoto photo) {
            return photo.caption != null ? photo.caption.text : null;
        } else if (content instanceof TdApi.MessageVideo video) {
            return video.caption != null ? video.caption.text : null;
        } else if (content instanceof TdApi.MessageDocument document) {
            return document.caption != null ? document.caption.text : null;
        }
        return null;
    }

    private void processMessage(TdApi.UpdateNewMessage message) {
        handleMessageGenerator.handleMessage(message.message);
    }

}