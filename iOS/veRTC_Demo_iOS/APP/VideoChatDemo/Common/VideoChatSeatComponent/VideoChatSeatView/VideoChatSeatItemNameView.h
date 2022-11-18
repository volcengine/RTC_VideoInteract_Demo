//
//  VideoChatSeatItemNameView.h
//  veRTC_Demo
//
//  Created by on 2021/12/22.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSeatItemNameView : UIView

@property (nonatomic, strong) VideoChatUserModel *userModel;
@property (nonatomic, assign) BOOL isPK;

@end

NS_ASSUME_NONNULL_END
