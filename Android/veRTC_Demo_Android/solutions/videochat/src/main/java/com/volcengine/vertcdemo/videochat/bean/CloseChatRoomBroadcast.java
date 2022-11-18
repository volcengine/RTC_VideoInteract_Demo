package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class CloseChatRoomBroadcast implements RTSBizInform {
    @SerializedName("room_id")
    public String roomId;

    @Override
    public String toString() {
        return "CloseChatRoomBroadcast{" +
                "roomId='" + roomId + '\'' +
                '}';
    }
}
