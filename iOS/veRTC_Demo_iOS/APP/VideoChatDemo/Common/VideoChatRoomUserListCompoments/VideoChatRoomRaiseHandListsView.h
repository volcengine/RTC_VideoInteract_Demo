//
//  VideoChatRoomRaiseHandListsView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/19.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
#import "VideoChatRoomUserListtCell.h"
@class VideoChatRoomRaiseHandListsView;

NS_ASSUME_NONNULL_BEGIN

static NSString *const KClearRedNotification = @"KClearRedNotification";

@protocol VideoChatRoomRaiseHandListsViewDelegate <NSObject>

- (void)VideoChatRoomRaiseHandListsView:(VideoChatRoomRaiseHandListsView *)VideoChatRoomRaiseHandListsView clickButton:(VideoChatUserModel *)model;

@end

@interface VideoChatRoomRaiseHandListsView : UIView

@property (nonatomic, copy) NSArray<VideoChatUserModel *> *dataLists;

@property (nonatomic, weak) id<VideoChatRoomRaiseHandListsViewDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
