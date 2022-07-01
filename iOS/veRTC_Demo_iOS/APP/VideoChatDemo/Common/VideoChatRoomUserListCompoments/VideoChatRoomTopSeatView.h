//
//  VideoChatRoomTopSeatView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/11/30.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomTopSeatView : UIView

@property (nonatomic, copy) void(^clickCloseChatRoomBlock)(void);

- (void)updateCloseChatRoom:(BOOL)isHidden;

@end

NS_ASSUME_NONNULL_END
