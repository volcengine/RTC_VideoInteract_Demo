//
//  VideoChatSeatItemView.h
//  veRTC_Demo
//
//  Created by on 2021/11/29.
//  
//

#import <UIKit/UIKit.h>

@interface VideoChatSeatItemView : UIView

@property (nonatomic, assign) NSInteger index;

@property (nonatomic, strong) VideoChatSeatModel *seatModel;

@property (nonatomic, copy) void (^clickBlock)(VideoChatSeatModel *seatModel);

- (void)updateRender;

- (void)updateNetworkQualityStstus:(VideoChatNetworkQualityStatus)status;

@end
