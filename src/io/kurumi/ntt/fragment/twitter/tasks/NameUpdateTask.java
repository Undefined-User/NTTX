package io.kurumi.ntt.fragment.twitter.tasks;

import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.BotFragment;
import java.util.TimerTask;
import io.kurumi.ntt.fragment.twitter.TAuth;
import twitter4j.TwitterException;
import java.util.Random;

public class NameUpdateTask extends TimerTask {

	public static void start() {
		
		BotFragment.mainTimer.schedule(new NameUpdateTask(),15 * 60 * 1000L);
		
	}
	
	@Override
	public void run() {
		
		for (TAuth account : TAuth.data.getAllByField("anu",true)) {
			
			try {
				
				account.createApi().updateProfile(randomString(14,true,true,true),null,null,null);
				
			} catch (TwitterException e) {}

		}
		
	}
	
	private static Random random = new Random();
	
	public static String randomString(int length,boolean lowEnglish,boolean upperEnglish,boolean number) {

        String baseString = "";

        if (lowEnglish) baseString += "abcdefghijklmnopqrstuvwxyz";

		if (upperEnglish) baseString += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		if (number) baseString += "0123456789";

        StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {

            sb.append(baseString.charAt(random.nextInt(baseString.length())));

        }

        return sb.toString();

    }

}
