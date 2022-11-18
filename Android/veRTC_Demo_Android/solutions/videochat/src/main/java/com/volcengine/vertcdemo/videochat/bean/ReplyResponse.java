package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

public class ReplyResponse extends VideoChatResponse {

    @SerializedName("user_info")
    public VideoChatUserInfo userInfo;

    @Override
    public String toString() {
        return "ReplyResponse{" +
                "userInfo=" + userInfo +
                '}';
    }
}
