package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class ManageOtherAnchorBroadcast implements RTSBizInform {
    @SerializedName("room_id")
    public String roomId;
    @SerializedName("other_anchor_user_id")
    public String otherAnchorUid;
    @SerializedName("type")
    public int type;

    @Override
    public String toString() {
        return "ManageOtherAnchorBroadcast{" +
                "roomId='" + roomId + '\'' +
                ", otherAnchorUid='" + otherAnchorUid + '\'' +
                ", type=" + type +
                '}';
    }
}
