//
//  VideoChatSeatItemView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/11/29.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

@interface VideoChatSeatItemView : UIView

@property (nonatomic, assign) NSInteger index;

@property (nonatomic, strong) VideoChatSeatModel *seatModel;

@property (nonatomic, copy) void (^clickBlock)(VideoChatSeatModel *seatModel);

- (void)updateRender;

- (void)updateNetworkQualityStstus:(VideoChatNetworkQualityStatus)status;

@end
