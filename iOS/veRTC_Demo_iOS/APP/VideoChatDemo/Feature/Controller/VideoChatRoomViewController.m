//
//  VideoChatRoomViewController.m
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/18.
//  Copyright © 2021 . All rights reserved.
//

#import "VideoChatRoomViewController.h"
#import "VideoChatRoomViewController+SocketControl.h"
#import "VideoChatStaticView.h"
#import "VideoChatRoomBottomView.h"
#import "VideoChatPeopleNumView.h"
#import "VideoChatSeatCompoments.h"
#import "VideoChatMusicCompoments.h"
#import "VideoChatTextInputCompoments.h"
#import "VideoChatRoomUserListCompoments.h"
#import "VideoChatIMCompoments.h"
#import "VideoChatRoomSettingCompoments.h"
#import "BytedEffectProtocol.h"
#import "VideoChatPKUserListComponents.h"
#import "VideoChatPKComponents.h"
#import "NetworkingTool.h"

@interface VideoChatRoomViewController ()
<
VideoChatRoomBottomViewDelegate,
VideoChatRTCManagerDelegate,
VideoChatSeatDelegate
>

@property (nonatomic, strong) UIImageView *bgImageImageView;
@property (nonatomic, strong) VideoChatStaticView *staticView;
@property (nonatomic, strong) VideoChatRoomBottomView *bottomView;
@property (nonatomic, strong) VideoChatMusicCompoments *musicCompoments;
@property (nonatomic, strong) VideoChatTextInputCompoments *textInputCompoments;
@property (nonatomic, strong) VideoChatRoomUserListCompoments *userListCompoments;
@property (nonatomic, strong) VideoChatRoomSettingCompoments *settingCompoments;
@property (nonatomic, strong) BytedEffectProtocol *beautyCompoments;
@property (nonatomic, strong) VideoChatIMCompoments *imCompoments;
@property (nonatomic, strong) VideoChatSeatCompoments *seatCompoments;
@property (nonatomic, strong) VideoChatPKUserListComponents *pkUserListComponents;
@property (nonatomic, strong) VideoChatPKComponents *pkComponents;
@property (nonatomic, strong) VideoChatRoomModel *roomModel;
@property (nonatomic, strong) VideoChatUserModel *hostUserModel;
@property (nonatomic, copy) NSString *rtcToken;

@property (nonatomic, assign) VideoChatRoomMode chatRoomMode;

@end

@implementation VideoChatRoomViewController

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel {
    self = [super init];
    if (self) {
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
    
    [self addSocketListener];
    [self addSubviewAndConstraints];
    [self joinRoom];
    
    [self.beautyCompoments resumeLocalEffect];
    
    __weak typeof(self) weakSelf = self;
    [VideoChatRTCManager shareRtc].rtcJoinRoomBlock = ^(NSString * _Nonnull roomId, NSInteger errorCode, NSInteger joinType) {
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
            [self updateRoomViewWithData:RTCToken
                               roomModel:roomModel
                               userModel:userModel
                           hostUserModel:hostUserModel
                                seatList:seatList
                              anchorList:anchorList];
            
            if ([self isHost] && anchorInteractList.count > 0) {
                for (VideoChatUserModel *anchorModel in anchorInteractList) {
                    if (![anchorModel.uid isEqualToString:self.hostUserModel.uid]) {
                        [self.pkUserListComponents startForwardStream:anchorModel.roomID token:anchorModel.pkToken];
                        break;
                    }
                    
                }
            }
            
            for (VideoChatSeatModel *seatModel in seatList) {
                if ([seatModel.userModel.uid isEqualToString:userModel.uid]) {
                    // Reconnect after disconnection, I need to turn on the microphone to collect
                    self.settingCompoments.mic = userModel.mic == VideoChatUserMicOn;
                    self.settingCompoments.camera = userModel.camera == VideoChatUserCameraOn;
                    [[VideoChatRTCManager shareRtc] enableLocalAudio:self.settingCompoments.mic];
                    [[VideoChatRTCManager shareRtc] enableLocalVideo:self.settingCompoments.camera];
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

- (void)clearRedNotification {
    [self.bottomView updateButtonStatus:VideoChatRoomBottomStatusPhone isRed:NO];
    [self.userListCompoments updateWithRed:NO];
}

#pragma mark - SocketControl


- (void)receivedJoinUser:(VideoChatUserModel *)userModel
                   count:(NSInteger)count {
    VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
    model.userModel = userModel;
    model.isJoin = YES;
    [self.imCompoments addIM:model];
    [self.staticView updatePeopleNum:count];
    [self.userListCompoments update];
}

- (void)receivedLeaveUser:(VideoChatUserModel *)userModel
                    count:(NSInteger)count {
    VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
    model.userModel = userModel;
    model.isJoin = NO;
    [self.imCompoments addIM:model];
    [self.staticView updatePeopleNum:count];
    [self.userListCompoments update];
}

- (void)receivedFinishLive:(NSInteger)type roomID:(NSString *)roomID {
    if (![roomID isEqualToString:self.roomModel.roomID]) {
        return;
    }
    [self hangUp:NO];
    if (type == 3) {
        [[ToastComponents shareToastComponents] showWithMessage:@"直播间内容违规，直播间已被关闭" delay:0.8];
    }
    else if (type == 2 && [self isHost]) {
        [[ToastComponents shareToastComponents] showWithMessage:@"本次体验时间已超过20mins" delay:0.8];
    } else {
        if (![self isHost]) {
            [[ToastComponents shareToastComponents] showWithMessage:@"直播间已结束" delay:0.8];
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
    [self.seatCompoments addSeatModel:seatModel];
    [self.userListCompoments update];
    if ([userModel.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        [self.bottomView updateBottomLists:userModel];
        [self.settingCompoments resetMicAndCameraStatus];
        // RTC Start Audio Capture
        [[VideoChatRTCManager shareRtc] setUserVisibility:YES];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:YES];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:YES];
        [[ToastComponents shareToastComponents] showWithMessage:@"你已上麦"];
    }
    
    //IM
    VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
    NSString *message = [NSString stringWithFormat:@"%@已上麦",userModel.name];
    model.message = message;
    [self.imCompoments addIM:model];
}

- (void)receivedLeaveInteractWithUser:(VideoChatUserModel *)userModel
                               seatID:(NSString *)seatID
                                 type:(NSInteger)type {
    [self.seatCompoments removeUserModel:userModel];
    [self.userListCompoments update];
    if ([userModel.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        [self.bottomView updateBottomLists:userModel];
        // RTC Stop Audio Capture
        [[VideoChatRTCManager shareRtc] setUserVisibility:NO];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:NO];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:NO];
        [[VideoChatRTCManager shareRtc] updateCameraID:YES];
        if (type == 1) {
            [[ToastComponents shareToastComponents] showWithMessage:@"你已被主播移出麦位"];
        } else if (type == 2) {
            [[ToastComponents shareToastComponents] showWithMessage:@"你已下麦"];
        }
    }
    
    //IM
    VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
    NSString *message = [NSString stringWithFormat:@"%@已下麦",userModel.name];
    model.message = message;
    [self.imCompoments addIM:model];
}

- (void)receivedSeatStatusChange:(NSString *)seatID
                            type:(NSInteger)type {
    VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
    seatModel.status = type;
    seatModel.userModel = nil;
    seatModel.index = seatID.integerValue;
    [self.seatCompoments updateSeatModel:seatModel];
}

- (void)receivedMediaStatusChangeWithUser:(VideoChatUserModel *)userModel
                                   seatID:(NSString *)seatID
                                      mic:(NSInteger)mic
                                   camera:(NSInteger)camera {
    if (self.chatRoomMode == VideoChatRoomModePK) {
        [self.pkComponents updateUserModel:userModel];
        if ([userModel.uid isEqualToString:self.hostUserModel.uid]) {
            self.seatCompoments.hostUserModel = userModel;
        }
    }
    else {
        VideoChatSeatModel *seatModel = [[VideoChatSeatModel alloc] init];
        seatModel.status = 1;
        seatModel.userModel = userModel;
        seatModel.index = seatID.integerValue;
        [self.seatCompoments updateSeatModel:seatModel];
    }
    
    
    if ([userModel.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        // RTC Mute/Unmute Audio Capture
        [[VideoChatRTCManager shareRtc] muteLocalAudio:userModel.mic == VideoChatUserMicOff];
        [[VideoChatRTCManager shareRtc] muteLocalVideo:userModel.camera == VideoChatUserCameraOff];
        [self.settingCompoments updateUserMic:userModel.mic == VideoChatUserMicOn];
        
    }
}

- (void)receivedMessageWithUser:(VideoChatUserModel *)userModel
                        message:(NSString *)message {
    if (![userModel.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
        NSString *imMessage = [NSString stringWithFormat:@"%@：%@",
                               userModel.name,
                               message];
        model.userModel = userModel;
        model.message = imMessage;
        [self.imCompoments addIM:model];
    }
}

- (void)receivedInviteInteractWithUser:(VideoChatUserModel *)hostUserModel
                                seatID:(NSString *)seatID {
    AlertActionModel *alertModel = [[AlertActionModel alloc] init];
    alertModel.title = @"接受";
    AlertActionModel *cancelModel = [[AlertActionModel alloc] init];
    cancelModel.title = @"拒绝";
    [[AlertActionManager shareAlertActionManager] showWithMessage:@"主播邀请你上麦，是否接受？" actions:@[cancelModel, alertModel]];
    
    __weak __typeof(self) wself = self;
    alertModel.alertModelClickBlock = ^(UIAlertAction * _Nonnull action) {
        if ([action.title isEqualToString:@"接受"]) {
            [wself loadDataWithReplyInvite:1];
        }
    };
    cancelModel.alertModelClickBlock = ^(UIAlertAction * _Nonnull action) {
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
        [self.userListCompoments updateWithRed:YES];
        [self.userListCompoments update];
    }
}

- (void)receivedInviteResultWithUser:(VideoChatUserModel *)hostUserModel
                               reply:(NSInteger)reply {
    if ([self isHost] && reply == 2) {
        NSString *message = [NSString stringWithFormat:@"观众%@拒绝了你的邀请", hostUserModel.name];
        [[ToastComponents shareToastComponents] showWithMessage:message];
    }
    
    if ([self isHost]) {
        [self.userListCompoments resetInviteInteractionWaitingReplyStstus];
    }
}

- (void)receivedMediaOperatWithUid:(NSInteger)mic camera:(NSInteger)camera {
    [VideoChatRTMManager updateMediaStatus:self.roomModel.roomID
                                              mic:mic
                                           camera:camera
                                            block:^(RTMACKModel * _Nonnull model) {
        
    }];
    if (mic) {
        [[ToastComponents shareToastComponents] showWithMessage:@"主播已解除对你的静音"];
    } else {
        [[ToastComponents shareToastComponents] showWithMessage:@"你已被主播静音"];
    }
}

- (void)receivedClearUserWithUid:(NSString *)uid {
    [self hangUp:NO];
    [[ToastComponents shareToastComponents] showWithMessage:@"相同ID用户已登录，您已被强制下线！" delay:0.8];
}

- (void)hangUp:(BOOL)isServer {
    if (isServer) {
        // socket api
        if ([self isHost]) {
            [VideoChatRTMManager finishLive:self.roomModel.roomID];
        } else {
            [VideoChatRTMManager leaveLiveRoom:self.roomModel.roomID];
        }
    }
    // sdk api
    [[VideoChatRTCManager shareRtc] leaveChannel];
    // ui
    [self navigationControllerPop];
}

- (void)receivedAnchorPKInvite:(VideoChatUserModel *)anchorModel {
    [self.pkUserListComponents receivedAnchorPKInvite:anchorModel];
}

- (void)receivedAnchorPKReply:(VideoChatPKReply)reply
                       roomID:(NSString *)roomID
                        token:(NSString *)token
                  anchorModel:(VideoChatUserModel *)anchorModel {
    [self.pkUserListComponents resetPkWaitingReplyStstus];
    
    if (reply == VideoChatPKReplyAccept) {
        [self.pkUserListComponents startForwardStream:roomID token:token];
    }
    else {
        [[ToastComponents shareToastComponents] showWithMessage:[NSString stringWithFormat:@"%@拒绝了你的邀请", anchorModel.name]];
    }
}

- (void)receivedAnchorPKNewAnchorJoined:(VideoChatUserModel *)anchorModel {
    self.chatRoomMode = VideoChatRoomModePK;
    self.pkComponents.anchorModel = anchorModel;
    [self.bottomView updateBottomListsWithPK:YES];
}

- (void)receivedAnchorPKEnd {
    if ([self isHost]) {
        if (self.pkComponents.activeEndPK) {
            [[ToastComponents shareToastComponents] showWithMessage:[NSString stringWithFormat:@"你已结束与%@的连麦", self.pkComponents.anchorModel.name]];
        } else {
            [[ToastComponents shareToastComponents] showWithMessage:[NSString stringWithFormat:@"%@结束了连麦", self.pkComponents.anchorModel.name]];
        }
        [self.pkComponents rtcStopForwardStream];
        self.pkComponents.activeEndPK = NO;
    }
    self.pkComponents.anchorModel = nil;
    [self.bottomView updateBottomListsWithPK:NO];
}

- (void)receiverCloseChatRoomMode {
    self.chatRoomMode = VideoChatRoomModePK;
}

- (void)receivedMuteOtherAnchorRoomID:(NSString *)roomID otherAnchorUserID:(NSString *)otherAnchorUserID type:(VideoChatOtherAnchorMicType)type {
    [self.pkComponents muteOtherAnchorRoomID:roomID otherAnchorUserID:otherAnchorUserID type:type];
}

#pragma mark - Load Data

- (void)loadDataWithJoinRoom {
    [[VideoChatRTCManager shareRtc] setDefaultVideoEncoderConfig];
    
    __weak __typeof(self) wself = self;
    [VideoChatRTMManager joinLiveRoom:self.roomModel.roomID
                             userName:[LocalUserComponents userModel].name
                                block:^(NSString * _Nonnull RTCToken, VideoChatRoomModel * _Nonnull roomModel, VideoChatUserModel * _Nonnull userModel, VideoChatUserModel * _Nonnull hostUserModel,
                                        NSArray<VideoChatSeatModel *> * _Nonnull seatList,
                                            NSArray<VideoChatUserModel *> * _Nonnull anchorList,
                                            RTMACKModel * _Nonnull model) {
        if (NOEmptyStr(roomModel.roomID)) {
            [wself updateRoomViewWithData:RTCToken
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

#pragma mark - VideoChatRoomBottomViewDelegate

- (void)videoChatRoomBottomView:(VideoChatRoomBottomView *_Nonnull)videoChatRoomBottomView
                     itemButton:(VideoChatRoomItemButton *_Nullable)itemButton
                didSelectStatus:(VideoChatRoomBottomStatus)status {
    if (status == VideoChatRoomBottomStatusInput) {
        [self.textInputCompoments showWithRoomModel:self.roomModel];
        __weak __typeof(self) wself = self;
        self.textInputCompoments.clickSenderBlock = ^(NSString * _Nonnull text) {
            VideoChatIMModel *model = [[VideoChatIMModel alloc] init];
            NSString *message = [NSString stringWithFormat:@"%@：%@",
                                 [LocalUserComponents userModel].name,
                                 text];
            model.message = message;
            [wself.imCompoments addIM:model];
        };
    } else if (status == VideoChatRoomBottomStatusPhone) {
        if (self.pkUserListComponents.isPKWaitingReply) {
            [[ToastComponents shareToastComponents] showWithMessage:@"主播暂时无法连麦"];
            return;
        }
        if ([self isHost]) {
            if (self.pkComponents.anchorModel) {
                [[ToastComponents shareToastComponents] showWithMessage:@"主播连线中，无法发起观众连线"];
            }
            else {
                [self.userListCompoments showRoomModel:self.roomModel
                                                seatID:@"-1"
                                          dismissBlock:^{
                    
                }];
            }
        }
        else {
            [self.seatCompoments audienceApplyInteraction];
        }
        
    } else if (status == VideoChatRoomBottomStatusBeauty) {
        if (self.beautyCompoments) {
            [self.beautyCompoments showWithType:EffectBeautyRoleTypeHost
                                  fromSuperView:self.view
                                   dismissBlock:^(BOOL result) {
                
            }];
        } else {
            [[ToastComponents shareToastComponents] showWithMessage:@"开源代码暂不支持美颜相关功能，体验效果请下载Demo"];
        }
    } else if (status == VideoChatRoomBottomStatusSet) {
        [self.settingCompoments showWithType:VideoChatRoomSettingTypeGuest
                               fromSuperView:self.view
                                   roomModel:self.roomModel];
    }
    else if (status == VideoChatRoomBottomStatusPK) {
        
        if (self.userListCompoments.isInviteInteractionWaitingReply) {
            [[ToastComponents shareToastComponents] showWithMessage:@"主播暂时无法连麦"];
            return;
        }
        
        if (itemButton.status == ButtonStatusActive) {
            
            AlertActionModel *alertModel = [[AlertActionModel alloc] init];
            alertModel.title = @"确认";
            AlertActionModel *cancelModel = [[AlertActionModel alloc] init];
            cancelModel.title = @"取消";
            [[AlertActionManager shareAlertActionManager] showWithMessage:@"当前主播连线中,确认结束连线?" actions:@[cancelModel, alertModel]];
            
            __weak typeof(self) weakSelf = self;
            alertModel.alertModelClickBlock = ^(UIAlertAction * _Nonnull action) {
                if ([action.title isEqualToString:@"确认"]) {
                    [weakSelf.pkComponents reqeustStopForwardStream];
                }
            };
            cancelModel.alertModelClickBlock = ^(UIAlertAction * _Nonnull action) {
                if ([action.title isEqualToString:@"取消"]) {
                }
            };
        }
        else {
            if (self.chatRoomMode == VideoChatRoomModeChatRoom) {
                [[ToastComponents shareToastComponents] showWithMessage:@"主播暂时无法连麦"];
            } else {
                [self.pkUserListComponents showAnchorList];
            }
            
        }
    }
}

#pragma mark - VideoChatSeatDelegate

- (void)VideoChatSeatCompoments:(VideoChatSeatCompoments *)VideoChatSeatCompoments
                    clickButton:(VideoChatSeatModel *)seatModel
                    sheetStatus:(VideoChatSheetStatus)sheetStatus {
    if (sheetStatus == VideoChatSheetStatusInvite) {
        [self.userListCompoments showRoomModel:self.roomModel
                                        seatID:[NSString stringWithFormat:@"%ld", (long)seatModel.index]
                                  dismissBlock:^{
            
        }];
    }
}

#pragma mark - VideoChatRTCManagerDelegate

- (void)VideoChatRTCManager:(VideoChatRTCManager *_Nonnull)VideoChatRTCManager reportAllAudioVolume:(NSDictionary<NSString *, NSNumber *> *_Nonnull)volumeInfo {
    if (volumeInfo.count > 0) {
        [self.seatCompoments updateSeatVolume:volumeInfo];
    }
}

- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager onFirstRemoteVideoUid:(NSString *)uid {
    if (self.chatRoomMode == VideoChatRoomModePK) {
        [self.pkComponents updateRenderView:uid];
    }
    else {
        [self.seatCompoments updateSeatRender:uid];
    }
}

#pragma mark - Network request

- (void)loadDataWithReplyInvite:(NSInteger)type {
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(dismissAnchorInvite) object:nil];
    [VideoChatRTMManager replyInvite:self.roomModel.roomID
                                      reply:type
                                      block:^(RTMACKModel * _Nonnull model) {
        if (!model.result) {
            [[ToastComponents shareToastComponents] showWithMessage:model.message];
        }
    }];
}

- (void)dismissAnchorInvite {
    [[AlertActionManager shareAlertActionManager] dismiss:^{
        
    }];
}

#pragma mark - Private Action

- (void)joinRoom {
    if (IsEmptyStr(self.hostUserModel.uid)) {
        [self loadDataWithJoinRoom];
        self.staticView.roomModel = self.roomModel;
    } else {
        [self updateRoomViewWithData:self.rtcToken
                           roomModel:self.roomModel
                           userModel:self.hostUserModel
                       hostUserModel:self.hostUserModel
                            seatList:[self getDefaultSeatDataList]
                          anchorList:nil];
    }
}

- (void)updateRoomViewWithData:(NSString *)rtcToken
                     roomModel:(VideoChatRoomModel *)roomModel
                     userModel:(VideoChatUserModel *)userModel
                 hostUserModel:(VideoChatUserModel *)hostUserModel
                      seatList:(NSArray<VideoChatSeatModel *> *)seatList
              anchorList:(NSArray<VideoChatUserModel *> *)anchorList {
    _hostUserModel = hostUserModel;
    _roomModel = roomModel;
    _rtcToken = rtcToken;
    //Activate SDK
    [[VideoChatRTCManager shareRtc] setUserVisibility:userModel.userRole == VideoChatUserRoleHost];
    [VideoChatRTCManager shareRtc].delegate = self;
    [[VideoChatRTCManager shareRtc] joinChannelWithToken:rtcToken
                                                  roomID:self.roomModel.roomID
                                                     uid:[LocalUserComponents userModel].uid];
    if (userModel.userRole == VideoChatUserRoleHost) {
        
        self.settingCompoments.mic = userModel.mic == VideoChatUserMicOn;
        self.settingCompoments.camera = userModel.camera == VideoChatUserCameraOn;
        
        [[VideoChatRTCManager shareRtc] setUserVisibility:YES];
        [[VideoChatRTCManager shareRtc] enableLocalAudio:self.settingCompoments.mic];
        [[VideoChatRTCManager shareRtc] enableLocalVideo:self.settingCompoments.camera];
    }
    self.staticView.roomModel = self.roomModel;
    [self.bottomView updateBottomLists:userModel isPKing:anchorList.count >= 2];
    
    if (roomModel.roomStatus == VideoChatRoomStatusLianmai) {
        self.chatRoomMode = VideoChatRoomModeChatRoom;
    } else {
        self.chatRoomMode = VideoChatRoomModePK;
    }
    
    self.pkComponents.hostModel = hostUserModel;
    if (self.chatRoomMode == VideoChatRoomModePK) {
        self.seatCompoments.loginUserModel = userModel;
        self.seatCompoments.hostUserModel = hostUserModel;
        [self.pkComponents updateRenderView:hostUserModel.uid];
        BOOL hasOtherAnchor = NO;
        for (VideoChatUserModel *anchorModel in anchorList) {
            if (![anchorModel.uid isEqualToString:self.hostUserModel.uid]) {
                self.pkComponents.anchorModel = anchorModel;
                [self.pkComponents muteOtherAnchorRoomID:anchorModel.roomID
                                       otherAnchorUserID:anchorModel.uid
                                                    type:anchorModel.otherAnchorMicType];
                hasOtherAnchor = YES;
                break;
            }
        }
        if (!hasOtherAnchor) {
            self.pkComponents.anchorModel = nil;
        }
    }
    else {
        [self.seatCompoments showSeatView:seatList
                           loginUserModel:userModel
                                hostUserModel:hostUserModel];
    }
    
    __weak __typeof(self) weakSelf = self;
    [[VideoChatRTCManager shareRtc] didChangeNetworkQuality:^(VideoChatNetworkQualityStatus status, NSString * _Nonnull uid) {
        dispatch_queue_async_safe(dispatch_get_main_queue(), ^{
            if (weakSelf.chatRoomMode == VideoChatRoomModePK) {
                [weakSelf.pkComponents updateNetworkQuality:status uid:uid];
            } else {
                [weakSelf.seatCompoments updateNetworkQuality:status uid:uid];
            }
        });
    }];
}

- (void)addSubviewAndConstraints {
    [self.view addSubview:self.bgImageImageView];
    [self.bgImageImageView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.edges.equalTo(self.view);
    }];
    
    [self pkComponents];
    [self staticView];
//    [self.view addSubview:self.staticView];
//    [self.staticView mas_makeConstraints:^(MASConstraintMaker *make) {
//        make.edges.equalTo(self.view);
//    }];
    
    [self.view addSubview:self.bottomView];
    [self.bottomView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.height.mas_equalTo([DeviceInforTool getVirtualHomeHeight] + 36 + 32);
        make.left.equalTo(self.view).offset(16);
        make.right.equalTo(self.view).offset(-16);
        make.bottom.equalTo(self.view);
    }];
    
    [self imCompoments];
    [self textInputCompoments];
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
    return [self.roomModel.hostUid isEqualToString:[LocalUserComponents userModel].uid];
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
    [self.pkComponents changeChatRoomMode:chatRoomMode];
    [self.seatCompoments changeChatRoomMode:chatRoomMode];
    [self.userListCompoments updateCloseChatRoom:chatRoomMode == VideoChatRoomModePK];
    self.bottomView.chatRoomMode = chatRoomMode;
    
    if (chatRoomMode == VideoChatRoomModeChatRoom && [self isHost]) {
        [self.userListCompoments changeChatRoomModeDismissUserListView];
    }
    else {
        if (![self isHost]) {
            [self.bottomView audienceResetBottomLists];
        }
    }
}

#pragma mark - Getter

- (VideoChatTextInputCompoments *)textInputCompoments {
    if (!_textInputCompoments) {
        _textInputCompoments = [[VideoChatTextInputCompoments alloc] init];
    }
    return _textInputCompoments;
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

- (VideoChatSeatCompoments *)seatCompoments {
    if (!_seatCompoments) {
        _seatCompoments = [[VideoChatSeatCompoments alloc] initWithSuperView:self.view];
        _seatCompoments.delegate = self;
    }
    return _seatCompoments;
}

- (VideoChatRoomBottomView *)bottomView {
    if (!_bottomView) {
        _bottomView = [[VideoChatRoomBottomView alloc] init];
        _bottomView.delegate = self;
    }
    return _bottomView;
}

- (VideoChatRoomUserListCompoments *)userListCompoments {
    if (!_userListCompoments) {
        _userListCompoments = [[VideoChatRoomUserListCompoments alloc] init];
    }
    return _userListCompoments;
}

- (VideoChatIMCompoments *)imCompoments {
    if (!_imCompoments) {
        _imCompoments = [[VideoChatIMCompoments alloc] initWithSuperView:self.view];
    }
    return _imCompoments;
}

- (VideoChatMusicCompoments *)musicCompoments {
    if (!_musicCompoments) {
        _musicCompoments = [[VideoChatMusicCompoments alloc] init];
    }
    return _musicCompoments;
}

- (BytedEffectProtocol *)beautyCompoments {
    if (!_beautyCompoments) {
        _beautyCompoments = [[BytedEffectProtocol alloc] initWithRTCEngineKit:[VideoChatRTCManager shareRtc].rtcEngineKit];
    }
    return _beautyCompoments;
}

- (VideoChatRoomSettingCompoments *)settingCompoments {
    if (!_settingCompoments) {
        _settingCompoments = [[VideoChatRoomSettingCompoments alloc] initWithHost:[self isHost]];
        __weak typeof(self) weakSelf = self;
        _settingCompoments.clickMusicBlock = ^{
            [weakSelf.musicCompoments show];
        };
    }
    return _settingCompoments;
}

- (VideoChatPKUserListComponents *)pkUserListComponents {
    if (!_pkUserListComponents) {
        _pkUserListComponents = [[VideoChatPKUserListComponents alloc] initWithRoomModel:self.roomModel];
    }
    return _pkUserListComponents;
}

- (VideoChatPKComponents *)pkComponents {
    if (!_pkComponents) {
        _pkComponents = [[VideoChatPKComponents alloc] initWithSuperView:self.view roomModel:self.roomModel];
    }
    return _pkComponents;
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
