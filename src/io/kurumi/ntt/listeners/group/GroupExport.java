package io.kurumi.ntt.listeners.group;

import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.td.TdApi.*;
import io.kurumi.ntt.td.client.TdException;
import io.kurumi.ntt.td.client.TdFunction;
import io.kurumi.ntt.td.model.TMsg;

import java.util.LinkedList;

public class GroupExport extends TdFunction {

    @Override
    public String functionName() {

        return "_export";
    }

    @Override
    public void onFunction(User user, TMsg msg, String function, String[] params) {

        if (!(msg.isBasicGroup() || msg.isSuperGroup())) {

            sendText(msg, getLocale(user).FN_GROUP_ONLY);

            return;

        }

        if (!isAdmin(msg.chatId, user.id)) {

            sendText(msg, getLocale(user).NOT_CHAT_ADMIN);

            return;

        }

        LinkedList<User> targetAccounts = new LinkedList<>();

        int size;

        if (msg.isBasicGroup()) {

            BasicGroupFullInfo info = execute(new GetBasicGroupFullInfo(msg.groupId));

            size = info.members.length;

            for (ChatMember memberId : info.members) {

                if (memberId.status instanceof ChatMemberStatusCreator || memberId.status instanceof ChatMemberStatusAdministrator || memberId.userId == client.me.id)
                    continue;

                User member = execute(new GetUser(memberId.userId));

                if (!(member.type instanceof UserTypeDeleted || member.type instanceof UserTypeBot)) {

                    targetAccounts.add(member);

                }

            }

        } else {

            SupergroupFullInfo group = execute(new GetSupergroupFullInfo(msg.groupId));

            size = group.memberCount;

            for (int index = 0; index < size; index += 200) {

                ChatMembers members = null;

                try {

                    members = execute(new GetSupergroupMembers(msg.groupId, new SupergroupMembersFilterRecent(), index, 200));

                } catch (TdException e) {

                    try {

                        members = execute(new GetSupergroupMembers(msg.groupId, new SupergroupMembersFilterRecent(), index, 200));

                        // TDLib 第一次取 会出错

                    } catch (TdException ex) {
                    }

                }

                for (ChatMember memberId : members.members) {

                    if (memberId.userId == client.me.id) continue;

                    User member = execute(new GetUser(memberId.userId));

                    if (!(member.type instanceof UserTypeDeleted || member.type instanceof UserTypeBot)) {

                        targetAccounts.add(member);

                    }

                }

            }

        }

        TextBuilder message = new TextBuilder();

        int count = 0;

        for (int index = 0; index < targetAccounts.size(); index++) {

            if (count == 50) {

                send(chatId(user.id).inputText(message));

                message = new TextBuilder();

                count = 0;

            }

            User target = targetAccounts.get(index);

            message.bold(target.id + "");

            String name = target.firstName;

            if (!StrUtil.isBlank(target.firstName)) name += " " + target.lastName;

            message.text(" : ").mention(name, target.id).text("\n");

            count++;

        }

        if (count != 0) {

            send(chatId(user.id).inputText(message));

        }

    }

    boolean isAdmin(long chatId, int userId) {

        ChatMember member = execute(new GetChatMember(chatId, userId));

        return member.status instanceof ChatMemberStatusCreator || member.status instanceof ChatMemberStatusAdministrator;

    }


}
