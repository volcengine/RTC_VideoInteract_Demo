//
//  VideoChatCreateRoomControlView.h
//  veRTC_Demo
//
//  Created by on 2021/10/21.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class VideoChatCreateRoomControlView;

@protocol VideoChatCreateRoomControlViewDelegate <NSObject>

- (void)videoChatCreateRoomControlView:(VideoChatCreateRoomControlView *)videoChatCreateRoomControlView didClickedSwitchCameraButton:(UIButton *)button;

- (void)videoChatCreateRoomControlView:(VideoChatCreateRoomControlView *)videoChatCreateRoomControlView didClickedBeautyButton:(UIButton *)button;

- (void)videoChatCreateRoomControlView:(VideoChatCreateRoomControlView *)videoChatCreateRoomControlView didClickedSettingButton:(UIButton *)button;

@end

@interface VideoChatCreateRoomControlView : UIView

@property (nonatomic, weak) id<VideoChatCreateRoomControlViewDelegate> delegate;

@end

NS_ASSUME_NONNULL_END
