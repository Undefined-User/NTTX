package com.pengrad.telegrambot.request;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;

/**
 * Stas Parshin
 * 07 May 2016
 */
public class EditMessageText extends BaseRequest<EditMessageText, BaseResponse> {

    private String text;

    public EditMessageText(Object chatId, int messageId, String text) {
        super(SendResponse.class);
        add("chat_id", chatId).add("message_id", messageId).add("text", this.text = text);

    }

    public EditMessageText(String inlineMessageId, String text) {
        super(BaseResponse.class);
        add("inline_message_id", inlineMessageId).add("text", text);
    }

    public void setText(String text) {
        this.text = text;
        add("text", text);
    }

    public String getText() {
        return text;
    }

    public EditMessageText parseMode(ParseMode parseMode) {
        return add("parse_mode", parseMode.name());
    }

    public EditMessageText disableWebPagePreview(boolean disableWebPagePreview) {
        return add("disable_web_page_preview", disableWebPagePreview);
    }

    public EditMessageText replyMarkup(InlineKeyboardMarkup replyMarkup) {
        return add("reply_markup", replyMarkup);
    }

}
