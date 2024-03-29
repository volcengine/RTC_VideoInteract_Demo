// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.event;

public class AudioStatsEvent {
    public String uid;
    public int rtt;
    public float upload;
    public float download;

    public AudioStatsEvent(int rtt, float upload, float download) {
        this.rtt = rtt;
        this.upload = upload;
        this.download = download;
    }

    public AudioStatsEvent(String uid, int rtt, float upload, float download) {
        this.uid = uid;
        this.rtt = rtt;
        this.upload = upload;
        this.download = download;
    }
}
