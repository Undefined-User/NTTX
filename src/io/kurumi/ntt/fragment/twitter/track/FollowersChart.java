package io.kurumi.ntt.fragment.twitter.track;

import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.abs.Msg;
import io.kurumi.ntt.fragment.twitter.TAuth;
import io.kurumi.ntt.fragment.twitter.user.Img;
import java.awt.Color;
import org.jfree.data.category.DefaultCategoryDataset;
import com.pengrad.telegrambot.request.SendPhoto;

public class FollowersChart extends Fragment {

	@Override
	public void init(BotFragment origin) {
		
		super.init(origin);
		
		registerFunction("chart");
		
	}

	@Override
	public void onFunction(UserData user,Msg msg,String function,String[] params) {
		
		requestTwitter(user,msg);
		
	}

	@Override
	public void onTwitterFunction(UserData user,Msg msg,String function,String[] params,TAuth account) {
		
		Img img = new Img(1000,800,Color.WHITE);
		
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		
		data.addValue(1,"Followers","9:00");
		
		data.addValue(2,"Followers","10:00");
		
		data.addValue(3,"Followers","11:00");
		
		data.addValue(4,"Followers","12:00");
		
		data.addValue(5,"Followers","13:00");
		
		data.addValue(5,"Nmsl","9:00");

		data.addValue(4,"Nmsl","10:00");

		data.addValue(3,"Nmsl","11:00");

		data.addValue(2,"Nmsl","12:00");

		data.addValue(1,"Nmsl","13:00");
		
		img.drawLineChart("Twitter 关 注 者 统 计","","",data);
		
		bot().execute(new SendPhoto(msg.chatId(),img.getBytes()));
		
	}
	
	
}