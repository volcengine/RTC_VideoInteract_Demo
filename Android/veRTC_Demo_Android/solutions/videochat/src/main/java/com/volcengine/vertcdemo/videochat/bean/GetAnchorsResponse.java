package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetAnchorsResponse extends VideoChatResponse {
    @SerializedName("anchor_list")
    public List<VideoChatUserInfo> anchorList;

    @Override
    public String toString() {
        return "GetAnchorsResponse{" +
                "anchorList=" + anchorList +
                '}';
    }
}
