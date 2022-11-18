//
//  VideoChatTextInputComponent.h
//  veRTC_Demo
//
//  Created by on 2021/11/30.
//  
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatTextInputComponent : NSObject

@property (nonatomic, copy) void (^clickSenderBlock)(NSString *text);

- (void)showWithRoomModel:(VideoChatRoomModel *)roomModel;

@end

NS_ASSUME_NONNULL_END
