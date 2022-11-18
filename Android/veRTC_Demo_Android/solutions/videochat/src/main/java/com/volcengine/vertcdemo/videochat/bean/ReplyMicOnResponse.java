package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

public class ReplyMicOnResponse extends VideoChatResponse {

    @SerializedName("is_need_apply")
    public boolean needApply = false;

    @Override
    public String toString() {
        return "ReplyMicOnRequest{" +
                "needApply=" + needApply +
                '}';
    }
}
