package io.kurumi.ntt.fragment;

import cn.hutool.core.util.*;
import cn.hutool.json.*;
import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.*;
import io.kurumi.ntt.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.fragment.admin.*;
import io.kurumi.ntt.fragment.twitter.*;
import io.kurumi.ntt.fragment.twitter.archive.*;
import io.kurumi.ntt.model.*;
import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.utils.*;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;

import io.kurumi.ntt.model.Callback;

public abstract class BotFragment extends Fragment implements UpdatesListener,ExceptionHandler {


	public static Timer mainTimer = new Timer();
	public static Timer trackTimer = new Timer();

	public static ExecutorService asyncPool = Executors.newCachedThreadPool();

    public User me;
    private TelegramBot bot;
    public LinkedList<Fragment> fragments = new LinkedList<>();
    private String token;
    private PointStore point;

	public List<Long> localAdmins = new ArrayList<>();

    @Override
    public TelegramBot bot() {

        return bot;
    }

    public void reload() {

		fragments.clear();

		addFragment(this);

		addFragment(new Firewall());

    }

	public HashMap<String,Fragment> functions = new HashMap<>();

	public HashMap<String,Fragment> adminFunctions = new HashMap<>();

	public HashMap<String,Fragment> payloads = new HashMap<>();

	public HashMap<String,Fragment> adminPayloads = new HashMap<>();

	public HashMap<String,Fragment> points = new HashMap<>();
	public HashMap<String,Fragment> callbacks = new HashMap<>();


	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("cancel");

		registerPoint(POINT_REQUEST_TWITTER);

	}

	@Override
	public int checkPoint(UserData user,Msg msg,String point,PointData data) {

		return PROCESS_SYNC;

	}

	@Override
	public void onPoint(final UserData user,Msg msg,String point,PointData data) {

		if (POINT_REQUEST_TWITTER.equals(point)) {

            final TwitterRequest request = (TwitterRequest) data;

			data.context.add(msg);

            if (!msg.hasText() || !msg.text().startsWith("@")) {

                msg.send("请选择 Twitter 账号 (˚☐˚! )/").withCancel().exec(data);

                return;

            }

            String screenName = msg.text().substring(1);

            final TAuth account = TAuth.getById(UserArchive.get(screenName).id);

            if (account == null) {

                msg.send("找不到这个账号 (？) 请重新选择 ((*゜Д゜)ゞ").withCancel().exec(data);

                return;

            }

			clearPrivatePoint(user);

            msg.send("选择了 : " + account.archive().urlHtml() + " (❁´▽`❁)").html().failed(2 * 1000);

			if (request.payload) {

				String payload = request.originMsg.payload()[0];

				String[] params = request.originMsg.payload().length > 1 ? ArrayUtil.sub(request.originMsg.payload(),1,request.originMsg.payload().length) : new String[0];

				int checked = request.fragment.checkTwitterPayload(user,request.originMsg,payload,params,account);

				request.fragment.onTwitterPayload(user,request.originMsg,payload,params,account);

				if (checked == PROCESS_ASYNC) {

					asyncPool.execute(new Runnable() {

							@Override
							public void run() {

								request.fragment.onTwitterFunction(user,request.originMsg,request.originMsg.command(),request.originMsg.params(),account);

							}

						});

				} else {

					request.fragment.onTwitterFunction(user,request.originMsg,request.originMsg.command(),request.originMsg.params(),account);

				}


			} else {

				int checked = request.fragment.checkTwitterFunction(user,request.originMsg,request.originMsg.command(),request.originMsg.params(),account);

				if (checked == PROCESS_ASYNC) {

					asyncPool.execute(new Runnable() {

							@Override
							public void run() {

								request.fragment.onTwitterFunction(user,request.originMsg,request.originMsg.command(),request.originMsg.params(),account);

							}

						});

				} else {

					request.fragment.onTwitterFunction(user,request.originMsg,request.originMsg.command(),request.originMsg.params(),account);

				}

			}

        }

	}

    public void addFragment(Fragment fragment) {

		fragment.init(this);

		fragments.add(fragment);

    }

    public abstract String botName();

    @Override
    public int process(List<Update> updates) {

        for (final Update update : updates) {

            try {

                processAsync(update);

            } catch (Exception e) {

                BotLog.error("更新出错",e);

                Launcher.INSTANCE.uncaughtException(Thread.currentThread(),e);

            }


        }

        return CONFIRMED_UPDATES_ALL;

    }

    @Override
    public PointStore point() {

        if (point != null) return point;

        synchronized (this) {

            if (point != null) return point;

            point = PointStore.getInstance(this);

            return point;

        }

    }

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		if ("cancel".equals(function)) {

			msg.send("没有什么需要取消的 :)").failedWith();

			return;

		}

	}

	@Override
	public void onPointedFunction(UserData user,Msg msg,String function,String[] params,String point,PointData data) {

		data.context.add(msg);

		if ("cancel".equals(function)) {

			if (data.type == 1) clearPrivatePoint(user).onCancel(user,msg); else clearGroupPoint(user).onCancel(user,msg);

			msg.send("已经取消当前操作 :) ","帮助文档 : @NTT_X").failedWith(9 * 1000);

			return;

		}

	}

    public void processAsync(final Update update) {

		System.out.println(new JSONObject(update.json).toStringPretty());

        final UserData user;

		long targetId = -1;

        if (update.message() != null) {

            user = UserData.get(update.message().from());

			if (update.message().chat().type() != Chat.Type.Private) {

				targetId = update.message().chat().id();

			}

        } else if (update.editedMessage() != null) {

			user = UserData.get(update.editedMessage().from());

			if (update.editedMessage().chat().type() != Chat.Type.Private) {

				targetId = update.editedMessage().chat().id();

			}

		} else if (update.channelPost() != null) {

            user = update.channelPost().from() != null ? UserData.get(update.channelPost().from()) : null;

			targetId = update.channelPost().chat().id();

		} else if (update.editedChannelPost() != null) {

			user = update.editedChannelPost().from() != null ? UserData.get(update.editedChannelPost().from()) : null;

			targetId = update.editedChannelPost().chat().id();

        } else if (update.callbackQuery() != null) {

            user = UserData.get(update.callbackQuery().from());

        } else if (update.inlineQuery() != null) {

            user = UserData.get(update.inlineQuery().from());

        } else user = null;

		if (onUpdate(user,update)) return;

		for (Fragment f : fragments) if (f.update() && f.onUpdate(user,update)) return;

		if (update.message() != null) {

			final Msg msg = new Msg(this,update.message());

			msg.update = update;

			if (msg.replyTo() != null) msg.replyTo().update = update;

			final PointData privatePoint = point().getPrivate(user);
			final PointData groupPoint = point().getGroup(user);

			if (msg.isGroup() && groupPoint != null) {

				final Fragment function = points.containsKey(groupPoint.point) ? points.get(groupPoint.point) : this;

				if (msg.isCommand()) {

					int checked = function.checkPointedFunction(user,msg,msg.command(),msg.params(),groupPoint.point,groupPoint);

					if (checked == PROCESS_REJECT) return;

					if (checked == PROCESS_ASYNC) {

						asyncPool.execute(new Runnable() {

								@Override
								public void run() {

									function.onPointedFunction(user,msg,msg.command(),msg.params(),groupPoint.point,groupPoint);

								}

							});

					} else {

						function.onPointedFunction(user,msg,msg.command(),msg.params(),groupPoint.point,groupPoint);

					}

				} else {

					int checked = function.checkPoint(user,msg,groupPoint.point,groupPoint);

					if (checked == PROCESS_REJECT) return;

					if (checked == PROCESS_ASYNC) {

						asyncPool.execute(new Runnable() {

								@Override
								public void run() {

									function.onPoint(user,msg,groupPoint.point,groupPoint);

								}

							});

					} else {

						function.onPoint(user,msg,groupPoint.point,groupPoint);

					}

				}

			} else if (msg.isPrivate() && privatePoint != null) {

				final Fragment function = !points.containsKey(privatePoint.point) || "cancel".equals(msg.command()) ? this : points.get(privatePoint.point);

				if (msg.isCommand()) {

					int checked = function.checkPointedFunction(user,msg,msg.command(),msg.params(),privatePoint.point,privatePoint);

					if (checked == PROCESS_REJECT) return;

					if (checked == PROCESS_ASYNC) {

						asyncPool.execute(new Runnable() {

								@Override
								public void run() {


									function.onPointedFunction(user,msg,msg.command(),msg.params(),privatePoint.point,privatePoint);

								}

							});

					} else {

						function.onPointedFunction(user,msg,msg.command(),msg.params(),privatePoint.point,privatePoint);


					}

				} else {

					int checked = function.checkPoint(user,msg,privatePoint.point,privatePoint);

					if (checked == PROCESS_REJECT) return;

					if (checked == PROCESS_ASYNC) {

						asyncPool.execute(new Runnable() {

								@Override
								public void run() {

									function.onPoint(user,msg,privatePoint.point,privatePoint);

								}

							});

					} else {

						function.onPoint(user,msg,privatePoint.point,privatePoint);

					}

				}

			} else {

				if (msg.isCommand()) {

					if (msg.isStartPayload()) {

						final String payload = msg.payload()[0];
						final String[] params = msg.payload().length > 1 ? ArrayUtil.sub(msg.payload(),1,msg.payload().length) : new String[0];

						if (payloads.containsKey(payload)) {

							final Fragment function = payloads.get(payload);

							int checked = function.checkPayload(user,msg,payload,params);

							if (checked == PROCESS_REJECT) return;

							if (checked == PROCESS_ASYNC) {

								asyncPool.execute(new Runnable() {

										@Override
										public void run() {

											function.onPayload(user,msg,payload,params);

										}

									});

							} else {

								function.onPayload(user,msg,payload,params);


							}

						} else if ((user.admin() || localAdmins.contains(user.id)) && adminPayloads.containsKey(payload)) {

							final Fragment function = adminPayloads.get(payload);

							int checked = function.checkPayload(user,msg,payload,params);

							if (checked == PROCESS_REJECT) return;

							if (checked == PROCESS_ASYNC) {

								asyncPool.execute(new Runnable() {

										@Override
										public void run() {

											function.onPayload(user,msg,payload,params);

										}

									});

							} else {

								function.onPayload(user,msg,payload,params);

							}


						} else {

							int checked = checkPayload(user,msg,payload,params);

							if (checked == PROCESS_REJECT) return;

							if (checked == PROCESS_ASYNC) {

								asyncPool.execute(new Runnable() {

										@Override
										public void run() {

											onPayload(user,msg,payload,params);

										}

									});

							} else {

								onPayload(user,msg,payload,params);

							}

						}


					} else if ((user.admin() || localAdmins.contains(user.id)) && adminFunctions.containsKey(msg.command())) {

						final Fragment function = adminFunctions.get(msg.command());

						int checked = function.checkFunction(user,msg,msg.command(),msg.params());

						if (checked == PROCESS_REJECT) return;

						if (checked == PROCESS_ASYNC) {

							asyncPool.execute(new Runnable() {

									@Override
									public void run() {

										function.onFunction(user,msg,msg.command(),msg.params());

									}

								});

						} else {

							function.onFunction(user,msg,msg.command(),msg.params());

						}

					} else {

						final Fragment function = functions.containsKey(msg.command()) ? functions.get(msg.command()) : this;

						int checked = function.checkFunction(user,msg,msg.command(),msg.params());

						if (checked == PROCESS_REJECT) return;

						if (function != this && function.checkFunctionContext(user,msg,msg.command(),msg.params()) == FUNCTION_GROUP && !msg.isGroup()) {

							msg.send("请在群组使用 :)").async();


						} else if (function != this && function.checkFunctionContext(user,msg,msg.command(),msg.params()) == FUNCTION_PRIVATE && !msg.isPrivate()) {

							asyncPool.execute(new Runnable() {

									@Override
									public void run() {

										msg.send("命令请在私聊使用 :)").failedWith();


									}

								});


						}

						if (checked == PROCESS_ASYNC) {

							asyncPool.execute(new Runnable() {

									@Override
									public void run() {

										function.onFunction(user,msg,msg.command(),msg.params());

									}

								});

						} else {

							function.onFunction(user,msg,msg.command(),msg.params());

						}

					}

				} else {

					for (final Fragment f : fragments) {

						int checked = f.checkMsg(user,msg); 

						if (checked == PROCESS_ASYNC) {

							asyncPool.execute(new Runnable() {

									@Override
									public void run() {

										f.onMsg(user,msg);

									}

								});

						} else if (checked == PROCESS_REJECT) {

							continue;

						} else {

							f.onMsg(user,msg);

						}

					}

				}

			}

		} else if (update.channelPost() != null) {

			final Msg msg = new Msg(this,update.channelPost());

			msg.update = update;

			if (msg.replyTo() != null) msg.replyTo().update = update;

			for (final Fragment f : fragments) {

				if (!f.post()) continue;

				int checked = f.checkChanPost(user,msg); 

				if (checked == PROCESS_REJECT) continue;

				if (checked == PROCESS_ASYNC) {

					asyncPool.execute(new Runnable() {

							@Override
							public void run() {

								f.onChanPost(user,msg);

							}

						});

				} else {

					f.onChanPost(user,msg);

				}

			}

		} else if (update.callbackQuery() != null) {

			final Callback callback = new Callback(this,update.callbackQuery());

			final String point = callback.params.length == 0 ? "" : callback.params[0];
			final String[] params = callback.params.length > 1 ? ArrayUtil.sub(callback.params,1,callback.params.length) : new String[0];

			final Fragment function = callbacks.containsKey(point) ?  callbacks.get(point): this;

			int checked = function.checkCallback(user,callback,point,params);

			if (checked == PROCESS_REJECT) return;

			if (checked == PROCESS_ASYNC) {

				asyncPool.execute(new Runnable() {

						@Override
						public void run() {

							function.onCallback(user,callback,point,params);


						}

					});

			} else {

				function.onCallback(user,callback,point,params);


			}


		} else if (update.inlineQuery() != null) {

			Query query = new Query(this,update.inlineQuery());

			query.update = update;

			for (Fragment f : fragments) {

				if (f.query()) f.onQuery(user,query);

			}

		} else if (update.poll() != null) {

			for (Fragment f : fragments) {

				if (f.poll()) f.onPollUpdate(update.poll());

			}

		}

	}

	@Override
	public void onCallback(UserData user,Callback callback,String point,String[] params) {

		if ("null".equals(point)) callback.confirm();
		else callback.alert("无效的回调指针 : " + point + "\n请联系开发者");

	}

	final String split = "------------------------\n";

	public void onFinalMsg(UserData user,Msg msg) {

		StringBuilder str = new StringBuilder();

		boolean no_reply = false;

		Message message = msg.message();

		str.append("消息ID : " + message.messageId()).append("\n");

		if (message.forwardFrom() != null) {

			no_reply = true;

			str.append("来自用户 : ").append(UserData.get(message.forwardFrom()).userName()).append("\n");
			str.append("用户ID : ").append(message.forwardFrom().id()).append("\n");

		}

		if (message.forwardFromChat() != null) {

			no_reply = true;

			if (message.forwardFromChat().type() == Chat.Type.channel) {

				str.append("来自频道 : ").append(message.forwardFromChat().username() == null ? message.forwardFromChat().title() : Html.a(message.forwardFromChat().username(),"https://t.me/" + message.forwardFromChat().username())).append("\n");

				str.append("频道ID : ").append(message.forwardFromChat().id());

			} else if (message.forwardFromChat().type() == Chat.Type.group || message.forwardFromChat().type() == Chat.Type.supergroup) {

				str.append("来自群组 : ").append(message.forwardFromChat().username() == null ? message.forwardFromChat().title() : Html.a(message.forwardFromChat().username(),"https://t.me/" + message.forwardFromChat().username())).append("\n");

			} else {

				if (message.forwardFrom() == null) {

					str.append("来自 : ").append(message.forwardSenderName()).append(" (隐藏来源)\n");

				}

			}

		}

		if (message.forwardSenderName() != null) {

			no_reply = true;

			str.append("来自用户 : ").append(message.forwardSenderName());

		}

		if (message.sticker() != null) {

			no_reply = true;

			str.append(split);

			str.append("贴纸ID : ").append(message.sticker().fileId()).append("\n");

			str.append("贴纸表情 : ").append(message.sticker().emoji()).append("\n");

			if (message.sticker().setName() != null) {

				str.append("贴纸包 : ").append("https://t.me/addstickers/" + message.sticker().setName()).append("\n");

			}

			msg.sendUpdatingPhoto();

			bot().execute(new SendPhoto(msg.chatId(),getFile(msg.message().sticker().fileId())).caption(str.toString()).parseMode(ParseMode.HTML).replyMarkup(new ReplyKeyboardRemove()).replyToMessageId(msg.messageId()));

		}

		if (!no_reply) {



			if (msg.hasText()) {

				String text = TentcentNlp.nlpTextchat(msg.chatId().toString(),msg.text());

				if (text != null) msg.send(text).exec();

				return;

			}

		}

		msg.send("喵......？",str.toString()).replyTo(msg).html().removeKeyboard().exec();

	}

	public boolean isLongPulling() {

		return false;

	}

	public String getToken() {

		return Env.get("token." + botName());

	}

	public void setToken(String botToken) {

		Env.set("token." + botName(),token);

	}

	public boolean silentStart() {

		reload();

		token = getToken();

		bot = new TelegramBot.Builder(token).build();

		GetMeResponse resp = bot.execute(new GetMe());

		if (resp == null || !resp.isOk()) return false;

		me = resp.user();

		realStart();

		return true;

	}

	public void start() {

		reload();

		token = getToken();

		if (token == null || !Env.verifyToken(token)) {

			token = Env.inputToken(botName());

		}

		setToken(token);

		OkHttpClient.Builder okhttpClient = new OkHttpClient.Builder();

		okhttpClient.networkInterceptors().clear();

		bot = new TelegramBot.Builder(token)
			.okHttpClient(okhttpClient.build()).build();

		me = bot.execute(new GetMe()).user();

		realStart();

	}

	public void realStart() {

		bot.execute(new DeleteWebhook());

		if (isLongPulling()) {

			bot.setUpdatesListener(this,this);

		} else {

			/*

			 GetUpdatesResponse update = bot.execute(new GetUpdates());

			 if (update.isOk()) {

			 process(update.updates());

			 }

			 */

			String url = "https://" + BotServer.INSTANCE.domain + "/" + token;

			BotServer.fragments.put(token,this);

			BaseResponse resp = bot.execute(new SetWebhook().url(url));

			if (!resp.isOk()) {

				BotLog.debug("SET WebHook for " + botName() + " Failed : " + resp.description());

				BotServer.fragments.remove(token);

			}


		}

	}

	public void stop() {

		for (Long id : point().privatePoints.keySet()) {

			new Send(this,id,"当前操作已经取消 : NTT 正在更新 / 重启").removeKeyboard().exec();

		}
		
		for (Fragment f : fragments) f.onStop();

		if (!isLongPulling()) {

			// bot.execute(new DeleteWebhook());

		} else {

			bot.removeGetUpdatesListener();

		}

	}

	@Override
	public void onException(TelegramException e) {

		BotLog.debug(UserData.get(me).userName() + " : " + BotLog.parseError(e));

	}


}
