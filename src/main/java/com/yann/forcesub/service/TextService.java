package com.yann.forcesub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextService {

    private final MessageSource messageSource;

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static final ThreadLocal<Locale> LOCALE_CONTEXT = ThreadLocal.withInitial(() -> DEFAULT_LOCALE);

    public Locale getLocale() {
        return LOCALE_CONTEXT.get();
    }

    public String get(String key) {
        return getMessage(key, getLocale());
    }

    public String get(String key, Object... args) {
        return getMessage(key, getLocale(), args);
    }

    public String get(String key, Map<String, Object> params) {
        return getMessage(key, getLocale(), params.values().toArray());
    }

    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    /**
     * Get message dengan locale dan args
     */
    public String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    public static class MessageBuilder {
        private final TextService textService;
        private String key;
        private Locale locale;
        private final Map<String, Object> params = new HashMap<>();
        private String defaultMessage;

        private MessageBuilder(TextService textService) {
            this.textService = textService;
        }

        public MessageBuilder key(String key) {
            this.key = key;
            return this;
        }

        public MessageBuilder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public MessageBuilder locale(String languageCode) {
            this.locale = Locale.forLanguageTag(languageCode);
            return this;
        }

        public MessageBuilder param(String name, Object value) {
            this.params.put(name, value);
            return this;
        }

        public MessageBuilder params(Map<String, Object> params) {
            this.params.putAll(params);
            return this;
        }

        public MessageBuilder defaultMessage(String defaultMessage) {
            this.defaultMessage = defaultMessage;
            return this;
        }

        public String build() {
            Locale loc = locale != null ? locale : textService.getLocale();
            Object[] args = params.values().toArray();

            if (defaultMessage != null) {
                return textService.messageSource.getMessage(key, args, defaultMessage, loc);
            }
            return textService.messageSource.getMessage(key, args, loc);
        }
    }

    public static class Common {
        private final TextService textService;

        public Common(TextService textService) {
            this.textService = textService;
        }

        public String welcome(String username) {
            return textService.get("common.welcome", username);
        }

        public String error() {
            return textService.get("common.error");
        }

        public String success() {
            return textService.get("common.success");
        }

        public String notFound() {
            return textService.get("common.notfound");
        }

        public String unauthorized() {
            return textService.get("common.unauthorized");
        }
    }

    public Common common() {
        return new Common(this);
    }
}