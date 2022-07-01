//
//  VideoChatRoomTableView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/18.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
#import "VideoChatRoomCell.h"
@class VideoChatRoomTableView;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatRoomTableViewDelegate <NSObject>

- (void)VideoChatRoomTableView:(VideoChatRoomTableView *)VideoChatRoomTableView didSelectRowAtIndexPath:(VideoChatRoomModel *)model;

@end

@interface VideoChatRoomTableView : UIView

@property (nonatomic, copy) NSArray *dataLists;

@property (nonatomic, weak) id<VideoChatRoomTableViewDelegate> delegate;


@end

NS_ASSUME_NONNULL_END
