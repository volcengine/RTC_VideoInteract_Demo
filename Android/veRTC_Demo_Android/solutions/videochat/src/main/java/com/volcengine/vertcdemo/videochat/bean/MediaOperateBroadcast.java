package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class MediaOperateBroadcast implements RTSBizInform {

    @SerializedName("mic")
    @VideoChatUserInfo.MicStatus
    public int mic;
    @SerializedName("camera")
    @VideoChatUserInfo.CameraStatus
    public int camera;

    @Override
    public String toString() {
        return "MediaOperateBroadcast{" +
                "mic=" + mic +
                ", camera=" + camera +
                '}';
    }
}
