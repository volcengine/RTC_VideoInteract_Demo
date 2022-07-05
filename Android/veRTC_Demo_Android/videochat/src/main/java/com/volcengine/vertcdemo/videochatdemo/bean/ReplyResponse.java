package com.volcengine.vertcdemo.videochatdemo.bean;

import com.google.gson.annotations.SerializedName;

public class ReplyResponse extends VideoChatResponse {

    @SerializedName("user_info")
    public VCUserInfo userInfo;

    @Override
    public String toString() {
        return "ReplyResponse{" +
                "userInfo=" + userInfo +
                '}';
    }
}
