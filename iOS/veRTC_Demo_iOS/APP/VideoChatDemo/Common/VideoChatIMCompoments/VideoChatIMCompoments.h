//
//  VideoChatIMCompoments.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/5/23.
//  Copyright © 2021 . All rights reserved.
//

#import <Foundation/Foundation.h>
#import "VideoChatIMModel.h"

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatIMCompoments : NSObject

- (instancetype)initWithSuperView:(UIView *)superView;

- (void)addIM:(VideoChatIMModel *)model;

@end

NS_ASSUME_NONNULL_END
