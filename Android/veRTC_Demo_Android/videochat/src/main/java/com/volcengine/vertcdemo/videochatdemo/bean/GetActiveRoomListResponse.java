package com.volcengine.vertcdemo.videochatdemo.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GetActiveRoomListResponse extends VideoChatResponse {

    @SerializedName("room_list")
    public List<VCRoomInfo> roomList;

    @Override
    public String toString() {
        return "GetActiveRoomListResponse{" +
                "roomList=" + roomList +
                '}';
    }
}
