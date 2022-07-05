package com.volcengine.vertcdemo.videochatdemo.core.event;

import com.ss.bytertc.engine.handler.IRTCEngineEventHandler;

public class AudioVolumeEvent {
    public IRTCEngineEventHandler.AudioVolumeInfo[] speakers;

    public AudioVolumeEvent(IRTCEngineEventHandler.AudioVolumeInfo[] speakers) {
        this.speakers = speakers;
    }
}
