package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class InviteAnchorReplyBroadcast implements RTSBizInform {
    @SerializedName("to_room_id")
    public String toRoomId;
    @SerializedName("to_user_id")
    public String toUserId;
    @SerializedName("to_user_name")
    public String toUserName;
    @SerializedName("interact_info")
    public InteractInfo interactInfo;
    @SerializedName("reply")
    public int reply;

    @Override
    public String toString() {
        return "InviteAnchorReplyBroadcast{" +
                "toRoomId='" + toRoomId + '\'' +
                ", toUserId='" + toUserId + '\'' +
                ", toUserName='" + toUserName + '\'' +
                ", interactInfo=" + interactInfo +
                ", reply=" + reply +
                '}';
    }
}