//
//  VideoChatTextInputCompoments.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/11/30.
//  Copyright © 2021 . All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatTextInputCompoments : NSObject

@property (nonatomic, copy) void (^clickSenderBlock)(NSString *text);

- (void)showWithRoomModel:(VideoChatRoomModel *)roomModel;

@end

NS_ASSUME_NONNULL_END
