//
//  VideoChatRoomUserListCompoments.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/19.
//  Copyright © 2021 . All rights reserved.
//

#import <Foundation/Foundation.h>
#import "VideoChatRoomAudienceListsView.h"
#import "VideoChatRoomRaiseHandListsView.h"

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatRoomUserListCompoments : NSObject

@property (nonatomic, assign) BOOL isInviteInteractionWaitingReply;

- (void)showRoomModel:(VideoChatRoomModel *)roomModel
               seatID:(NSString *)seatID
         dismissBlock:(void (^)(void))dismissBlock;

- (void)update;

- (void)updateWithRed:(BOOL)isRed;

- (void)updateCloseChatRoom:(BOOL)isHidden;

- (void)changeChatRoomModeDismissUserListView;

- (void)resetInviteInteractionWaitingReplyStstus;

@end

NS_ASSUME_NONNULL_END
