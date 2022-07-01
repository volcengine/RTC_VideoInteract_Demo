//
//  VideoChatPKUserListComponents.h
//  veRTC_Demo
//
//  Created by bytedance on 2022/3/14.
//  Copyright © 2022 bytedance. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatPKUserListComponents : NSObject

@property (nonatomic, assign) BOOL isPKWaitingReply;

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel;

- (void)showAnchorList;

- (void)startForwardStream:(NSString *)roomID token:(NSString *)token;

- (void)receivedAnchorPKInvite:(VideoChatUserModel *)anchorModel;

- (void)resetPkWaitingReplyStstus;

@end

NS_ASSUME_NONNULL_END
