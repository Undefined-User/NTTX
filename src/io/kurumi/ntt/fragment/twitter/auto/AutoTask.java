package io.kurumi.ntt.fragment.twitter.auto;

import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.twitter.*;
import java.util.*;
import twitter4j.*;
import cn.hutool.core.util.*;
import io.kurumi.ntt.utils.*;
import io.kurumi.ntt.twitter.archive.*;

public class AutoTask extends TimerTask {

	public static AutoTask INSTANCE = new AutoTask();

	public static Timer timer;

	public static void start() {

		stop();

		timer = new Timer();

		timer.scheduleAtFixedRate(INSTANCE,new Date(),5 * 60 * 1000);

	}

	public static void stop() {

		if (timer != null) {

			timer.cancel();

			timer = null;

		}

	}
	
	public static void onNewFollower(TAuth auth,Twitter api,UserArchive archive,Relationship ship) {
		
		if (!ship.isSourceFollowingTarget() && AutoUI.autoData.fieldEquals(auth.id,"foback",true)) {
			
			try {
				
				api.createFriendship(archive.id);
				
				new Send(auth.user,archive.isProtected ? "已发送关注请求 :" : "已回Fo : " + archive.url(),"账号 : " + auth.archive().urlHtml()).html().exec();
				
			} catch (TwitterException e) {
				
				new Send(auth.user,"回Fo失败 : " + archive.url(),"账号 : " + auth.archive().urlHtml()).html().exec();
				
				
			}

		}
		
	}

	@Override
	public void run() {

		for (AutoUI.AutoSetting auto :  AutoUI.autoData.collection.find()) {

			TAuth auth = TAuth.getById(auto.id);

			if (auth == null) {

				AutoUI.autoData.deleteById(auto.id);

				BotLog.debug("autotask removed for " + auto.id);

				return;

			}

			if (auto.like) {

				try {

					startLikeService(auth);

				} catch (TwitterException e) {

					auto.like = false;

					AutoUI.autoData.setById(auto.id,auto);

					new Send(auth.user,"auto.like disabled : " + e).exec();

				}

			}

		}

	}

	void startLikeService(TAuth auth) throws TwitterException {

		Twitter api = auth.createApi();

		ResponseList<Status> tl = api.getHomeTimeline(new Paging().count(800));

		int count = 0;

		int max = RandomUtil.randomInt(5,40);

		for (Status status : tl) {

			if (count >= max) break;

			//	count += loopLike(auth,api,status);

			if (status.isFavorited()) continue;
			if (auth.id.equals(status.getUser().getId())) continue;
			if (status.isRetweet()) continue;

			try {

				api.createFavorite(status.getId());

				count ++;

			} catch (TwitterException ex) {

				if (ex.getErrorCode() != 34 && ex.getErrorCode() != 134) {

					throw ex;

				}

			}

		}

		if (count > 0) {

			//	new Send(auth.user,"sended " + count + " likes to home_timeline","account : " + Html.a("@" + auth.archive().screenName,"https://twitter.com/" + auth.archive().screenName)).html().exec();

		}

	}

	int loopLike(TAuth auth,Twitter api,Status status) throws TwitterException {

		int like = 0;

		if (status.isFavorited()) return 0;

		if (!auth.id.equals(status.getUser().getId())) {

			try {

				api.createFavorite(status.getId());

				like ++;

			} catch (TwitterException e) {

				throw e;

			}

		}

		if (status.getInReplyToStatusId() != -1) {

			try {

				like += loopLike(auth,api,api.showStatus(status.getInReplyToStatusId()));

			} catch (TwitterException e) {

				try {

					api.createFavorite(status.getInReplyToStatusId());

				} catch (TwitterException ex) {}

			}

		}

		if (status.getQuotedStatusId() != -1) {

			try {

				if (status.getQuotedStatus() != null) {

					like += loopLike(auth,api,status.getQuotedStatus());

				} else {

					api.createFavorite(status.getQuotedStatusId());

					like ++;

				}

			} catch (TwitterException e) {}


		}

		return like;


	}

}
