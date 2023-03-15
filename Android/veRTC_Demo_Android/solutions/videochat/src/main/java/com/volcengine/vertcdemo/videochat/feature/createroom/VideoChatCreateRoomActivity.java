package com.volcengine.vertcdemo.videochat.feature.createroom;

import static com.volcengine.vertcdemo.core.SolutionConstants.CLICK_RESET_INTERVAL;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.SocketConnectEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochat.bean.CreateRoomResponse;
import com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo;
import com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTCManager;
import com.volcengine.vertcdemo.videochat.feature.roommain.VideoChatRoomMainActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VideoChatCreateRoomActivity extends BaseActivity implements View.OnClickListener {

    private View mTopTip;
    private View mBackBtn;
    private View mSwitchCameraBtn;
    private View mVideoEffectBtn;
    private View mVideoSettingBtn;
    private View mStartLiveBtn;
    private TextureView mLocalVideoView;

    private boolean mEnableStartLive = false;
    private VideoChatRoomInfo mRoomInfo;
    private VideoChatUserInfo mSelfInfo;
    private String mRTCToken;

    private long mLastClickStartLiveTs = 0;

    private final IRequestCallback<CreateRoomResponse> mCreateRoomRequest = new IRequestCallback<CreateRoomResponse>() {
        @Override
        public void onSuccess(CreateRoomResponse data) {
            mEnableStartLive = true;
            mRoomInfo = data.roomInfo;
            mSelfInfo = data.userInfo;
            mRTCToken = data.rtcToken;
            VideoChatRTCManager.ins().setLocalVideoView(mLocalVideoView);
            VideoChatRTCManager.ins().startVideoCapture(true);
            VideoChatRTCManager.ins().startAudioCapture(true);
        }

        @Override
        public void onError(int errorCode, String message) {
            SafeToast.show(message);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_video_chat_room);
        requestCreateVideoRoom();
        VideoChatRTCManager.ins().turnOnCamera(true);
        VideoChatRTCManager.ins().turnOnMic(true);
        VideoChatRTCManager.ins().switchCamera(true);
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        super.onGlobalLayoutCompleted();

        mTopTip = findViewById(R.id.create_disconnect_tip);
        mStartLiveBtn = findViewById(R.id.start_live);
        mStartLiveBtn.setOnClickListener(this);
        mBackBtn = findViewById(R.id.exit_create_live);
        mBackBtn.setOnClickListener(this);
        mSwitchCameraBtn = findViewById(R.id.switch_camera_iv);
        mSwitchCameraBtn.setOnClickListener(this);
        mVideoEffectBtn = findViewById(R.id.effect_iv);
        mVideoEffectBtn.setOnClickListener(this);
        mVideoSettingBtn = findViewById(R.id.settings_iv);
        mVideoSettingBtn.setOnClickListener(this);
        FrameLayout mLocalVideoViewContainer = findViewById(R.id.preview_view_container);
        mLocalVideoView = VideoChatRTCManager.ins().getUserRenderView(SolutionDataManager.ins().getUserId());
        Utilities.removeFromParent(mLocalVideoView);
        mLocalVideoViewContainer.removeAllViews();
        mLocalVideoViewContainer.addView(mLocalVideoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        SolutionDemoEventManager.register(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mBackBtn) {
            onBackPressed();
        } else if (v == mSwitchCameraBtn) {
            switchCamera();
        } else if (v == mVideoEffectBtn) {
            openVideoEffectDialog();
        } else if (v == mVideoSettingBtn) {
            openVideoVideoSettingDialog();
        } else if (v == mStartLiveBtn) {
            startLive();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoChatRTCManager.ins().startVideoCapture(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoChatRTCManager.ins().startVideoCapture(false);
    }

    @Override
    public void finish() {
        super.finish();
        //华为P50关闭时如果为后置摄像头下次有可能打开失败
        VideoChatRTCManager.ins().switchCamera(true);
        SolutionDemoEventManager.unregister(this);
    }

    private void switchCamera() {
        VideoChatRTCManager.ins().switchCamera();
    }

    private void openVideoEffectDialog() {
        VideoChatRTCManager.ins().openEffectDialog(this);
    }

    private void openVideoVideoSettingDialog() {
        VideoChatCreateSettingDialog settingDialog = new VideoChatCreateSettingDialog(this, false, null);
        settingDialog.show();
    }

    private void startLive() {
        if (!mEnableStartLive) {
            showToast("获取直播信息失败，无法开始直播");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - mLastClickStartLiveTs <= CLICK_RESET_INTERVAL) {
            return;
        }
        mLastClickStartLiveTs = now;
        VideoChatRTCManager.ins().getRTSClient().requestStartLive(mRoomInfo.roomId,
                new IRequestCallback<VideoChatUserInfo>() {
                    @Override
                    public void onSuccess(VideoChatUserInfo userInfo) {
                        mLastClickStartLiveTs = 0;
                        showToast("创建直播成功");
                        mRoomInfo.status = VideoChatRoomInfo.ROOM_STATUS_LIVING;
                        VideoChatRoomMainActivity.openFromCreate(VideoChatCreateRoomActivity.this,
                                mRoomInfo, mSelfInfo, mRTCToken);
                        finish();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        mLastClickStartLiveTs = 0;
                        showToast("创建直播失败");
                    }
                });
    }

    private void requestCreateVideoRoom() {
        VideoChatRTCManager.ins().getRTSClient().requestCreateRoom(SolutionDataManager.ins().getUserName(),
                SolutionDataManager.ins().getUserName(), "voicechat_background_1.jpg", mCreateRoomRequest);
    }

    private void showToast(String toast) {
        SafeToast.show(toast);
    }

    private void showTopTip(boolean isShow) {
        mTopTip.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketConnectEvent(SocketConnectEvent event) {
        boolean show = event.status == SocketConnectEvent.ConnectStatus.DISCONNECTED
                || event.status == SocketConnectEvent.ConnectStatus.CONNECTING;
        showTopTip(show);
    }

    public static void open(Activity activity) {
        Intent intent = new Intent(activity, VideoChatCreateRoomActivity.class);
        activity.startActivity(intent);
    }
}
