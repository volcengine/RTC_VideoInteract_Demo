
#import "VideoChatRTCManager.h"
#import "AlertActionManager.h"
#import "SystemAuthority.h"
#import "VideoChatSettingVideoConfig.h"

@interface VideoChatRTCManager () <ByteRTCVideoDelegate>

@property (nonatomic, assign) int audioMixingID;
@property (nonatomic, assign) ByteRTCCameraID cameraID;
@property (nonatomic, strong) NSMutableDictionary<NSString *, UIView *> *streamViewDic;
@property (nonatomic, copy) VideoChatNetworkQualityChangeBlock networkQualityBlock;
@property (nonatomic, strong) ByteRTCVideoEncoderConfig *solution;
@property (nonatomic, assign) BOOL isStartAudioCapture;

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

#pragma mark - Publish Action
- (void)configeRTCEngine {
    
    _cameraID = ByteRTCCameraIDFront;
    
    _audioMixingID = 3001;
}

- (void)updateVideoConfigWithHost:(BOOL)isHost {
    // Encoder config
    if (isHost) {
        VideoChatSettingVideoConfig *config = [VideoChatSettingVideoConfig defultVideoConfig];
        
        self.solution.width = config.videoSize.width;
        self.solution.height = config.videoSize.height;
        self.solution.frameRate = config.fps;
        self.solution.maxBitrate = config.bitrate;
        [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
    } else {
        self.solution.width = 240;
        self.solution.height = 320;
        self.solution.frameRate = 15;
        self.solution.maxBitrate = 400;
        [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
    }
}

- (void)joinRTCRoomWithToken:(NSString *)token
                      roomID:(NSString *)roomID
                         uid:(NSString *)uid
                    userRole:(BOOL)isHost {
    if (!isHost) {
        //关闭 本地音频/视频采集
        //Turn on/off local audio capture
        [self.rtcEngineKit stopAudioCapture];
        [self.rtcEngineKit stopVideoCapture];
        self.isStartAudioCapture = NO;
    } else {
        self.isStartAudioCapture = YES;
    }
    
    //设置音频路由模式，YES 扬声器/NO 听筒
    //Set the audio routing mode, YES speaker/NO earpiece
    [self.rtcEngineKit setDefaultAudioRoute:ByteRTCAudioRouteSpeakerphone];

    //开启/关闭发言者音量键控
    //Turn on/off speaker volume keying
    ByteRTCAudioPropertiesConfig *audioPropertiesConfig = [[ByteRTCAudioPropertiesConfig alloc] init];
    audioPropertiesConfig.interval = 300;
    [self.rtcEngineKit enableAudioPropertiesReport:audioPropertiesConfig];
    
    // 开启本地和编码镜像
    // Enable local and encoded mirroring
    if (self.cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }

    //加入房间，开始连麦,需要申请AppId和Token
    //Join the room, start connecting the microphone, you need to apply for AppId and Token
    ByteRTCUserInfo *userInfo = [[ByteRTCUserInfo alloc] init];
    userInfo.userId = uid;
    
    ByteRTCRoomConfig *config = [[ByteRTCRoomConfig alloc] init];
    config.profile = ByteRTCRoomProfileInteractivePodcast;
    config.isAutoPublish = YES;
    config.isAutoSubscribeAudio = YES;
    config.isAutoSubscribeVideo = YES;
    
    self.rtcRoom = [self.rtcEngineKit createRTCRoom:roomID];
    self.rtcRoom.delegate = self;
    [self.rtcRoom joinRoomByToken:token userInfo:userInfo roomConfig:config];
}

- (NSString *_Nullable)getSdkVersion {
    return [ByteRTCVideo getSdkVersion];
}

- (void)setUserVisibility:(BOOL)enable {
    [self.rtcRoom setUserVisibility:enable];
}

#pragma mark - rtc method

- (void)enableLocalAudio:(BOOL)enable {
    //开启/关闭 本地音频采集
    //Turn on/off local audio capture
    if (enable) {
        [SystemAuthority authorizationStatusWithType:AuthorizationTypeAudio
                                               block:^(BOOL isAuthorize) {
            if (isAuthorize) {
                [self.rtcEngineKit startAudioCapture];
                [self.rtcRoom publishStream:ByteRTCMediaStreamTypeAudio];
                self.isStartAudioCapture = YES;
                NSLog(@"Manager RTCSDK startAudioCapture");
            }
        }];
    } else {
        [self.rtcEngineKit stopAudioCapture];
        self.isStartAudioCapture = NO;
        NSLog(@"Manager RTCSDK stopAudioCapture");
    }
}

- (void)enableLocalVideo:(BOOL)enable {
    if (enable) {
        [SystemAuthority authorizationStatusWithType:AuthorizationTypeCamera
                                               block:^(BOOL isAuthorize) {
            if (isAuthorize) {
                [self.rtcEngineKit startVideoCapture];
                [self.rtcRoom publishStream:ByteRTCMediaStreamTypeVideo];
                NSLog(@"Manager RTCSDK startVideoCapture");
            }
        }];
    } else {
        [self.rtcEngineKit stopVideoCapture];
        NSLog(@"Manager RTCSDK stopVideoCapture");
    }
}

- (void)switchCamera {
    if (self.cameraID == ByteRTCCameraIDFront) {
        self.cameraID = ByteRTCCameraIDBack;
    } else {
        self.cameraID = ByteRTCCameraIDFront;
    }
    
    if (self.cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }
    
    [self.rtcEngineKit switchCamera:self.cameraID];
}

- (void)updateCameraID:(BOOL)isFront {
    self.cameraID = isFront ? ByteRTCCameraIDFront : ByteRTCCameraIDBack;
    
    if (self.cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }
    
    [self.rtcEngineKit switchCamera:self.cameraID];
}

- (void)muteLocalVideo:(BOOL)mute {
    if (mute) {
        [self.rtcRoom unpublishStream:ByteRTCMediaStreamTypeVideo];
    }else {
        [self.rtcRoom publishStream:ByteRTCMediaStreamTypeVideo];
    }
    NSLog(@"Manager RTCSDK muteLocalVideo");
}

- (void)muteLocalAudio:(BOOL)mute {
    //开启/关闭 本地音频采集
    //Turn on/off local audio capture
    if (!mute && !self.isStartAudioCapture) {
        [self.rtcEngineKit startAudioCapture];
        self.isStartAudioCapture = YES;
    }
    
    if (mute) {
        [self.rtcRoom unpublishStream:ByteRTCMediaStreamTypeAudio];
    } else {
        [self.rtcRoom publishStream:ByteRTCMediaStreamTypeAudio];
    }
    
    NSLog(@"Manager RTCSDK muteLocalAudio");
}


- (void)updateRes:(CGSize)size {
    self.solution.width = size.width;
    self.solution.height = size.height;
    [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
}

- (void)updateFPS:(CGFloat)fps {
    self.solution.frameRate = fps;
    [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
}

- (void)updateKBitrate:(NSInteger)kbitrate {
    self.solution.maxBitrate = kbitrate;
    [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
}

- (void)leaveChannel {
    //离开频道
    //Leave the channel
    ByteRTCAudioMixingManager *audioMixingManager = [self.rtcEngineKit getAudioMixingManager];
    [audioMixingManager stopAudioMixing:_audioMixingID];
    [self.rtcEngineKit stopAudioCapture];
    [self.rtcRoom leaveRoom];
    [self.streamViewDic removeAllObjects];
    NSLog(@"Manager RTCSDK leaveChannel");
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
    NSLog(@"Manager RTCSDK getStreamViewWithUid : %@|%@", view, uid);
    return view;
}

- (void)bingCanvasViewToUid:(NSString *)uid {
    dispatch_queue_async_safe(dispatch_get_main_queue(), (^{
        if ([uid isEqualToString:[LocalUserComponent userModel].uid]) {
            UIView *view = [self getStreamViewWithUid:uid];
            if (!view) {
                UIView *streamView = [[UIView alloc] init];
                streamView.hidden = YES;
                ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
                canvas.uid = uid;
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
                canvas.uid = uid;
                canvas.renderMode = ByteRTCRenderModeHidden;
                canvas.view.backgroundColor = [UIColor clearColor];
                canvas.view = remoteRoomView;
                canvas.roomId = self.rtcRoom.getRoomId;
                [self.rtcEngineKit setRemoteVideoCanvas:uid
                                        withIndex:ByteRTCStreamIndexMain
                                       withCanvas:canvas];
                
                NSString *groupKey = [NSString stringWithFormat:@"remote_%@", uid];
                [self.streamViewDic setValue:remoteRoomView forKey:groupKey];
            }
        }
        NSLog(@"Manager RTCSDK bingCanvasViewToUid : %@", self.streamViewDic);
    }));
}


#pragma mark - CV

- (void)initVideoEffectWithLicense:(NSString *)licensePath model:(NSString *)modelPath {
    int errorCode = [self.rtcEngineKit checkVideoEffectLicense:licensePath];
    if (errorCode == 0) {
        NSLog(@"check license success");
    } else {
        NSLog(@"check license failed");
    }
    [self.rtcEngineKit setVideoEffectAlgoModelPath:modelPath];
    [self.rtcEngineKit enableVideoEffect:YES];
    NSLog(@"Manager RTCSDK initVideoEffectWithLicense");
}

- (void)setVideoEffectNodes:(NSArray *)nodes {
    [self.rtcEngineKit setVideoEffectNodes:nodes];
    NSLog(@"Manager RTCSDK setVideoEffectNodes");
}

- (void)setVideoEffectColorFilter:(NSString *)filterPath {
    [self.rtcEngineKit setVideoEffectColorFilter:filterPath];
}

- (void)setVideoEffectColorFilterIntensity:(CGFloat)intensity {
    [self.rtcEngineKit setVideoEffectColorFilterIntensity:intensity];
}

- (void)updateVideoEffectNode:(NSString *_Nonnull)nodePath
                      nodeKey:(NSString *_Nonnull)nodeKey
                    nodeValue:(float)nodeValue {
    [self.rtcEngineKit updateVideoEffectNode:nodePath nodeKey:nodeKey nodeValue:nodeValue];
}

- (void)setDefaultVideoEncoderConfig {
    self.solution.width = 240;
    self.solution.height = 320;
    self.solution.frameRate = 15;
    self.solution.maxBitrate = 400;
    [self.rtcEngineKit SetMaxVideoEncoderConfig:self.solution];
}

#pragma mark - NetworkQuality

- (void)didChangeNetworkQuality:(VideoChatNetworkQualityChangeBlock)block {
    self.networkQualityBlock = block;
}

#pragma mark - PK
- (void)startForwardStream:(NSString *)roomID token:(NSString *)token {
    [self stopForwardStream];
    ForwardStreamConfiguration *configuration = [[ForwardStreamConfiguration alloc] init];
    configuration.roomId = roomID;
    configuration.token = token;
    int res = [self.rtcRoom startForwardStreamToRooms:@[configuration]];
    NSLog(@"startForwardStream-%d", res);
}

- (void)stopForwardStream {
    [self.rtcRoom stopForwardStreamToRooms];
}

- (void)muteOtherAnchorUserID:(NSString *)userID mute:(BOOL)isMute {
    if (isMute) {
        [self.rtcRoom unSubscribeStream:userID mediaStreamType:ByteRTCMediaStreamTypeAudio];
    } else {
        [self.rtcRoom subscribeStream:userID mediaStreamType:ByteRTCMediaStreamTypeAudio];
    }
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onForwardStreamStateChanged:(NSArray<ForwardStreamStateInfo *> *)infos {
    
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onForwardStreamEvent:(NSArray<ForwardStreamEventInfo *> *)infos {
    
}

#pragma mark - ByteRTCVideoDelegate
- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onRoomStateChanged:(NSString *)roomId withUid:(NSString *)uid state:(NSInteger)state extraInfo:(NSString *)extraInfo {
    [super rtcRoom:rtcRoom onRoomStateChanged:roomId withUid:uid state:state extraInfo:extraInfo];

    [[VideoChatRTCManager shareRtc] bingCanvasViewToUid:uid];
    NSLog(@"Manager RTCSDK join %@|%ld", uid, (long)state);
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserJoined:(ByteRTCUserInfo *)userInfo elapsed:(NSInteger)elapsed {
    NSLog(@"Manager RTCSDK onUserJoined %@", userInfo.userId);
    [[VideoChatRTCManager shareRtc] bingCanvasViewToUid:userInfo.userId];
}

- (void)rtcEngine:(ByteRTCVideo *)engine onFirstRemoteVideoFrameRendered:(ByteRTCRemoteStreamKey *)streamKey withFrameInfo:(ByteRTCVideoFrameInfo *)frameInfo {
    NSLog(@"Manager RTCSDK onFirstRemoteVideoFrameRendered %@", streamKey.userId);
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
//    NSLog(@"RTM Manager onRemoteAudioPropertiesReport %@", dic);
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onLocalStreamStats:(ByteRTCLocalStreamStats *)stats {

    VideoChatNetworkQualityStatus liveStatus = VideoChatNetworkQualityStatusNone;
    if (stats.tx_quality == ByteRTCNetworkQualityExcellent ||
        stats.tx_quality == ByteRTCNetworkQualityGood) {
        liveStatus = VideoChatNetworkQualityStatusGood;
    } else {
        liveStatus = VideoChatNetworkQualityStatusBad;
    }
    if (self.networkQualityBlock) {
        self.networkQualityBlock(liveStatus, [LocalUserComponent userModel].uid);
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
    if (self.networkQualityBlock) {
        self.networkQualityBlock(liveStatus, stats.uid);
    }
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

#pragma mark - Getter

- (NSMutableDictionary<NSString *, UIView *> *)streamViewDic {
    if (!_streamViewDic) {
        _streamViewDic = [[NSMutableDictionary alloc] init];
    }
    return _streamViewDic;
}

- (ByteRTCVideoEncoderConfig *)solution {
    if(!_solution) {
        _solution = [[ByteRTCVideoEncoderConfig alloc] init];
    }
    return _solution;
}
@end
