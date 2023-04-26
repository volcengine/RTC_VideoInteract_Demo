// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.core;

import static com.ss.bytertc.engine.VideoCanvas.RENDER_MODE_HIDDEN;
import static com.ss.bytertc.engine.data.AudioMixingType.AUDIO_MIXING_TYPE_PLAYOUT_AND_PUBLISH;
import static com.volcengine.vertcdemo.utils.FileUtils.copyAssetFile;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.AudioMixingConfig;
import com.ss.bytertc.engine.data.AudioPropertiesConfig;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.ForwardStreamEventInfo;
import com.ss.bytertc.engine.data.ForwardStreamInfo;
import com.ss.bytertc.engine.data.ForwardStreamStateInfo;
import com.ss.bytertc.engine.data.LocalAudioPropertiesInfo;
import com.ss.bytertc.engine.data.MirrorType;
import com.ss.bytertc.engine.data.RemoteAudioPropertiesInfo;
import com.ss.bytertc.engine.data.RemoteStreamKey;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.NetworkQualityStats;
import com.volcengine.vertcdemo.common.AppExecutors;
import com.volcengine.vertcdemo.core.eventbus.SDKReconnectToRoomEvent;
import com.volcengine.vertcdemo.utils.AppUtil;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.rts.RTCRoomEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTCVideoEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.protocol.IEffect;
import com.volcengine.vertcdemo.protocol.ProtocolUtil;
import com.volcengine.vertcdemo.videochat.bean.UserJoinedEvent;
import com.volcengine.vertcdemo.videochat.bean.UserLeaveEvent;
import com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo;
import com.volcengine.vertcdemo.videochat.event.SDKAudioPropertiesEvent;
import com.volcengine.vertcdemo.videochat.event.SDKNetStatusEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RTC对象管理类
 *
 * 使用单例形式，调用RTC接口，并在调用中更新 VideoChatDataManager 数据
 * 内部记录开关状态
 *
 * 功能：
 * 1.开关和媒体状态
 * 2.获取当前媒体状态
 * 3.接收RTC各种回调，例如：用户进退房、媒体状态改变、媒体状态数据回调、网络状态回调、音量大小回调
 * 4.管理用户视频渲染view
 * 5.加入离开房间
 * 6.创建和销毁引擎
 */
public class VideoChatRTCManager {

    private static final String TAG = "VideoChatRTCManager";

    private static VideoChatRTCManager sInstance;

    private final RTCVideoEventHandlerWithRTS mRTCVideoEventHandler = new RTCVideoEventHandlerWithRTS() {

        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
            Log.d(TAG, String.format("onWarning: %d", warn));
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.d(TAG, String.format("onError: %d", err));
        }

        // 本地音量记录。
        private SDKAudioPropertiesEvent.SDKAudioProperties mLocalProperties = null;

        /**
         * 本地音频包括使用 RTC SDK 内部机制采集的麦克风音频和屏幕音频。
         * @param audioPropertiesInfos 本地音频信息，详见 LocalAudioPropertiesInfo 。
         */
        @Override
        public void onLocalAudioPropertiesReport(LocalAudioPropertiesInfo[] audioPropertiesInfos) {
            super.onLocalAudioPropertiesReport(audioPropertiesInfos);
            if (audioPropertiesInfos == null) {
                return;
            }
            for (LocalAudioPropertiesInfo info : audioPropertiesInfos) {
                if (info.streamIndex == StreamIndex.STREAM_INDEX_MAIN) {
                    SDKAudioPropertiesEvent.SDKAudioProperties properties = new SDKAudioPropertiesEvent.SDKAudioProperties(
                            SolutionDataManager.ins().getUserId(),
                            info.audioPropertiesInfo);
                    mLocalProperties = properties;
                    List<SDKAudioPropertiesEvent.SDKAudioProperties> audioPropertiesList = new ArrayList<>();
                    audioPropertiesList.add(properties);
                    SolutionDemoEventManager.post(new SDKAudioPropertiesEvent(audioPropertiesList));
                    return;
                }
            }
        }

        /**
         * 远端用户的音频包括使用 RTC SDK 内部机制/自定义机制采集的麦克风音频和屏幕音频。
         * @param audioPropertiesInfos 远端音频信息，其中包含音频流属性、房间 ID、用户 ID ，详见 RemoteAudioPropertiesInfo。
         * @param totalRemoteVolume 订阅的所有远端流的总音量。
         */
        @Override
        public void onRemoteAudioPropertiesReport(RemoteAudioPropertiesInfo[] audioPropertiesInfos, int totalRemoteVolume) {
            super.onRemoteAudioPropertiesReport(audioPropertiesInfos, totalRemoteVolume);
            if (audioPropertiesInfos == null) {
                return;
            }
            List<SDKAudioPropertiesEvent.SDKAudioProperties> audioPropertiesList = new ArrayList<>();
            if (mLocalProperties != null) {
                audioPropertiesList.add(mLocalProperties);
            }
            for (RemoteAudioPropertiesInfo info : audioPropertiesInfos) {
                if (info.streamKey.getStreamIndex() == StreamIndex.STREAM_INDEX_MAIN) {
                    audioPropertiesList.add(new SDKAudioPropertiesEvent.SDKAudioProperties(
                            info.streamKey.getUserId(),
                            info.audioPropertiesInfo));
                }
            }
            SolutionDemoEventManager.post(new SDKAudioPropertiesEvent(audioPropertiesList));
        }
    };

    /**
     * RTC room 事件回调监听。
     */
    private final RTCRoomEventHandlerWithRTS mRTCRoomEventHandler = new RTCRoomEventHandlerWithRTS() {

        /**
         * 房间状态改变回调，加入房间、离开房间、发生房间相关的警告或错误时会收到此回调。
         * @param roomId 房间id
         * @param uid 用户id
         * @param state 房间状态码
         * @param extraInfo 额外信息
         */
        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            super.onRoomStateChanged(roomId, uid, state, extraInfo);
            Log.d(TAG, String.format("onRoomStateChanged: %s, %s, %d, %s", roomId, uid, state, extraInfo));
            mRoomId = roomId;
            if (isReconnectSuccess(state, extraInfo)) {
                SolutionDemoEventManager.post(new SDKReconnectToRoomEvent(roomId));
            }
        }

        /**
         * 可见用户加入房间，或房内隐身用户切换为可见的回调。
         * @param userInfo 用户信息
         * @param elapsed 主播角色用户调用 joinRoom 加入房间到房间内其他用户收到该事件经历的时间，单位为 ms。
         */
        @Override
        public void onUserJoined(UserInfo userInfo, int elapsed) {
            super.onUserJoined(userInfo, elapsed);
            Log.d(TAG, String.format("onUserJoined: %s, %d", userInfo.getUid(), elapsed));
            SolutionDemoEventManager.post(new UserJoinedEvent(userInfo));
            setRemoteVideoView(userInfo.getUid(), mRoomId, getUserRenderView(userInfo.getUid()));
        }

        /**
         * 远端用户离开房间，或切至不可见时，本地用户会收到此事件
         * @param uid 离开房间，或切至不可见的的远端用户 ID。
         * @param reason 用户离开房间的原因：
         * • 0: 远端用户调用 leaveRoom 主动退出房间。
         * • 1: 远端用户因 Token 过期或网络原因等掉线。
         * • 2: 远端用户调用 setUserVisibility 切换至不可见状态。
         * • 3: 服务端调用 OpenAPI 将该远端用户踢出房间。
         */
        @Override
        public void onUserLeave(String uid, int reason) {
            Log.d(TAG, String.format("onUserLeave: %s, %d", uid, reason));
            SolutionDemoEventManager.post(new UserLeaveEvent(uid, reason));
        }

        /**
         * 房间内新增远端摄像头/麦克风采集音视频流的回调。
         * @param uid 远端流发布用户的用户 ID。
         * @param type 远端媒体流的类型，参看 MediaStreamType。
         */
        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            super.onUserPublishStream(uid, type);
            if (type != MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO && !TextUtils.isEmpty(mRoomId)) {
                setRemoteVideoView(uid, mRoomId, getUserRenderView(uid));
            }
        }

        /**
         * 加入房间后， 以 2 秒 1 次的频率，报告用户的网络质量信息
         *
         * @param localQuality    本地网络质量，详见 NetworkQualityStats。
         * @param remoteQualities 已订阅用户的网络质量，详见 NetworkQualityStats。
         * @see NetworkQualityStats
         */
        @Override
        public void onNetworkQuality(NetworkQualityStats localQuality, NetworkQualityStats[] remoteQualities) {
            super.onNetworkQuality(localQuality, remoteQualities);
            SolutionDemoEventManager.post(new SDKNetStatusEvent(localQuality.uid, localQuality.txQuality));
            if (remoteQualities != null) {
                for (NetworkQualityStats stats : remoteQualities) {
                    SolutionDemoEventManager.post(new SDKNetStatusEvent(stats.uid, stats.rxQuality));
                }
            }
        }

        /**
         * 跨房间媒体流转发状态和错误回调
         * @param stateInfos 跨房间媒体流转发目标房间信息数组，详见 ForwardStreamStateInfo{@link #ForwardStreamStateInfo}
         */
        @Override
        public void onForwardStreamStateChanged(ForwardStreamStateInfo[] stateInfos) {
            String result = "";
            for (ForwardStreamStateInfo info : stateInfos) {
                result = result.concat("|" + info.roomId + "," + info.error + "," + info.state + "|");
            }
            Log.d(TAG, String.format("onForwardStreamStateChanged: %s", result));
        }

        /**
         * 跨房间媒体流转发事件回调
         * @param eventInfos 跨房间媒体流转发目标房间事件数组，详见 ForwardStreamEventInfo{@link #ForwardStreamEventInfo}
         */
        @Override
        public void onForwardStreamEvent(ForwardStreamEventInfo[] eventInfos) {
            String result = "";
            for (ForwardStreamEventInfo info : eventInfos) {
                result = result.concat("|" + info.roomId + "," + info.event + "|");
            }
            Log.d(TAG, String.format("onForwardStreamStateChanged: %s", result));
        }
    };

    private VideoChatRTSClient mRTSClient;

    private RTCVideo mRTCVideo;
    private RTCRoom mRTCRoom;

    private final Map<String, TextureView> mUidViewMap = new HashMap<>();

    private boolean mIsCameraOn = true;
    private boolean mIsMicOn = true;
    private boolean mIsFront = true;
    private int mFrameRate = 15;
    private int mFrameWidth = 720;
    private int mFrameHeight = 1280;
    private int mBitrate = 1600;
    public boolean isTest = false;
    // 当前加入 RTC 房间的 RoomId。
    private String mRoomId = "";

    public static VideoChatRTCManager ins() {
        if (sInstance == null) {
            sInstance = new VideoChatRTCManager();
        }
        return sInstance;
    }

    /**
     * 初始化RTC。
     */
    public void initEngine(RTSInfo info) {
        destroyEngine();
        mRTCVideo = RTCVideo.createRTCVideo(AppUtil.getApplicationContext(), info.appId, mRTCVideoEventHandler, null, null);
        mRTCVideo.setBusinessId(info.bid);
        mRTCVideo.stopVideoCapture();
        enableAudioVolumeIndication(2000);

        VideoEncoderConfig config = new VideoEncoderConfig();
        config.width = 720;
        config.height = 1280;
        config.frameRate = 15;
        config.maxBitrate = 1600;
        mRTCVideo.setVideoEncoderConfig(config);
        switchCamera(mIsFront);

        initVideoEffect();
        initBGMRes();
        mRTSClient = new VideoChatRTSClient(mRTCVideo, info);
        mRTCVideoEventHandler.setBaseClient(mRTSClient);
        mRTCRoomEventHandler.setBaseClient(mRTSClient);
    }

    /**
     * 获取 RTS Client 对象。
     * @return VideoChatRTSClient 视频聊天场景 RTS client 对象，支持用户在 RTC room 内发送和接收消息。
     */
    public VideoChatRTSClient getRTSClient() {
        return mRTSClient;
    }

    /**
     * 启用音频信息提示。
     * @param interval 回调周期
     */
    public void enableAudioVolumeIndication(int interval) {
        MLog.d(TAG, String.format(Locale.ENGLISH, "enableAudioVolumeIndication: %d", interval));
        if (mRTCVideo == null) {
            return;
        }
        AudioPropertiesConfig config = new AudioPropertiesConfig(interval);
        mRTCVideo.enableAudioPropertiesReport(config);
    }

    /**
     * 翻转摄像头
     * @param isFrontCamera 是否是前置摄像头
     */
    public void switchCamera(boolean isFront) {
        if (mRTCVideo != null) {
            mRTCVideo.setLocalVideoMirrorType(isFront ? MirrorType.MIRROR_TYPE_RENDER_AND_ENCODER : MirrorType.MIRROR_TYPE_NONE);
            mRTCVideo.switchCamera(isFront ? CameraId.CAMERA_ID_FRONT : CameraId.CAMERA_ID_BACK);
        }
        mIsFront = isFront;
    }

    public void switchCamera() {
        switchCamera(!mIsFront);
    }

    public boolean isCameraOn() {
        return mIsCameraOn;
    }

    public boolean isMicOn() {
        return mIsMicOn;
    }

    public void turnOnMic(boolean isMicOn) {
        if (mRTCVideo != null) {
            if (isMicOn) {
                mRTCVideo.startAudioCapture();
            } else {
                mRTCVideo.stopAudioCapture();
            }
        }
        if (mRTCRoom != null) {
            if (isMicOn) {
                mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            } else {
                mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            }
        }

        Log.d(TAG, "turnOnMic : " + isMicOn);
        mIsMicOn = isMicOn;
        updateMicAndCameraStatus();
    }

    public void turnOnMic() {
        turnOnMic(!mIsMicOn);
    }

    public void turnOnCamera(boolean isCameraOn) {
        if (mRTCVideo != null) {
            if (isCameraOn) {
                mRTCVideo.startVideoCapture();
            } else {
                mRTCVideo.stopVideoCapture();
            }
        }
        mIsCameraOn = isCameraOn;
        Log.d(TAG, "turnOnCamera : " + isCameraOn);
        updateMicAndCameraStatus();
    }

    private void updateMicAndCameraStatus() {
        VideoChatUserInfo selfUserInfo = VideoChatDataManager.ins().selfUserInfo;
        if (selfUserInfo == null) {
            return;
        }
        String hostUid = VideoChatDataManager.ins().hostUserInfo == null ? null : VideoChatDataManager.ins().hostUserInfo.userId;
        VideoChatDataManager.ins().selfUserInfo.mic = mIsMicOn ? VideoChatUserInfo.MIC_STATUS_ON : VideoChatUserInfo.MIC_STATUS_OFF;
        VideoChatDataManager.ins().selfUserInfo.camera = mIsCameraOn ? VideoChatUserInfo.CAMERA_STATUS_ON : VideoChatUserInfo.CAMERA_STATUS_OFF;
        if (selfUserInfo.isHost() && TextUtils.equals(selfUserInfo.userId, hostUid)) {
            VideoChatDataManager.ins().hostUserInfo.mic = mIsMicOn ? VideoChatUserInfo.MIC_STATUS_ON : VideoChatUserInfo.MIC_STATUS_OFF;
            VideoChatDataManager.ins().hostUserInfo.camera = mIsCameraOn ? VideoChatUserInfo.CAMERA_STATUS_ON : VideoChatUserInfo.CAMERA_STATUS_OFF;
        }
    }

    public void turnOnCamera() {
        turnOnCamera(!mIsCameraOn);
    }

    public void setFrameRate(int frameRate) {
        mFrameRate = frameRate;
        updateVideoConfig();
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public void setResolution(int width, int height) {
        mFrameWidth = width;
        mFrameHeight = height;
        updateVideoConfig();
    }

    public void setBitrate(int bitrate) {
        mBitrate = bitrate;
        updateVideoConfig();
    }

    public int getBitrate() {
        return mBitrate;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    private void updateVideoConfig() {
        if (mRTCVideo != null) {
            VideoEncoderConfig config = new VideoEncoderConfig();
            config.width = mFrameWidth;
            config.height = mFrameHeight;
            config.frameRate = mFrameRate;
            config.maxBitrate = mBitrate;
            mRTCVideo.setVideoEncoderConfig(config);
        }
    }

    public void startMuteVideo(boolean isStart) {
        Log.d(TAG, "startMuteVideo : " + isStart);
        if (mRTCRoom != null) {
            if (isStart) {
                mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
            } else {
                mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
            }
        }
    }

    public void startMuteAudio(boolean isStart) {
        Log.d(TAG, String.format("startMuteAudio: %b", isStart));
        if (mRTCRoom != null) {
            if (isStart) {
                mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            } else {
                mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            }
        }
    }

    /**
     * 房间内的可见用户调用 startVideoCapture 开启内部视频采集时，房间内其他用户会收到此回调。
     * @param isStart true: 开启视频采集。
     *                false: 关闭视频采集。
     */
    public void startVideoCapture(boolean isStart) {
        if (mRTCVideo != null) {
            if (isStart) {
                mRTCVideo.startVideoCapture();
            } else {
                mRTCVideo.stopVideoCapture();
            }
        }
        mIsCameraOn = isStart;
        Log.d(TAG, "startCaptureVideo : " + isStart);
    }

    /**
     * 房间内的可见用户调用 startAudioCapture 开启内部音频采集时，房间内其他用户会收到此回调。
     * @param isStart true: 开启音频采集。
     *                false: 关闭音频采集。
     */
    public void startAudioCapture(boolean isStart) {
        if (mRTCVideo != null) {
            if (isStart) {
                mRTCVideo.startAudioCapture();
            } else {
                mRTCVideo.stopAudioCapture();
            }
        }
        mIsMicOn = isStart;
        Log.d(TAG, "startCaptureAudio : " + isStart);
    }

    public void setLocalVideoView(@NonNull TextureView surfaceView) {
        if (mRTCVideo == null) {
            return;
        }
        VideoCanvas videoCanvas = new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN);
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
        Log.d(TAG, "setLocalVideoView");
    }

    public TextureView getUserRenderView(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        TextureView view = mUidViewMap.get(userId);
        if (view == null) {
            view = new TextureView(AppUtil.getApplicationContext());
            mUidViewMap.put(userId, view);
        }
        return view;
    }

    public void setRemoteVideoView(String userId, String roomId, TextureView textureView) {
        Log.d(TAG, String.format(Locale.ENGLISH, "setRemoteVideoView : %s  %s", userId, roomId));
        if (mRTCVideo != null) {
            VideoCanvas canvas = new VideoCanvas(textureView, RENDER_MODE_HIDDEN);
            RemoteStreamKey remoteStreamKey = new RemoteStreamKey(roomId, userId, StreamIndex.STREAM_INDEX_MAIN);
            mRTCVideo.setRemoteVideoCanvas(remoteStreamKey, canvas);
        }
    }

    private void initBGMRes() {
        AppExecutors.diskIO().execute(() -> {
            File bgmPath = new File(getExternalResourcePath(), "bgm/voicechat_bgm.mp3");
            if (!bgmPath.exists()) {
                File dir = new File(getExternalResourcePath() + "bgm");
                if (!dir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();
                }
                copyAssetFile(AppUtil.getApplicationContext(), "voicechat_bgm.mp3", bgmPath.getAbsolutePath());
            }
        });
    }

    private String getExternalResourcePath() {
        return AppUtil.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath() + "/resource/";
    }

    public void destroyEngine() {
        Log.d(TAG, "destroyEngine");
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom.destroy();
        }
        mRTCRoom = null;
        if (mRTCVideo == null) {
            return;
        }
        RTCVideo.destroyRTCVideo();
        mRTCVideo = null;
    }

    /**
     * 加入 RTC 房间
     * @param token 动态密钥。用于对进房用户进行鉴权验证。
     *              进入房间需要携带 Token。测试时可使用控制台生成临时 Token，正式上线需要使用密钥 SDK 在你的服务端生成并下发 Token。
     *              使用不同 AppID 的 App 是不能互通的。
     *              请务必保证生成 Token 使用的 AppID 和创建引擎时使用的 AppID 相同，否则会导致加入房间失败。
     * @param roomId RTC 房间 id。
     * @param userId    用户 id。
     * @param userVisible 用户对房间内的其他用户是否可见。
     */
    public void joinRoom(String roomId, String token, String userId, boolean userVisible) {
        Log.d(TAG, String.format("joinRoom: %s %s %s", roomId, userId, token));
        leaveRoom();
        if (mRTCVideo == null) {
            return;
        }
        mRTCRoom = mRTCVideo.createRTCRoom(roomId);
        mRTCRoom.setRTCRoomEventHandler(mRTCRoomEventHandler);
        UserInfo userInfo = new UserInfo(userId, null);
        RTCRoomConfig roomConfig = new RTCRoomConfig(ChannelProfile.CHANNEL_PROFILE_COMMUNICATION,
                true, true, true);
        mRTCRoom.joinRoom(token, userInfo, roomConfig);
    }

    /**
     * 离开房间。
     */
    public void leaveRoom() {
        Log.d(TAG, "leaveRoom");
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom.destroy();
            mRTCRoom = null;
        }
    }

    public void startAudioMixing(boolean isStart) {
        Log.d(TAG, String.format("startAudioMixing: %b", isStart));
        if (mRTCVideo != null) {
            if (isStart) {
                String bgmPath = getExternalResourcePath() + "bgm/voicechat_bgm.mp3";
                mRTCVideo.getAudioMixingManager().preloadAudioMixing(0, bgmPath);
                AudioMixingConfig config = new AudioMixingConfig(AUDIO_MIXING_TYPE_PLAYOUT_AND_PUBLISH, -1);
                mRTCVideo.getAudioMixingManager().startAudioMixing(0, bgmPath, config);
            } else {
                mRTCVideo.getAudioMixingManager().stopAudioMixing(0);
            }
        }
    }

    public void resumeAudioMixing() {
        Log.d(TAG, "resumeAudioMixing");
        if (mRTCVideo != null) {
            mRTCVideo.getAudioMixingManager().resumeAudioMixing(0);
        }
    }

    public void pauseAudioMixing() {
        Log.d(TAG, "pauseAudioMixing");
        if (mRTCVideo != null) {
            mRTCVideo.getAudioMixingManager().pauseAudioMixing(0);
        }
    }

    public void stopAudioMixing() {
        Log.d(TAG, "stopAudioMixing");
        if (mRTCVideo != null) {
            mRTCVideo.getAudioMixingManager().stopAudioMixing(0);
        }
    }

    public void adjustBGMVolume(int progress) {
        Log.d(TAG, String.format("adjustBGMVolume: %d", progress));
        if (mRTCVideo != null) {
            mRTCVideo.getAudioMixingManager().setAudioMixingVolume(0, progress, AUDIO_MIXING_TYPE_PLAYOUT_AND_PUBLISH);
        }
    }

    public void adjustUserVolume(int progress) {
        Log.d(TAG, String.format("adjustUserVolume: %d", progress));
        if (mRTCVideo != null) {
            mRTCVideo.setCaptureVolume(StreamIndex.STREAM_INDEX_MAIN, progress);
        }
    }

    public void forwardStreamToRoom(ForwardStreamInfo targetRoomInfo) {
        if (mRTCRoom != null) {
            ArrayList<ForwardStreamInfo> list = new ArrayList<>(1);
            list.add(targetRoomInfo);
            int result = mRTCRoom.startForwardStreamToRooms(list);
            Log.d(TAG, String.format("forwardStreamToRoom result: %d", result));
        }
    }

    public void stopForwardStreamToRoom() {
        if (mRTCRoom != null) {
            Log.d(TAG, "stopForwardStreamToRoom");
            mRTCRoom.stopForwardStreamToRooms();
        }
    }

    public void muteRemoteAudio(String uid, boolean mute) {
        Log.d(TAG, "muteRemoteAudio uid:" + uid + ",mute:" + mute);
        if (mRTCRoom != null) {
            if (mute) {
                mRTCRoom.unsubscribeStream(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            } else {
                mRTCRoom.subscribeStream(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
            }
        }
    }

    public void setUserVisibility(boolean userVisible) {
        if (mRTCRoom != null) {
            mRTCRoom.setUserVisibility(userVisible);
        }
    }

    /**
     * 初始化美颜。
     */
    private void initVideoEffect() {
        IEffect effect = ProtocolUtil.getIEffect();
        if (effect != null) {
            effect.initWithRTCVideo(mRTCVideo);
        }
    }

    public void resumeVideoEffect() {
        IEffect effect = ProtocolUtil.getIEffect();
        if (effect != null) {
            effect.resume();
        }
    }

    /**
     * 打开美颜对话框
     * @param context 上下文对象
     */
    public void openEffectDialog(Context context) {
        IEffect effect = ProtocolUtil.getIEffect();
        if (effect != null) {
            effect.showEffectDialog(context, null);
        }
    }
}
