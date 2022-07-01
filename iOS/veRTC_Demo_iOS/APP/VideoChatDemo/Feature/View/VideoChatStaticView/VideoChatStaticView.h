//
//  VideoChatStaticView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/11/29.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatStaticView : NSObject

@property (nonatomic, strong) VideoChatRoomModel *roomModel;
@property (nonatomic, copy) void(^quitLiveBlock)(void);

- (instancetype)initWithSuperView:(UIView *)superView;
- (void)updatePeopleNum:(NSInteger)count;

@end

NS_ASSUME_NONNULL_END
