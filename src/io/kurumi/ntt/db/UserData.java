package io.kurumi.ntt.db;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.pengrad.telegrambot.model.User;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.fragment.admin.Firewall;
import io.kurumi.ntt.utils.Html;
import io.kurumi.ntt.utils.NTT;

import java.util.*;

public class UserData {

    public static Data<UserData> data = new Data<UserData>(UserData.class);

    public static HashMap<Long, UserData> userDataIndex = new HashMap<>();
    public Long id;
    public Boolean isBot;
    public String firstName;
    public String lastName;
    public String userName;
    public Boolean contactable;

    public transient User userObj;

    public static UserData get(Long userId) {

        if (userDataIndex.size() > 1000) {

            userDataIndex.clear();

        } else if (userDataIndex.containsKey(userId)) return userDataIndex.get(userId);

        synchronized (userDataIndex) {

            if (userDataIndex.containsKey(userId)) {

                return userDataIndex.get(userId);

            }

            UserData userData = data.getById(userId);

            if (userData != null) {

                userDataIndex.put(userId, userData);
            }

            return userData;

        }

    }

    public static UserData get(User user) {

        if (user == null) return null;

        if (userDataIndex.size() > 1000) {

            userDataIndex.clear();

        } else if (userDataIndex.containsKey(user.id())) {

            checkUpdate(userDataIndex.get(user.id()), user);


        }

        synchronized (userDataIndex) {

            if (userDataIndex.containsKey(user.id())) {

                return checkUpdate(userDataIndex.get(user.id()), user);
            }


            if (data.containsId(user.id())) {

                UserData userData = data.getById(user.id());

                userDataIndex.put(user.id(), userData);

                return checkUpdate(userData, user);


            } else {

                UserData userData = new UserData();

                userData.userObj = user;

                userData.id = user.id();

                userData.read(user);

                data.setById(user.id(), userData);

                userDataIndex.put(user.id(), userData);

                return userData;

            }

        }

    }

    static UserData checkUpdate(UserData userData, User user) {

        userData.userObj = user;

        if (!ObjectUtil.equal(user.firstName(), userData.firstName) ||
                ObjectUtil.equal(user.lastName(), userData.lastName)) {

            userData.read(user);

            data.setById(userData.id, userData);

        }

        return userData;

    }

    public void read(User user) {

        userName = user.username();

        firstName = user.firstName();

        lastName = user.lastName();

        isBot = user.isBot();

    }

    public boolean contactable(Fragment fragment) {

        if (fragment.origin == Launcher.INSTANCE && contactable) return true;

        if (NTT.isUserContactable(fragment, id) != contactable && fragment.origin == Launcher.INSTANCE) {

            contactable = !contactable;

            data.setById(id, this);

        }

        return contactable;

    }

    public boolean blocked() {

        return Firewall.block.containsId(id);

    }

    public String name() {

        String name = firstName;

        if (lastName != null) {

            name = name + " " + lastName;

        }

        return name;

    }


    public String userName() {

        return Html.a(StrUtil.isBlank(name()) ? "[已重置]" : name(), "tg://user?id=" + id);

    }

    public String format() {

        return Html.a("ID", "tg://user?id=" + id) + " : " + Html.code(id) + "\n" + "用户名 : " + Html.code(name());

    }

    public boolean admin() {

        return ArrayUtil.contains(Env.ADMINS, id.intValue());

    }

}
