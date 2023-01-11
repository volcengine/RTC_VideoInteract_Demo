//
//  VideoChatRoomUserListView.h
//  veRTC_Demo
//
//  Created by on 2021/5/18.
//  
//

#import <UIKit/UIKit.h>
#import "VideoChatRoomUserListtCell.h"
@class VideoChatRoomAudienceListsView;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatRoomAudienceListsViewDelegate <NSObject>

- (void)videoChatRoomAudienceListsView:(VideoChatRoomAudienceListsView *)videoChatRoomAudienceListsView
                           clickButton:(UIButton *)button
                                 model:(VideoChatUserModel *)model;

@end


@interface VideoChatRoomAudienceListsView : UIView

@property (nonatomic, copy) NSArray<VideoChatUserModel *> *dataLists;

@property (nonatomic, weak) id<VideoChatRoomAudienceListsViewDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
