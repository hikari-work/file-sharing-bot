package com.yann.forcesub.manager;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailParseException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackDispatcher {

    private final ApplicationContext context;
    private final Map<String, CallbackHandler> exactCallback = new HashMap<>();
    private final Map<String, CallbackHandler> startWithCallback = new HashMap<>();

    @PostConstruct
    public void init() {
        Map<String, Object> beans = context.getBeansWithAnnotation(Callback.class);
        beans.forEach((name, bean) -> {
            if (bean instanceof CallbackHandler handler) {
                Callback annotation = handler.getClass().getAnnotation(Callback.class);
                String trigger = annotation.trigger();
                if (annotation.type() == CallbackType.EXACT) {
                    exactCallback.put(trigger, handler);
                } else {
                    startWithCallback.put(trigger, handler);
                }
                log.info("Register callback {} for {}", trigger, name);
            }
        });
    }

    public void onCallbackQuery(TdApi.UpdateNewCallbackQuery update) {
        TdApi.CallbackQueryPayload payload = update.payload;
        if (payload instanceof TdApi.CallbackQueryPayloadData data) {
            String payloadData = new String(data.data, StandardCharsets.UTF_8);
            if (exactCallback.containsKey(payloadData)) {
                exactCallback.get(payloadData).handle(null, update, payloadData);
                return;
            }
            for (Map.Entry<String, CallbackHandler> entry : startWithCallback.entrySet()) {
                if (payloadData.startsWith(entry.getKey())) {
                    entry.getValue().handle(null, update, payloadData);
                }
            }
        }
    }
}
