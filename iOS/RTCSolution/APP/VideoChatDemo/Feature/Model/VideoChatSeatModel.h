// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <Foundation/Foundation.h>
#import "VideoChatUserModel.h"

@interface VideoChatSeatModel : NSObject
// status. 0 : 封锁, 1 : 解锁麦位
@property (nonatomic, assign) NSInteger status;

@property (nonatomic, assign) NSInteger index;

@property (nonatomic, strong) VideoChatUserModel *userModel;

@end
