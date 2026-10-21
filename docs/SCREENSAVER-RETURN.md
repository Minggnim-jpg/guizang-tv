# 小米系统屏保返回

针对 MiTV-ASTP0 的系统屏保 `com.mitv.screensaver.RenderActivity`。屏保不等于关屏，所以原来的 SCREEN_ON/SREEN_OFF 路径不会覆盖。

## 信号边界

设备屏保 2.4.11 接收 show/exit 指令，但真正 onResume/onPause/按键退出发送的 `com.duokan.screensaver.begin/end` 要求接收方持有 `mitv.permission.ACCESS_INNER_APPLICATION`（signature|privileged）。本应用不尝试获取或绕过此系统权限。

## 实现

新增默认关闭的“屏保退出返回”，需要用户明确允许无障碍服务。服务只订阅 TYPE_WINDOW_STATE_CHANGED，只使用 packageName/className，不读取 text/source/root/windows，不拦截按键、不执行手势、无定时轮询。XML 明确关闭内容检索、按键过滤和手势能力。

只有观测到“哈怪鱼 -> RenderActivity -> 小米主桌面”才延迟 600ms 返回一次。小米 BootActivity 是中间态，不立即抢占。若其间转到视频/其他应用、哈怪鱼自己已恢复、发生关屏，则取消返回。直接进入原桌面以及从视频进入屏保都不触发。系统 UI 临时浮层不视为应用切换。

不保存跨进程的屏保来源，以免恢复陈旧状态后抢占其他应用；服务刚连接且来源未知时保持保守。已授权的通知恢复服务会在小米清理后恢复这个用户开启的无障碍服务。关闭应用内开关会只移除本服务，不动其他无障碍服务。设置和启动参数仍保留。

单元测试覆盖 9 类切换路径。构建与路径测试不等于真机验收；须分别实测从哈怪鱼进入屏保和从其他应用进入屏保。
