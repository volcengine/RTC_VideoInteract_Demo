//
//  VideoChatPKUserListTableViewCell.h
//  veRTC_Demo
//
//  Created by bytedance on 2022/3/14.
//  Copyright © 2022 bytedance. All rights reserved.
//

#import <UIKit/UIKit.h>
@class VideoChatPKUserListTableViewCell;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatPKUserListTableViewCellDelegate <NSObject>

- (void)videoChatPKUserListTableViewCell:(VideoChatPKUserListTableViewCell *)cell didClickUserModel:(VideoChatUserModel *)model;

@end

@interface VideoChatPKUserListTableViewCell : UITableViewCell

@property (nonatomic, strong) VideoChatUserModel *model;
@property (nonatomic, weak) id<VideoChatPKUserListTableViewCellDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
