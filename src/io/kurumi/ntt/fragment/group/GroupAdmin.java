package io.kurumi.ntt.fragment.group;

import cn.hutool.core.util.ArrayUtil;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.request.GetChatAdministrators;
import com.pengrad.telegrambot.response.GetChatAdministratorsResponse;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.Launcher;
import io.kurumi.ntt.db.GroupData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.utils.NTT;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupAdmin extends Fragment {

    public static HashMap<Long, Long> lastUpdate = new HashMap<>();

    public static boolean fastAdminCheck(Fragment fragment, long chatId, long userId, boolean full) {

        return fastAdminCheck(fragment, GroupData.get(chatId), userId, full);

    }

    public static boolean fastAdminCheck(Fragment fragment, GroupData data, long userId, boolean full) {

        if (ArrayUtil.contains(Env.ADMINS, (int) userId)) return true;

        updateGroupAdmins(fragment, data);

        if (full) {

            return (data.full_admins != null && data.full_admins.contains(userId)) || (data.owner != null && data.owner.equals(userId));

        } else {

            return data.admins != null && data.admins.contains(userId);

        }

    }

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerFunction("update_admins_cache");

    }

    @Override
    public int checkFunctionContext(UserData user, Msg msg, String function, String[] params) {

        return FUNCTION_GROUP;

    }

    public static boolean updateGroupAdmins(Fragment fragment, GroupData data) {

        if (!lastUpdate.containsKey(data.id) || lastUpdate.get(data.id) - System.currentTimeMillis() > 30 * 60 * 1000) {

            GetChatAdministratorsResponse resp = Launcher.INSTANCE.execute(new GetChatAdministrators(data.id));

            if (resp == null || !resp.isOk()) return false;

            data.admins = new ArrayList<>();
            data.full_admins = new ArrayList<>();

            for (ChatMember admin : resp.administrators()) {

                if (admin.status() == ChatMember.Status.creator) {

                    data.owner = admin.user().id();

                    data.admins.add(admin.user().id());
                    data.full_admins.add(admin.user().id());

                    continue;

                }

                if (admin.user().firstName().equals("")) {

                    // 死号

                    continue;

                }

                if (admin.user().isBot()) continue; // 自己

                data.admins.add(admin.user().id());

                if (admin.canChangeInfo() != null && !admin.canChangeInfo()) continue;
                if (admin.canDeleteMessages() != null && !admin.canDeleteMessages()) continue;
                if (admin.canRestrictMembers() != null && !admin.canRestrictMembers()) continue;
                if (admin.canInviteUsers() != null && !admin.canInviteUsers()) continue;
                if (admin.canPinMessages() != null && !admin.canPinMessages()) continue;
                if (admin.canPromoteMembers() != null && !admin.canPromoteMembers()) continue;

                data.full_admins.add(admin.user().id());


            }

            if (data.admins.isEmpty()) data.admins = null;
            if (data.full_admins.isEmpty()) data.full_admins = null;

            lastUpdate.put(data.id, System.currentTimeMillis());

        }

        return true;

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (NTT.checkGroupAdmin(msg)) return;

        updateGroupAdmins(this, GroupData.get(this, msg.chat()));

        msg.send("管理员缓存更新完成！").failedWith();

    }


}
