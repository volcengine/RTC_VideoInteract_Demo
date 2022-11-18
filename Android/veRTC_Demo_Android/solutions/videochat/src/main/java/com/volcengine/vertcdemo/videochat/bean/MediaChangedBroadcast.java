package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class MediaChangedBroadcast implements RTSBizInform {

    @SerializedName("mic")
    @VideoChatUserInfo.MicStatus
    public int mic;
    @SerializedName("camera")
    @VideoChatUserInfo.CameraStatus
    public int camera;
    @SerializedName("user_info")
    public VideoChatUserInfo userInfo;

    @Override
    public String toString() {
        return "MediaChangedBroadcast{" +
                "mic=" + mic +
                ", camera=" + camera +
                ", userInfo=" + userInfo +
                '}';
    }
}
