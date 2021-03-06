package io.kurumi.ntt.fragment.admin;

import cn.hutool.core.util.NumberUtil;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetChatAdministrators;
import com.pengrad.telegrambot.response.GetChatAdministratorsResponse;
import io.kurumi.ntt.Env;
import io.kurumi.ntt.db.Data;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.BotFragment;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.utils.Html;

import java.util.HashMap;

public class Firewall extends Fragment {

    public static Data<Id> block = new Data<Id>("UserBlock", Id.class);

    @Override
    public void init(BotFragment origin) {

        super.init(origin);

        registerAdminFunction("accept", "drop");
        registerAdminPayload("accept", "drop");

    }

    @Override
    public void onFunction(UserData user, Msg msg, String function, String[] params) {

        if (params.length == 0) {

            msg.send("invlid params").exec();

            return;

        }

        long target = -1;

        if (NumberUtil.isNumber(params[0])) {

            target = NumberUtil.parseLong(params[0]);

        } else {

            UserData userD = UserData.data.getByField("userName", params[0]);

            if (userD != null) {

                target = userD.id;

            }

        }

        if (target == -1) {

            msg.send("无记录").exec();

            return;

        }

        boolean exists = block.containsId(target);

        if ("accept".equals(function)) {

            if (exists) {

                block.deleteById(target);

                msg.send("removed block").exec();

            } else {

                msg.send("not blocked").exec();

            }

        } else {

            if (exists) {

                msg.send("already blocked").exec();

            } else {

                block.setById(target, new Id(target));

                msg.send("blocked").exec();

            }

        }

    }

    @Override
    public void onPayload(UserData user, Msg msg, String payload, String[] params) {

        if ("accept".equals(payload) || "drop".equals(payload)) {

            if (params.length < 1) {

                msg.send("invlid params").exec();

                return;

            }

            UserData target = UserData.get(NumberUtil.parseLong(params[0]));

            if (target.admin()) {

                msg.send("不可以...！").exec();

                return;

            }

            boolean exists = block.containsId(target.id);

            if ("accept".equals(payload)) {

                if (exists) {

                    block.deleteById(target.id);

                    msg.send("removed block").exec();

                } else {

                    msg.send("not blocked").exec();

                }

            } else {

                if (exists) {

                    msg.send("already blocked").exec();

                } else {

                    block.setById(target.id, new Id(target.id));

                    msg.send("blocked").exec();

                }

            }

            return;


        }

    }

    @Override
    public boolean update() {

        return true;

    }

    public static HashMap<Long, Lock> instances = new HashMap<>();

    static class Lock {

        public static Lock getInstcnae(UserData user) {

            synchronized (user) {

                if (instances.containsKey(user.id)) {


                }

            }

            return null;

        }

    }

    @Override
    public boolean onUpdate(UserData user, Update update) {

        Message msg = update.message();

        if (msg == null || user == null) return false;

        if (msg.newChatMembers() != null) {

            if (!msg.newChatMembers()[0].id().equals(origin.me.id())) return false;

            if (block.containsId(user.id)) {

                // if (isMainInstance()) bot().execute(new LeaveChat(msg.chat().id()));

                new Send(Env.LOG_CHANNEL, "BOT " + UserData.get(origin.me).userName() + " 被 " + user.userName() + " 邀请到 " + msg.chat().title() + " [" + Html.code(msg.chat().id()) + "]").html().async();

                //return true;

            }

            GetChatAdministratorsResponse resp = bot().execute(new GetChatAdministrators(msg.chat().id()));

            if (resp == null || !resp.isOk()) return false;

            for (ChatMember member : resp.administrators()) {

                UserData current = UserData.get(member.user());

                if (!block.containsId(current.id)) continue;

                // if (isMainInstance()) bot().execute(new LeaveChat(msg.chat().id()));

                new Send(Env.LOG_CHANNEL, "BOT " + UserData.get(origin.me).userName() + " 被 " + user.userName() + " 邀请到 " + msg.chat().title() + " [" + Html.code(msg.chat().id()) + "] 因为管理员 " + current.userName()).html().async();

                // return true;

            }

            return false;

        } /* else if (block.containsId(user.id)) {

		 Msg message = new Msg(this,msg);

		 for (Fragment f : origin.fragments) {

		 switch (f.onBlockedMsg(user,message)) {

		 case 0 : continue;
		 case 1 : break;
		 case 2 : return false;

		 }

		 }

		 return true;

		 } */

        return false;

    }

    public static class Id {

        public long id;

        public Id() {
        }

        public Id(long id) {
            this.id = id;
        }

    }

}
