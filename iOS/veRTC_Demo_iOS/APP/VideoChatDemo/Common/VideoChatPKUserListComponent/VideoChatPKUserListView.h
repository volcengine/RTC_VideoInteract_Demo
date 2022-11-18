//
//  VideoChatPKUserListView.h
//  veRTC_Demo
//
//  Created by on 2022/3/14.
//  
//

#import <UIKit/UIKit.h>
@class VideoChatPKUserListView;

NS_ASSUME_NONNULL_BEGIN

@protocol VideoChatPKUserListViewDelegate <NSObject>

- (void)videoChatPKUserListView:(VideoChatPKUserListView *)view didClickUserModel:(VideoChatUserModel *)userModel;

@end

@interface VideoChatPKUserListView : UIView

@property (nonatomic, copy) NSArray<VideoChatUserModel *> *dataArray;
@property (nonatomic, weak) id<VideoChatPKUserListViewDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
