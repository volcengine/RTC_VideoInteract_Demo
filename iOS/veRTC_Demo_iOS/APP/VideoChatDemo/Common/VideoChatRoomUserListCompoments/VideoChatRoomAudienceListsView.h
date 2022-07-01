//
//  VideoChatRoomUserListView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/18.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
#import "VideoChatRoomUserListtCell.h"
@class VideoChatRoomAudienceListsView;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatRoomAudienceListsViewDelegate <NSObject>

- (void)VideoChatRoomAudienceListsView:(VideoChatRoomAudienceListsView *)VideoChatRoomAudienceListsView clickButton:(VideoChatUserModel *)model;

@end


@interface VideoChatRoomAudienceListsView : UIView

@property (nonatomic, copy) NSArray<VideoChatUserModel *> *dataLists;

@property (nonatomic, weak) id<VideoChatRoomAudienceListsViewDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
