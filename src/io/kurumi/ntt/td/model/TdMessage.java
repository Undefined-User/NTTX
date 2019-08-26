package io.kurumi.ntt.td.model;

import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.td.TdApi.*;
import io.kurumi.ntt.td.client.TdListener;
import io.kurumi.ntt.td.client.TdBot;
import io.kurumi.ntt.td.client.TdClient;

public class TdMessage {

	public TdClient client;
	public Message message;

	private int isCommand = 0;
    private boolean noPayload = false;
    private String payload[];
    private boolean noParams = false;

    private String name;
	private String function;

    private String[] params;
	private String[] fixedParams;
	private String param;

	private static String[] NO_PARAMS = new String[0];

	public TdMessage(TdClient client,UpdateNewMessage update) {

		this.client = client;
		this.message = update.message;

	}

	public SendMessage send(InputMessageContent input) {

		return new SendMessage(message.chatId,0,false,false,null,input);

	}
	
	public String text() {
		
		if (message.content instanceof MessageText) {
			
			return ((MessageText)message.content).text.text;
			
		}
		
		return null;
		
	}

	public boolean isCommand() {

        if (isCommand == 0) {

			String message = text();

            if (message != null && (message.startsWith("/")) && message.length() > 1) {

                String body = message.substring(1);

                if (body.contains(" ")) {

                    String cmdAndUser = StrUtil.subBefore(body," ",false);

                    if (cmdAndUser.contains("@" + client.me.username)) {

                        name = StrUtil.subBefore(cmdAndUser,"@",false);

                    } else {

                        name = cmdAndUser;

                    }

                } else if (body.contains("@" + client.me.username)) {

                    name = StrUtil.subBefore(body,"@",false);

                } else {

                    name = body;

                }

                isCommand = 1;

 			} else {

                isCommand = 2;

            }

        }

        return isCommand == 1;

    }

    public String command() {

        if (!isCommand()) return null;

		if (function != null) return function;
		
        String body = text().substring(1);

        if (body.contains(" ")) {

            String cmdAndUser = StrUtil.subBefore(body," ",false);

            if (cmdAndUser.contains("@" + client.me.username)) {

                name = StrUtil.subBefore(cmdAndUser,"@",false);

            } else {

                name = cmdAndUser;

            }

        } else if (body.contains("@" + client.me.username)) {

            name = StrUtil.subBefore(body,"@",false);

        } else {

            name = body;

        }

		function = name;

        return name;

    }

	public boolean isStartPayload() {

        return "start".equals(command()) && params().length > 0;

    }

    public String[] payload() {

		if (noPayload) return NO_PARAMS;

        if (payload != null) return payload;

        if (!isStartPayload()) {

            noPayload = true;

            return NO_PARAMS;

        }

        payload = params()[0].split("_");

        return payload;

    }

	public String param() {

		if (param != null) return param;

		if (noParams) {

            return null;

        }

		if (!isCommand()) {

            noParams = true;

            return null;

        }
		
		String body = StrUtil.subAfter(text(),"/",false);

        if (body.contains(" ")) {

            param = StrUtil.subAfter(body," ",false);
			params = param.split(" ");
			fixedParams = param.replace("  "," ").split(" ");

        } else {

            noParams = true;

			param = "";
			params = NO_PARAMS;
            fixedParams = NO_PARAMS;

        }

		return param;

	}

	public String[] fixedParams() {

		if (fixedParams != null) return fixedParams;

		if (noParams) {

            return NO_PARAMS;

        }

		if (!isCommand()) {

            noParams = true;

            return NO_PARAMS;

        }

		String body = StrUtil.subAfter(text(),"/",false);

        if (body.contains(" ")) {

            param = StrUtil.subAfter(body," ",false);
			params = param.split(" ");
			fixedParams = param.replace("  "," ").split(" ");

        } else {

            noParams = true;

			param = "";
			params = NO_PARAMS;
            fixedParams = NO_PARAMS;

        }

		return fixedParams;

	}


    public String[] params() {

        if (params != null) return params;

        if (noParams) {

            return NO_PARAMS;

        }

        if (!isCommand()) {

            noParams = true;

            return NO_PARAMS;

        }
		
        String body = StrUtil.subAfter(text(),"/",false);

        if (body.contains(" ")) {

            param = StrUtil.subAfter(body," ",false);
			params = param.split(" ");
			fixedParams = param.replace("  "," ").split(" ");

        } else {

            noParams = true;

			param = "";
			params = NO_PARAMS;
            fixedParams = NO_PARAMS;

        }

        return params;

    }


}