package io.kurumi.ntt.twitter.archive;

import cn.hutool.json.JSONObject;
import io.kurumi.ntt.model.data.IdDataModel;
import java.util.LinkedList;
import twitter4j.User;
import java.util.LinkedHashSet;
import cn.hutool.json.JSONArray;
import java.sql.Struct;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import io.kurumi.ntt.utils.*;
import cn.hutool.core.util.ObjectUtil;
import io.kurumi.ntt.twitter.track.UserTackTask;
import java.util.HashMap;

public class UserArchive extends IdDataModel {

    public static HashMap<String,UserArchive> screenNameIndex = new HashMap<>();

    public static UserArchive findByScreenName(String screenName) {
        
        if (screenNameIndex.containsKey(screenName)) return screenNameIndex.get(screenName);
        
        for (Long id : INSTANCE.idList) {
            
            if (INSTANCE.idIndex.containsKey(id)) continue;
            
            if (screenName.equals(INSTANCE.get(id).screenName)) {
                
                return screenNameIndex.get(screenName);
                
            }
            
        }
        
        return null;
        
    }
    
    public static Factory<UserArchive> INSTANCE = new Factory<UserArchive>(UserArchive.class,"twitter_archives/users") {

        public HashMap<String,UserArchive> screenNameIndex = new HashMap<String,UserArchive>() {{

                UserArchive.screenNameIndex = this;

            }};

        @Override
        public UserArchive get(Long id) {

            UserArchive user = super.get(id);

            if (user != null) screenNameIndex.put(user.screenName,user);

            return user;

        }

        @Override
        public void saveObj(UserArchive obj) {

            super.saveObj(obj);


            screenNameIndex.put(obj.screenName,obj);



        }

        @Override
        public void delObj(UserArchive obj) {

            super.delObj(obj);

            screenNameIndex.remove(obj.screenName);

        }

    };

    public UserArchive(String dirName,long id) { super(dirName,id); }

    public Long createdAt;

    public String name;
    public String screenName;
    public String bio;
    public String photoUrl;

    public Boolean isProtected;

    public Boolean isDisappeared;

    @Override
    protected void init() {
    }

    public void read(User user) {

        boolean change = false;
        StringBuilder str = new StringBuilder();
        String split = "\n--------------------------------\n";
        
        String nameL = name;

        if (!(name = user.getName()).equals(nameL)) {

            str.append(split).append("名称更改 : ").append(name).append(" ------> ").append(name);

            change = true;

        }

        String screenNameL = screenName;

        if (!(screenName = user.getScreenName()).equals(screenNameL)) {

        str.append(split).append("用户名更改 @").append(screenNameL).append(" ------> @").append(screenName);

            change = true;

        }

        String bioL = bio;

        if (!ObjectUtil.equal(bio = user.getDescription(),bioL)) {

            str.append(split).append("简介更改 : \n\n").append(bioL).append(" \n\n ------> \n\n").append(bio);

            change = true;

        }

        String photoL = photoUrl;

        if (!ObjectUtil.equal(photoUrl = user.getBiggerProfileImageURL(),photoL)) {

            str.append(split).append("头像更改 : " + Html.a("媒体文件",photoL) + " ------> " + Html.a("媒体文件",photoUrl));

            change = true;

        }

        boolean protectL = isProtected;

        if (protectL != (isProtected = user.isProtected())) {

            str.append("保护状态更改 : ").append(isProtected ? "开启了锁推" : "关闭了锁推");

            change = true;

        }

        isDisappeared = false;

        if (createdAt == null) {

            createdAt = user.getCreatedAt().getTime();

            change = false;

        }

        if (change) {

            UserTackTask.onUserChange(this,str.toString());

        }

    }

    @Override
    protected void load(JSONObject obj) {

        createdAt = obj.getLong("created_at");
        name = obj.getStr("name");
        screenName = obj.getStr("screen_name");
        bio = obj.getStr("bio");
        photoUrl = obj.getStr("photo_url");

        isProtected = obj.getBool("is_protected");
        isDisappeared = obj.getBool("is_disappeared");

    }

    public String getHtmlURL() {

        return Html.a(name,getURL());

    }

    public String getURL() {

        return "https://twitter.com/" + screenName;

    }

    @Override
    protected void save(JSONObject obj) {

        obj.put("created_at",createdAt);
        obj.put("name",name);
        obj.put("screen_name",screenName);
        obj.put("bio",bio);
        obj.put("photo_url",photoUrl);

        obj.put("is_protected",isProtected);
        obj.put("is_disappeared",isDisappeared);

    }

}
