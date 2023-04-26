// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "BaseButton.h"

typedef NS_ENUM(NSInteger, VideoChatSheetStatus) {
    // 主播发起邀请
    VideoChatSheetStatusInvite = 0,
    // 主播提嘉宾下麦
    VideoChatSheetStatusKick,
    // 开启麦克风
    VideoChatSheetStatusOpenMic,
    // 关闭麦克风
    VideoChatSheetStatusCloseMic,
    // 封锁麦位
    VideoChatSheetStatusLock,
    // 解锁麦位
    VideoChatSheetStatusUnlock,
    // 观众申请上麦
    VideoChatSheetStatusApply,
    // 嘉宾主动下麦
    VideoChatSheetStatusLeave,
};

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSeatItemButton : BaseButton

@property (nonatomic, copy) NSString *desTitle;

@property (nonatomic, assign) VideoChatSheetStatus sheetState;

@end

NS_ASSUME_NONNULL_END
