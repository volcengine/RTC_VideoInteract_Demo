// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

public class AudienceApplyEvent implements RTSBizInform {

    public boolean hasNewApply;
    @SerializedName("user_info")
    public VideoChatUserInfo userInfo;
    @SerializedName("seat_id")
    public int seatId;

    @Override
    public String toString() {
        return "AudienceApplyEvent{" +
                "userInfo=" + userInfo +
                ", seatId=" + seatId +
                '}';
    }
}
