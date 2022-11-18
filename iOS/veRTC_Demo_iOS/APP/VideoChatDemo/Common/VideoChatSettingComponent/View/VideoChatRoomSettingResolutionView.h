//
//  VideoChatRoomSettingResolutionView.h
//  veRTC_Demo
//
//  Created by on 2021/10/24.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomSettingResolutionView : UIView
@property (nonatomic, copy) void (^resolutionChangeBlock)(NSInteger index);

- (void)setSelectedResKey:(NSString *)resKey;
@end

NS_ASSUME_NONNULL_END
