package com.yann.forcesub.service;

import it.tdlight.jni.TdApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReplyBuilder {
    public static InlineKeyboardBuilder inline() {
        return new InlineKeyboardBuilder();
    }


    public static class InlineKeyboardBuilder {
        private final List<List<TdApi.InlineKeyboardButton>> rows = new ArrayList<>();
        private List<TdApi.InlineKeyboardButton> currentRow = new ArrayList<>();
        private int maxButtonsPerRow = Integer.MAX_VALUE;

        public InlineKeyboardBuilder row(int maxButtonsPerRow) {
            this.maxButtonsPerRow = maxButtonsPerRow;
            return this;
        }
        public InlineKeyboardBuilder addCallback(String text, String callbackData) {
            checkAndWrapRow();
            TdApi.InlineKeyboardButton button = new TdApi.InlineKeyboardButton();
            button.text = text;
            button.type = new TdApi.InlineKeyboardButtonTypeCallback(callbackData.getBytes());
            currentRow.add(button);
            return this;
        }
        public InlineKeyboardBuilder addUrl(String text, String url) {
            checkAndWrapRow();
            TdApi.InlineKeyboardButton button = new TdApi.InlineKeyboardButton();
            button.text = text;
            button.type = new TdApi.InlineKeyboardButtonTypeUrl(url);
            currentRow.add(button);
            return this;
        }
        public InlineKeyboardBuilder add(TdApi.InlineKeyboardButton button) {
            checkAndWrapRow();
            currentRow.add(button);
            return this;
        }
        public InlineKeyboardBuilder addCopy(String text, String copy) {
            checkAndWrapRow();
            TdApi.InlineKeyboardButton button = new TdApi.InlineKeyboardButton();
            button.text = text;
            button.type = new TdApi.InlineKeyboardButtonTypeCopyText(copy);
            currentRow.add(button);
            return this;
        }

        public TdApi.ReplyMarkup build() {
            if (!currentRow.isEmpty()) {
                rows.add(currentRow);
            }
            if (rows.isEmpty()) {
                return null;
            }
            TdApi.InlineKeyboardButton[][] buttons = new TdApi.InlineKeyboardButton[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                buttons[i] = rows.get(i).toArray(new TdApi.InlineKeyboardButton[0]);
            }
            TdApi.ReplyMarkupInlineKeyboard markup = new TdApi.ReplyMarkupInlineKeyboard();
            markup.rows = buttons;
            return markup;
        }

        public InlineKeyboardBuilder newRow() {
            if (!currentRow.isEmpty()) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
            return this;
        }

        private void checkAndWrapRow() {
         if (currentRow.size() >= maxButtonsPerRow) {
             newRow();
         }
        }
    }
}
