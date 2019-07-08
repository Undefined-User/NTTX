package io.kurumi.ntt.fragment.twitter.list;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.db.PointData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.twitter.TApi;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.model.request.Keyboard;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.NTT;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UserList;
import com.pengrad.telegrambot.response.SendResponse;
import java.util.LinkedHashMap;
import twitter4j.User;
import cn.hutool.core.util.ArrayUtil;
import java.util.Map;
import io.kurumi.ntt.fragment.twitter.archive.UserArchive;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import io.kurumi.ntt.fragment.twitter.list.ListImport.ImportThread;
import com.pengrad.telegrambot.request.SendDocument;

public class ListImport extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("import","import_cancel");
		registerPoint(POINT_LIST_IMPORT);

	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		requestTwitter(user,msg);

	}

	final String POINT_LIST_IMPORT = "import_list";

	static class ImportList extends PointData {

		LinkedList<Long> list = new LinkedList<>();
		TAuth auth;
		int type = 0;
		long target = 0;
		int mode = 0;

	}

	@Override
	public void onTwitterFunction(UserData user,Msg msg,String function,String[] params,TAuth account) {

		if (function.endsWith("_cancel")) {

			ImportThread toCancel = threads.remove(account.id);

			if (toCancel == null) msg.send("没有正在执行的导入...").exec();
			else toCancel.stopped.set(true);

			return;

		}

		if (threads.containsKey(account.id)) {

			msg.send("有导入正在执行... 请等待或使用 /import_cancel 取消执行").exec();

			return;

		}

		ImportList data = new ImportList();

		data.auth = account;

		data.context.add(msg);

		setPrivatePoint(user,POINT_LIST_IMPORT,data);

		msg.send("现在发送要导入的列表文件 (.csv) :").exec(data);

	}

	final String FOLLOWING = "关注中列表";
	//final String FOLLOWER = "关注者列表";
	final String BLOCK = "屏蔽列表";
	final String MUTE = "静音列表";
	final String MUTE_RT = "转推静音列表";
	final String USER = "我创建的列表";

	final String APPEND = "追加模式";
	final String REWRITE = "覆盖模式";
	final String REMOVE = "删除模式";

	@Override
	public void onPoint(UserData user,Msg msg,String point,PointData data) {

		ImportList list = (ImportList) data;

		data.context.add(msg);

		if (list.type == 0) {

			if (msg.doc() == null || !msg.doc().fileName().endsWith(".csv")) {

				msg.send("正在导入列表 请发送文件 (.csv)").exec(data);

				return;

			}

			msg.sendTyping();

			File csv = msg.file();

			if (csv == null) {

				msg.send("读取文件失败 由于 Telegram 官方限制，BOT无法下载超过20m的文件。").exec(data);

				return;

			}

			msg.sendTyping();

			for (String line : FileUtil.readUtf8Lines(csv)) {

				try {

					list.list.add(Long.parseLong(line));

				} catch (Exception ex) {

					msg.send("第 " + (list.list.size() + 1) + " 行解析失败 请重新发送文件 : ",line).exec(data);

					list.list.clear();

					return;

				}

			}

			if (list.list.isEmpty()) {

				msg.send("好像是空列表... 请重新发送").withCancel().exec(data);

				return;

			}

			list.type = 1;

			msg
				.send("导入成功！ 已导入 " + list.list.size() + " 条记录 请选择要导入到的列表")
				.keyboard(new Keyboard() {{

						newButtonLine().newButton(FOLLOWING)/*.newButton(FOLLOWER)*/;
						//newButtonLine().newButton(MUTE).newButton(BLOCK);

						//newButtonLine().newButton(MUTE_RT).newButton(USER);

					}})
				.withCancel()
				.exec(data);

		} else if (list.type == 1) {

			Keyboard button = new Keyboard();

			if (FOLLOWING.equals(msg.text())) {

				list.target = 1;

				button.newButtonLine(REWRITE);
				button.newButtonLine(REMOVE);

			} else if (MUTE.equals(msg.text())) {

				button.newButtonLine(APPEND);
				button.newButtonLine(REWRITE);
				button.newButtonLine(REMOVE);

				list.target = 3;

			} else if (BLOCK.equals(msg.text())) {

				button.newButtonLine(APPEND);
				button.newButtonLine(REWRITE);
				button.newButtonLine(REMOVE);

				list.target = 4;

			} else if (MUTE_RT.equals(msg.text())) {

				button.newButtonLine(APPEND);
				button.newButtonLine(REWRITE);
				button.newButtonLine(REMOVE);

				list.target = 5;

			} else if (USER.equals(msg.text())) {

				try {

					final LinkedList<UserList> lists = TApi.getLists(list.auth.createApi());

					Iterator<UserList> iter = lists.iterator();

					while (iter.hasNext()) {

						if (!list.auth.id.equals(iter.next().getUser().getId())) {

							iter.remove();

						}

					}

					if (lists.isEmpty()) {

						msg.send("你还没有创建列表，请重新选择 )").exec(data);

						return;

					}

					list.type = 2;

					msg
						.send("请在你创建的列表中选择 :")
						.keyboard(new Keyboard() {{

								for (UserList userList : lists) {

									newButtonLine(userList.getFullName() + " [ " + userList.getId() + " ]");

								}

							}})
						.withCancel()
						.exec(data);

				} catch (TwitterException e) {

					msg.send("读取创建的列表失败",NTT.parseTwitterException(e)).exec(data);

				}

				return;

			} else {

				msg.send("请在下方按钮中选择要导入到的位置...").withCancel().exec(data);

				return;

			}

			list.type = 3;

			msg
				.send("请在下方按钮中选择导入模式....\n","追加 : 普通导入模式","覆盖 : 去重导入 删除不包含的项目","删除 : 删除列表包含的项目","\n注意 : 当导入关注者列表时 追加模式不可用，覆盖模式仅删除列表中不存在的项目。\n当导入转推静音列表时 覆盖模式不可用 (因为转推静音列表无法读取)\n\n关于导入关注者 : 可能会被限制 (")
				.keyboard(button)
				.withCancel()
				.exec(data);

		} else if (list.type == 2) {

			if (msg.hasText() && msg.text().contains("[ ")) {

				list.target = NumberUtil.parseLong(StrUtil.subBefore(StrUtil.subAfter(msg.text(),"[ ",true)," ]",false));

				list.type = 3;

			} else {

				msg.send("请在下方键盘中选择你的列表 :").withCancel().exec(data);

			}

		} else if (list.type == 3) {

			if (APPEND.equals(msg.text())) {

				list.mode = 0;

			} else if (REWRITE.equals(msg.text())) {

				list.mode = 1;

			} else if (REMOVE.equals(msg.text())) {

				list.mode = 2;

			} else {

				msg.send("请选择导入模式").withCancel().exec(data);

				return;

			}

			list.type = 4;

			msg
				.send("任务已创建 某些操作无法撤销 是否继续？")
				.keyboard(new Keyboard() {{

						newButtonLine().newButton("确认").newButton("取消");

					}}).exec(data);

		} else if (list.type == 4) {

			if (!"确认".equals(msg.text())) {

				msg.send().withCancel().exec(data);

				return;

			}

			clearPrivatePoint(user);

			threads.put(list.auth.id,new ImportThread(list) {{ start(); }});

		}

	}

	HashMap<Long,ImportThread> threads = new HashMap<>();

	class ImportThread extends Thread {

		private ImportList action;

		public ImportThread(ImportList action) {

			this.action = action;

		}

		public AtomicBoolean stopped = new AtomicBoolean(false);

		Send send(String... text) {

			return new Send(ListImport.this,action.auth.user,text);

		}

		@Override
		public void run() {

			Twitter api = action.auth.createApi();

			Msg status = send("正在导入...","使用 /import_cancel 取消 ~").send();

			if (action.target == 1) {

				if (action.mode == 1) {

					try {

						LinkedList<Long> toUnFollow = TApi.getAllFrIDs(api,action.auth.id);

						ArrayList<Long> retain = new ArrayList<Long>(toUnFollow);
						retain.retainAll(action.list);

						toUnFollow.removeAll(retain);
						//action.list.remove(retain);

						int count = 0;

						LinkedList<String> unfo = new LinkedList<>();
						LinkedHashMap<Long,String> unfoError = new LinkedHashMap<>();

						for (Long id : toUnFollow) {

							if (stopped.get()) break;

							count ++;

							try {

								unfo.add(UserArchive.save(api.destroyFriendship(id)).urlHtml());

							} catch (TwitterException e) {

								unfoError.put(id,NTT.parseTwitterException(e));

							}

							if (count % 10 == 0) {

								status.edit("正在导入中 : ","取关成功 : " + unfo.size() + " 取关出错 : " + unfoError.size(),"使用 /import_cancel 取消操作").exec();

							}

						}

						status.delete();

						status.send("导入结束 : ","已执行 " + count + " / " + toUnFollow.size() + " 条 ","\n取关成功 : " + (unfo.size() == 0 ? "无" : (unfo.size() + "\n\n" + ArrayUtil.join(unfo.toArray(),"\n"))),"\n取关出错 : " + (unfoError.size() == 0 ? "无" : (unfoError.size() + "\n" + parseError(api,unfoError)))).html().exec();

						bot().execute(new SendDocument(action.auth.user,StrUtil.utf8Bytes(ArrayUtil.join(toUnFollow.toArray(),"\n"))).fileName("UnFollowedList.csv"));

					} catch (TwitterException e) {

						status.edit("读取关注中列表失败",NTT.parseTwitterException(e)).exec();

					}

				} else if (action.mode == 2) {

					int index = 0;
					int size = action.list.size();

					LinkedList<String> success = new LinkedList<>();
					LinkedHashMap<Long,String> error = new LinkedHashMap<>();

					for (;index < size;index ++) {

						if (stopped.get()) break;

						long id = action.list.get(index);

						try {

							success.add(UserArchive.save(api.destroyFriendship(id)).urlHtml());

						} catch (TwitterException e) {

							error.put(id,NTT.parseTwitterException(e));

						}

						if ((index % 10 == 1) && index != (size - 1)) {

							status.edit("正在导入中 : ","取关成功 : " + success.size() + " 取关出错 : " + error.size(),"使用 /import_cancel 取消操作").exec();

						}

					}

					status.delete();

					status.send("导入结束 : ","已执行 " + index + " / " + size + " 条 ","取关成功 : " + (success.size() == 0 ? "无" : ("\n\n" + ArrayUtil.join(success.toArray(),"\n"))) ,"\n取关出错 : " + (error.size() == 0 ? "无" : ("\n" + parseError(api,error)))).html().exec();

					bot().execute(new SendDocument(action.auth.user,StrUtil.utf8Bytes(ArrayUtil.join(action.list.toArray(),"\n"))).fileName("UnFollowedList.csv"));

				}

			} else if (action.target == 3) {

				if (action.mode == 0) {

					int index = 0;
					int size = action.list.size();

					LinkedList<String> success = new LinkedList<>();
					LinkedHashMap<Long,String> error = new LinkedHashMap<>();

					for (;index < size;index ++) {

						if (stopped.get()) break;

						long id = action.list.get(index);

						try {

							success.add(UserArchive.save(api.createMute(id)).urlHtml());

						} catch (TwitterException e) {

							error.put(id,NTT.parseTwitterException(e));

						}

						if ((index % 10 == 1) && index != (size - 1)) {

							status.edit("正在导入中 : ","静音成功 : " + success.size() + " 静音出错 : " + error.size(),"使用 /import_cancel 取消操作").exec();

						}

					}

					status.delete();

					status.send("导入结束 : ","已执行 " + index + " / " + size + " 条 ","静音成功 : " + (success.size() == 0 ? "无" : ("\n\n" + ArrayUtil.join(success.toArray(),"\n"))) ,"\n静音出错 : " + (error.size() == 0 ? "无" : ("\n" + parseError(api,error)))).html().exec();

					bot().execute(new SendDocument(action.auth.user,StrUtil.utf8Bytes(ArrayUtil.join(action.list.toArray(),"\n"))).fileName("MutedList.csv"));

				} else if (action.mode == 1) {

					try {

						LinkedList<Long> toUnMute = TApi.getAllMuteIDs(api);

						ArrayList<Long> retain = new ArrayList<Long>(toUnMute);
						retain.retainAll(action.list);

						toUnMute.removeAll(retain);
						action.list.remove(retain);

						int count = 0;

						LinkedList<String> mute = new LinkedList<>();
						LinkedList<String> unmute = new LinkedList<>();

						LinkedHashMap<Long,String> muteError = new LinkedHashMap<>();
						LinkedHashMap<Long,String> unmuteError = new LinkedHashMap<>();

						for (Long id : toUnMute) {

							if (stopped.get()) break;

							count ++;

							try {

								unmute.add(UserArchive.save(api.destroyMute(id)).urlHtml());

							} catch (TwitterException e) {

								unmuteError.put(id,NTT.parseTwitterException(e));

							}

							if (count % 10 == 0) {

								status.edit("正在导入中 : ","取消静音成功 : " + unmute.size() + " 取关静音出错 : " + unmuteError.size(),"静音成功 : 0 静音出错 : 0","\n使用 /import_cancel 取消操作").exec();

							}

						}

						for (Long id : action.list) {

							if (stopped.get()) break;

							count ++;

							try {

								mute.add(UserArchive.save(api.createMute(id)).urlHtml());

							} catch (TwitterException e) {

								muteError.put(id,NTT.parseTwitterException(e));

							}

							if (count % 10 == 0) {

								status.edit("正在导入中 : ","取消静音成功 : " + unmute.size() + " 取关静音出错 : " + unmuteError.size(),"静音成功 : " + mute.size() + " 静音出错 : " + muteError.size(),"\n使用 /import_cancel 取消操作").exec();

							}

						}

						status.delete();

						status.send("导入结束 : ","已执行 " + count + " / " + (toUnMute.size() + action.list.size()) + " 条 ","\n静音成功 : " + (mute.size() == 0 ? "无" : (mute.size() + "\n\n" + ArrayUtil.join(mute.toArray(),"\n"))),"\n静音出错 : " + (muteError.size() == 0 ? "无" : (muteError.size() + "\n" + parseError(api,muteError))),"\n取消静音成功 : " + (unmute.size() == 0 ? "无" : (unmute.size() + "\n\n" + ArrayUtil.join(unmute.toArray(),"\n"))),"\n取消静音出错 : " + (unmuteError.size() == 0 ? "无" : (unmuteError.size() + "\n" + parseError(api,unmuteError)))).html().exec();

						bot().execute(new SendDocument(action.auth.user,StrUtil.utf8Bytes(ArrayUtil.join(action.list.toArray(),"\n"))).fileName("MutedList.csv"));
						bot().execute(new SendDocument(action.auth.user,StrUtil.utf8Bytes(ArrayUtil.join(toUnMute.toArray(),"\n"))).fileName("UnMutedList.csv"));



					} catch (TwitterException e) {

						status.edit("读取静音列表失败",NTT.parseTwitterException(e)).exec();

					}



				}

			}

			threads.remove(action.auth.id);

		}

		String parseError(Twitter api,LinkedHashMap<Long, String> error) {

			StringBuilder str = new StringBuilder();

			for (Map.Entry<Long,String> entry  : error.entrySet()) {

				String user;

				UserArchive archive = UserArchive.show(api,entry.getKey());

				if (archive != null) {

					user = archive.urlHtml();

				} else {

					user = entry.getKey().toString();

				}

				str.append("\n").append(user).append(" : ").append(entry.getValue());

			}

			return str.toString();

		}

	}

}