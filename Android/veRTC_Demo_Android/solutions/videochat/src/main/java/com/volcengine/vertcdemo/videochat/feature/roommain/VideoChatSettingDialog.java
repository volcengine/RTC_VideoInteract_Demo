package com.volcengine.vertcdemo.videochat.feature.roommain;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.volcengine.vertcdemo.common.BaseDialog;
import com.volcengine.vertcdemo.common.IAction;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.annotation.CameraStatus;
import com.volcengine.vertcdemo.core.annotation.MicStatus;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochat.bean.InteractChangedBroadcast;
import com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo;
import com.volcengine.vertcdemo.videochat.core.VideoChatDataManager;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTCManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VideoChatSettingDialog extends BaseDialog implements View.OnClickListener {

    private ImageView mBgmBtn;
    private TextView mBgmTv;
    private ImageView mMicBtn;
    private ImageView mCameraBtn;
    private ImageView mCameraSwitchBtn;
    private String mRoomId;
    IAction mBgmBtnAction;

    public VideoChatSettingDialog(@NonNull Context context, String roomId, IAction bgmBtnAction) {
        super(context);
        this.mRoomId = roomId;
        this.mBgmBtnAction = bgmBtnAction;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.layout_video_chat_setting_dialog);
        super.onCreate(savedInstanceState);
        initUI();
    }

    private void initUI() {
        mBgmBtn = findViewById(R.id.bgm_iv);
        mBgmBtn.setOnClickListener(this);
        mBgmTv = findViewById(R.id.bgm_tv);
        VideoChatUserInfo selfUser = VideoChatDataManager.ins().selfUserInfo;
        String selfUid = selfUser == null ? null : selfUser.userId;
        VideoChatUserInfo hostUser = VideoChatDataManager.ins().hostUserInfo;
        String hostUid = hostUser == null ? null : hostUser.userId;
        if (!TextUtils.isEmpty(selfUid)) {
            boolean isHost = TextUtils.equals(selfUid, hostUid);
            mBgmBtn.setVisibility(isHost ? View.VISIBLE : View.GONE);
            mBgmTv.setVisibility(isHost ? View.VISIBLE : View.GONE);
        }
        mMicBtn = findViewById(R.id.mic_iv);
        mMicBtn.setOnClickListener(this);
        mCameraBtn = findViewById(R.id.camera_iv);
        mCameraBtn.setOnClickListener(this);
        mCameraSwitchBtn = findViewById(R.id.switch_camera_iv);
        mCameraSwitchBtn.setOnClickListener(this);
        updateCameraAndMicView();
    }

    @Override
    public void show() {
        super.show();
        SolutionDemoEventManager.register(this);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        SolutionDemoEventManager.unregister(this);
    }

    private void updateCameraAndMicView() {
        mCameraBtn.setImageResource(VideoChatRTCManager.ins().isCameraOn() ? R.drawable.camera_on : R.drawable.camera_off_red);
        mMicBtn.setImageResource(VideoChatRTCManager.ins().isMicOn() ? R.drawable.mic_on : R.drawable.mic_off_red);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bgm_iv) {
            if (mBgmBtnAction != null) {
                mBgmBtnAction.act(null);
            }
            cancel();
        } else if (id == R.id.switch_camera_iv) {
            VideoChatRTCManager.ins().switchCamera();
        } else if (id == R.id.mic_iv) {
            VideoChatRTCManager.ins().turnOnMic();
            updateMediaStatus();
            cancel();
        } else if (id == R.id.camera_iv) {
            VideoChatRTCManager.ins().turnOnCamera();
            updateMediaStatus();
            cancel();
        }
        updateCameraAndMicView();
    }

    private void updateMediaStatus() {
        if (!TextUtils.isEmpty(mRoomId)) {
            int micStatus = VideoChatRTCManager.ins().isMicOn() ? MicStatus.ON : MicStatus.OFF;
            int cameraStatus = VideoChatRTCManager.ins().isCameraOn() ? CameraStatus.ON : CameraStatus.OFF;
            VideoChatRTCManager.ins().getRTSClient().updateMediaStatus(mRoomId, SolutionDataManager.ins().getUserId(), micStatus, cameraStatus, null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInteractChangedBroadcast(InteractChangedBroadcast event) {
        if (!event.isStart && TextUtils.equals(SolutionDataManager.ins().getUserId(), event.userInfo.userId)) {
            dismiss();
        }
    }
}
