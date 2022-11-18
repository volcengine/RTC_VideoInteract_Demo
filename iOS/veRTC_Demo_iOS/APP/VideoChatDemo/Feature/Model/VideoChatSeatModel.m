//
//  VideoChatSeatModel.m
//  veRTC_Demo
//
//  Created by on 2021/11/23.
//  
//

#import "VideoChatSeatModel.h"

@implementation VideoChatSeatModel

+ (NSDictionary *)modelCustomPropertyMapper {
    return @{@"userModel" : @"guest_info"};
}

@end
