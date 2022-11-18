package com.volcengine.vertcdemo.videochat.bean;

public class UserLeaveBroadcast {
   public String uid;
    public int reason;

    public UserLeaveBroadcast(String uid,int reason) {
        this.uid = uid;
        this.reason = reason;
    }
}
