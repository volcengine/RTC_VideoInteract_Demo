//
//  VideoChatSeatNetworkQualityView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/12/24.
//  Copyright © 2021 bytedance. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSeatNetworkQualityView : UIView

- (void)updateNetworkQualityStstus:(VideoChatNetworkQualityStatus)status;

@end

NS_ASSUME_NONNULL_END
