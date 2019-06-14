package com.pengrad.telegrambot.request;

import com.pengrad.telegrambot.model.request.ParseMode;

/**
 * stas
 * 5/1/16.
 */
public class SendMessage extends AbstractSendRequest<SendMessage> {

    public ParseMode mode;
    public boolean disablePreview = false;
    String text;

    public SendMessage(Object chatId, String text) {
        super(chatId);
        add("text", text);
        this.text = text;
    }

    public String getText() {

        return text;

    }

    public void setText(String text) {

        add("text", text);
        this.text = text;

    }

    public SendMessage parseMode(ParseMode parseMode) {
        mode = parseMode;
        return add("parse_mode", parseMode.name());
    }

    public SendMessage disableWebPagePreview(boolean disableWebPagePreview) {
        disablePreview = disableWebPagePreview;
        return add("disable_web_page_preview", disableWebPagePreview);
    }
}
