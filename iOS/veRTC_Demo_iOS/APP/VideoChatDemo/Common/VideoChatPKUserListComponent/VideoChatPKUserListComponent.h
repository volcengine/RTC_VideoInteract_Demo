//
//  VideoChatPKUserListComponent.h
//  veRTC_Demo
//
//  Created by on 2022/3/14.
//  
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatPKUserListComponent : NSObject

@property (nonatomic, assign) BOOL isPKWaitingReply;

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel;

- (void)showAnchorList;

- (void)startForwardStream:(NSString *)roomID token:(NSString *)token;

- (void)receivedAnchorPKInvite:(VideoChatUserModel *)anchorModel;

- (void)resetPkWaitingReplyStstus;

@end

NS_ASSUME_NONNULL_END
