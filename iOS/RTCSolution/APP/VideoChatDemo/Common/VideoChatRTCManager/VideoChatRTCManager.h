// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "VideoChatRTCManager.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, VideoChatNetworkQualityStatus) {
    VideoChatNetworkQualityStatusNone,
    VideoChatNetworkQualityStatusGood,
    VideoChatNetworkQualityStatusBad,
};
typedef void(^VideoChatNetworkQualityChangeBlock)(VideoChatNetworkQualityStatus status, NSString *uid);

@class VideoChatRTCManager;
@protocol VideoChatRTCManagerDelegate <NSObject>

/**
 * @brief 房间状态改变时的回调。 通过此回调，您会收到与房间相关的警告、错误和事件的通知。 例如，用户加入房间，用户被移出房间等。
 * @param manager GameRTCManager 模型
 * @param joinModel RTCJoinModel模型房间信息、加入成功失败等信息。
 */
- (void)videoChatRTCManager:(VideoChatRTCManager *)manager
         onRoomStateChanged:(RTCJoinModel *)joinModel;

/**
 * @brief 音量信息变化的回调
 * @param videoChatRTCManager GameRTCManager 对象
 * @param volumeInfo 语音音量数据，key 为 user id， value 为音量分贝大小范围[0,255]。
 */
- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager reportAllAudioVolume:(NSDictionary<NSString *, NSNumber *> *_Nonnull)volumeInfo;

/**
 * @brief 收到远端首帧回调
 * @param videoChatRTCManager GameRTCManager 对象
 * @param uid RTC 用户ID
 */
- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager onFirstRemoteVideoUid:(NSString *)uid;

/**
 * @brief 收到本地和远端用户RTC网络状态回调
 * @param status 网络质量枚举
 * @param uid 用户ID
 */
- (void)videoChatRTCManager:(VideoChatRTCManager *_Nonnull)videoChatRTCManager
    didChangeNetworkQuality:(VideoChatNetworkQualityStatus)status
                        uid:(NSString *)uid;

@end

@interface VideoChatRTCManager : BaseRTCManager

@property (nonatomic, weak) id<VideoChatRTCManagerDelegate> delegate;

+ (VideoChatRTCManager *_Nullable)shareRtc;

/**
 * @brief 加入RTC房间
 * @param token RTC Token
 * @param roomID RTC 房间ID
 * @param uid RTC 用户ID
 * @param isHost 用户角色。YES：主播， NO：观众
 */
- (void)joinRTCRoomWithToken:(NSString *)token
                      roomID:(NSString *)roomID
                         uid:(NSString *)uid
                    userRole:(BOOL)isHost;

/**
 * @brief 离开 RTC 房间
 */
- (void)leaveRTCRoom;

#pragma mark - Audience on mic

/**
 * @brief 成为上麦嘉宾
 * @param camera 相机开关状态。YES：开启， NO：关闭
 * @param microphone 麦克风开关状态。YES：开启， NO：关闭
 */
- (void)makeGuest:(BOOL)camera
       microphone:(BOOL)microphone;

/**
 * @brief 成为观众，嘉宾下麦后成为观众
 */
- (void)makeAudience;

#pragma mark - Host PK

/**
 * @brief 开启跨房间转推
 * @param roomID 对方的房间ID
 * @param token 加入对方房间所需要的 RTC Token
 */
- (void)startForwardStream:(NSString *)roomID
                     token:(NSString *)token;

/**
 * @brief 关闭跨房间转推
 */
- (void)stopForwardStream;

#pragma mark - Setting

/**
 * @brief 切换前置/后置相机
 * @param enable true：开启, false：关闭
 */
- (void)switchVideoCapture:(BOOL)enable;

/**
 * @brief 切换前置/后置相机
 */
- (void)switchCamera;

/**
 * @brief 控制本地视频流的发送状态：发送/不发送
 * @param isPublish true：发送, false：不发送
 */
- (void)publishVideoStream:(BOOL)isPublish;

/**
 * @brief 控制本地音频流的发送状态：发送/不发送
 * @param isPublish true：发送, false：不发送
 */
- (void)publishAudioStream:(BOOL)isPublish;

/**
 * @brief 订阅远端音频留
 * @param userID RTC 用户ID
 * @param isSubscribe true：订阅, false：取消订阅
 */
- (void)subscribeRemoteAudioStream:(NSString *)userID
                         subscribe:(BOOL)isSubscribe;

/**
 * @brief 根据用户角色，更新 RTC 视频编码参数
 * @param isHost true：主播, false：观众
 */
- (void)updateVideoConfigWithHost:(BOOL)isHost;

/**
 * @brief 更新 RTC 编码分辨率。
 * @param size 分辨率
 */
- (void)updateResolution:(CGSize)size;

/**
 * @brief 更新 RTC 编码帧率
 * @param fps 帧率
 */
- (void)updateFrameRate:(CGFloat)fps;

/**
 * @brief 更新 RTC 编码码率
 * @param bitRate 码率
 */
- (void)updateBitRate:(NSInteger)bitRate;

#pragma mark - Background Music Method

/**
 * @brief 开启背景音乐
 * @param filePath 音乐沙盒路径
 */
- (void)startBackgroundMusic:(NSString *)filePath;

/**
 * @brief 暂停背景音乐
 */
- (void)pauseBackgroundMusic;

/**
 * @brief 恢复背景音乐播放
 */
- (void)resumeBackgroundMusic;

/**
 * @brief 设置人声音量
 * @param volume 音量
 */
- (void)setRecordingVolume:(NSInteger)volume;

/**
 * @brief 设置背景音量
 * @param volume 音量
 */
- (void)setMusicVolume:(NSInteger)volume;

#pragma mark - Render

/**
 * @brief 获取 RTC 渲染 UIView
 * @param uid 用户ID
 */
- (UIView *)getStreamViewWithUid:(NSString *)uid;
- (void)bindCanvasViewToUid:(NSString *)uid;

@end

NS_ASSUME_NONNULL_END
