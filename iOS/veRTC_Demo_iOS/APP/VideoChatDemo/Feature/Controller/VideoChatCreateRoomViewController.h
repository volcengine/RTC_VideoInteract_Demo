//
//  CreateRoomViewController.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/18.
//  Copyright © 2021 . All rights reserved.
//

#import "VideoChatNavViewController.h"

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatCreateRoomViewController : VideoChatNavViewController

- (instancetype)initWithRoomModel:(VideoChatRoomModel *)roomModel
                        userModel:(VideoChatUserModel *)userModel
                         rtcToekn:(NSString *)rtcToekn;

@end

NS_ASSUME_NONNULL_END
