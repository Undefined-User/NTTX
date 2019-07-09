/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.kurumi.ntt.fragment;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteWebhook;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.BotLog;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.stream.ChunkedFile;
import java.io.File;
import javax.activation.MimetypesFileTypeMap;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class BotServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private FullHttpRequest request;

	@Override
	public void channelRead0(ChannelHandlerContext ctx,FullHttpRequest request) throws Exception {

		this.request = request;

		if (!request.decoderResult().isSuccess()) {

			sendError(ctx,BAD_REQUEST);

			return;

		}

		if (true) {

			sendError(ctx,INTERNAL_SERVER_ERROR);

			return;

		}

		if (request.uri().equals("/data/" + Launcher.INSTANCE.getToken())) {

			File dataFile = new File(Env.CACHE_DIR,"data.zip");

			if (!dataFile.isFile()) {

				sendError(ctx,NOT_FOUND);

				return;

			}

			FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1,OK);

			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

			resp.headers().set(HttpHeaderNames.CONTENT_TYPE,mimeTypesMap.getContentType(dataFile));
			resp.headers().set(HttpHeaderNames.CONTENT_LENGTH,dataFile.length());

			final boolean keepAlive = HttpUtil.isKeepAlive(request);

			if (!keepAlive) {

				resp.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.CLOSE);

			} else if (request.protocolVersion().equals(HTTP_1_0)) {

				resp.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);

			}

			ctx.write(resp);

			ChannelFuture last = ctx.writeAndFlush(new ChunkedFile(dataFile)).addListener(ChannelFutureListener.CLOSE);

			if (!keepAlive) {

				last.addListener(ChannelFutureListener.CLOSE);

			}

			return;

		} else if (request.uri().equals("/upgrade/" + Launcher.INSTANCE.getToken())) {

			sendOk(ctx);

			new Thread() {

				@Override
				public void run() {

					new Send(Env.GROUP,"Bot Update Executed : By WebHook").exec();

					// Launcher.INSTANCE.stop();

					try {

						String str = RuntimeUtil.execForStr("bash update.sh");

						new Send(Env.GROUP,"update successful , now restarting...\n",str).exec();

						RuntimeUtil.exec("service ntt restart");

					} catch (Exception e) {

						new Send(Env.GROUP,"update failed",BotLog.parseError(e)).exec();

					}

				}

			}.start();

			return;

		} else 

		if (request.getMethod() != POST || request.uri().length() <= 1) {

			sendError(ctx,NOT_FOUND);

			return;

		}

		String botToken = request.uri().substring(1);

		if (!BotServer.fragments.containsKey(botToken)) {

			sendError(ctx,INTERNAL_SERVER_ERROR);

			new TelegramBot(botToken).execute(new DeleteWebhook());

			return;

		}

		try {

			Update update = BotUtils.parseUpdate(request.content().toString(CharsetUtil.CHARSET_UTF_8));

			BotServer.fragments.get(botToken).processAsync(update);

			sendOk(ctx);

		} catch (Exception ex) {

			sendError(ctx,INTERNAL_SERVER_ERROR);

		}

		//update.lock = new ProcessLock();

		/*BaseRequest webhookResponse =*/ 
		/*

		 if (webhookResponse == null) {



		 sendOk(ctx); 

		 } else 	{

		 FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1,OK,stringByteBuf(webhookResponse.toWebhookResponse()));

		 resp.headers().set(HttpHeaderNames.CONTENT_TYPE,"application/json; charset=UTF-8");

		 boolean keepAlive = HttpUtil.isKeepAlive(request);

		 if (!keepAlive) {

		 ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

		 } else {

		 resp.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);

		 ctx.writeAndFlush(resp);

		 }

		 }

		 */

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause) {

		cause.printStackTrace();

		if (ctx.channel().isActive()) {

			sendError(ctx,INTERNAL_SERVER_ERROR);

		}

	}

	void sendOk(ChannelHandlerContext ctx) {

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,OK);
		
		this.sendAndCleanupConnection(ctx,response);

	}


	void sendRedirect(ChannelHandlerContext ctx,String newUri) {

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,FOUND);

		response.headers().set(HttpHeaderNames.LOCATION,newUri);

		this.sendAndCleanupConnection(ctx,response);

	}

	void sendError(ChannelHandlerContext ctx,HttpResponseStatus status) {

		FullHttpResponse response = new DefaultFullHttpResponse(
			HTTP_1_1,status,Unpooled.copiedBuffer("Failure: " + status + "\r\n",CharsetUtil.CHARSET_UTF_8));

		response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain; charset=UTF-8");

		this.sendAndCleanupConnection(ctx,response);

	}

	void sendAndCleanupConnection(ChannelHandlerContext ctx,FullHttpResponse response) {

		final FullHttpRequest request = this.request;

		final boolean keepAlive = HttpUtil.isKeepAlive(request);

		HttpUtil.setContentLength(response,response.content().readableBytes());

		if (!keepAlive) {

			// We're going to close the connection as soon as the response is sent,
			// so we should also make it clear for the client.

			response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.CLOSE);

		} else if (request.protocolVersion().equals(HTTP_1_0)) {

			response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);

		}

		ChannelFuture flushPromise = ctx.writeAndFlush(response);

		if (!keepAlive) {

			// Close the connection as soon as the response is sent.

			flushPromise.addListener(ChannelFutureListener.CLOSE);

		}
	}

}