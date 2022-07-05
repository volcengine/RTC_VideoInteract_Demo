package com.volcengine.vertcdemo.videochatdemo.bean;

import com.google.gson.annotations.SerializedName;

public class CreateRoomResponse extends VideoChatResponse {

    @SerializedName("room_info")
    public VCRoomInfo roomInfo;
    @SerializedName("user_info")
    public VCUserInfo userInfo;
    @SerializedName("rtc_token")
    public String rtcToken;

    @Override
    public String toString() {
        return "CreateRoomResponse{" +
                "roomInfo=" + roomInfo +
                ", userInfo=" + userInfo +
                ", rtcToken='" + rtcToken + '\'' +
                '}';
    }
}