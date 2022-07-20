视频互动是火山引擎实时音视频提供的一个开源示例项目。本文介绍如何快速跑通该示例项目，体验 RTC 视频互动效果。

## 应用使用说明

使用该工程文件构建应用后，即可使用构建的应用进行视频互动。
你和你的同事必须加入同一个房间，才能进行视频互动。
美颜效果请下载[火山引擎场景化 Demo 安装包](https://www.volcengine.com/docs/6348/75707#%E4%B8%8B%E8%BD%BD%E5%92%8C%E4%BD%93%E9%AA%8C%E5%9C%BA%E6%99%AF%E5%8C%96-demo)体验，示例项目暂不支持美颜相关功能。
如果你已经安装过 火山引擎场景化 Demo 安装包，示例项目编译运行前请先卸载原有安装包，否则会提示安装失败。

## 前置条件

- [Xcode](https://developer.apple.com/download/all/?q=Xcode) 12.0+
	

- iOS 12.0+ 真机
	

- 有效的 [AppleID](http://appleid.apple.com/)
	

- 有效的 [火山引擎开发者账号](https://console.volcengine.com/auth/login)
	

- [CocoaPods](https://guides.cocoapods.org/using/getting-started.html#getting-started) 1.10.0+
	

## 操作步骤

### **步骤 1：获取 AppID 和 AppKey**

在火山引擎控制台->[应用管理](https://console.volcengine.com/rtc/listRTC)页面创建应用或使用已创建应用获取 AppID 和 AppAppKey

### **步骤 2：获取 AccessKeyID 和 SecretAccessKey**

在火山引擎控制台-> [密钥管理](https://console.volcengine.com/iam/keymanage/)页面获取 **AccessKeyID 和 SecretAccessKey**

### 步骤 3：构建工程

1. 打开终端窗口，进入 `RTC_VideoInteract_Demo-master/iOS/veRTC_Demo_iOS` 根目录
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_2018ac77c34a152da72cb9787817a2bd" width="500px" >

2. 执行 `pod install` 命令构建工程
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_e438c644f4345f911fdd546d7d8d0886" width="500px" >

3. 进入 `RTC_VideoInteract_Demo-master/iOS/veRTC_Demo_iOS` 根目录，使用 Xcode 打开 `veRTC_Demo.xcworkspace`
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_f2c16bcdcab32de04dd71b9c5a6ec999" width="500px" >

4. 在 Xcode 中打开 `Pods/Development Pods/Core/BuildConfig.h` 文件
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_709fb27ed82f3d3eaaad36e1027a09a1" width="500px" >

5. 填写 **LoginUrl**
	

当前你可以使用 **`http://rtc-test.bytedance.com/rtc_demo_special/login`** 作为测试服务器域名，仅提供跑通测试服务，无法保障正式需求。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_26187051b4ca386816a62d637b4a5195" width="500px" >

6. **填写 APPID、APPKey、AccessKeyID 和 SecretAccessKey**
	

使用在控制台获取的 **APPID、APPKey、AccessKeyID 和 SecretAccessKey** 填写到 `BuildConfig.h`文件的对应位置**。** 

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_d72d6c657c5bf20bc9562559648a268e" width="500px" >

### **步骤 4：配置开发者证书**

1. 将手机连接到电脑，在 `iOS Device` 选项中勾选您的 iOS 设备。
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_f1f2feb28e8ae2860dda58f48c7d575a" width="500px" >
<br>

2. 登录 Apple ID。
	

2.1 选择 Xcode 页面左上角 **Xcode** > **Preferences**，或通过快捷键 **Command** + **,**  打开 Preferences。
2.2 选择 **Accounts**，点击左下部 **+**，选择 Apple ID 进行账号登录。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_3598a44a488f24cba77908d5b222b458" width="500px" >

3. 配置开发者证书。
	

3.1 单击 Xcode 左侧导航栏中的 `VeRTC_Demo` 项目，单击 `TARGETS` 下的 `VeRTC_Demo` 项目，选择 **Signing & Capabilities** > **Automatically manage signing** 自动生成证书

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_df6ff59c20b42530f696646d08294cc1" width="500px" >

3.2 在 **Team** 中选择 Personal Team。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_1f74db52dbf9825f2fabfcea207769e0" width="500px" >

3.3 **修改 Bundle** **Identifier****。** 

默认的 `vertc.veRTCDemo.ios` 已被注册， 将其修改为其他 Bundle ID，格式为 `vertc.xxx`。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_0159cf55b5a783d4875c89fdc075a3ab" width="500px" >

### **步骤 5：编译运行**

选择 **Product** > **Run**， 开始编译。编译成功后你的 iOS 设备上会出现新应用。若为免费苹果账号，需先在`设置->通用-> VPN与设备管理 -> 描述文件与设备管理`中信任开发者 APP。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_702f3f683b490eb357f7e6e2a0c3076e" width="500px" >

运行开始界面如下：

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_4177da8c449b7e572634b8e358415f92" width="200px" >
