//
//  VideoChatRoomSettingCompoments.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/10/25.
//  Copyright © 2021 . All rights reserved.
//

#import "VideoChatUserModel.h"
#import <UIKit/UIKit.h>

typedef NS_ENUM(NSUInteger, VideoChatRoomSettingType) {
    VideoChatRoomSettingTypeCreateRoom,
    VideoChatRoomSettingTypeGuest
};

@interface VideoChatRoomSettingCompoments : NSObject

@property (nonatomic, assign) BOOL mic;
@property (nonatomic, assign) BOOL camera;
@property (nonatomic, copy) void(^clickMusicBlock)(void);

- (instancetype)initWithHost:(BOOL)isHost;

- (void)showWithType:(VideoChatRoomSettingType)type
       fromSuperView:(UIView *)superView
           roomModel:(VideoChatRoomModel *)videoChatRoomModel;

- (void)updateUserMic:(BOOL)isEnable;

- (void)resetMicAndCameraStatus;

@end
