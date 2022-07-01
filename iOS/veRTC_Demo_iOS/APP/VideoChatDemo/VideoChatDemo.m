//
//  VideoChatDemo.m
//  VideoChatDemo
//
//  Created by bytedance on 2022/5/18.
//

#import "VideoChatDemo.h"
#import "VideoChatRoomListsViewController.h"
#import "NetworkReachabilityManager.h"

@implementation VideoChatDemo

- (void)pushDemoViewControllerBlock:(void (^)(BOOL))block {
    [VideoChatRTCManager shareRtc].networkDelegate = [NetworkReachabilityManager sharedManager];
    [[VideoChatRTCManager shareRtc] connect:@"videochat"
                                 loginToken:[LocalUserComponents userModel].loginToken
                                      block:^(BOOL result) {
        if (result) {
            VideoChatRoomListsViewController *next = [[VideoChatRoomListsViewController alloc] init];
            UIViewController *topVC = [DeviceInforTool topViewController];
            [topVC.navigationController pushViewController:next animated:YES];
        } else {
            [[ToastComponents shareToastComponents] showWithMessage:@"连接失败"];
        }
        if (block) {
            block(result);
        }
    }];
}

@end
