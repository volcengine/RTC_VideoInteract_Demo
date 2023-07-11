// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.feature.roomlist;

import static com.volcengine.vertcdemo.core.net.rts.RTSInfo.KEY_RTS;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vertcdemo.joinrtsparams.bean.JoinRTSRequest;
import com.vertcdemo.joinrtsparams.common.JoinRTSManager;
import com.volcengine.vertcdemo.common.IAction;
import com.volcengine.vertcdemo.common.SolutionBaseActivity;
import com.volcengine.vertcdemo.common.SolutionToast;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.AppTokenExpiredEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.ServerResponse;
import com.volcengine.vertcdemo.core.net.rts.RTSBaseClient;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.utils.AppUtil;
import com.volcengine.vertcdemo.utils.DebounceClickListener;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochat.bean.GetActiveRoomListEvent;
import com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo;
import com.volcengine.vertcdemo.videochat.core.Constants;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTCManager;
import com.volcengine.vertcdemo.videochat.databinding.ActivityVideoChatListBinding;
import com.volcengine.vertcdemo.videochat.feature.createroom.VideoChatCreateRoomActivity;
import com.volcengine.vertcdemo.videochat.feature.roommain.VideoChatRoomMainActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * 视频互动房间列表页面。
 */
public class VideoChatListActivity extends SolutionBaseActivity {

    private static final String TAG = "VideoChatListActivity";

    private ActivityVideoChatListBinding mViewBinding;

    private RTSInfo mRTSInfo;

    private final IAction<VideoChatRoomInfo> mOnClickRoomInfo = roomInfo
            -> VideoChatRoomMainActivity.openFromList(VideoChatListActivity.this, roomInfo);

    private final VideoChatRoomListAdapter mVoiceChatRoomListAdapter = new VideoChatRoomListAdapter(mOnClickRoomInfo);

    private final IRequestCallback<GetActiveRoomListEvent> mRequestRoomList =
            new IRequestCallback<GetActiveRoomListEvent>() {
                @Override
                public void onSuccess(GetActiveRoomListEvent data) {
                    if (isFinishing()){
                        return;
                    }
                    setRoomList(data.roomList);
                }

                @Override
                public void onError(int errorCode, String message) {
                    SolutionToast.show(message);
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityVideoChatListBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());
        
        initRTSInfo();

        mViewBinding.videoChatListTitleBarLayout.setLeftBack(v -> finish());
        mViewBinding.videoChatListTitleBarLayout.setTitle(R.string.video_chat);
        mViewBinding.videoChatListTitleBarLayout.setRightRefresh(DebounceClickListener.create(v -> requestRoomList()));
        
        mViewBinding.videoChatListCreateRoom.setOnClickListener(DebounceClickListener.create((v) -> onClickCreateRoom()));

        LinearLayoutManager manager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mViewBinding.videoChatListRv.setLayoutManager(manager);
        mViewBinding.videoChatListRv.setAdapter(mVoiceChatRoomListAdapter);

        initRTC();
    }

    /**
     * 获取RTS信息
     */
    private void initRTSInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mRTSInfo = intent.getParcelableExtra(KEY_RTS);
        if (mRTSInfo == null || !mRTSInfo.isValid()) {
            finish();
        }
    }

    /**
     * 初始化RTC。
     */
    private void initRTC() {
        VideoChatRTCManager.ins().initEngine(mRTSInfo);
        RTSBaseClient rtsClient = VideoChatRTCManager.ins().getRTSClient();
        if (rtsClient == null) {
            finish();
            return;
        }
        rtsClient.login(mRTSInfo.rtsToken, (resultCode, message) -> {
            if (resultCode == RTSBaseClient.LoginCallBack.SUCCESS) {
                requestRoomList();
            } else {
                SolutionToast.show("Login Rtm Fail Error:" + resultCode + ",Message:" + message);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(AppTokenExpiredEvent event) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoChatRTCManager.ins().getRTSClient().removeAllEventListener();
        VideoChatRTCManager.ins().getRTSClient().logout();
        VideoChatRTCManager.ins().destroyEngine();
    }

    @Override
    protected boolean onMicrophonePermissionClose() {
        Log.d(TAG, "onMicrophonePermissionClose");
        finish();
        return true;
    }

    @Override
    protected boolean onCameraPermissionClose() {
        Log.d(TAG, "onCameraPermissionClose");
        finish();
        return true;
    }

    /**
     * 请求视频房间列表。
     */
    private void requestRoomList() {
        VideoChatRTCManager.ins().getRTSClient().requestClearUser(null);
        VideoChatRTCManager.ins().getRTSClient().getActiveRoomList(mRequestRoomList);
    }

    /**
     * 设置房间列表。
     * @param roomList 视频房间列表，详见 VideoChatRoomInfo。
     */
    private void setRoomList(List<VideoChatRoomInfo> roomList) {
        mVoiceChatRoomListAdapter.setRoomList(roomList);
        mViewBinding.videoChatEmptyListView.setVisibility((roomList == null || roomList.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    /**
     * 创建视频聊天房间。
     */
    private void onClickCreateRoom() {
        VideoChatCreateRoomActivity.open(this);
    }

    @Keep
    @SuppressWarnings("unused")
    public static void prepareSolutionParams(Activity activity, IAction<Object> doneAction) {
        Log.d(TAG, "prepareSolutionParams() invoked");
        IRequestCallback<ServerResponse<RTSInfo>> callback = new IRequestCallback<ServerResponse<RTSInfo>>() {
                        @Override
            public void onSuccess(ServerResponse<RTSInfo> response) {
                RTSInfo data = response == null ? null : response.getData();
                if (data == null || !data.isValid()) {
                    onError(-1, "");
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClass(AppUtil.getApplicationContext(), VideoChatListActivity.class);
                intent.putExtra(KEY_RTS, data);
                activity.startActivity(intent);
                if (doneAction != null) {
                    doneAction.act(null);
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                if (doneAction != null) {
                    doneAction.act(null);
                }
            }
        };
        JoinRTSRequest request = new JoinRTSRequest(Constants.SOLUTION_NAME_ABBR, SolutionDataManager.ins().getToken());
        JoinRTSManager.setAppInfoAndJoinRTM(request, callback);
    }
}
