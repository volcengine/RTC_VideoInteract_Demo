package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;
import com.volcengine.vertcdemo.videochat.core.VideoChatDataManager;

public class InteractResultBroadcast implements RTSBizInform {

    @SerializedName("reply")
    @VideoChatDataManager.ReplyType
    public int reply;
    @SerializedName("user_info")
    public VideoChatUserInfo userInfo;

    @Override
    public String toString() {
        return "InteractResultBroadcast{" +
                "reply=" + reply +
                ", userInfo=" + userInfo +
                '}';
    }
}
