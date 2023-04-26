// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "VideoChatRTCManager.h"
#import "VideoChatSettingVideoConfig.h"

@interface VideoChatRTCManager () <ByteRTCVideoDelegate>
// RTC / RTS 房间
@property (nonatomic, strong, nullable) ByteRTCRoom *rtcRoom;
@property (nonatomic, assign) int audioMixingID;
@property (nonatomic, assign) ByteRTCCameraID cameraID;
@property (nonatomic, strong) NSMutableDictionary<NSString *, UIView *> *streamViewDic;
@property (nonatomic, copy) VideoChatNetworkQualityChangeBlock networkQualityBlock;
@property (nonatomic, strong) ByteRTCVideoEncoderConfig *encoderConfig;

@end

@implementation VideoChatRTCManager

+ (VideoChatRTCManager *_Nullable)shareRtc {
    static VideoChatRTCManager *rtcManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        rtcManager = [[VideoChatRTCManager alloc] init];
    });
    return rtcManager;
}

- (void)configeRTCEngine {
    [super configeRTCEngine];
    _cameraID = ByteRTCCameraIDFront;
    _audioMixingID = 3001;
}

- (void)joinRTCRoomWithToken:(NSString *)token
                      roomID:(NSString *)roomID
                         uid:(NSString *)uid
                    userRole:(BOOL)isHost {
    // 设置音频路由模式，YES 扬声器/NO 听筒
    [self.rtcEngineKit setDefaultAudioRoute:ByteRTCAudioRouteSpeakerphone];

    // 开启/关闭发言者音量键控
    ByteRTCAudioPropertiesConfig *audioPropertiesConfig = [[ByteRTCAudioPropertiesConfig alloc] init];
    audioPropertiesConfig.interval = 300;
    [self.rtcEngineKit enableAudioPropertiesReport:audioPropertiesConfig];
    
    // 开启本地和编码镜像
    if (self.cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }

    // 加入房间，开始连麦,需要申请AppId和Token
    ByteRTCUserInfo *userInfo = [[ByteRTCUserInfo alloc] init];
    userInfo.userId = uid;
    
    ByteRTCRoomConfig *config = [[ByteRTCRoomConfig alloc] init];
    config.profile = ByteRTCRoomProfileInteractivePodcast;
    config.isAutoPublish = YES;
    config.isAutoSubscribeAudio = YES;
    config.isAutoSubscribeVideo = YES;
    
    self.rtcRoom = [self.rtcEngineKit createRTCRoom:roomID];
    self.rtcRoom.delegate = self;
    [self.rtcRoom joinRoom:token userInfo:userInfo roomConfig:config];
    
    if (!isHost) {
        // 观众加入房间时，需要关闭本地音频/视频采集
        [self.rtcEngineKit stopAudioCapture];
        [self.rtcEngineKit stopVideoCapture];
        // 观众加入房间时，需要设置为隐身状态
        [self.rtcRoom setUserVisibility:NO];
    }
}

- (void)leaveRTCRoom {
    // 离开频道
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    [audioMixingManager stopAudioMixing:_audioMixingID];
    [self.rtcEngineKit stopAudioCapture];
    [self switchFrontFacingCamera:YES];
    [self.rtcRoom leaveRoom];
    [self.streamViewDic removeAllObjects];
}

#pragma mark - Make Guest

- (void)makeGuest:(BOOL)camera
       microphone:(BOOL)microphone {
    // 设置为可见状态
    // Set to visible state
    [self.rtcRoom setUserVisibility:YES];
    
    // 开关麦克风、相机采集
    [self switchVideoCapture:camera];
    [self switchAudioCapture:microphone];
}

- (void)makeAudience {
    // 设置为隐身状态
    // Set to invisibility state
    [self.rtcRoom setUserVisibility:NO];
    
    // 关闭麦克风、相机采集
    [self switchVideoCapture:NO];
    [self switchAudioCapture:NO];
    
    // 切换为前置摄像头
    [self switchFrontFacingCamera:YES];
}

#pragma mark - Make CoHost

- (void)startForwardStream:(NSString *)roomID token:(NSString *)token {
    [self stopForwardStream];
    
    // 开启转推
    ForwardStreamConfiguration *configuration = [[ForwardStreamConfiguration alloc] init];
    configuration.roomId = roomID;
    configuration.token = token;
    [self.rtcRoom startForwardStreamToRooms:@[configuration]];
}

- (void)stopForwardStream {
    // 关闭转推
    [self.rtcRoom stopForwardStreamToRooms];
}

#pragma mark - Setting

- (void)switchVideoCapture:(BOOL)enable {
    // 开启/关闭 本地视频采集
    if (enable) {
        [SystemAuthority authorizationStatusWithType:AuthorizationTypeCamera
                                               block:^(BOOL isAuthorize) {
            if (isAuthorize) {
                [self.rtcEngineKit startVideoCapture];
                [self.rtcRoom publishStream:ByteRTCMediaStreamTypeVideo];
            }
        }];
    } else {
        [self.rtcEngineKit stopVideoCapture];
    }
}

- (void)switchCamera {
    // 切换 前置/后置 摄像头
    if (self.cameraID == ByteRTCCameraIDFront) {
        self.cameraID = ByteRTCCameraIDBack;
    } else {
        self.cameraID = ByteRTCCameraIDFront;
    }
    [self switchFrontFacingCamera:(self.cameraID == ByteRTCCameraIDFront)];
}

- (void)publishVideoStream:(BOOL)isPublish {
    if (isPublish) {
        [self.rtcRoom publishStream:ByteRTCMediaStreamTypeVideo];
    } else {
        [self.rtcRoom unpublishStream:ByteRTCMediaStreamTypeVideo];
    }
}

- (void)publishAudioStream:(BOOL)isPublish {
    if (isPublish) {
        [self.rtcRoom publishStream:ByteRTCMediaStreamTypeAudio];
    } else {
        [self.rtcRoom unpublishStream:ByteRTCMediaStreamTypeAudio];
    }
}

- (void)subscribeRemoteAudioStream:(NSString *)userID
                         subscribe:(BOOL)isSubscribe {
    // 订阅/取消订阅远端音频流
    if (isSubscribe) {
        [self.rtcRoom subscribeStream:userID
                      mediaStreamType:ByteRTCMediaStreamTypeAudio];
    } else {
        [self.rtcRoom unsubscribeStream:userID
                        mediaStreamType:ByteRTCMediaStreamTypeAudio];
    }
}

- (void)updateVideoConfigWithHost:(BOOL)isHost {
    if (isHost) {
        VideoChatSettingVideoConfig *config = [VideoChatSettingVideoConfig defultVideoConfig];
        
        self.encoderConfig.width = config.videoSize.width;
        self.encoderConfig.height = config.videoSize.height;
        self.encoderConfig.frameRate = config.fps;
        self.encoderConfig.maxBitrate = config.bitrate;
        
        [self.rtcEngineKit setMaxVideoEncoderConfig:self.encoderConfig];
    } else {
        self.encoderConfig.width = 240;
        self.encoderConfig.height = 320;
        self.encoderConfig.frameRate = 15;
        self.encoderConfig.maxBitrate = 400;
        [self.rtcEngineKit setMaxVideoEncoderConfig:self.encoderConfig];
    }
}

- (void)updateResolution:(CGSize)size {
    self.encoderConfig.width = size.width;
    self.encoderConfig.height = size.height;
    [self.rtcEngineKit setMaxVideoEncoderConfig:self.encoderConfig];
}

- (void)updateFrameRate:(CGFloat)fps {
    self.encoderConfig.frameRate = fps;
    [self.rtcEngineKit setMaxVideoEncoderConfig:self.encoderConfig];
}

- (void)updateBitRate:(NSInteger)bitRate {
    self.encoderConfig.maxBitrate = bitRate;
    [self.rtcEngineKit setMaxVideoEncoderConfig:self.encoderConfig];
}

#pragma mark - Background Music Method

- (void)startBackgroundMusic:(NSString *)filePath {
    if (IsEmptyStr(filePath)) {
        return;
    }
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    
    ByteRTCAudioMixingConfig *config = [[ByteRTCAudioMixingConfig alloc] init];
    config.type = ByteRTCAudioMixingTypePlayoutAndPublish;
    config.playCount = -1;
    [audioMixingManager startAudioMixing:_audioMixingID filePath:filePath config:config];
}

- (void)pauseBackgroundMusic {
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    
    [audioMixingManager pauseAudioMixing:_audioMixingID];
}

- (void)resumeBackgroundMusic {
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    
    [audioMixingManager resumeAudioMixing:_audioMixingID];
}

- (void)setRecordingVolume:(NSInteger)volume {
    [self.rtcEngineKit setCaptureVolume:ByteRTCStreamIndexMain volume:(int)volume];
}

- (void)setMusicVolume:(NSInteger)volume {
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    
    [audioMixingManager setAudioMixingVolume:_audioMixingID volume:(int)volume type:ByteRTCAudioMixingTypePlayoutAndPublish];
}

#pragma mark - Render

- (UIView *)getStreamViewWithUid:(NSString *)uid {
    if (IsEmptyStr(uid)) {
        return nil;
    }
    NSString *typeStr = @"";
    if ([uid isEqualToString:[LocalUserComponent userModel].uid]) {
        typeStr = @"self";
    } else {
        typeStr = @"remote";
    }
    NSString *key = [NSString stringWithFormat:@"%@_%@", typeStr, uid];
    UIView *view = self.streamViewDic[key];
    return view;
}

- (void)bindCanvasViewToUid:(NSString *)uid {
    dispatch_queue_async_safe(dispatch_get_main_queue(), (^{
        if ([uid isEqualToString:[LocalUserComponent userModel].uid]) {
            UIView *view = [self getStreamViewWithUid:uid];
            if (!view) {
                UIView *streamView = [[UIView alloc] init];
                streamView.hidden = YES;
                ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
                canvas.renderMode = ByteRTCRenderModeHidden;
                canvas.view.backgroundColor = [UIColor clearColor];
                canvas.view = streamView;
                [self.rtcEngineKit setLocalVideoCanvas:ByteRTCStreamIndexMain
                                            withCanvas:canvas];
                NSString *key = [NSString stringWithFormat:@"self_%@", uid];
                [self.streamViewDic setValue:streamView forKey:key];
            }
        } else {
            UIView *remoteRoomView = [self getStreamViewWithUid:uid];
            if (!remoteRoomView) {
                remoteRoomView = [[UIView alloc] init];
                remoteRoomView.hidden = NO;
                ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
                canvas.renderMode = ByteRTCRenderModeHidden;
                canvas.view.backgroundColor = [UIColor clearColor];
                canvas.view = remoteRoomView;
                
                ByteRTCRemoteStreamKey *streamKey = [[ByteRTCRemoteStreamKey alloc] init];
                streamKey.userId = uid;
                streamKey.roomId = self.rtcRoom.getRoomId;
                streamKey.streamIndex = ByteRTCStreamIndexMain;
                
                [self.rtcEngineKit setRemoteVideoCanvas:streamKey withCanvas:canvas];
                
                NSString *groupKey = [NSString stringWithFormat:@"remote_%@", uid];
                [self.streamViewDic setValue:remoteRoomView forKey:groupKey];
            }
        }
    }));
}

#pragma mark - NetworkQuality

- (void)didChangeNetworkQuality:(VideoChatNetworkQualityChangeBlock)block {
    self.networkQualityBlock = block;
}

#pragma mark - ByteRTCRoomDelegate

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onRoomStateChanged:(NSString *)roomId
        withUid:(NSString *)uid
          state:(NSInteger)state
      extraInfo:(NSString *)extraInfo {
    [super rtcRoom:rtcRoom onRoomStateChanged:roomId withUid:uid state:state extraInfo:extraInfo];
    [[VideoChatRTCManager shareRtc] bindCanvasViewToUid:uid];
    
    dispatch_queue_async_safe(dispatch_get_main_queue(), ^{
        RTCJoinModel *joinModel = [RTCJoinModel modelArrayWithClass:extraInfo state:state roomId:roomId];
        if ([self.delegate respondsToSelector:@selector(videoChatRTCManager:onRoomStateChanged:)]) {
            [self.delegate videoChatRTCManager:self onRoomStateChanged:joinModel];
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserJoined:(ByteRTCUserInfo *)userInfo elapsed:(NSInteger)elapsed {
    [[VideoChatRTCManager shareRtc] bindCanvasViewToUid:userInfo.userId];
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onLocalStreamStats:(ByteRTCLocalStreamStats *)stats {

    VideoChatNetworkQualityStatus liveStatus = VideoChatNetworkQualityStatusNone;
    if (stats.tx_quality == ByteRTCNetworkQualityExcellent ||
        stats.tx_quality == ByteRTCNetworkQualityGood) {
        liveStatus = VideoChatNetworkQualityStatusGood;
    } else {
        liveStatus = VideoChatNetworkQualityStatusBad;
    }
    if ([self.delegate respondsToSelector:@selector(videoChatRTCManager:didChangeNetworkQuality:uid:)]) {
        [self.delegate videoChatRTCManager:self
                   didChangeNetworkQuality:liveStatus
                                       uid:[LocalUserComponent userModel].uid];
    }
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onRemoteStreamStats:(ByteRTCRemoteStreamStats *)stats {
    VideoChatNetworkQualityStatus liveStatus = VideoChatNetworkQualityStatusNone;
    if (stats.tx_quality == ByteRTCNetworkQualityExcellent ||
        stats.tx_quality == ByteRTCNetworkQualityGood) {
        liveStatus = VideoChatNetworkQualityStatusGood;
    } else {
        liveStatus = VideoChatNetworkQualityStatusBad;
    }
    if ([self.delegate respondsToSelector:@selector(videoChatRTCManager:didChangeNetworkQuality:uid:)]) {
        [self.delegate videoChatRTCManager:self
                   didChangeNetworkQuality:liveStatus
                                       uid:stats.uid];
    }
}

#pragma mark - ByteRTCVideoDelegate

- (void)rtcEngine:(ByteRTCVideo *)engine onFirstRemoteVideoFrameRendered:(ByteRTCRemoteStreamKey *)streamKey withFrameInfo:(ByteRTCVideoFrameInfo *)frameInfo {
    dispatch_queue_async_safe(dispatch_get_main_queue(), ^{
        if ([self.delegate respondsToSelector:@selector(videoChatRTCManager:onFirstRemoteVideoUid:)]) {
            [self.delegate videoChatRTCManager:self onFirstRemoteVideoUid:streamKey.userId];
        }
    });   
}

- (void)rtcEngine:(ByteRTCVideo *)engine onRemoteAudioPropertiesReport:(NSArray<ByteRTCRemoteAudioPropertiesInfo *> *)audioPropertiesInfos totalRemoteVolume:(NSInteger)totalRemoteVolume {
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    for (int i = 0; i < audioPropertiesInfos.count; i++) {
        ByteRTCRemoteAudioPropertiesInfo *model = audioPropertiesInfos[i];
        [dic setValue:@(model.audioPropertiesInfo.linearVolume) forKey:model.streamKey.userId];
    }
    if ([self.delegate respondsToSelector:@selector(videoChatRTCManager:reportAllAudioVolume:)]) {
        [self.delegate videoChatRTCManager:self reportAllAudioVolume:dic];
    }
}

#pragma mark - Private Action

- (void)switchAudioCapture:(BOOL)enable {
    // 开启/关闭 本地音频采集
    if (enable) {
        [SystemAuthority authorizationStatusWithType:AuthorizationTypeAudio
                                               block:^(BOOL isAuthorize) {
            if (isAuthorize) {
                [self.rtcEngineKit startAudioCapture];
                [self.rtcRoom publishStream:ByteRTCMediaStreamTypeAudio];
            }
        }];
    } else {
        [self.rtcEngineKit stopAudioCapture];
    }
}

- (void)switchFrontFacingCamera:(BOOL)isFront {
    self.cameraID = isFront ? ByteRTCCameraIDFront : ByteRTCCameraIDBack;
    
    if (self.cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }
    
    [self.rtcEngineKit switchCamera:self.cameraID];
}

#pragma mark - Getter

- (NSMutableDictionary<NSString *, UIView *> *)streamViewDic {
    if (!_streamViewDic) {
        _streamViewDic = [[NSMutableDictionary alloc] init];
    }
    return _streamViewDic;
}

- (ByteRTCVideoEncoderConfig *)encoderConfig {
    if(!_encoderConfig) {
        _encoderConfig = [[ByteRTCVideoEncoderConfig alloc] init];
    }
    return _encoderConfig;
}
@end
