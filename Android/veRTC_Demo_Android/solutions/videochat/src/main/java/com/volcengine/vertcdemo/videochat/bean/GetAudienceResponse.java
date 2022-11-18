package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetAudienceResponse extends VideoChatResponse {
    @SerializedName("audience_list")
    public List<VideoChatUserInfo> audienceList;

    @Override
    public String toString() {
        return "GetAudienceResponse{" +
                "audienceList=" + audienceList +
                '}';
    }
}
