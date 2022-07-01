//
//  VideoChatRoomSettingResolutionView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/10/24.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomSettingResolutionView : UIView
@property (nonatomic, copy) void (^resolutionChangeBlock)(NSInteger index);

- (void)setSelectedResKey:(NSString *)resKey;
@end

NS_ASSUME_NONNULL_END
