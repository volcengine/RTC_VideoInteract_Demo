//
//  VideoChatSeatModel.m
//  veRTC_Demo
//
//  Created by bytedance on 2021/11/23.
//  Copyright © 2021 . All rights reserved.
//

#import "VideoChatSeatModel.h"

@implementation VideoChatSeatModel

+ (NSDictionary *)modelCustomPropertyMapper {
    return @{@"userModel" : @"guest_info"};
}

@end
