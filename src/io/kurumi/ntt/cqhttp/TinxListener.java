package io.kurumi.ntt.cqhttp;

import io.kurumi.ntt.cqhttp.update.*;

public class TinxListener {

    public TinxBot bot;
    public TinxApi api;

    public void onUpdate(Update update) {
    }

    public void onMsg(MessageUpdate msg) {
    }

    public void onPrivate(MessageUpdate msg) {
    }

    public void onGroup(MessageUpdate msg) {
    }

    // Notice

    public void onNotice(NoticeUpdate notice) {
    }

    public void onGroupUpload(GroupUploadNotice upload) {
    }

    public void onGroupAdminSet(GroupAdminNotice admin) {
    }

    public void onGroupAdminUnSet(GroupAdminNotice admin) {
    }

    public void onGroupIncrease(GroupIncreaseNotice member) {
    }

    public void onGroupInviteMember(GroupIncreaseNotice member) {
    }

    public void onGroupApproveMember(GroupIncreaseNotice member) {
    }

    public void onGroupDecrease(GroupDecreaseNotice member) {
    }

    public void onGroupLeftMember(GroupDecreaseNotice member) {
    }

    public void onGroupKickMember(GroupDecreaseNotice member) {
    }

    public void onGroupKickMe(GroupDecreaseNotice member) {
    }

    public void onFriendAdd(FriendAddNotice friend) {
    }

    // Request

    public void onRequest(RequestUpdate request) {
    }

    public void onGroupRequest(GroupRequest request) {
    }

    public void onGroupAddRequest(GroupRequest request) {
    }

    public void onGroupInviteRequest(GroupRequest request) {
    }

    public void onFriendAddRequest(FriendRequest request) {
    }

}
