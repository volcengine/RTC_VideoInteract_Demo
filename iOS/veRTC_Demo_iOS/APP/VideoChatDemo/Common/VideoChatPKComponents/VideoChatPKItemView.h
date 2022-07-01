//
//  VideoChatPKItemView.h
//  veRTC_Demo
//
//  Created by bytedance on 2022/3/15.
//  Copyright © 2022 bytedance. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatPKItemView : UIView

@property (nonatomic, strong) UIView *contentView;
@property (nonatomic, strong) VideoChatUserModel *userModel;
@property (nonatomic, copy) void(^clickMuteButtonBlock)(BOOL isMute);

- (instancetype)initWithOtherAcnhor:(BOOL)isOtherAnchor;

- (void)updateMuteOtherAnchor:(BOOL)isMute;

- (void)updateNetworkQuality:(VideoChatNetworkQualityStatus)status;

@end

NS_ASSUME_NONNULL_END
