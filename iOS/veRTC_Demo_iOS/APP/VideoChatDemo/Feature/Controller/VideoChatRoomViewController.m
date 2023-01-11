//
//  VideoChatRoomViewController.m
//  veRTC_Demo
//
//  Created by on 2021/5/18.
//
//

#import "BytedEffectProtocol.h"
#import "Core.h"
#import "VideoChatMusicComponent.h"
#import "VideoChatPKComponent.h"
#import "VideoChatPKUserListComponent.h"
#import "VideoChatPeopleNumView.h"
#import "VideoChatRoomBottomView.h"
#import "VideoChatRoomSettingComponent.h"
#import "VideoChatRoomUserListComponent.h"
#import "VideoChatRoomViewController+SocketControl.h"
#import "VideoChatRoomViewController.h"
#import "VideoChatSeatComponent.h"
#import "VideoChatStaticView.h"
#import "VideoChatTextInputComponent.h"

@interface VideoChatRoomViewController () <
    VideoChatRoomBottomViewDelegate,
    VideoChatRTCManagerDelegate,
    VideoChatSeatDelegate>

@property (nonatomic, strong) UIImageView *bgImageImageView;
@property (nonatomic, strong) VideoChatStaticView *staticView;
@property (nonatomic, strong) VideoChatRoomBottomView *bottomView;
@property (nonatomic, strong) VideoChatMusicComponent *musicComponent;
@property (nonatomic, strong) VideoChatTextInputComponent *textInputComponent;
@property (nonatomic, strong) VideoChatRoomUserListComponent *userListComponent;
@property (nonatomic, strong) VideoChatRoomSettingComponent *settingComponent;
@property (nonatomic, strong) BytedEffectProtocol *beautyComponent;
@property (nonatomic, strong) BaseIMComponent *imComponent;
@property (nonatomic, strong) VideoChatSeatComponent *seatComponent;
@property (nonatomic, strong) VideoChatPKUserListComponent *pkUserListComponent;
@property (nonatomic, strong) VideoChatPKComponent *pkComponent;
@property (nonatomic, strong) VideoChatRoomModel *roomModel;
@property (nonatomic, strong) VideoChatUserModel *hostUserModel;
@property (nonatomic, copy) NSString *rtcToken;

@property (nonatomic, assign) VideoChatRoomMode chatRoomMode;

@end

@implementation VideoChatRoomViewController

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel {
    self = [super init];
    if (self) {
        // 主持人初始化
        // Host initialization
        _roomModel = roomModel;
        [[VideoChatRTCManager shareRtc] updateVideoConfigWithHost:NO];
    }
    return self;
}

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel
                         rtcToken:(NSString *)rtcToken
                    hostUserModel:(VideoChatUserModel *)hostUserModel {
    self = [super init];
    if (self) {
        // 观众初始化
        // Audience initialization
        _hostUserModel = hostUserModel;
        _roomModel = roomModel;
        _rtcToken = rtcToken;
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [UIApplication sharedApplication].idleTimerDisabled = YES;
    self.view.backgroundColor = [UIColor colorFromHexString:@"#394254"];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(clearRedNotification) name:KClearRedNotification object:nil];

    // 开启业务服务器监听
    // Enable business server monitoring
    [self addSocketListener];

    // 初始化UI
    // Initialize UI
    [self addSubviewAndConstraints];

    // 加入业务房间和RTC房间
    // Join business room and RTC room
    [self joinRoom];

    // 初始化美颜组件
    // Initialize the beauty component
    [self.beautyComponent resumeLocalEffect];

    // RTC首次/重连加房回调
    // RTC first/reconnect to add room callback
    __weak typeof(self) weakSelf = self;
    [VideoChatRTCManager shareRtc].rtcJoinRoomBlock = ^(NSString *_Nonnull roomId, NSInteger errorCode, NSInteger joinType) {
        [weakSelf receivedJoinRoom:roomId errorCode:errorCode joinType:joinType];
    };
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    self.navigationController.interactivePopGestureRecognizer.enabled = NO;
}

#pragma mark - Notification

- (void)clearRedNotification {
    [self.bottomView updateButtonStatus:VideoChatRoomBottomStatusPhone isRed:NO];
    [self.userListComponent updateWithRed:NO];
}

#pragma mark - SocketControl

- (void)receivedJoinUser:(VideoChatUserModel *)userModel
                   count:(NSInteger)count {
    [self addIMMessage:YES userModel:userModel];
    [self.staticView updatePeopleNum:count];
    [self.userListComponent update];
}

- (void)receivedLeaveUser:(VideoChatUserModel *)userModel
                    count:(NSInteger)count {
    [self addIMMessage:NO userModel:userModel];
    [self.staticView updatePeopleNum:count];
    [self.userListComponent update];
}

- (void)receivedFinishLive:(NSInteger)type roomID:(NSString *)roomID {
    if (![roomID isEqualToString:self.roomModel.roomID]) {
        return;
    }
    [self hangUp:NO];
    if (type == 3) {
        [[ToastComponent shareToastComponent] showWithMessage:@"直播间内容违规，直播间已被关闭" delay:0.8];
    } else if (type == 2 && [self isHost]) {
        [[ToastComponent shareToastComponent] showWithMessage:@"本次体验时间已超过20分钟" delay:0.8];
    } else {
        if (![self isHost]) {
            [[ToastComponent shareToastComponent] showWithMessage:@"直播间已结束" delay:0.8];
        }
    }
}

- (void)receivedJoinInteractWithUser:(VideoChatUserModel *)userModel
                              seatID:(NSString *)seatID {
    self.chatRoomMode = VideoChatRoomModeChatRoom;

    VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
    seatModel.status = 1;
    seatModel.userModel = userModel;
    seatModel.index = seatID.integerValue;
    [self.seatComponent addSeatModel:seatModel];
    [self.userListComponent update];
    if ([userModel.uid isEqualToString:[LocalUserComponent userModel].uid]) {
        [self.bottomView updateBottomLists:userModel];

        self.settingComponent.mic = (userModel.mic == VideoChatUserMicOn);
        self.settingComponent.camera = (userModel.camera == VideoChatUserCameraOn);
        // RTC Start Audio Capture
        [[VideoChatRTCManager shareRtc] setUserVisibility:YES];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:self.settingComponent.mic];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:self.settingComponent.camera];
        [[ToastComponent shareToastComponent] showWithMessage:@"你已上麦"];
    }

    //IM
    BaseIMModel *model = [[BaseIMModel alloc] init];
    NSString *message = [NSString stringWithFormat:@"%@已上麦", userModel.name];
    model.message = message;
    [self.imComponent addIM:model];
}

- (void)receivedLeaveInteractWithUser:(VideoChatUserModel *)userModel
                               seatID:(NSString *)seatID
                                 type:(NSInteger)type {
    [self.seatComponent removeUserModel:userModel];
    [self.userListComponent update];
    if ([userModel.uid isEqualToString:[LocalUserComponent userModel].uid]) {
        [self.bottomView updateBottomLists:userModel];
        // RTC Stop Audio Capture
        [[VideoChatRTCManager shareRtc] setUserVisibility:NO];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:NO];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:NO];
        [[VideoChatRTCManager shareRtc] updateCameraID:YES];
        if (type == 1) {
            [[ToastComponent shareToastComponent] showWithMessage:@"你已被主播移出麦位"];
        } else if (type == 2) {
            [[ToastComponent shareToastComponent] showWithMessage:@"你已下麦"];
        }
    }

    //IM
    BaseIMModel *model = [[BaseIMModel alloc] init];
    NSString *message = [NSString stringWithFormat:@"%@已下麦", userModel.name];
    model.message = message;
    [self.imComponent addIM:model];
}

- (void)receivedSeatStatusChange:(NSString *)seatID
                            type:(NSInteger)type {
    VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
    seatModel.status = type;
    seatModel.userModel = nil;
    seatModel.index = seatID.integerValue;
    [self.seatComponent updateSeatModel:seatModel];
}

- (void)receivedMediaStatusChangeWithUser:(VideoChatUserModel *)userModel
                                   seatID:(NSString *)seatID
                                      mic:(NSInteger)mic
                                   camera:(NSInteger)camera {
    if ([userModel.uid isEqualToString:self.hostUserModel.uid]) {
        self.hostUserModel = userModel;
    }

    if (self.chatRoomMode == VideoChatRoomModePK) {
        [self.pkComponent updateUserModel:userModel];
        if ([userModel.uid isEqualToString:self.hostUserModel.uid]) {
            self.seatComponent.hostUserModel = userModel;
        }
    } else {
        VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
        seatModel.status = 1;
        seatModel.userModel = userModel;
        seatModel.index = seatID.integerValue;
        [self.seatComponent updateSeatModel:seatModel];
    }

    if ([userModel.uid isEqualToString:[LocalUserComponent userModel].uid]) {
        // RTC Mute/Unmute Audio Capture
        [[VideoChatRTCManager shareRtc] muteLocalAudio:userModel.mic == VideoChatUserMicOff];
        [[VideoChatRTCManager shareRtc] muteLocalVideo:userModel.camera == VideoChatUserCameraOff];
        [self.settingComponent updateUserMic:userModel.mic == VideoChatUserMicOn];
    }
}

- (void)receivedMessageWithUser:(VideoChatUserModel *)userModel
                        message:(NSString *)message {
    if (![userModel.uid isEqualToString:[LocalUserComponent userModel].uid]) {
        BaseIMModel *model = [[BaseIMModel alloc] init];
        NSString *imMessage = [NSString stringWithFormat:@"%@：%@",
                                                         userModel.name,
                                                         message];
        model.message = imMessage;
        [self.imComponent addIM:model];
    }
}

- (void)receivedInviteInteractWithUser:(VideoChatUserModel *)hostUserModel
                                seatID:(NSString *)seatID {
    AlertActionModel *alertModel = [[AlertActionModel alloc] init];
    alertModel.title = @"接受";
    AlertActionModel *cancelModel = [[AlertActionModel alloc] init];
    cancelModel.title = @"拒绝";
    [[AlertActionManager shareAlertActionManager] showWithMessage:@"主播邀请你上麦，是否接受？" actions:@[ cancelModel, alertModel ]];

    __weak __typeof(self) wself = self;
    alertModel.alertModelClickBlock = ^(UIAlertAction *_Nonnull action) {
      if ([action.title isEqualToString:@"接受"]) {
          [wself loadDataWithReplyInvite:1];
      }
    };
    cancelModel.alertModelClickBlock = ^(UIAlertAction *_Nonnull action) {
      if ([action.title isEqualToString:@"拒绝"]) {
          [wself loadDataWithReplyInvite:2];
      }
    };
    [self performSelector:@selector(dismissAnchorInvite) withObject:nil afterDelay:5];
}

- (void)receivedApplyInteractWithUser:(VideoChatUserModel *)userModel
                               seatID:(NSString *)seatID {
    if ([self isHost]) {
        [self.bottomView updateButtonStatus:VideoChatRoomBottomStatusPhone isRed:YES];
        [self.userListComponent updateWithRed:YES];
        [self.userListComponent update];
    }
}

- (void)receivedInviteResultWithUser:(VideoChatUserModel *)hostUserModel
                               reply:(NSInteger)reply {
    if ([self isHost] && reply == 2) {
        NSString *message = [NSString stringWithFormat:@"观众%@拒绝了你的邀请", hostUserModel.name];
        [[ToastComponent shareToastComponent] showWithMessage:message];
        [self.userListComponent update];
    }

    if ([self isHost]) {
        [self.userListComponent resetInviteInteractionWaitingReplyStstus];
    }
}

- (void)receivedMediaOperatWithUid:(NSInteger)mic camera:(NSInteger)camera {
    [VideoChatRTMManager updateMediaStatus:self.roomModel.roomID
                                       mic:mic
                                    camera:camera
                                     block:^(RTMACKModel *_Nonnull model){

                                     }];
    if (mic) {
        [[ToastComponent shareToastComponent] showWithMessage:@"主播已解除对你的静音"];
    } else {
        [[ToastComponent shareToastComponent] showWithMessage:@"你已被主播静音"];
    }
}

- (void)receivedClearUserWithUid:(NSString *)uid {
    [self hangUp:NO];
    [[ToastComponent shareToastComponent] showWithMessage:@"相同ID用户已登录，您已被强制下线！" delay:0.8];
}

- (void)hangUp:(BOOL)isServer {
    if (isServer) {
        if ([self isHost]) {
            [VideoChatRTMManager finishLive:self.roomModel.roomID];
        } else {
            [VideoChatRTMManager leaveLiveRoom:self.roomModel.roomID];
        }
    }
    [[VideoChatRTCManager shareRtc] leaveChannel];
    [self navigationControllerPop];
}

- (void)receivedAnchorPKInvite:(VideoChatUserModel *)anchorModel {
    [self.pkUserListComponent receivedAnchorPKInvite:anchorModel];
}

- (void)receivedAnchorPKReply:(VideoChatPKReply)reply
                       roomID:(NSString *)roomID
                        token:(NSString *)token
                  anchorModel:(VideoChatUserModel *)anchorModel {
    [self.pkUserListComponent resetPkWaitingReplyStstus];

    if (reply == VideoChatPKReplyAccept) {
        [self.pkUserListComponent startForwardStream:roomID token:token];
    } else {
        [[ToastComponent shareToastComponent] showWithMessage:[NSString stringWithFormat:@"%@拒绝了你的邀请", anchorModel.name]];
    }
}

- (void)receivedAnchorPKNewAnchorJoined:(VideoChatUserModel *)anchorModel {
    self.chatRoomMode = VideoChatRoomModePK;
    self.pkComponent.anchorModel = anchorModel;
    [self.bottomView updateBottomListsWithPK:YES];
}

- (void)receivedAnchorPKEnd {
    if ([self isHost]) {
        if (self.pkComponent.activeEndPK) {
            [[ToastComponent shareToastComponent] showWithMessage:[NSString stringWithFormat:@"你已结束与%@的连麦", self.pkComponent.anchorModel.name]];
        } else {
            [[ToastComponent shareToastComponent] showWithMessage:[NSString stringWithFormat:@"%@结束了连麦", self.pkComponent.anchorModel.name]];
        }
        [self.pkComponent rtcStopForwardStream];
        self.pkComponent.activeEndPK = NO;
    }
    self.pkComponent.anchorModel = nil;
    [self.bottomView updateBottomListsWithPK:NO];
}

- (void)receiverCloseChatRoomMode {
    self.chatRoomMode = VideoChatRoomModePK;
}

- (void)receivedMuteOtherAnchorRoomID:(NSString *)roomID otherAnchorUserID:(NSString *)otherAnchorUserID type:(VideoChatOtherAnchorMicType)type {
    [self.pkComponent muteOtherAnchorRoomID:roomID otherAnchorUserID:otherAnchorUserID type:type];
}

#pragma mark - Load Data

- (void)loadDataWithJoinRoom {
    [[VideoChatRTCManager shareRtc] setDefaultVideoEncoderConfig];

    __weak __typeof(self) wself = self;
    [VideoChatRTMManager joinLiveRoom:self.roomModel.roomID
                             userName:[LocalUserComponent userModel].name
                                block:^(NSString * _Nonnull RTCToken, VideoChatRoomModel *_Nonnull roomModel, VideoChatUserModel *_Nonnull userModel, VideoChatUserModel *_Nonnull hostUserModel,
                                        NSArray<VideoChatSeatModel *> * _Nonnull seatList,
                                            NSArray<VideoChatUserModel *> * _Nonnull anchorList,
                                            RTMACKModel * _Nonnull model) {
        if (NOEmptyStr(roomModel.roomID)) {
            [wself joinRTCRoomWithData:RTCToken
                             roomModel:roomModel
                             userModel:userModel
                         hostUserModel:hostUserModel
                              seatList:seatList
                            anchorList:anchorList];
        } else {
            AlertActionModel *alertModel = [[AlertActionModel alloc] init];
            alertModel.title = @"确定";
            alertModel.alertModelClickBlock = ^(UIAlertAction * _Nonnull action) {
                if ([action.title isEqualToString:@"确定"]) {
                    [wself hangUp:NO];
                }
            };
            [[AlertActionManager shareAlertActionManager] showWithMessage:@"加入房间失败，回到房间列表页" actions:@[alertModel]];
        }
    }];
}

- (void)loadDataWithReplyInvite:(NSInteger)type {
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(dismissAnchorInvite) object:nil];
    [VideoChatRTMManager replyInvite:self.roomModel.roomID
                                      reply:type
                                      block:^(RTMACKModel * _Nonnull model) {
        if (!model.result) {
            [[ToastComponent shareToastComponent] showWithMessage:model.message];
        }
    }];
}

#pragma mark - VideoChatRoomBottomViewDelegate

- (void)videoChatRoomBottomView:(VideoChatRoomBottomView *_Nonnull)videoChatRoomBottomView
                     itemButton:(VideoChatRoomItemButton *_Nullable)itemButton
                didSelectStatus:(VideoChatRoomBottomStatus)status {
    if (status == VideoChatRoomBottomStatusInput) {
        [self.textInputComponent showWithRoomModel:self.roomModel];
        __weak __typeof(self) wself = self;
        self.textInputComponent.clickSenderBlock = ^(NSString *_Nonnull text) {
          BaseIMModel *model = [[BaseIMModel alloc] init];
          NSString *message = [NSString stringWithFormat:@"%@：%@",
                                                         [LocalUserComponent userModel].name,
                                                         text];
          model.message = message;
          [wself.imComponent addIM:model];
        };
    } else if (status == VideoChatRoomBottomStatusPhone) {
        if (self.pkUserListComponent.isPKWaitingReply) {
            [[ToastComponent shareToastComponent] showWithMessage:@"主播暂时无法连麦"];
            return;
        }
        if ([self isHost]) {
            if (self.pkComponent.anchorModel) {
                [[ToastComponent shareToastComponent] showWithMessage:@"主播连线中，无法发起观众连线"];
            } else {
                [self.userListComponent showRoomModel:self.roomModel
                                               seatID:@"-1"
                                         dismissBlock:^{

                                         }];
            }
        } else {
            [self.seatComponent audienceApplyInteraction];
        }

    } else if (status == VideoChatRoomBottomStatusBeauty) {
        if (self.beautyComponent) {
            [self.beautyComponent showWithType:EffectBeautyRoleTypeHost
                                 fromSuperView:self.view
                                  dismissBlock:^(BOOL result){

                                  }];
        } else {
            [[ToastComponent shareToastComponent] showWithMessage:@"开源代码暂不支持美颜相关功能，体验效果请下载Demo"];
        }
    } else if (status == VideoChatRoomBottomStatusSet) {
        [self.settingComponent showWithType:VideoChatRoomSettingTypeGuest
                              fromSuperView:self.view
                                  roomModel:self.roomModel];
    } else if (status == VideoChatRoomBottomStatusPK) {
        if (self.userListComponent.isInviteInteractionWaitingReply) {
            [[ToastComponent shareToastComponent] showWithMessage:@"主播暂时无法连麦"];
            return;
        }

        if (itemButton.status == ButtonStatusActive) {
            AlertActionModel *alertModel = [[AlertActionModel alloc] init];
            alertModel.title = @"确认";
            AlertActionModel *cancelModel = [[AlertActionModel alloc] init];
            cancelModel.title = @"取消";
            [[AlertActionManager shareAlertActionManager] showWithMessage:@"当前主播连线中,确认结束连线?" actions:@[ cancelModel, alertModel ]];

            __weak typeof(self) weakSelf = self;
            alertModel.alertModelClickBlock = ^(UIAlertAction *_Nonnull action) {
                if ([action.title isEqualToString:@"确认"]) {
                    [weakSelf.pkComponent reqeustStopForwardStream];
                }
            };
            cancelModel.alertModelClickBlock = ^(UIAlertAction *_Nonnull action) {
                if ([action.title isEqualToString:@"取消"]) {
                }
            };
        } else {
            if (self.chatRoomMode == VideoChatRoomModeChatRoom) {
                [[ToastComponent shareToastComponent] showWithMessage:@"主播暂时无法连麦"];
            } else {
                [self.pkUserListComponent showAnchorList];
            }
        }
    }
}

#pragma mark - VideoChatSeatDelegate

- (void)VideoChatSeatComponent:(VideoChatSeatComponent *)VideoChatSeatComponent
                   clickButton:(VideoChatSeatModel *)seatModel
                   sheetStatus:(VideoChatSheetStatus)sheetStatus {
    if (sheetStatus == VideoChatSheetStatusInvite) {
        [self.userListComponent showRoomModel:self.roomModel
                                       seatID:[NSString stringWithFormat:@"%ld", (long)seatModel.index]
                                 dismissBlock:^{

                                 }];
    }
}

#pragma mark - VideoChatRTCManagerDelegate

- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager reportAllAudioVolume:(NSDictionary<NSString *, NSNumber *> *_Nonnull)volumeInfo {
    if (volumeInfo.count > 0) {
        [self.seatComponent updateSeatVolume:volumeInfo];
    }
}

- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager
      onFirstRemoteVideoUid:(NSString *)uid {
    if (self.chatRoomMode == VideoChatRoomModePK) {
        [self.pkComponent updateRenderView:uid];
    } else {
        [self.seatComponent updateSeatRender:uid];
    }
}

#pragma mark - Private Action

- (void)joinRoom {
    if (IsEmptyStr(self.hostUserModel.uid)) {
        [self loadDataWithJoinRoom];
        self.staticView.roomModel = self.roomModel;
    } else {
        [self joinRTCRoomWithData:self.rtcToken
                        roomModel:self.roomModel
                        userModel:self.hostUserModel
                    hostUserModel:self.hostUserModel
                         seatList:[self getDefaultSeatDataList]
                       anchorList:nil];
    }
}

- (void)joinRTCRoomWithData:(NSString *)rtcToken
                  roomModel:(VideoChatRoomModel *)roomModel
                  userModel:(VideoChatUserModel *)userModel
              hostUserModel:(VideoChatUserModel *)hostUserModel
                   seatList:(NSArray<VideoChatSeatModel *> *)seatList
                 anchorList:(NSArray<VideoChatUserModel *> *)anchorList {
    _hostUserModel = hostUserModel;
    _roomModel = roomModel;
    _rtcToken = rtcToken;
    BOOL isHost = (userModel.userRole == VideoChatUserRoleHost) ? YES : NO;
    
    //Activate SDK
    [[VideoChatRTCManager shareRtc] setUserVisibility:userModel.userRole == VideoChatUserRoleHost];
    [VideoChatRTCManager shareRtc].delegate = self;
    [[VideoChatRTCManager shareRtc] joinRTCRoomWithToken:rtcToken
                                                  roomID:self.roomModel.roomID
                                                     uid:[LocalUserComponent userModel].uid
                                                userRole:isHost];
    if (isHost) {
        self.settingComponent.mic = userModel.mic == VideoChatUserMicOn;
        self.settingComponent.camera = userModel.camera == VideoChatUserCameraOn;
        [[VideoChatRTCManager shareRtc] setUserVisibility:YES];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:self.settingComponent.mic];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:self.settingComponent.camera];
    }
    self.staticView.roomModel = self.roomModel;
    [self.bottomView updateBottomLists:userModel isPKing:anchorList.count >= 2];

    if (roomModel.roomStatus == VideoChatRoomStatusLianmai) {
        self.chatRoomMode = VideoChatRoomModeChatRoom;
    } else {
        self.chatRoomMode = VideoChatRoomModePK;
    }

    self.pkComponent.hostModel = hostUserModel;
    if (self.chatRoomMode == VideoChatRoomModePK) {
        self.seatComponent.loginUserModel = userModel;
        self.seatComponent.hostUserModel = hostUserModel;
        [self.pkComponent updateRenderView:hostUserModel.uid];
        BOOL hasOtherAnchor = NO;
        for (VideoChatUserModel *anchorModel in anchorList) {
            if (![anchorModel.uid isEqualToString:self.hostUserModel.uid]) {
                self.pkComponent.anchorModel = anchorModel;
                [self.pkComponent muteOtherAnchorRoomID:anchorModel.roomID
                                      otherAnchorUserID:anchorModel.uid
                                                   type:anchorModel.otherAnchorMicType];
                hasOtherAnchor = YES;
                break;
            }
        }
        if (!hasOtherAnchor) {
            self.pkComponent.anchorModel = nil;
        }
    } else {
        [self.seatComponent showSeatView:seatList
                          loginUserModel:userModel
                           hostUserModel:hostUserModel];
    }

    __weak __typeof(self) weakSelf = self;
    [[VideoChatRTCManager shareRtc] didChangeNetworkQuality:^(VideoChatNetworkQualityStatus status, NSString * _Nonnull uid) {
        dispatch_queue_async_safe(dispatch_get_main_queue(), ^{
            if (weakSelf.chatRoomMode == VideoChatRoomModePK) {
                [weakSelf.pkComponent updateNetworkQuality:status uid:uid];
            } else {
                [weakSelf.seatComponent updateNetworkQuality:status uid:uid];
            }
        });
    }];
}

- (void)addSubviewAndConstraints {
    [self.view addSubview:self.bgImageImageView];
    [self.bgImageImageView mas_makeConstraints:^(MASConstraintMaker *make) {
      make.edges.equalTo(self.view);
    }];

    [self pkComponent];
    [self staticView];
    
    [self.view addSubview:self.bottomView];
    [self.bottomView mas_makeConstraints:^(MASConstraintMaker *make) {
      make.height.mas_equalTo([DeviceInforTool getVirtualHomeHeight] + 36 + 32);
      make.left.equalTo(self.view).offset(16);
      make.right.equalTo(self.view).offset(-16);
      make.bottom.equalTo(self.view);
    }];

    [self imComponent];
    [self textInputComponent];
}

- (void)showEndView {
    __weak __typeof(self) wself = self;
    AlertActionModel *alertModel = [[AlertActionModel alloc] init];
    alertModel.title = @"结束直播";
    alertModel.alertModelClickBlock = ^(UIAlertAction *_Nonnull action) {
        if ([action.title isEqualToString:@"结束直播"]) {
            [wself hangUp:YES];
        }
    };
    AlertActionModel *alertCancelModel = [[AlertActionModel alloc] init];
    alertCancelModel.title = @"取消";
    NSString *message = @"是否结束直播？";
    [[AlertActionManager shareAlertActionManager] showWithMessage:message actions:@[ alertCancelModel, alertModel ]];
}

- (void)navigationControllerPop {
    UIViewController *jumpVC = nil;
    for (UIViewController *vc in self.navigationController.viewControllers) {
        if ([NSStringFromClass([vc class]) isEqualToString:@"VideoChatRoomListsViewController"]) {
            jumpVC = vc;
            break;
        }
    }
    if (jumpVC) {
        [self.navigationController popToViewController:jumpVC animated:YES];
    } else {
        [self.navigationController popViewControllerAnimated:YES];
    }
}

- (BOOL)isHost {
    return [self.roomModel.hostUid isEqualToString:[LocalUserComponent userModel].uid];
}

- (NSArray *)getDefaultSeatDataList {
    NSMutableArray *list = [[NSMutableArray alloc] init];
    for (int i = 0; i < 5; i++) {
        VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
        seatModel.status = 1;
        seatModel.index = i + 1;
        [list addObject:seatModel];
    }
    return [list copy];
}

- (void)setChatRoomMode:(VideoChatRoomMode)chatRoomMode {
    if (_chatRoomMode == chatRoomMode) {
        return;
    }
    _chatRoomMode = chatRoomMode;
    self.pkComponent.hostModel = self.hostUserModel;
    self.seatComponent.hostUserModel = self.hostUserModel;
    [self.pkComponent changeChatRoomMode:chatRoomMode];
    [self.seatComponent changeChatRoomMode:chatRoomMode];
    [self.userListComponent updateCloseChatRoom:chatRoomMode == VideoChatRoomModePK];
    self.bottomView.chatRoomMode = chatRoomMode;

    if (chatRoomMode == VideoChatRoomModeChatRoom && [self isHost]) {
        [self.userListComponent changeChatRoomModeDismissUserListView];
    } else {
        if (![self isHost]) {
            [self.bottomView audienceResetBottomLists];
            self.seatComponent.loginUserModel.status = VideoChatUserStatusDefault;
        }
    }
}

- (void)addIMMessage:(BOOL)isJoin
           userModel:(VideoChatUserModel *)userModel {
    NSString *unitStr = isJoin ? @"加入了房间" : @"退出了房间";
    BaseIMModel *imModel = [[BaseIMModel alloc] init];
    imModel.message = [NSString stringWithFormat:@"%@ %@", userModel.name, unitStr];
    [self.imComponent addIM:imModel];
}

- (void)receivedJoinRoom:(NSString *)roomId
               errorCode:(NSInteger)errorCode
                joinType:(NSInteger)joinType {
    if ([roomId isEqualToString:self.roomModel.roomID]) {
        if (errorCode == 0) {
        }
        if (joinType != 0 && errorCode == 0) {
            [self reconnectVideoChatRoom];
        }
        return;
    }
}

- (void)reconnectVideoChatRoom {
    [VideoChatRTMManager reconnectWithBlock:^(NSString * _Nonnull RTCToken,
                                              VideoChatRoomModel * _Nonnull roomModel,
                                              VideoChatUserModel * _Nonnull userModel,
                                              VideoChatUserModel * _Nonnull hostUserModel,
                                              NSArray<VideoChatSeatModel *> * _Nonnull seatList,
                                              NSArray<VideoChatUserModel *> * _Nonnull anchorList,
                                              NSArray<VideoChatUserModel *> * _Nonnull anchorInteractList,
                                              RTMACKModel * _Nonnull model) {
        
        if (model.result) {
            [self joinRTCRoomWithData:RTCToken
                            roomModel:roomModel
                            userModel:userModel
                        hostUserModel:hostUserModel
                             seatList:seatList
                           anchorList:anchorList];
            
            if ([self isHost] && anchorInteractList.count > 0) {
                for (VideoChatUserModel *anchorModel in anchorInteractList) {
                    if (![anchorModel.uid isEqualToString:self.hostUserModel.uid]) {
                        [self.pkUserListComponent startForwardStream:anchorModel.roomID token:anchorModel.pkToken];
                        break;
                    }
                    
                }
            }
            
            for (VideoChatSeatModel *seatModel in seatList) {
                if ([seatModel.userModel.uid isEqualToString:userModel.uid]) {
                    // Reconnect after disconnection, I need to turn on the microphone to collect
                    self.settingComponent.mic = userModel.mic == VideoChatUserMicOn;
                    self.settingComponent.camera = userModel.camera == VideoChatUserCameraOn;
                    [[VideoChatRTCManager shareRtc] enableLocalAudio:self.settingComponent.mic];
                    [[VideoChatRTCManager shareRtc] enableLocalVideo:self.settingComponent.camera];
                    [[VideoChatRTCManager shareRtc] setUserVisibility:YES];
                    
                    break;
                }
            }
        } else if (model.code == RTMStatusCodeUserIsInactive ||
                   model.code == RTMStatusCodeRoomDisbanded ||
                   model.code == RTMStatusCodeUserNotFound) {
            [self hangUp:NO];
        }
    }];
}

- (void)dismissAnchorInvite {
    [[AlertActionManager shareAlertActionManager] dismiss:^{

    }];
}

#pragma mark - Getter

- (VideoChatTextInputComponent *)textInputComponent {
    if (!_textInputComponent) {
        _textInputComponent = [[VideoChatTextInputComponent alloc] init];
    }
    return _textInputComponent;
}

- (VideoChatStaticView *)staticView {
    if (!_staticView) {
        _staticView = [[VideoChatStaticView alloc] initWithSuperView:self.view];
        __weak typeof(self) weakSelf = self;
        _staticView.quitLiveBlock = ^{
            if ([weakSelf isHost]) {
                [weakSelf showEndView];
            } else {
                [weakSelf hangUp:YES];
            }
        };
    }
    return _staticView;
}

- (VideoChatSeatComponent *)seatComponent {
    if (!_seatComponent) {
        _seatComponent = [[VideoChatSeatComponent alloc] initWithSuperView:self.view];
        _seatComponent.delegate = self;
    }
    return _seatComponent;
}

- (VideoChatRoomBottomView *)bottomView {
    if (!_bottomView) {
        _bottomView = [[VideoChatRoomBottomView alloc] init];
        _bottomView.delegate = self;
    }
    return _bottomView;
}

- (VideoChatRoomUserListComponent *)userListComponent {
    if (!_userListComponent) {
        _userListComponent = [[VideoChatRoomUserListComponent alloc] init];
    }
    return _userListComponent;
}

- (BaseIMComponent *)imComponent {
    if (!_imComponent) {
        _imComponent = [[BaseIMComponent alloc] initWithSuperView:self.view];
    }
    return _imComponent;
}

- (VideoChatMusicComponent *)musicComponent {
    if (!_musicComponent) {
        _musicComponent = [[VideoChatMusicComponent alloc] init];
    }
    return _musicComponent;
}

- (BytedEffectProtocol *)beautyComponent {
    if (!_beautyComponent) {
        _beautyComponent = [[BytedEffectProtocol alloc] initWithRTCEngineKit:[VideoChatRTCManager shareRtc].rtcEngineKit];
    }
    return _beautyComponent;
}

- (VideoChatRoomSettingComponent *)settingComponent {
    if (!_settingComponent) {
        _settingComponent = [[VideoChatRoomSettingComponent alloc] initWithHost:[self isHost]];
        __weak typeof(self) weakSelf = self;
        _settingComponent.clickMusicBlock = ^{
            [weakSelf.musicComponent show];
        };
    }
    return _settingComponent;
}

- (VideoChatPKUserListComponent *)pkUserListComponent {
    if (!_pkUserListComponent) {
        _pkUserListComponent = [[VideoChatPKUserListComponent alloc] initWithRoomModel:self.roomModel];
    }
    return _pkUserListComponent;
}

- (VideoChatPKComponent *)pkComponent {
    if (!_pkComponent) {
        _pkComponent = [[VideoChatPKComponent alloc] initWithSuperView:self.view roomModel:self.roomModel];
    }
    return _pkComponent;
}

- (UIImageView *)bgImageImageView {
    if (!_bgImageImageView) {
        NSString *bgImageName = @"videochat_room";
        _bgImageImageView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:bgImageName bundleName:HomeBundleName]];
        _bgImageImageView.contentMode = UIViewContentModeScaleAspectFill;
        _bgImageImageView.clipsToBounds = YES;
    }
    return _bgImageImageView;
}

- (void)dealloc {
    [UIApplication sharedApplication].idleTimerDisabled = NO;
    [[AlertActionManager shareAlertActionManager] dismiss:^{

    }];
}

@end
