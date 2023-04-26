// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.videochat.feature.roommain;

import static com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo.ROOM_STATUS_CHATTING;
import static com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo.ROOM_STATUS_LIVING;
import static com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo.ROOM_STATUS_PK_ING;
import static com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo.USER_STATUS_INTERACT;
import static com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo.USER_STATUS_NORMAL;
import static com.volcengine.vertcdemo.videochat.core.VideoChatDataManager.INTERACT_STATUS_NORMAL;
import static com.volcengine.vertcdemo.videochat.core.VideoChatDataManager.SEAT_STATUS_UNLOCKED;
import static com.volcengine.vertcdemo.videochat.feature.roommain.AudienceManagerDialog.SEAT_ID_BY_SERVER;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.volcengine.vertcdemo.common.GsonUtils;
import com.volcengine.vertcdemo.common.InputTextDialogFragment;
import com.volcengine.vertcdemo.common.SolutionBaseActivity;
import com.volcengine.vertcdemo.common.SolutionCommonDialog;
import com.volcengine.vertcdemo.common.SolutionToast;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.annotation.CameraStatus;
import com.volcengine.vertcdemo.core.annotation.MicStatus;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.AppTokenExpiredEvent;
import com.volcengine.vertcdemo.core.net.ErrorTool;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.eventbus.SDKReconnectToRoomEvent;
import com.volcengine.vertcdemo.utils.IMEUtils;
import com.volcengine.vertcdemo.utils.Utils;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochat.bean.AnchorInfo;
import com.volcengine.vertcdemo.videochat.bean.AnchorPkFinishEvent;
import com.volcengine.vertcdemo.videochat.bean.AudienceApplyEvent;
import com.volcengine.vertcdemo.videochat.bean.AudienceChangedEvent;
import com.volcengine.vertcdemo.videochat.bean.ChatMessageEvent;
import com.volcengine.vertcdemo.videochat.bean.ClearUserEvent;
import com.volcengine.vertcdemo.videochat.bean.CloseChatRoomEvent;
import com.volcengine.vertcdemo.videochat.bean.FinishLiveEvent;
import com.volcengine.vertcdemo.videochat.bean.InteractChangedEvent;
import com.volcengine.vertcdemo.videochat.bean.InteractInfo;
import com.volcengine.vertcdemo.videochat.bean.InteractReplyEvent;
import com.volcengine.vertcdemo.videochat.bean.InteractResultEvent;
import com.volcengine.vertcdemo.videochat.bean.InviteAnchorEvent;
import com.volcengine.vertcdemo.videochat.bean.InviteAnchorReplyEvent;
import com.volcengine.vertcdemo.videochat.bean.JoinRoomEvent;
import com.volcengine.vertcdemo.videochat.bean.MediaChangedEvent;
import com.volcengine.vertcdemo.videochat.bean.ReceivedInteractEvent;
import com.volcengine.vertcdemo.videochat.bean.ReplyAnchorsEvent;
import com.volcengine.vertcdemo.videochat.bean.VideoChatResponse;
import com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo;
import com.volcengine.vertcdemo.videochat.bean.VideoChatSeatInfo;
import com.volcengine.vertcdemo.videochat.bean.VideoChatUserInfo;
import com.volcengine.vertcdemo.videochat.core.VideoChatDataManager;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTCManager;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTSClient;
import com.volcengine.vertcdemo.videochat.databinding.ActivityVideoChatMainBinding;
import com.volcengine.vertcdemo.videochat.event.AudioStatsEvent;
import com.volcengine.vertcdemo.videochat.feature.roommain.fragment.VideoAnchorPkFragment;
import com.volcengine.vertcdemo.videochat.feature.roommain.fragment.VideoChatRoomFragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 视频互动房间主页页面。
 */
public class VideoChatRoomMainActivity extends SolutionBaseActivity {

    private static final String TAG = "VideoChatRoom";
    private static final String TAG_FRAGMENT_CHAT_ROOM = "fragment_chat_room";
    private static final String TAG_FRAGMENT_ANCHOR_PK = "fragment_anchor_pk";
    private static final String REFER_KEY = "refer";
    private static final String REFER_FROM_CREATE = "create";
    private static final String REFER_FROM_LIST = "list";
    private static final String REFER_EXTRA_ROOM = "extra_room";
    private static final String REFER_EXTRA_USER = "extra_user";
    private static final String REFER_EXTRA_CREATE_JSON = "extra_create_json";

    private ActivityVideoChatMainBinding mViewBinding;

    private boolean mAgreeHostInvite;

    private ChatAdapter mChatAdapter;
    // 是否由自己关闭的主播连线。
    private boolean mIsFinishAnchorLinkBySelf = false;
    private boolean isLeaveByKickOut = false;
    private VideoChatRoomFragment mVideoChatFragment;
    private VideoAnchorPkFragment mVideoPkFragment;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final IRequestCallback<JoinRoomEvent> mJoinCallback = new IRequestCallback<JoinRoomEvent>() {
        @Override
        public void onSuccess(JoinRoomEvent data) {
            data.isFromCreate = true;
            initViewWithData(data);
        }

        @Override
        public void onError(int errorCode, String message) {
            onArgsError(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
        }
    };

    private final AudienceManagerDialog.ICloseChatRoom mCloseChatRoom = new AudienceManagerDialog.ICloseChatRoom() {
        @Override
        public void closeChatRoom() {
            SolutionCommonDialog dialog = new SolutionCommonDialog(VideoChatRoomMainActivity.this);
            dialog.setMessage(getString(R.string.turn_off_chat_room));
            dialog.setPositiveBtnText(R.string.ok);
            dialog.setPositiveListener(v -> {
                VideoChatRTCManager.ins().getRTSClient().closeChatRoom(
                        VideoChatDataManager.ins().hostUserInfo.roomId,
                        VideoChatDataManager.ins().hostUserInfo.userId,
                        new IRequestCallback<VideoChatResponse>() {
                            @Override
                            public void onSuccess(VideoChatResponse data) {
                                mViewBinding.bizFl.post(VideoChatRoomMainActivity.this::closeChat);
                            }

                            @Override
                            public void onError(int errorCode, String message) {
                                Log.i(TAG, "closeChatRoom onError errorCode:" + errorCode + ",message:" + message);
                            }
                        });
                dialog.dismiss();
            });
            dialog.setNegativeListener(v -> dialog.dismiss());
            dialog.show();
        }
    };

    private final IRequestCallback<JoinRoomEvent> mReconnectCallback = new IRequestCallback<JoinRoomEvent>() {
        @Override
        public void onSuccess(JoinRoomEvent data) {
            data.isFromCreate = false;
            initViewWithData(data);
        }

        @Override
        public void onError(int errorCode, String message) {
            SolutionToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
            finish();
        }
    };

    private final VideoChatBottomOptionLayout.IBottomOptions mIBottomOptions = new VideoChatBottomOptionLayout.IBottomOptions() {
        @Override
        public void onInputClick() {
            openInput();
        }

        @Override
        public void onPkClick() {
            if (getRoomInfo().status == ROOM_STATUS_CHATTING) {
                SolutionToast.show(R.string.host_busy);
                return;
            }
            if (VideoChatDataManager.ins().selfInviteStatus == VideoChatDataManager.INTERACT_STATUS_INVITING_CHAT) {
                SolutionToast.show(R.string.host_busy);
                return;
            }
            if (VideoChatDataManager.ins().selfInviteStatus == VideoChatDataManager.INTERACT_STATUS_INVITING_PK) {
                SolutionToast.show(R.string.video_sent_invitation);
                return;
            }
            if (getRoomInfo().status == ROOM_STATUS_PK_ING) {
                SolutionCommonDialog dialog = new SolutionCommonDialog(VideoChatRoomMainActivity.this);
                dialog.setMessage(getString(R.string.video_confirm_disconnecting));
                dialog.setPositiveBtnText(R.string.ok);
                dialog.setPositiveListener(v -> {
                    mIsFinishAnchorLinkBySelf = true;
                    VideoChatRTCManager.ins().getRTSClient().finishAnchorInteract(getRoomInfo().roomId,
                            new IRequestCallback<VideoChatResponse>() {
                                @Override
                                public void onSuccess(VideoChatResponse data) {
                                    closePk();
                                    dialog.dismiss();
                                }

                                @Override
                                public void onError(int errorCode, String message) {
                                    dialog.dismiss();
                                }
                            });
                });
                dialog.setNegativeListener(v -> dialog.dismiss());
                dialog.show();
                return;
            }
            RemoteAnchorsDialog dialog = new RemoteAnchorsDialog(VideoChatRoomMainActivity.this);
            dialog.show();
        }

        @Override
        public void onInteractClick() {
            boolean isHost = getSelfUserInfo() != null && getSelfUserInfo().isHost();
            if (getSelfInteractStatus() == VideoChatDataManager.INTERACT_STATUS_INVITING_PK) {
                SolutionToast.show(R.string.host_busy);
                return;
            }
            if (VideoChatDataManager.ins().selfInviteStatus == VideoChatDataManager.INTERACT_STATUS_INVITING_CHAT && !isHost) {
                SolutionToast.show(R.string.application_sent_host);
                return;
            }
            if (getRoomInfo().status == ROOM_STATUS_PK_ING) {
                SolutionToast.show(R.string.video_host_in_room_title);
                return;
            }
            if (isHost) {
                AudienceManagerDialog dialog = new AudienceManagerDialog(VideoChatRoomMainActivity.this);
                dialog.setData(getRoomInfo().roomId, VideoChatDataManager.ins().hasNewApply(), SEAT_ID_BY_SERVER);
                dialog.setICloseChatRoom(mCloseChatRoom);
                dialog.show();
                return;
            }
            int selfStatus = getSelfUserInfo() != null ? getSelfUserInfo().userStatus : USER_STATUS_NORMAL;
            if (selfStatus == VideoChatUserInfo.USER_STATUS_APPLYING) {
                SolutionToast.show(R.string.application_sent_host);
            } else if (selfStatus == USER_STATUS_INTERACT) {
                SolutionToast.show(R.string.video_chat_on_mic);
            } else if (selfStatus == USER_STATUS_NORMAL) {
                SeatOptionDialog dialog = new SeatOptionDialog(VideoChatRoomMainActivity.this);
                VideoChatSeatInfo seatInfo = new VideoChatSeatInfo();
                seatInfo.userInfo = null;
                seatInfo.seatIndex = SEAT_ID_BY_SERVER;
                seatInfo.status = SEAT_STATUS_UNLOCKED;
                dialog.setData(getRoomInfo().roomId, seatInfo, getSelfUserInfo().userRole, getSelfUserInfo().userStatus);
                dialog.setICloseChatRoom(mCloseChatRoom);
                dialog.show();
            }
        }

        public void onBGMClick() {
            VideoChatBGMSettingDialog dialog = new VideoChatBGMSettingDialog(VideoChatRoomMainActivity.this);
            dialog.setData(VideoChatDataManager.ins().getBGMOpening(),
                    VideoChatDataManager.ins().getBGMVolume(),
                    VideoChatDataManager.ins().getUserVolume());
            dialog.show();
        }

        @Override
        public void onEffectClick() {
            VideoChatRTCManager.ins().openEffectDialog(VideoChatRoomMainActivity.this);
        }

        @Override
        public void onSettingsClick() {
            new VideoChatSettingDialog(VideoChatRoomMainActivity.this, getRoomInfo().roomId, obj -> onBGMClick()).show();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityVideoChatMainBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        mViewBinding.videoChatMainRoot.setOnClickListener((v) -> closeInput());
        mViewBinding.leaveIv.setOnClickListener(v -> attemptLeave());
        mViewBinding.videoChatMainBottomOption.setOptionCallback(mIBottomOptions);

        mChatAdapter = new ChatAdapter();
        mViewBinding.videoChatMainChatRv.setLayoutManager(new LinearLayoutManager(VideoChatRoomMainActivity.this, RecyclerView.VERTICAL, false));
        mViewBinding.videoChatMainChatRv.setAdapter(mChatAdapter);
        mViewBinding.videoChatMainChatRv.setOnClickListener((v) -> closeInput());

        closeInput();
        if (!checkArgs()) {
            onArgsError(getString(R.string.joining_room_failed));
        }

        SolutionDemoEventManager.register(this);
    }

    /**
     * 校验视频连麦房间参数。
     * @return
     */
    private boolean checkArgs() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        String refer = intent.getStringExtra(REFER_KEY);
        String roomJson = intent.getStringExtra(REFER_EXTRA_ROOM);
        String selfJson = intent.getStringExtra(REFER_EXTRA_USER);
        VideoChatRoomInfo roomInfo = GsonUtils.gson().fromJson(roomJson, VideoChatRoomInfo.class);
        VideoChatUserInfo selfInfo = GsonUtils.gson().fromJson(selfJson, VideoChatUserInfo.class);
        if (TextUtils.equals(refer, REFER_FROM_LIST)) {
            VideoChatRTCManager.ins().getRTSClient().requestJoinRoom(selfInfo.userName, roomInfo.roomId, mJoinCallback);
            return true;
        } else if (TextUtils.equals(refer, REFER_FROM_CREATE)) {
            String createJson = intent.getStringExtra(REFER_EXTRA_CREATE_JSON);
            if (TextUtils.isEmpty(createJson)) {
                return false;
            }
            JoinRoomEvent createResponse = GsonUtils.gson().fromJson(createJson, JoinRoomEvent.class);
            initViewWithData(createResponse);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 初始化视图界面。
     * @param data 加入房间事件，详见 JoinRoomEvent。
     */

    /**
     * Initialize view.
     * @param data Join room event, see JoinRoomEvent for details.
     */

    private void initViewWithData(JoinRoomEvent data) {
        mViewBinding.videoChatMainAudienceNum.setText(String.valueOf(data.audienceCount + 1));
        VideoChatDataManager.ins().roomInfo = data.roomInfo;
        VideoChatDataManager.ins().hostUserInfo = data.hostInfo;
        VideoChatDataManager.ins().selfUserInfo = data.userInfo;

        if (data.isFromCreate) {
            VideoChatRTCManager.ins().joinRoom(data.roomInfo.roomId, data.rtcToken, data.userInfo.userId, data.userInfo.isHost());
        }
        String hostNamePrefix = getRoomInfo().hostUserName.substring(0, 1);
        mViewBinding.videoChatMainTitlePrefix.setText(hostNamePrefix);
        mViewBinding.videoChatMainTitleName.setText(getRoomInfo().hostUserName);
        mViewBinding.localNameTv.setText(hostNamePrefix);
        mViewBinding.videoChatMainTitleId.setText("ID:" + getRoomInfo().roomId);
        mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(data.roomInfo.status, data.userInfo.userRole, data.userInfo.userStatus);

        int roomStatus = data.roomInfo.status;
        if (roomStatus == ROOM_STATUS_CHATTING) {
            // 已经在聊天中，自己只能是观众。
            boolean isSelfHost = TextUtils.equals(data.userInfo.userId, getHostUserInfo().userId);
            boolean isInteract = data.userInfo.userStatus == USER_STATUS_INTERACT;
            VideoChatRTCManager.ins().startAudioCapture(isSelfHost || isInteract);
            VideoChatRTCManager.ins().startMuteAudio(!data.userInfo.isMicOn());
            if (isSelfHost || isInteract) {
                VideoChatRTCManager.ins().startVideoCapture(true);
            }
            startChatRoom(data);
        } else if (roomStatus == VideoChatRoomInfo.ROOM_STATUS_PK_ING) {
            // 已经PK中，自己只能是观众。
            if (data.anchorList == null || data.anchorList.size() != 2) {
                Log.i(TAG, "anchors or hostInfo is null!!");
                finish();
                return;
            }
            AnchorInfo peerAnchor = null;
            AnchorInfo localAnchor = null;
            for (AnchorInfo info : data.anchorList) {
                if (info == null) continue;
                if (TextUtils.equals(info.userId, getHostUserInfo().userId)) {
                    localAnchor = info;
                } else {
                    peerAnchor = info;
                }
            }
            if (peerAnchor != null && localAnchor != null) {
                startPk(peerAnchor, localAnchor, null);
            }
        } else if (roomStatus == VideoChatRoomInfo.ROOM_STATUS_LIVING) {
            if (getSelfUserInfo() != null && getSelfUserInfo().isHost()) {
                VideoChatRTCManager.ins().startAudioCapture(true);
                VideoChatRTCManager.ins().startMuteAudio(!data.userInfo.isMicOn());
                VideoChatRTCManager.ins().startVideoCapture(true);
            }
            setLocalLive();
        }
    }

    /**
     * 开始视频聊天室。
     * @param data 加入房间事件，详见 JoinRoomEvent。
     */
    private void startChatRoom(JoinRoomEvent data) {
        if (isFinishing()) {
            return;
        }
        mVideoChatFragment = new VideoChatRoomFragment();
        mVideoChatFragment.setICloseChatRoom(mCloseChatRoom);
        Bundle args = new Bundle();
        args.putParcelable(VideoChatRoomFragment.KEY_JOIN_DATA, data);
        mVideoChatFragment.setArguments(args);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.biz_fl, mVideoChatFragment, TAG_FRAGMENT_CHAT_ROOM)
                .commit();
        mViewBinding.localLiveFl.removeAllViews();
        mViewBinding.localAnchorNameFl.setVisibility(View.GONE);
    }

    /**
     * 设置本地直播配置。
     */
    private void setLocalLive() {
        TextureView renderView = VideoChatRTCManager.ins().getUserRenderView(getHostUserInfo().userId);
        Utils.removeFromParent(renderView);
        if (getSelfUserInfo() != null && getSelfUserInfo().isHost()) {
            VideoChatRTCManager.ins().setLocalVideoView(renderView);
        } else {
            VideoChatRTCManager.ins().setRemoteVideoView(getHostUserInfo().userId, getHostUserInfo().roomId, renderView);
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mViewBinding.localLiveFl.addView(renderView, params);
        if (getHostUserInfo().camera == VideoChatUserInfo.CAMERA_STATUS_OFF) {
            mViewBinding.localAnchorNameFl.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onBackPressed() {
        attemptLeave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeInput();
        SolutionDemoEventManager.unregister(this);
        VideoChatRTCManager.ins().startVideoCapture(false);
        VideoChatRTCManager.ins().startAudioCapture(false);
        VideoChatRTCManager.ins().leaveRoom();
        VideoChatRTCManager.ins().stopAudioMixing();
        if (getSelfUserInfo() == null || getRoomInfo() == null) {
            return;
        }

        VideoChatRTSClient rtsClient = VideoChatRTCManager.ins().getRTSClient();
        if (!isLeaveByKickOut && rtsClient != null) {
            if (getSelfUserInfo().isHost()) {
                rtsClient.requestFinishLive(getRoomInfo().roomId, null);
            } else {
                rtsClient.requestLeaveRoom(getRoomInfo().roomId, null);
            }
        }
        VideoChatDataManager.ins().clearData();
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
     * 参数错误回调。
     * @param message 错误信息。
     */
    private void onArgsError(String message) {
        SolutionCommonDialog dialog = new SolutionCommonDialog(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveListener((v) -> {
            finish();
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * 尝试离开房间。
     */
    private void attemptLeave() {
        final VideoChatUserInfo selfUserInfo = getSelfUserInfo();
        if (selfUserInfo == null || !selfUserInfo.isHost()) {
            if (selfUserInfo != null && selfUserInfo.userStatus == USER_STATUS_INTERACT) {
                SolutionToast.show(R.string.video_chat_you_off_mic);
            }
            finish();
            return;
        }
        SolutionCommonDialog dialog = new SolutionCommonDialog(this);
        dialog.setMessage(getString(R.string.video_chat_end_live_alert));
        dialog.setPositiveBtnText(R.string.video_chat_end_live);
        dialog.setPositiveListener((v) -> {
            finish();
            dialog.dismiss();
        });
        dialog.setNegativeListener((v) -> dialog.dismiss());
        dialog.show();
    }

    /**
     * 打开聊天消息输入对话框。
     */
    public void openInput() {
        InputTextDialogFragment.showInput(getSupportFragmentManager(), (this::onSendMessage));
    }

    /**
     * 发送聊天消息。
     * @param fragment 输入消息对话框。
     * @param message 聊天消息。
     */
    private void onSendMessage(InputTextDialogFragment fragment, String message) {
        if (getRoomInfo() == null) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            SolutionToast.show(getString(R.string.send_empty_message_cannot_be_empty));
            return;
        }
        closeInput();
        onReceivedMessage(String.format("%s : %s", SolutionDataManager.ins().getUserName(), message));
        try {
            message = URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        VideoChatRTSClient rtsClient = VideoChatRTCManager.ins().getRTSClient();
        if (rtsClient != null) {
            rtsClient.sendMessage(getRoomInfo().roomId, message, null);
        }
        fragment.dismiss();
    }

    /**
     * 关闭聊天消息发送。
     */
    public void closeInput() {
        IMEUtils.closeIME(mViewBinding.videoChatMainChatRv);
    }

    /**
     * 收到聊天消息回调。
     * @param message 聊天消息。
     */
    private void onReceivedMessage(String message) {
        mChatAdapter.addChatMsg(message);
        mViewBinding.videoChatMainChatRv.post(() -> mViewBinding.videoChatMainChatRv.smoothScrollToPosition(mChatAdapter.getItemCount()));
    }

    /**
     * 显示置顶提示。
     */
    private void showTopTip() {
        mViewBinding.mainDisconnectTip.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏置顶提示。
     */
    private void hideTopTip() {
        mViewBinding.mainDisconnectTip.setVisibility(View.GONE);
    }

    /**
     * 观众状态监听。
     * @param event 观众状态改变事件，详见 AudienceChangedEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudienceChangedBroadcast(AudienceChangedEvent event) {
        String suffix = event.isJoin
                ? getString(R.string.video_chat_join_room)
                : getString(R.string.video_chat_left_room);
        onReceivedMessage(event.userInfo.userName + suffix);
        mViewBinding.videoChatMainAudienceNum.setText(String.valueOf(event.audienceCount + 1));
    }

    /**
     * 直播结束事件回调。
     * @param event 直播结束事件，详见 FinishLiveEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFinishLiveBroadcast(FinishLiveEvent event) {
        if (getRoomInfo() == null || !TextUtils.equals(event.roomId, getRoomInfo().roomId)) {
            return;
        }
        String message = null;
        boolean isHost = getSelfUserInfo() != null && getSelfUserInfo().isHost();
        if (event.type == FinishLiveEvent.FINISH_TYPE_AGAINST) {
            message = getString(R.string.closed_terms_service);
        } else if (event.type == FinishLiveEvent.FINISH_TYPE_TIMEOUT && isHost) {
            message = getString(R.string.minutes_error_message);
        } else if (!isHost) {
            message = getString(R.string.live_ended);
        }
        if (!TextUtils.isEmpty(message)) {
            SolutionToast.show(message);
        }
        finish();
    }

    /**
     * 聊天室关闭事件回调。
     * @param event 关闭聊天室事件，详见 CloseChatRoomEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloseChatRoomBroadcast(CloseChatRoomEvent event) {
        boolean chatVisible = mVideoChatFragment != null && mVideoChatFragment.isVisible();
        Log.i(TAG, "onCloseChatRoomBroadcast event:" + event + ",chatVisible:" + chatVisible);
        if (!chatVisible) {
            return;
        }
        if (getRoomInfo() == null || !TextUtils.equals(event.roomId, getRoomInfo().roomId)) {
            return;
        }
        final VideoChatUserInfo selfUserInfo = getSelfUserInfo();
        if (selfUserInfo.userStatus == USER_STATUS_INTERACT) {
            SolutionToast.show(getString(R.string.video_host_disconnected));
        }
        mViewBinding.bizFl.post(VideoChatRoomMainActivity.this::closeChat);
        if (!selfUserInfo.isHost()) {
            VideoChatRTCManager.ins().startMuteAudio(true);
        }
    }

    /**
     * 关闭聊天室。
     */
    private void closeChat() {
        if (mVideoChatFragment == null || !mVideoChatFragment.isVisible()) {
            Log.i(TAG, "mVideoChatFragment un visible");
            return;
        }
        getRoomInfo().status = ROOM_STATUS_LIVING;
        getSelfUserInfo().userStatus = USER_STATUS_NORMAL;
        VideoChatDataManager.ins().selfInviteStatus = INTERACT_STATUS_NORMAL;
        mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(ROOM_STATUS_LIVING, getSelfUserInfo().userRole, USER_STATUS_NORMAL);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment videoChatFragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT_CHAT_ROOM);
        if (videoChatFragment == mVideoChatFragment && videoChatFragment != null) {
            fragmentManager.beginTransaction()
                    .remove(videoChatFragment)
                    .commitAllowingStateLoss();
            Log.i(TAG, "closeChat remove ChatFragment");
            mViewBinding.bizFl.removeAllViews();
            mVideoChatFragment = null;
        }
        setLocalLive();
    }

    /**
     * 聊天消息回调。
     * @param event 聊天消息事件，详见 ChatMessageEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageBroadcast(ChatMessageEvent event) {
        if (TextUtils.equals(event.userInfo.userId, getSelfUserInfo().userId)) {
            return;
        }
        String message;
        try {
            message = URLDecoder.decode(event.message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            message = event.message;
        }
        onReceivedMessage(String.format("%s : %s", event.userInfo.userName, message));
    }

    /**
     * 连麦状态改变事件回调。
     * @param event 连麦状态事件，详见 InteractChangedEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInteractChangedBroadcast(InteractChangedEvent event) {
        Log.i(TAG, "onInteractChangedBroadcast:" + event + ",mAgreeHostInvite:" + mAgreeHostInvite);
        if (mAgreeHostInvite) {
            return;
        }
        VideoChatSeatInfo info = new VideoChatSeatInfo();
        info.userInfo = event.userInfo;
        info.status = SEAT_STATUS_UNLOCKED;
        String message = getString(event.isStart
                        ? R.string.video_chat_xxx_you_on_mic
                        : R.string.video_chat_xxx_you_off_mic, event.userInfo.userName);
        onReceivedMessage(message);
        boolean isSelf = TextUtils.equals(SolutionDataManager.ins().getUserId(), event.userInfo.userId);
        if (isSelf) {
            getSelfUserInfo().userStatus = event.isStart ? USER_STATUS_INTERACT : USER_STATUS_NORMAL;
        }
        mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(ROOM_STATUS_CHATTING, getSelfUserInfo().userRole, getSelfUserInfo().userStatus);
        if (event.isStart && getRoomInfo().status != ROOM_STATUS_CHATTING) {
            getRoomInfo().status = ROOM_STATUS_CHATTING;
            VideoChatDataManager.ins().selfInviteStatus = INTERACT_STATUS_NORMAL;
            JoinRoomEvent response = new JoinRoomEvent();
            response.roomInfo = getRoomInfo();
            response.userInfo = getSelfUserInfo();
            response.hostInfo = getHostUserInfo();
            Map<Integer, VideoChatSeatInfo> seatMap = new ArrayMap<>(1);
            seatMap.put(event.seatId, info);
            response.seatMap = seatMap;
            startChatRoom(response);
            mViewBinding.bizFl.post(() -> {
                mVideoChatFragment.onInteractChangedBroadcast(event);
            });
        }
    }

    /**
     * 邀请连麦对话框。
     */
    private SolutionCommonDialog mInviteInteractDialog;
    private final Runnable mCloseInviteInteractDialogTask = () -> {
        if (mInviteInteractDialog != null) {
            mInviteInteractDialog.dismiss();
        }
    };

    /**
     * 收到连麦事件回调。
     * @param event 收到的连麦事件，详见 ReceivedInteractEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceivedInteractBroadcast(ReceivedInteractEvent event) {
        int oldRoomStatus = getRoomInfo().status;
        Log.i(TAG, "onReceivedInteractBroadcast:" + event + ",oldRoomStatus:" + oldRoomStatus);
        mInviteInteractDialog = new SolutionCommonDialog(this);
        mInviteInteractDialog.setMessage(getString(R.string.host_invited_you_on_mic));
        mInviteInteractDialog.setPositiveBtnText(R.string.accept);
        mInviteInteractDialog.setNegativeBtnText(R.string.decline);
        mInviteInteractDialog.setPositiveListener((v) -> {
            mAgreeHostInvite = true;
            VideoChatRTCManager.ins().getRTSClient().replyInvite(
                    getRoomInfo().roomId,
                    VideoChatDataManager.REPLY_TYPE_ACCEPT,
                    event.seatId,
                    new IRequestCallback<InteractReplyEvent>() {
                        @Override
                        public void onSuccess(InteractReplyEvent data) {
                            mAgreeHostInvite = false;
                            getRoomInfo().status = ROOM_STATUS_CHATTING;
                            VideoChatDataManager.ins().selfInviteStatus = INTERACT_STATUS_NORMAL;
                            mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(ROOM_STATUS_CHATTING, getSelfUserInfo().userRole, USER_STATUS_INTERACT);
                            VideoChatUserInfo selfUserInfo = getSelfUserInfo();
                            selfUserInfo.userStatus = USER_STATUS_INTERACT;
                            if (oldRoomStatus == ROOM_STATUS_CHATTING) {
                                return;
                            }
                            JoinRoomEvent response = new JoinRoomEvent();
                            response.roomInfo = getRoomInfo();
                            response.userInfo = getSelfUserInfo();
                            response.hostInfo = getHostUserInfo();
                            Map<Integer, VideoChatSeatInfo> seatMap = new ArrayMap<>(1);
                            VideoChatSeatInfo seatInfo = new VideoChatSeatInfo();
                            seatInfo.userInfo = getSelfUserInfo();
                            seatInfo.seatIndex = event.seatId;
                            seatMap.put(event.seatId, seatInfo);
                            response.seatMap = seatMap;
                            startChatRoom(response);
                            mViewBinding.bizFl.post(() -> {
                                InteractChangedEvent info = new InteractChangedEvent();
                                info.isStart = true;
                                info.userInfo = getSelfUserInfo();
                                info.seatId = event.seatId;
                                mVideoChatFragment.onInteractChangedBroadcast(info);
                            });
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            mAgreeHostInvite = false;
                            VideoChatDataManager.ins().selfInviteStatus = INTERACT_STATUS_NORMAL;
                            Log.i(TAG, "replyInvite onError errorCode:" + errorCode + ",message:" + message);
                            SolutionToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
                        }
                    });
            mInviteInteractDialog.dismiss();
            mHandler.removeCallbacks(mCloseInviteInteractDialogTask);
        });
        mInviteInteractDialog.setNegativeListener((v) -> {
            VideoChatRTCManager.ins().getRTSClient().replyInvite(
                    getRoomInfo().roomId,
                    VideoChatDataManager.REPLY_TYPE_REJECT,
                    event.seatId,
                    new IRequestCallback<InteractReplyEvent>() {
                        @Override
                        public void onSuccess(InteractReplyEvent data) {
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                        }
                    });
            mInviteInteractDialog.dismiss();
            mHandler.removeCallbacks(mCloseInviteInteractDialogTask);
        });
        mInviteInteractDialog.show();
        mHandler.postDelayed(mCloseInviteInteractDialogTask, TimeUnit.SECONDS.toMillis(5));
    }

    /**
     * 观众申请事件回调。
     * @param event 观众申请事件。详见 AudienceApplyEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudienceApplyBroadcast(AudienceApplyEvent event) {
        VideoChatDataManager.ins().setNewApply(event.hasNewApply);
        mViewBinding.videoChatMainBottomOption.updateDotTip(event.hasNewApply);
    }

    /**
     * 清除用户事件回调。
     * @param event 清除用户事件，详见 ClearUserEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onClearUserBroadcast(ClearUserEvent event) {
        if (TextUtils.equals(getRoomInfo().roomId, event.roomId) &&
                TextUtils.equals(SolutionDataManager.ins().getUserId(), event.userId)) {
            SolutionToast.show(R.string.same_logged_in);
            isLeaveByKickOut = true;
            finish();
        }
    }

    /**
     * 重连进房事件回调。
     * @param event 重连进房事件，详见 VideoChatReconnectToRoomEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReconnectToRoom(SDKReconnectToRoomEvent event) {
        final String roomId = getRoomInfo().roomId;
        VideoChatRTCManager.ins().getRTSClient()
                .reconnectToServer(roomId, mReconnectCallback);
    }

    /**
     * 音频统计事件回调。
     * @param event 音频统计事件，详见 AudioStatsEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioStatsEvent(AudioStatsEvent event) {

    }

    /**
     * 连麦事件结果回调。
     * @param event 连麦结果事件，详见 InteractResultEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInteractResultBroadcast(InteractResultEvent event) {
        Log.i(TAG, "onInteractResultBroadcast event:" + event);
        VideoChatDataManager.ins().selfInviteStatus = VideoChatDataManager.INTERACT_STATUS_NORMAL;
        if (event.reply == VideoChatDataManager.REPLY_TYPE_REJECT) {
            SolutionToast.show(getString(R.string.video_xxx_declined_invitation, event.userInfo.userName));
        }
    }

    /**
     * 媒体状态改变回调。
     * @param event 媒体改变事件，详见 MediaChangedEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChangedBroadcast(MediaChangedEvent event) {
        Log.i(TAG, "MediaChangedBroadcast event:" + event);
        String hostUid = getHostUserInfo() == null ? null : getHostUserInfo().userId;
        if (TextUtils.equals(hostUid, event.userInfo.userId)) {
            getHostUserInfo().mic = event.userInfo.mic;
            getHostUserInfo().camera = event.userInfo.camera;
            if (getRoomInfo().status == ROOM_STATUS_LIVING) {
                if (event.userInfo.camera == VideoChatUserInfo.CAMERA_STATUS_OFF) {
                    mViewBinding.localLiveFl.setVisibility(View.GONE);
                    mViewBinding.localAnchorNameFl.setVisibility(View.VISIBLE);
                } else if (event.userInfo.camera == VideoChatUserInfo.CAMERA_STATUS_ON) {
                    mViewBinding.localLiveFl.setVisibility(View.VISIBLE);
                    mViewBinding.localAnchorNameFl.setVisibility(View.GONE);
                }
            }
        }
        if (mVideoChatFragment != null && mVideoChatFragment.isVisible()) {
            mVideoChatFragment.onMediaChangedBroadcast(event);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(AppTokenExpiredEvent event) {
        finish();
    }

    /**
     * 邀请主播对话框。
     */
    private SolutionCommonDialog mOnInviteAnchorDialog;

    /**
     * 关闭邀请主播对话框任务。
     */
    private final Runnable mCloseInviteAnchorDialogTask = () -> {
        if (mOnInviteAnchorDialog != null) {
            mOnInviteAnchorDialog.dismiss();
        }
    };

    /**
     * 主播连麦邀请回调.
     * @param event 邀请主播事件，详见 InviteAnchorEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInviteAnchor(InviteAnchorEvent event) {
        Log.i(TAG, "onInviteAnchor event:" + event);
        if (mOnInviteAnchorDialog != null) {
            mOnInviteAnchorDialog.dismiss();
        }
        mOnInviteAnchorDialog = new SolutionCommonDialog(this);
        mOnInviteAnchorDialog.setMessage(getString(R.string.xxx_invites_you_connect, event.fromUserName));
        mOnInviteAnchorDialog.setPositiveBtnText(R.string.accept);
        mOnInviteAnchorDialog.setNegativeBtnText(R.string.decline);
        mOnInviteAnchorDialog.setPositiveListener(v -> {
            VideoChatRTCManager.ins().getRTSClient().replyAnchor(
                    VideoChatDataManager.ins().selfUserInfo.roomId,
                    VideoChatDataManager.ins().selfUserInfo.userId,
                    event.fromRoomId, event.fromUserId,
                    1, new IRequestCallback<ReplyAnchorsEvent>() {
                        @Override
                        public void onSuccess(ReplyAnchorsEvent data) {
                            Log.i(TAG, "onInviteAnchor replyAnchor onSuccess:" + data);
                            if (data == null || data.interactInfoList == null
                                    || data.interactInfoList.size() == 0
                                    || data.interactInfoList.get(0) == null) {
                                return;
                            }
                            InteractInfo info = data.interactInfoList.get(0);
                            AnchorInfo peerAnchor = new AnchorInfo();
                            peerAnchor.roomId = info.roomId;
                            peerAnchor.userId = info.userId;
                            peerAnchor.userName = info.userName;
                            peerAnchor.mic = info.mic;
                            peerAnchor.camera = info.camera;
                            peerAnchor.audioStatusThisRoom = 1;
                            AnchorInfo localAnchor = new AnchorInfo();
                            localAnchor.mic = VideoChatRTCManager.ins().isMicOn() ? MicStatus.ON : MicStatus.OFF;
                            localAnchor.camera = VideoChatRTCManager.ins().isCameraOn() ? CameraStatus.ON : CameraStatus.OFF;
                            startPk(peerAnchor, localAnchor, info.token);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            Log.i(TAG, "onInviteAnchor replyAnchor onError:" + errorCode + "," + message);
                        }
                    });
            mOnInviteAnchorDialog.cancel();
            mHandler.removeCallbacks(mCloseInviteAnchorDialogTask);
        });
        mOnInviteAnchorDialog.setNegativeListener(v -> {
            VideoChatRTCManager.ins().getRTSClient().replyAnchor(
                    VideoChatDataManager.ins().selfUserInfo.roomId,
                    VideoChatDataManager.ins().selfUserInfo.userId,
                    event.fromRoomId, event.fromUserId,
                    2, null);
            mOnInviteAnchorDialog.cancel();
            mHandler.removeCallbacks(mCloseInviteAnchorDialogTask);
        });
        mOnInviteAnchorDialog.show();
        mHandler.postDelayed(mCloseInviteAnchorDialogTask, TimeUnit.SECONDS.toMillis(5));
    }

    /**
     * 邀请主播连麦回复回调。
     * @param event 邀请主播回复事件，详见 InviteAnchorReplyEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInviteAnchorReply(InviteAnchorReplyEvent event) {
        Log.i(TAG, "onInviteAnchorReply event:" + event);
        VideoChatDataManager.ins().selfInviteStatus = VideoChatDataManager.INTERACT_STATUS_NORMAL;
        if (event.reply == VideoChatDataManager.REPLY_TYPE_REJECT) {
            SolutionToast.show(getString(R.string.video_xxx_declined_invitation, event.toUserName));
        } else if (event.reply == VideoChatDataManager.REPLY_TYPE_ACCEPT) {
            AnchorInfo peerAnchor = new AnchorInfo();
            peerAnchor.roomId = event.toRoomId;
            peerAnchor.userId = event.toUserId;
            peerAnchor.userName = event.toUserName;
            peerAnchor.mic = event.interactInfo.mic;
            peerAnchor.camera = event.interactInfo.camera;
            AnchorInfo localAnchor = new AnchorInfo();
            localAnchor.mic = VideoChatRTCManager.ins().isMicOn() ? MicStatus.ON : MicStatus.OFF;
            localAnchor.camera = VideoChatRTCManager.ins().isCameraOn() ? CameraStatus.ON : CameraStatus.OFF;
            startPk(peerAnchor, localAnchor, event.interactInfo.token);
        }
    }

    /**
     * 主播 pk 结束事件回调。
     * @param event 主播 pk 结束事件，详见 AnchorPkFinishEvent。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAnchorPkFinish(AnchorPkFinishEvent event) {
        boolean pkVisible = mVideoPkFragment != null && mVideoPkFragment.isVisible();
        Log.i(TAG, "onAnchorPkFinish event:" + event + ",pkVisible:" + pkVisible);
        if (!pkVisible) {
            return;
        }
        String peerName = "";
        if (mVideoPkFragment != null) {
            peerName = mVideoPkFragment.getPeerUname();
        }
        if (!TextUtils.isEmpty(peerName) && getSelfUserInfo().isHost()) {
            if (mIsFinishAnchorLinkBySelf) {
                SolutionToast.show(getString(R.string.video_disconnected_xxx, mVideoPkFragment.getPeerUname()));
            } else {
                SolutionToast.show(getString(R.string.video_xxx_has_left, peerName));
            }
        }
        mIsFinishAnchorLinkBySelf = false;
        closePk();
    }

    /**
     * 关闭 pk。
     */
    private void closePk() {
        boolean pkVisible = mVideoPkFragment != null && mVideoPkFragment.isVisible();
        Log.i(TAG, "closePk pkVisible:" + pkVisible);
        if (!pkVisible) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment videoPkFragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT_ANCHOR_PK);
        if (videoPkFragment == mVideoPkFragment && videoPkFragment != null) {
            fragmentManager.beginTransaction()
                    .remove(mVideoPkFragment)
                    .commitAllowingStateLoss();
            mVideoPkFragment = null;
            mViewBinding.bizFl.removeAllViews();
            Log.i(TAG, "closePk remove PkFragment");
        }
        setLocalLive();
        getRoomInfo().status = ROOM_STATUS_LIVING;
        getSelfUserInfo().userStatus = USER_STATUS_NORMAL;
        mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(ROOM_STATUS_LIVING, getSelfUserInfo().userRole, getSelfUserInfo().userStatus);
    }

    /**
     * 新主播加入回调。
     * @param event 聊天室用户信息，详见 VideoChatUserInfo。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOnNewAnchorJoin(VideoChatUserInfo event) {
        if (getSelfUserInfo() != null && getSelfUserInfo().isHost()) {
            return;
        }
        Log.i(TAG, "onOnNewAnchorJoin event:" + event);
        AnchorInfo peerAnchor = new AnchorInfo();
        peerAnchor.roomId = event.roomId;
        peerAnchor.userId = event.userId;
        peerAnchor.userName = event.userName;
        peerAnchor.mic = event.mic;
        peerAnchor.camera = event.camera;
        AnchorInfo localAnchor = new AnchorInfo();
        localAnchor.mic = getHostUserInfo().mic;
        localAnchor.camera = getHostUserInfo().camera;
        startPk(peerAnchor, localAnchor, null);
    }

    /**
     * 开始 pk。
     * @param peerAnchor 对端主播信息，详见 AnchorInfo。
     * @param localAnchor 本地主播信息，详见 AnchorInfo。
     * @param rtcToken 主播 token。
     */
    private void startPk(AnchorInfo peerAnchor, AnchorInfo localAnchor, String rtcToken) {
        getRoomInfo().status = ROOM_STATUS_PK_ING;
        mIsFinishAnchorLinkBySelf = false;
        mViewBinding.videoChatMainBottomOption.updateUIByRoleAndStatus(ROOM_STATUS_PK_ING, getSelfUserInfo().userRole, getSelfUserInfo().userStatus);
        mVideoPkFragment = new VideoAnchorPkFragment();
        Bundle args = new Bundle();
        args.putString(VideoAnchorPkFragment.KEY_PEER_ROOM_ID, peerAnchor.roomId);
        args.putString(VideoAnchorPkFragment.KEY_PEER_USER_ID, peerAnchor.userId);
        args.putString(VideoAnchorPkFragment.KEY_PEER_USER_NAME, peerAnchor.userName);
        args.putString(VideoAnchorPkFragment.KEY_RTC_TOKEN, rtcToken);
        args.putBoolean(VideoAnchorPkFragment.KEY_PEER_ANCHOR_MUTED, peerAnchor.audioStatusThisRoom == 0);
        args.putBoolean(VideoAnchorPkFragment.KEY_PEER_ANCHOR_MIC_ON, peerAnchor.mic == 1);
        args.putBoolean(VideoAnchorPkFragment.KEY_PEER_ANCHOR_CAMERA_ON, peerAnchor.camera == 1);
        args.putBoolean(VideoAnchorPkFragment.KEY_LOCAL_ANCHOR_MIC_ON, localAnchor.mic == 1);
        args.putBoolean(VideoAnchorPkFragment.KEY_LOCAL_ANCHOR_CAMERA_ON, localAnchor.camera == 1);
        mVideoPkFragment.setArguments(args);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.biz_fl, mVideoPkFragment, TAG_FRAGMENT_ANCHOR_PK)
                .commit();
        mViewBinding.localLiveFl.removeAllViews();
        mViewBinding.localAnchorNameFl.setVisibility(View.GONE);
    }

    public static void openFromList(Activity activity, VideoChatRoomInfo roomInfo) {
        Intent intent = new Intent(activity, VideoChatRoomMainActivity.class);
        intent.putExtra(REFER_KEY, REFER_FROM_LIST);
        intent.putExtra(REFER_EXTRA_ROOM, GsonUtils.gson().toJson(roomInfo));
        VideoChatUserInfo userInfo = new VideoChatUserInfo();
        userInfo.userId = SolutionDataManager.ins().getUserId();
        userInfo.userName = SolutionDataManager.ins().getUserName();
        intent.putExtra(REFER_EXTRA_USER, GsonUtils.gson().toJson(userInfo));
        activity.startActivity(intent);
    }

    public static void openFromCreate(Activity activity, VideoChatRoomInfo roomInfo, VideoChatUserInfo userInfo, String rtcToken) {
        Intent intent = new Intent(activity, VideoChatRoomMainActivity.class);
        intent.putExtra(REFER_KEY, REFER_FROM_CREATE);
        JoinRoomEvent response = new JoinRoomEvent();
        response.hostInfo = userInfo;
        response.userInfo = userInfo;
        response.roomInfo = roomInfo;
        response.rtcToken = rtcToken;
        response.audienceCount = 0;
        intent.putExtra(REFER_EXTRA_CREATE_JSON, GsonUtils.gson().toJson(response));
        activity.startActivity(intent);
    }

    /**
     * 获取主播用户信息。
     * @return 视频聊天用户信息，详见 VideoChatUserInfo。
     */
    private VideoChatUserInfo getHostUserInfo() {
        return VideoChatDataManager.ins().hostUserInfo;
    }

    /**
     * 获取用户自己信息。
     * @return 视频聊天用户信息，详见 VideoChatUserInfo。
     */
    private VideoChatUserInfo getSelfUserInfo() {
        return VideoChatDataManager.ins().selfUserInfo;
    }

    /**
     * 获取聊天室信息。
     * @return 视频聊天室信息，详见 VideoChatRoomInfo。
     */
    private VideoChatRoomInfo getRoomInfo() {
        return VideoChatDataManager.ins().roomInfo;
    }

    /**
     * 获取连麦状态。
     * @return 用户连麦状态。
     */
    private int getSelfInteractStatus() {
        return VideoChatDataManager.ins().selfInviteStatus;
    }
}
