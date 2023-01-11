//
//  VideoChatRoomUserListtCell.h
//  veRTC_Demo
//
//  Created by on 2021/5/19.
//  
//

#import <UIKit/UIKit.h>

@class VideoChatRoomUserListtCell;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatRoomUserListtCellDelegate <NSObject>

- (void)videoChatRoomUserListtCell:(VideoChatRoomUserListtCell *)VideoChatRoomUserListtCell
                       clickButton:(UIButton *)button
                             model:(VideoChatUserModel *)model;


@end

@interface VideoChatRoomUserListtCell : UITableViewCell

@property (nonatomic, strong) VideoChatUserModel *model;

@property (nonatomic, weak) id<VideoChatRoomUserListtCellDelegate> delegate;

@end

NS_ASSUME_NONNULL_END