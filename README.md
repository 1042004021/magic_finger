# 魔法手指 Android 16 独立原型

**最新模块+APK交付及验证边界见 [MODULE.md](MODULE.md)，以下记录包括旧短时原型，不代表常驻版已验收。**

## 最新实验状态

- 0.2 采用设置 APK + Root 输入后端，提供 KernelSU、Magisk、APatch 三种模块打包入口；Magisk/APatch 尚未实机验证。
- 普通应用悬浮层可跨应用显示，但无法覆盖部分系统界面。
- `LayerProbe` 是隔离实验，静态标记已在通知面板和最近任务显示；**用户确认高层光环运行时下层无法点击、滑动，触摸透传验收失败，不可作为常驻方案使用**。测试已结束，设备操作恢复正常。
- 当前源码包含未部署的实验修改，不等同于手机已安装版本；不自动安装或启动探针。
- 不提交构建缓存、APK/ZIP/DEX 和签名私钥。首次构建会生成新的本地测试签名，无法覆盖安装由其他签名签署的 APK。
- 构建脚本中的 SDK/JDK 路径为本机路径，其他环境需自行调整。运行 `sh build.sh`，再用 `python3 package_module.py --target kernelsu`（或 `magisk` / `apatch`）打包。

## 历史实验记录（非当前使用说明）

## APK + Root relay 首次验证

已改为 `magic.finger.ParticleActivity` 正常应用进程创建 TYPE_APPLICATION_OVERLAY 窗口；独立 `magic.finger.TouchRelay` 使用被动 InputMonitor，通过随机命名的本地 socket 传输临时坐标，不落盘。新实现不使用旧 ROM 代码/素材。

已生成并验签 `dist/magic-finger-experimental.apk`，安装到目标手机，授予本应用 SYSTEM_ALERT_WINDOW appop（测试后仍保留，未还原）。没有开机项。安装后由电脑显式启动，界面30秒关闭显示层，relay25秒注销监听并结束。

首次25秒实测：RELAY_READY 成功，接收1399个 MotionEvent，结束输出 RELAY_FINISHED；检查无残留 TouchRelay 进程或 MagicFingerParticles 窗口。此日志只证明监听和数据传输路径启动，视觉效果及正常应用是否收到点击等待用户确认。界面 APK 进程可能留在缓存中，但特效窗口已移除。

当前悬浮层 alpha=0.7，低于Android默认不受信任悬浮窗触摸遮挡阈值；厂商行为及与漂移同时运行的坐标仍需测试。多指事件目前简化为逐点发射，非完整多指轨迹模型。尚未处理权限撤销、配置变化、后台长期保活和模块封装。

构建：`sh build.sh`（本机SDK34/JDK8编译，运行目标Android16，compile-only隐藏API stub不打包）；旧根进程直接建窗口失败路径保留为参考，不再作为当前测试入口。

全新实现，不复制旧 ROM 粒子代码或素材，不修改 screen-drift 模块。

## 当前状态

- 已确认目标设备 API36、Root、1200×2670。
- 只读反射探针确认 InputManagerGlobal.monitorGestureInput / InputMonitor.getInputChannel / InputEventReceiver 可用的方法签名。存在方法不代表实际权限或注册成功。
- 已构建 Canvas 硬件加速透明粒子层：按下绽放星星，移动生成轨迹，750ms 渐隐，最多220粒子；空闲停止请求绘制。
- 拟使用 TYPE_SECURE_SYSTEM_OVERLAY（2015）及 NOT_TOUCHABLE/NOT_FOCUSABLE；旁听 monitorGestureInput，不调用 pilferPointers，不注入事件。
- 仅计数输入事件，不记录坐标/轨迹/应用信息，不抓屏。
- 编译及 D8 构建通过，compile-only 隐藏 API stub 不打包。
- 手机处于 Dozing，尚未部署或启动特效；输入权限、窗口权限、坐标映射、实际穿透、旋转及与漂移模块共存均未验证。

## 首次亮屏测试结果

亮屏后推送并执行20秒短测，在 WindowManager.openSession 阶段被拒绝：`IllegalStateException: Unknown pid=16100 uid=0`。透明窗口未成功建立，输入监听尚未注册，所以没有显示粒子。Root 独立 app_process 并非 AMS 注册的应用进程，当前 ROM 对窗口 session 的进程身份校验不接受此用法；具体检查位置尚未进一步定位。

异常路径原先尝试移除未附加窗口，产生二次异常；已增加 attached 标志保护，并在失败清理后结束自身。检查无残留 MagicFinger 进程，未修改系统设置或现有漂移模块。后续需要调整窗口创建架构，不能通过 Root 身份直接假设 WindowManager 接受窗口。

## 构建

`sh build.sh`，生成 dist/classes.dex。

## 计划短测（亮屏解锁后）

推送至 `/data/local/tmp/magic-finger.dex`，Root 下运行：

```sh
CLASSPATH=/data/local/tmp/magic-finger.dex app_process /system/bin MagicFinger 20
```

原型限制5～120秒，到期销毁输入监听及窗口后终止自身。root 进程被杀后窗口/输入通道也应随 Binder 生命周期释放，该异常场景尚未实测。不安装APK，不改系统设置或开机项。
