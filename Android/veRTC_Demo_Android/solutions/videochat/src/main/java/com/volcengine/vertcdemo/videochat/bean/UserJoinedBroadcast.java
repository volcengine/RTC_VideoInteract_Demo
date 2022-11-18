package com.volcengine.vertcdemo.videochat.bean;

import com.ss.bytertc.engine.UserInfo;

public class UserJoinedBroadcast {
    public UserInfo userInfo;

    public UserJoinedBroadcast(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
