package io.kurumi.ntt.fragment.twitter.list;

import java.util.*;
import twitter4j.*;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.twitter.TApi;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.fragment.twitter.archive.UserArchive;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.NTT;

public class MutesClean extends Fragment {

	@Override
	public void init(BotFragment origin) {

		super.init(origin);

		registerFunction("clean_mutes");

	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {

		if (params.length == 0 || !params[0].matches("[arob]*")) {

			String message = "清理静音 : /" + function + " <参数...>\n\n";

			message += "a - 清理所有\nr - 正在关注\no - 关注者\nb - 屏蔽的用户";

			message += "\n\n注意 : 多个筛选参数叠加时都匹配才清理";

			msg.send(message).async();

			return;

		}

		requestTwitter(user,msg);

	}

	@Override
	public void onTwitterFunction(UserData user,Msg msg,String function,String[] params,TAuth account) {

		Msg status = msg.send("正在查找...").send();

		String param = params[0];

		boolean r = param.contains("r");

		boolean o = param.contains("o");

		boolean b = param.contains("b");

		Twitter api = account.createApi();

		LinkedList<User> mutes;

		try {

			LinkedList<Long> mutesIds = TApi.getAllMuteIDs(api);

			if (b) {

				mutesIds.retainAll(TApi.getAllBlockIDs(api));

			} else {

				if (r) {

					mutesIds.retainAll(TApi.getAllFrIDs(api,account.id));

				}

				if (o) {

					mutesIds.retainAll((TApi.getAllFoIDs(api,account.id)));

				}

			}
			
			mutes = lookupUsers(api,mutesIds);

		} catch (TwitterException e) {

			status.edit(NTT.parseTwitterException(e)).async();

			return;

		}

		if (mutes.isEmpty()) {

			status.edit("没有目标用户 )").async();

			return;

		}

		status.edit("正在清理...").async();

		LinkedList<User> successful = new LinkedList<>();
		LinkedList<User> failed = new LinkedList<>();

		for (User target : mutes) {

			try {

				api.destroyMute(target.getId());

				successful.add(target);

			} catch (TwitterException e) {

				failed.add(target);

			}

		}

		status.delete();

		StringBuilder message = new StringBuilder();

		if (!successful.isEmpty()) {

			message.append("已清理 : \n\n");

			for (User su : successful) {

				message.append(UserArchive.save(su).bName()).append("\n");

			}

			message.append("\n");

		}

		if (!failed.isEmpty()) {

			message.append("清理失败 : \n");

			for (User su : failed) {

				message.append("\n").append(UserArchive.save(su).bName());

			}

		}

		msg.send(message.toString()).html().async();

	}

	LinkedList<User> lookupUsers(Twitter api,LinkedList<Long> users) throws TwitterException {

		LinkedList<User> results = new LinkedList<>();

		while (!users.isEmpty()) {

			List<Long> target;

			if (users.size() > 100) {

				target = new LinkedList<Long>(CollectionUtil.sub(users,0,100));
				users.removeAll(target);

			} else {

				target = new LinkedList<>();
				target.addAll(users);

				users.clear();

			}

			try {

				ResponseList<User> result = api.lookupUsers(ArrayUtil.unWrap(target.toArray(new Long[target.size()])));

				result.addAll(result);

			} catch (TwitterException e) {

				if (e.getErrorCode() == 17) {

					for (Long da : target) {

						UserArchive.saveDisappeared(da);

					}

				} else throw e;

			}

		}

		return results;

	}

}
