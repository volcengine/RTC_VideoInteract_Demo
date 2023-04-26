// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.bean;

import com.google.gson.annotations.SerializedName;

public class InteractReplyEvent extends VideoChatResponse {

    @SerializedName("user_info")
    public VideoChatUserInfo userInfo;

    @Override
    public String toString() {
        return "InteractReplyEvent{" +
                "userInfo=" + userInfo +
                '}';
    }
}
