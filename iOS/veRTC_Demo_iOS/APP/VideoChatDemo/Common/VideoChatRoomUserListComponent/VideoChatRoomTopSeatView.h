//
//  VideoChatRoomTopSeatView.h
//  veRTC_Demo
//
//  Created by on 2021/11/30.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomTopSeatView : UIView

@property (nonatomic, copy) void(^clickCloseChatRoomBlock)(void);

- (void)updateCloseChatRoom:(BOOL)isHidden;

@end

NS_ASSUME_NONNULL_END
