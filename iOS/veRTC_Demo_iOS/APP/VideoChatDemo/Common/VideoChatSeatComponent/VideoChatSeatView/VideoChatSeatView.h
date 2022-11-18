//
//  VideoChatSeatView.h
//  veRTC_Demo
//
//  Created by on 2021/11/29.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSeatView : UIView

@property (nonatomic, copy) void (^clickBlock)(VideoChatSeatModel *seatModel);

@property (nonatomic, copy) NSArray<VideoChatSeatModel *> *seatList;

- (void)addSeatModel:(VideoChatSeatModel *)seatModel;

- (void)removeUserModel:(VideoChatUserModel *)userModel;

- (void)updateSeatModel:(VideoChatSeatModel *)seatModel;

- (void)updateSeatRender:(NSString *)uid;

- (void)updateSeatVolume:(NSDictionary *)volumeDic;

- (void)updateNetworkQuality:(VideoChatNetworkQualityStatus)status uid:(NSString *)uid;

@end

NS_ASSUME_NONNULL_END
