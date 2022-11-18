package com.volcengine.vertcdemo.videochat.feature.roomlist;

import static com.volcengine.vertcdemo.core.SolutionConstants.CLICK_RESET_INTERVAL;
import static com.volcengine.vertcdemo.core.net.rts.RTSInfo.KEY_RTM;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.ss.video.rtc.demo.basic_module.utils.WindowUtils;
import com.vertcdemo.joinrtsparams.bean.JoinRTSRequest;
import com.vertcdemo.joinrtsparams.common.JoinRTSManager;
import com.volcengine.vertcdemo.common.IAction;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.ServerResponse;
import com.volcengine.vertcdemo.core.net.rts.RTSBaseClient;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochat.bean.GetActiveRoomListResponse;
import com.volcengine.vertcdemo.videochat.bean.VideoChatRoomInfo;
import com.volcengine.vertcdemo.videochat.bean.VideoChatResponse;
import com.volcengine.vertcdemo.videochat.core.Constants;
import com.volcengine.vertcdemo.videochat.core.VideoChatRTCManager;
import com.volcengine.vertcdemo.videochat.feature.createroom.VideoChatCreateRoomActivity;
import com.volcengine.vertcdemo.videochat.feature.roommain.VideoChatRoomMainActivity;

import java.util.List;

public class VideoChatListActivity extends BaseActivity {

    private static final String TAG = "VideoChatListActivity";

    private View mEmptyListView;

    private RTSInfo mRtmInfo;

    private long mLastClickCreateTs = 0;
    private long mLastClickRequestTs = 0;

    private final IAction<VideoChatRoomInfo> mOnClickRoomInfo = roomInfo
            -> VideoChatRoomMainActivity.openFromList(VideoChatListActivity.this, roomInfo);

    private final VideoChatRoomListAdapter mVoiceChatRoomListAdapter = new VideoChatRoomListAdapter(mOnClickRoomInfo);

    private final IRequestCallback<GetActiveRoomListResponse> mRequestRoomList =
            new IRequestCallback<GetActiveRoomListResponse>() {
                @Override
                public void onSuccess(GetActiveRoomListResponse data) {
                    setRoomList(data.roomList);
                }

                @Override
                public void onError(int errorCode, String message) {
                    SafeToast.show(message);
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_video_chat_demo_list);

        initRtmInfo();
    }

    /**
     * 获取RTM信息
     */
    private void initRtmInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mRtmInfo = intent.getParcelableExtra(KEY_RTM);
        if (mRtmInfo == null || !mRtmInfo.isValid()) {
            finish();
        }
    }

    @Override
    protected void setupStatusBar() {
        WindowUtils.setLayoutFullScreen(getWindow());
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        super.onGlobalLayoutCompleted();

        ((TextView) findViewById(R.id.title_bar_title_tv)).setText("视频互动");
        ImageView backArrow = findViewById(R.id.title_bar_left_iv);
        backArrow.setImageResource(R.drawable.back_arrow);
        backArrow.setOnClickListener(v -> finish());
        ImageView rightIv = findViewById(R.id.title_bar_right_iv);
        rightIv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        rightIv.setImageResource(R.drawable.refresh);
        rightIv.setOnClickListener(v -> requestRoomList());

        View createBtn = findViewById(R.id.voice_chat_list_create_room);
        createBtn.setOnClickListener((v) -> onClickCreateRoom());

        RecyclerView dataRv = findViewById(R.id.voice_chat_list_rv);
        dataRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        dataRv.setAdapter(mVoiceChatRoomListAdapter);
        mEmptyListView = findViewById(R.id.voice_chat_empty_list_view);

        initRTC();
    }

    /**
     * 初始化RTC
     */
    private void initRTC() {
        VideoChatRTCManager.ins().initEngine(mRtmInfo);
        RTSBaseClient rtmClient = VideoChatRTCManager.ins().getRTSClient();
        if (rtmClient == null) {
            finish();
            return;
        }
        rtmClient.login(mRtmInfo.rtmToken, (resultCode, message) -> {
            if (resultCode == RTSBaseClient.LoginCallBack.SUCCESS) {
                requestRoomList();
            } else {
                SafeToast.show("Login Rtm Fail Error:" + resultCode + ",Message:" + message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoChatRTCManager.ins().getRTSClient().removeAllEventListener();
        VideoChatRTCManager.ins().getRTSClient().logout();
        VideoChatRTCManager.ins().destroyEngine();
    }

    private void requestRoomList() {
        long now = System.currentTimeMillis();
        if (now - mLastClickRequestTs <= CLICK_RESET_INTERVAL) {
            return;
        }
        mLastClickRequestTs = now;

        VideoChatRTCManager.ins().getRTSClient().requestClearUser(new IRequestCallback<VideoChatResponse>() {
            @Override
            public void onSuccess(VideoChatResponse data) {
                VideoChatRTCManager.ins().getRTSClient().getActiveRoomList(mRequestRoomList);
            }

            @Override
            public void onError(int errorCode, String message) {
                VideoChatRTCManager.ins().getRTSClient().getActiveRoomList(mRequestRoomList);
            }
        });
    }

    private void setRoomList(List<VideoChatRoomInfo> roomList) {
        mVoiceChatRoomListAdapter.setRoomList(roomList);
        mEmptyListView.setVisibility((roomList == null || roomList.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    private void onClickCreateRoom() {
        long now = System.currentTimeMillis();
        if (now - mLastClickCreateTs <= CLICK_RESET_INTERVAL) {
            return;
        }
        mLastClickCreateTs = now;
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
                intent.setClass(Utilities.getApplicationContext(), VideoChatListActivity.class);
                intent.putExtra(KEY_RTM, data);
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
        JoinRTSRequest request = new JoinRTSRequest();
        request.scenesName = Constants.SOLUTION_NAME_ABBR;
        request.loginToken = SolutionDataManager.ins().getToken();

        JoinRTSManager.setAppInfoAndJoinRTM(request, callback);
    }
}
