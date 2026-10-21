# 哈怪鱼电视 · Haguaiyu TV

一个用于个人小米电视的简洁 Android 桌面：单色应用图标、照片壁纸、列表 / Dock 布局，以及针对 MiTV-ASTP0 的开机、待机唤醒和 ADB 恢复适配。

基于 [op7418/guizang-tv](https://github.com/op7418/guizang-tv) v0.3.0 修改，保留上游来源说明及 MIT 许可。本仓库为个人定制 fork，不代表上游官方版本。

[下载当前测试版](https://github.com/Minggnim-jpg/guizang-tv/releases/tag/v0.3.0-haguaiyu.1) · [适配说明](docs/ASTP0-FORK.md) · [壁纸来源](docs/WALLPAPERS.md) · [单色图标](docs/MONOCHROME.md)

## 当前版本

- 版本：`0.3.0-haguaiyu.1`，versionCode 9。
- 包名：`io.github.minggnim.guizangtv`。显示名称改为哈怪鱼电视；沿用此包名以保留已安装数据。
- 发布包约 2.06 MB，采用固定个人签名。当前是 **可调试的个人测试构建**，不是商店正式发行版。
- SHA256SUMS.txt 随发布包提供。后续升级须使用同一签名；签名私钥仅存本机，不在仓库或 CI 产物中。

## 功能

- 列表 / 横向 Dock，文字 / 图标 / 图标加文字，字体与字号设置。
- 首页 1–9 项，可添加、排序、隐藏，保存用户偏好。
- 单色应用标识、统一圆角底板、选中描边和轻微放大；全部应用列表也显示图标。
- 已覆盖 13 个常见电视应用及系统入口。其他应用读取 APK 图标并转换为保留明暗细节的灰度图，不依赖在线图标库。
- 7 张离线壁纸：热带绿叶、雾中山湖、落日沙丘、深蓝海浪、莫奈《睡莲》、葛饰北斋《神奈川冲浪里》、石墨曲面。显示图片及缩略图共约 1.86 MiB。
- 固定壁纸或定时轮播，离开桌面暂停轮播；图库目前随 APK 打包。
- 小米原桌面公开入口，不卸载或停用原厂桌面。
- 三个独立启动选项：完整重启后进入、待机唤醒后进入、保持 ADB 调试。新安装默认关闭。

## 已验证范围

2026-10-21，在 **MiTV-ASTP0 / Android 9 / 固件 22.3.16.898** 上验证：

| 项目 | 结果 |
|---|---|
| 安装、同签名覆盖升级、设置保留 | 通过 |
| 单色首页及全部应用列表滚动 | 通过；已修复 Android 9 的 SVG 弧线解析兼容问题 |
| 返回小米原桌面按钮 | 通过 |
| 实体遥控器待机后唤醒 | 自动进入哈怪鱼电视，用户观察与日志一致 |
| 应用被强停后的系统服务重绑 | 通过；正常观看状态下不会因此反复拉回桌面 |
| ADB 开关被关闭后的自动恢复 | 通过 |
| 完整系统重启 | 启动编号递增，桌面自动进入，ADB 自动恢复并重连成功 |

以上是该机型的单次流程验收，不代表长时间稳定性或其他固件均已验证。**创维、TCL、索尼等品牌尚未适配验证**；当前启动功能限定在 MiTV-ASTP0 / Android 9。最低安装要求 Android 8，满足最低版本不等于完成机型适配。

## 安装与权限

普通试用只需安装 APK，然后从电视应用列表打开。安装 APK 本身不会启用三个启动选项：

```sh
adb connect <电视IP>:5555
adb -s <电视IP>:5555 install -r haguaiyu-tv-0.3.0-haguaiyu.1.apk
```

完整启动恢复需要额外授权，不能只依据开关显示判断成功：

- **通知访问权限**：用于系统绑定 StartupRevivalListener，从小米后台强停中恢复进程。权限本身能访问通知；当前代码不读取、保存或处理通知内容。
- **WRITE_SECURE_SETTINGS**：保持 ADB 调试所需。开启后会自动重新打开被系统关闭的 ADB；若要手动关闭调试，应先关闭应用里的“保持 ADB 调试”。
- 不需要 root、无障碍服务或停用系统应用。不接管遥控器 Home 键。

授权与启动适配仍需按 [机型说明](docs/ASTP0-FORK.md) 检查，不建议将其他机型直接加入白名单。HDMI 入口沿用上游独立适配限制，不随启动功能解锁。

## 构建

JDK 17、Gradle 8.9、Android SDK 34 / Build Tools 34.0.0：

```sh
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Python 壁纸检查依赖 Pillow。GitHub Actions 自动构建测试 APK；**CI 临时 debug 签名不能直接覆盖发布的个人固定签名包**。个人偏好备份、候选图片原件、签名材料及本地构建文件均排除在版本管理之外。

## 来源与许可

代码采用 [MIT](LICENSE)，上游及图标来源见 [NOTICE.md](NOTICE.md) 和 `app/src/main/assets/icons/SOURCES.json`。图片分别适用 Unsplash License、Public Domain / PD-Art 或 CC0；详细作者及原始页面见 [壁纸清单](docs/WALLPAPERS.md)。
