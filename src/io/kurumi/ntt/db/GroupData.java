package io.kurumi.ntt.db;

import cn.hutool.core.io.*;
import cn.hutool.json.*;
import io.kurumi.ntt.*;

import java.io.*;
import java.util.*;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.SendAnimation;

public class GroupData {

    public static CachedData<GroupData> data = new CachedData<GroupData>(GroupData.class);

    public static GroupData get(Chat chat) {

        synchronized (data.idIndex) {

            if (data.idIndex.size() > 1000) {

                data.idIndex.clear();

            } else if (data.idIndex.containsKey(chat.id())) return data.idIndex.get(chat.id());

            GroupData group = data.getNoCache(chat.id());

            if (group == null) {

                group = new GroupData();

                group.id = chat.id();

            }
						
						group.title = chat.title();

            data.idIndex.put(chat.id(),group);

            return group;

        }

    }

    public long id;
		
		public String title;

    public List<Long> admins;
	
		public Boolean delete_service_msg;
    public Boolean delete_channel_msg;
		
		public Integer no_invite_user;
		public Integer no_invite_bot;
		
		public Integer no_sticker;
		public Integer no_image;
		public Integer no_animation;
		public Integer no_audio;
		public Integer no_video;
		public Integer no_video_note;
		public Integer no_contact;
		public Integer no_location;
		public Integer no_game;
		public Integer no_voice;
		public Integer no_file;
		
		public Integer max_count;
		public Integer rest_action;
		
		public Integer last_warn_msg;
		
		public Map<String,Integer> restWarn;
		
		public String actionName() {
				
				return rest_action == null ? "限制" :
				rest_action == 0 ? "禁言" :
				/* rest_action == 1 ? */"封锁";
				
		}
		
    //public Boolean anti_esu;

    public List<String> ban_sticker_set;

}