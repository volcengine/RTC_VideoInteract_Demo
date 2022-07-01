//
//  VideoChatSeatItemCenterView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/12/22.
//  Copyright © 2021 bytedance. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSeatItemCenterView : UIView

- (void)updateImageName:(NSString *)imageName
                message:(NSString *)message;

@end

NS_ASSUME_NONNULL_END
