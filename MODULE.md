# 魔法手指 0.2 实验版：KernelSU + APK

## 0.4-beta 发布

APK 与模块版本更新为0.4-beta，versionCode=5；APK功能与0.3-beta相同。本版修复模块开机拉不起绘制服务的问题：`am` 实际由 system_server 执行，原先把输出重定向到模块目录会被SELinux拒绝，导致启动事务失败并报 `Failed transaction (2147483646)`；现改为经管道由root侧写入日志。必须重刷模块并重启验证。

## 0.3-beta 发布（模块包已作废）

> ⚠️ 本版模块 ZIP 存在开机启动失败问题，请勿刷入，改用 0.4-beta。APK 代码正常，可继续使用。

APK 与模块版本更新为0.3-beta，versionCode=4；APK沿用本地测试签名。提供APK、KernelSU/Magisk/APatch三种模块包和SHA256校验。本版移除APK总开关和启动按钮，刷入模块即代表开启，关闭需停用或卸载模块后重启；设置页重做为深色卡片界面，并显示悬浮窗授权状态。设置页已在目标设备安装验证，模块开机链路未做本版实机回归。详细变化见CHANGELOG.md。

下文0.2-beta及更早记录为历史状态，当前安装文件以README、CHANGELOG和最新Release为准。

## 0.2-beta 发布

APK 与模块版本更新为0.2-beta，versionCode=3；APK沿用本地原测试签名。提供APK、KernelSU/Magisk/APatch三种模块包和SHA256校验。普通悬浮窗仍为正常入口，高层探针未接入服务。已完成构建、签名验证、模块脚本及ZIP检查，未将新包安装到手机，未完成Beta实机回归。下文0.2-experimental文件名与实验记录为历史状态，当前安装文件以README和Beta Release为准。

## 系统界面层级诊断

### 高层绘制隔离实验（未成功，未部署到模块）

更新：高层Surface路径现已通过静态标记和限时输入探针；尚未整合进常驻版本，以下早期失败记录保留用于追溯。

### 后续隔离验证：高层 Surface 可用，尚未交付

**用户实测否决：高层光环出现时，下层界面无法点击、滑动。该探针未通过触摸透传验收，禁止将其整合或作为可用功能发布。** 静态标记可见仅证明合成显示成功；事件计数仅证明探针收到输入，不证明原目标应用正常收到输入。后续只读检查未发现探针进程/图层残留；当次读取的近期日志未匹配到触摸遮挡错误，不能据此排除遮挡机制。根因尚未确定，需要拆分纯显示层与输入监听进行受控验证，而非直接认定Surface或InputMonitor某一方导致。

- 添加阶段日志后确认Surface构建、lockCanvas、Paint创建均成功；断言发生于文字绘制。移除drawText后纯图形提交成功，不需要ActivityThread.systemMain。此前将失败概括为整个图形初始化不可用不准确。
- 10秒固定白心粉环标记：桌面、展开通知面板、最近任务均在(600,240)采到白色(255,255,255)，在(630,240)采到粉色(255,102,204)。screencap仅经电脑内存解码采样，无截图文件保存。SurfaceFlinger列出独立根层z=2000000000。检查后收起面板并返回桌面。
- `LayerProbe touch` 增加25秒被动InputMonitor与纯图形光环，不调用pilferPointers、不注入输入、不存坐标。第一轮收到911个事件，但退出调用主Looper.quitSafely报错，finally仍清理。改用可退出的Looper.prepare后第二轮收到415个事件，正常到LAYER_FINISHED。结束时kill自身仍显示Killed/137，是当前显式终止方式，未声称退出码问题已解决。
- 第二轮后确认无LayerProbe进程或图层残留，原APK PID9990仍在。手机现有APK和模块未替换；仅更新临时dex。触摸探针的系统界面实际跟手与不干扰操作仍需用户确认，不能将静态标记可见或总事件数当作所有场景输入验证。
- 当前探针尺寸固定1200x2670、display/layerStack固定0，未处理旋转、多屏、设置同步或功耗；未打入新发布模块。正式集成前需决定由Root直接纯图形绘制还是APK绘制并传Surface句柄。

1. 使用 `su 1000` 独立进程尝试 TYPE_SECURE_SYSTEM_OVERLAY，依旧在 openSession 报 Unknown pid uid=1000；仅换 UID 不能解决会话身份限制。修正历史实验 Sparks 对 android 系统Context读取SharedPreferences导致的无data目录错误；APK原有参数读取不变。
2. 新增 LayerProbe.java，用独立根SurfaceControl + layerStack0 + 临时高Z层绘制10秒标记，不改现有窗口层级、不接触输入。直接app_process与补充ActivityThread.systemMain两种方式均在图形/字体初始化触发 `Assertion failed: src == nullptr && gDefaultTypeface == nullptr`，探针Aborted，未到LAYER_READY。
3. 没有升级已安装APK或模块；仅推送 `/data/local/tmp/magic-finger-layer-test.dex`。首轮失败后检查无探针进程/图层残留，后续仍需结束检查。未关闭SELinux，未注入system_server。
4. 编译/签名检查通过不能等同高层路径可用。下一步可调查独立进程图形初始化兼容，或让APK绘制、特权后端仅管理Surface合成层关系；后者尚未实现，需评估Surface句柄IPC与输入/遮挡影响。

目标机只读窗口检查：MagicFingerParticles APPLICATION_OVERLAY 基础层级111000，StatusBar151000，NotificationShade171000。通过cmd statusbar临时展开通知栏后，面板与粒子窗口均有surface且isVisible=true，粒子窗口未从窗口层面消失；支持通知面板层级覆盖解释。检查后收起面板。

通过KEYCODE_APP_SWITCH检查最近任务时，粒子窗口仍报告可见、surface存在。桌面窗口基础层级21000；没有进一步检查SurfaceFlinger实际合成排序或输入计数，不能仅凭窗口基础层级认定最近任务无效果的根因。最后回到桌面。未抓屏、未保存界面文字/坐标、未改显示参数。

## APatch 独立打包

运行 `python3 package_module.py --target apatch`，生成 `dist/magic-finger-apatch-0.2-experimental.zip`。依据官方 APModule 指南 https://apatch.dev/apm-guide.html ，使用根目录 module.prop、customize.sh、service.sh 和 DEX，由APatch管理器安装，不包含依赖Magisk路径的META-INF引导，不支持Recovery安装。

后端和APK共用，不依赖Zygisk/LSPosed、不需要KernelPatch模块、不进行system挂载或SELinux变更。当前APatch包与KernelSU包内容相同，独立文件名方便选择；框架差异暂未要求额外代码。已做脚本语法及ZIP结构完整性检查，未在APatch设备实测安装/开机/权限。不能据此声称所有APatch系统可用。

三种包的模块ID均为magic_finger，同一手机按实际Root框架选择一种，APK相同。

## Magisk 独立打包

运行 `python3 package_module.py --target magisk` 生成 `dist/magic-finger-magisk-0.2-experimental.zip`。使用同一 module.prop、service.sh、customize.sh、DEX 和 APK，不改模块ID（同一功能的不同Root框架包，不应同时安装）。原KernelSU ZIP保留。

额外提供 META-INF 安装入口及 #MAGISK 标记，入口加载设备已有 `/data/adb/magisk/util_functions.sh` 并调用 install_module，不捆绑下载的未知安装器。引导要求Magisk20.4+，此条件仅为安装器下限，不表示旧版本Magisk支持Android16；请使用适配目标系统的Magisk版本，推荐在Magisk管理器内安装，不保证Recovery安装。

Magisk包脚本语法和ZIP完整性/必需条目检查通过；当前设备为KernelSU，**未实测Magisk安装、开机和权限行为**，不能将共用后端验证等同于Magisk已验收。APK无需另做版本。

## 交付

- `dist/magic-finger-experimental.apk`：设置、本页预览、独立前台绘制服务。
- `dist/magic-finger-0.2-experimental.zip`：KernelSU 开机 Root 输入后端监督脚本。
- 已构建签名、APK签名验证、模块脚本语法和 ZIP 完整性检查通过。
- 已在目标设备覆盖安装 APK 并执行启动设置页，进程存在；没有自动刷入模块。模块开机与整条常驻链路未验收。

## 设置

APK 不提供开关，刷入模块即开启，界面只保留样式调节与本页预览。支持星星/爱心/光点/光环/随机；密度1～8、大小6～40px、寿命200～1600ms。预览不依赖Root；设置在后续发射的粒子上即时生效，现有粒子保持原参数消散。没有颜色选择器或扩散参数界面。所有图形全新绘制，未复制旧ROM素材/代码。

## 安装

先安装APK并授予悬浮窗权限。随后在KernelSU中刷模块ZIP，重启。模块不负责安装APK。无需授予APK su；Root输入后端由模块启动。通知权限可以手动允许，便于返回设置。关闭效果只能停用或卸载模块后重启。

APK不申请联网权限、不保存触摸坐标。模块用系统包UID校验socket服务端，APK仅接受UID0连接；这是同机UID身份校验，不是抵抗恶意Root的安全边界。Android多用户尚未支持，启动固定user0。

## 运行

模块等待开机完成，亮屏时尝试启动绘制服务（服务内仅检查悬浮窗授权）并连接Root监听，断线后每5秒重试。息屏时终止监听，APK清空粒子；下次亮屏重连。未承诺锁屏不显示或锁屏不监听，Awake判断不是解锁判断。没有Hook系统进程，不改SELinux，不改漂移模块。

最大220粒子，空闲不请求动画帧。透明层alpha0.7以减少普通悬浮窗阻断触摸的风险；其他应用仍可选择隐藏悬浮窗。不能保证所有敏感页面有效。前台服务受厂商后台政策影响，需实测。

## 验证边界

早期25秒版本已由用户确认跨应用星星有效、普通操作目前正常。0.2已编译并覆盖安装，不能将早期短测等同于0.2模块/服务链路完成验证。

待测：服务从后台/开机启动限制，独立service身份权限，用户关闭/开启、断线重连、息屏恢复、旋转及状态栏坐标、多指效果、密度上限功耗、漂移同时运行时对齐。Root relay主动退出仍采用终止自身，不把Killed提示视为故障恢复已解决。

## 实现位置

- `ParticleActivity.java`：设置和预览。
- `ParticleService.java`：前台通知、透明窗、Root peer校验、断线accept、销毁清理。
- `MagicFinger.Sparks`：五类粒子及参数读取；旧直接建窗口的main仅历史实验，不作为入口。
- `TouchRelay.java`：被动输入监听、服务UID校验、流式转发，不调用pilferPointers。
- `module/service.sh`：等待开机、Root监听监督与重连。
- `AndroidManifest.xml`：悬浮窗/前台服务权限；服务导出入口受DUMP权限保护（供Root启动）。

构建命令：`sh build.sh && python3 package_module.py`。不需要公司任务文档。源码长期保存在 `/home/elder/Projects/magic-finger`。
