//
//  VideoChatSettingSingleSelectView.h
//  veRTC_Demo
//
//  Created by on 2021/10/22.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface VideoChatSettingSingleSelectView : UIView
@property (nonatomic, copy) void (^itemChangeBlock)(NSInteger index);

- (instancetype)initWithTitle:(NSString *)title optionArray:(NSArray *)optionArray;

- (void)setSelectedIndex:(NSInteger)selectedIndex;
@end

NS_ASSUME_NONNULL_END
