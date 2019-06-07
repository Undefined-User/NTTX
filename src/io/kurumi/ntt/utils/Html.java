package io.kurumi.ntt.utils;

import cn.hutool.http.*;
import io.kurumi.ntt.Launcher;
import cn.hutool.core.util.ArrayUtil;

public class Html {
	
	public static String a(String text,String href) {
		
		return "<a href=\"" + href + "\">" + HtmlUtil.escape(text) + "</a>";
		
	}
	
	public static String startPayload(String text,Object... payload) {

		return a(text,"https://t.me/" + Launcher.INSTANCE.me.username() + "?start=" + ArrayUtil.join(payload,"_"));
		
		}
	
	
}
