//
//  VideoChatRoomTopSelectView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/24.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
@class VideoChatRoomTopSelectView;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatRoomTopSelectViewDelegate <NSObject>

- (void)VideoChatRoomTopSelectView:(VideoChatRoomTopSelectView *)VideoChatRoomTopSelectView clickCancelAction:(id)model;

- (void)VideoChatRoomTopSelectView:(VideoChatRoomTopSelectView *)VideoChatRoomTopSelectView clickSwitchItem:(BOOL)isAudience;

@end

@interface VideoChatRoomTopSelectView : UIView

@property (nonatomic, weak) id<VideoChatRoomTopSelectViewDelegate> delegate;

@property (nonatomic, copy) NSString *titleStr;

- (void)updateWithRed:(BOOL)isRed;

- (void)updateSelectItem:(BOOL)isLeft;

@end

NS_ASSUME_NONNULL_END
