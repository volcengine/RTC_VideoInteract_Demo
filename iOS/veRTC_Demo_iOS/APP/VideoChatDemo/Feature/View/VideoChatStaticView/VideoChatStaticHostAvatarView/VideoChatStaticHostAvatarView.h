//
//  VideoChatStaticHostAvatarView.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/19.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
#import "VideoChatRoomModel.h"

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatStaticHostAvatarView : UIView

@property (nonatomic, strong) VideoChatRoomModel *roomModel;

@end

NS_ASSUME_NONNULL_END
