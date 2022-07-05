package com.volcengine.vertcdemo.videochatdemo.bean;

import com.ss.bytertc.engine.UserInfo;

public class UserJoinedBroadcast {
    public UserInfo userInfo;

    public UserJoinedBroadcast(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
