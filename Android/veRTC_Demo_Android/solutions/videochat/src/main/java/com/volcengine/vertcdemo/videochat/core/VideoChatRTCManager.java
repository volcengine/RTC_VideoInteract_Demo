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
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.NetworkQualityStats;
import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.rts.RTCRoomEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTCVideoEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.videochat.bean.UserJoinedBroadcast;
import com.volcengine.vertcdemo.videochat.bean.UserLeaveBroadcast;
import com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo;
import com.volcengine.vertcdemo.videochat.event.SDKAudioPropertiesEvent;
import com.volcengine.vertcdemo.videochat.event.SDKNetStatusEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        private SDKAudioPropertiesEvent.SDKAudioProperties mLocalProperties = null; // 本地音量记录

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

    private final RTCRoomEventHandlerWithRTS mRTCRoomEventHandler = new RTCRoomEventHandlerWithRTS() {

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            super.onRoomStateChanged(roomId, uid, state, extraInfo);
            Log.d(TAG, String.format("onRoomStateChanged: %s, %s, %d, %s", roomId, uid, state, extraInfo));
            mRoomId = roomId;
        }

        @Override
        public void onUserJoined(UserInfo userInfo, int elapsed) {
            super.onUserJoined(userInfo, elapsed);
            Log.d(TAG, String.format("onUserJoined: %s, %d", userInfo.getUid(), elapsed));
            SolutionDemoEventManager.post(new UserJoinedBroadcast(userInfo));
            setRemoteVideoView(userInfo.getUid(), mRoomId, getUserRenderView(userInfo.getUid()));
        }

        @Override
        public void onUserLeave(String uid, int reason) {
            Log.d(TAG, String.format("onUserLeave: %s, %d", uid, reason));
            SolutionDemoEventManager.post(new UserLeaveBroadcast(uid, reason));
        }

        @Override
        public void onUserPublishStream(String uid, MediaStreamType type) {
            super.onUserPublishStream(uid, type);
            if (type != MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO && !TextUtils.isEmpty(mRoomId)) {
                setRemoteVideoView(uid, mRoomId, getUserRenderView(uid));
            }
        }

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

        @Override
        public void onForwardStreamStateChanged(ForwardStreamStateInfo[] stateInfos) {
            String result = "";
            for (ForwardStreamStateInfo info : stateInfos) {
                result = result.concat("|" + info.roomId + "," + info.error + "," + info.state + "|");
            }
            Log.d(TAG, String.format("onForwardStreamStateChanged: %s", result));
        }

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
    private String mRoomId = ""; // 当前加入 RTC 房间的 RoomId

    public static VideoChatRTCManager ins() {
        if (sInstance == null) {
            sInstance = new VideoChatRTCManager();
        }
        return sInstance;
    }

    public void initEngine(RTSInfo info) {
        destroyEngine();
        mRTCVideo = RTCVideo.createRTCVideo(Utilities.getApplicationContext(), info.appId, mRTCVideoEventHandler, null, null);
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

    public VideoChatRTSClient getRTSClient() {
        return mRTSClient;
    }

    public void enableAudioVolumeIndication(int interval) {
        MLog.d(TAG, String.format(Locale.ENGLISH, "enableAudioVolumeIndication: %d", interval));
        if (mRTCVideo == null) {
            return;
        }
        AudioPropertiesConfig config = new AudioPropertiesConfig(interval);
        mRTCVideo.enableAudioPropertiesReport(config);
    }

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
        VideoCanvas videoCanvas = new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, "", false);
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, videoCanvas);
        Log.d(TAG, "setLocalVideoView");
    }

    public TextureView getUserRenderView(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        TextureView view = mUidViewMap.get(userId);
        if (view == null) {
            view = new TextureView(Utilities.getApplicationContext());
            mUidViewMap.put(userId, view);
        }
        return view;
    }

    public void setRemoteVideoView(String userId, String roomId, TextureView textureView) {
        Log.d(TAG, String.format(Locale.ENGLISH, "setRemoteVideoView : %s  %s", userId, roomId));
        if (mRTCVideo != null) {
            VideoCanvas canvas = new VideoCanvas(textureView, RENDER_MODE_HIDDEN, userId, false);
            canvas.roomId = roomId; // 该参数必传，否则订阅不到对方的视频画面
            mRTCVideo.setRemoteVideoCanvas(userId, StreamIndex.STREAM_INDEX_MAIN, canvas);
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
                copyAssetFile(Utilities.getApplicationContext(), "voicechat_bgm.mp3", bgmPath.getAbsolutePath());
            }
        });
    }

    private String getExternalResourcePath() {
        return Utilities.getApplicationContext().getExternalFilesDir("assets").getAbsolutePath() + "/resource/";
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
     * 初始化美颜
     */
    private void initVideoEffect() {

    }

    /**
     * 打开美颜对话框
     * @param context 上下文对象
     */
    public void openEffectDialog(Context context) {
        SafeToast.show("开源代码暂不支持美颜相关功能，体验效果请下载Demo");
    }
}
