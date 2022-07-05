package com.volcengine.vertcdemo.videochatdemo.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetAnchorsResponse extends VideoChatResponse {
    @SerializedName("anchor_list")
    public List<VCUserInfo> anchorList;

    @Override
    public String toString() {
        return "GetAnchorsResponse{" +
                "anchorList=" + anchorList +
                '}';
    }
}
