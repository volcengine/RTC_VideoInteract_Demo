// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomViewController : UIViewController

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel;

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel
                         rtcToken:(NSString *)rtcToken
                    hostUserModel:(VideoChatUserModel *)hostUserModel;

#pragma mark - RTS Listener

/**
 * @brief 收到用户加入房间
 * @param userModel 用户模型
 * @param count 当前房间用户数量
 */
- (void)receivedJoinUser:(VideoChatUserModel *)userModel
                   count:(NSInteger)count;

/**
 * @brief 收到用户离开房间
 * @param userModel 用户模型
 * @param count 当前房间用户数量
 */
- (void)receivedLeaveUser:(VideoChatUserModel *)userModel
                    count:(NSInteger)count;

/**
 * @brief 收到直播结束消息
 * @param type 直播结束类型。2：因为超时关闭，3：因为违规关闭。
 */
- (void)receivedFinishLive:(NSInteger)type
                    roomID:(NSString *)roomID;

/**
 * @brief 收到麦位状态变化消息
 * @param seatID 麦位ID
 * @param type 状态。0 : 封锁麦位, 1 : 解锁麦位
 */
- (void)receivedSeatStatusChange:(NSString *)seatID
                            type:(NSInteger)type;

/**
 * @brief 收到相机麦克风状态变化消息
 * @param userModel 用户模型
 * @param seatID 麦位ID
 * @param mic 麦克风采集状态
 * @param camera 相机采集状态
 */
- (void)receivedMediaStatusChangeWithUser:(VideoChatUserModel *)userModel
                                   seatID:(NSString *)seatID
                                      mic:(NSInteger)mic
                                   camera:(NSInteger)camera;

/**
 * @brief 收到 IM 消息
 * @param userModel 用户模型
 * @param message 消息
 */
- (void)receivedMessageWithUser:(VideoChatUserModel *)userModel
                        message:(NSString *)message;

/**
 * @brief 收到自己相机、麦克风变化消息
 * @param mic 麦克风采集状态
 * @param camera 相机采集状态
 */
- (void)receivedMediaOperatWithUid:(NSInteger)mic
                            camera:(NSInteger)camera;

/**
 * @brief 收账号重复登录，单点登录
 * @param uid 用户ID
 */
- (void)receivedClearUserWithUid:(NSString *)uid;

#pragma mark - Listener Guests

/**
 * @brief 收到嘉宾加入连麦消息
 * @param userModel 嘉宾用户模型
 * @param seatID 麦位ID
 */
- (void)receivedJoinInteractWithUser:(VideoChatUserModel *)userModel
                              seatID:(NSString *)seatID;

/**
 * @brief 收到嘉宾离开连麦消息
 * @param userModel 嘉宾用户模型
 * @param seatID 麦位ID
 * @param type 状态。1 : 被动下麦, 2 : 主动下麦
 */
- (void)receivedLeaveInteractWithUser:(VideoChatUserModel *)userModel
                               seatID:(NSString *)seatID
                                 type:(NSInteger)type;

/**
 * @brief 观众收到主播连麦邀请
 * @param hostUserModel 主播用户模型
 * @param seatID 麦位ID
 */
- (void)receivedInviteInteractWithUser:(VideoChatUserModel *)hostUserModel
                                seatID:(NSString *)seatID;

/**
 * @brief 主播收到观众连麦申请
 * @param userModel 观众用户模型
 * @param seatID 麦位ID
 */
- (void)receivedApplyInteractWithUser:(VideoChatUserModel *)userModel
                               seatID:(NSString *)seatID;

/**
 * @brief 主播收到观众邀请被拒绝消息
 * @param hostUserModel 主播用户模型
 * @param reply 回复
 */
- (void)receivedInviteResultWithUser:(VideoChatUserModel *)hostUserModel
                               reply:(NSInteger)reply;

/**
 * @brief 收到主播嘉宾连麦结束消息
 */
- (void)receiverCloseChatRoomMode;

#pragma mark - Listener Cohost

/**
 * @brief 收到多主播连麦邀请
 * @param anchorModel 主播用户模型
 */
- (void)receivedAnchorPKInvite:(VideoChatUserModel *)anchorModel;

/**
 * @brief 收到多主播连麦邀请回复
 * @param reply 回复类型。1：接受，2：拒绝，3：超时
 * @param roomID 房间ID
 * @param token 加入 RTC 房间所需要的 Token
 * @param anchorModel 主播用户模型
 */
- (void)receivedAnchorPKReply:(VideoChatPKReply)reply
                       roomID:(NSString *)roomID
                        token:(NSString *)token
                  anchorModel:(VideoChatUserModel *)anchorModel;

/**
 * @brief 收到多主播连麦开启
 * @param anchorModel 主播用户模型
 */
- (void)receivedAnchorPKNewAnchorJoined:(VideoChatUserModel *)anchorModel;

/**
 * @brief 收到多主播连麦结束
 */
- (void)receivedAnchorPKEnd;

/**
 * @brief 收到主播静音对方主播消息
 * @param roomID 房间ID
 * @param otherAnchorUserID 对方主播用户ID
 * @param type 类型。0：静音，1：取消静音
 */
- (void)receivedMuteOtherAnchorRoomID:(NSString *)roomID
                    otherAnchorUserID:(NSString *)otherAnchorUserID
                                 type:(VideoChatOtherAnchorMicType)type;

@end

NS_ASSUME_NONNULL_END
