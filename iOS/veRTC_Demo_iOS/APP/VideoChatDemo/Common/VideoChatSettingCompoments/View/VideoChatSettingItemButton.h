//
//  VideoChatSettingItemButton.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/10/24.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSettingItemButton : UIButton
@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) NSString *imageName;
@property (nonatomic, copy) NSString *imageNameSelected;
@property (nonatomic, assign) BOOL active;
@end

NS_ASSUME_NONNULL_END
