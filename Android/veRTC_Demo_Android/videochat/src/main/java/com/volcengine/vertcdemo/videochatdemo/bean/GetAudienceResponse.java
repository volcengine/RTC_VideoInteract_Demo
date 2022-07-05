package com.volcengine.vertcdemo.videochatdemo.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetAudienceResponse extends VideoChatResponse {
    @SerializedName("audience_list")
    public List<VCUserInfo> audienceList;

    @Override
    public String toString() {
        return "GetAudienceResponse{" +
                "audienceList=" + audienceList +
                '}';
    }
}
