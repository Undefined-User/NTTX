package io.kurumi.ntt.utils;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.util.TreeSet;

public class TwitterWeb {

	/*
	
	public static TreeSet<Long> fetchStatus(String screenName,int maxDepth) {

		TreeSet<Long> statuses = new TreeSet<>();

		HttpResponse result = HttpUtil
			.createGet("https://mobile.twitter.com/" + screenName)
			.header(Header.USER_AGENT,"MSIE 6.0")
			.execute();

		if (!result.isOk()) return statuses;

		String statusHtml = result.body();
		
		if (statusHtml.contains("<div class=\"timeline\">")) return statuses;

		
		
	}
	
	*/

    public static TreeSet<Long> fetchStatusReplies(String screenName, Long statusId, boolean loop) {

        TreeSet<Long> replies = new TreeSet<>();

        HttpResponse result = HttpUtil
                .createGet("https://mobile.twitter.com/" + screenName + "/status/" + statusId)
                .header(Header.USER_AGENT, "MSIE 6.0")
                .execute();

        if (result.getStatus() == 301) {

            result = HttpUtil
                    .createGet("https://mobile.twitter.com" + result.header(Header.LOCATION))
                    .header(Header.USER_AGENT, "MSIE 6.0")
                    .execute();

        }

        if (!result.isOk()) return replies;

        String statusHtml = result.body();

        if (!statusHtml.contains("<div class=\"timeline replies\">")) return replies;

        statusHtml = StrUtil.subAfter(statusHtml, "<div class=\"timeline replies\">", false);

        while (statusHtml.contains("timestamp")) {

            statusHtml = StrUtil.subAfter(statusHtml, "timestamp", false);

            String replyUrl = StrUtil.subBefore(statusHtml, "</a>", false);

            String replyFrom = StrUtil.subBetween(replyUrl, "href=\"/", "/status");

            Long replyId = NumberUtil.parseLong(StrUtil.subBetween(replyUrl, "status/", "?"));

            replies.add(replyId);

            if (loop) {

                replies.addAll(fetchStatusReplies(replyFrom, replyId, loop));

            }

        }

        return replies;

    }

}
