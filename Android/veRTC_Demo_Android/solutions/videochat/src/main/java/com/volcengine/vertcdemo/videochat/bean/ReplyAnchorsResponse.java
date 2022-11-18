package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReplyAnchorsResponse extends VideoChatResponse {
    @SerializedName("interact_info_list")
    public List<InteractInfo> interactInfoList;

    @Override
    public String toString() {
        return "ReplyAnchorsResponse{" +
                "interactInfoList=" + interactInfoList +
                '}';
    }
}
