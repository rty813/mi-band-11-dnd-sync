# 小米手环 11 勿扰同步补丁

让三星手机上的小米运动健康（中国版 3.61.0）把手机勿扰状态同步到小米手环 11。

> 仅供学习研究。Mi Fitness 及相关商标归各自权利人所有。仓库不包含 APK 或签名材料。

## 状态

- 已验证：三星手机 + Android 17 + 小米运动健康 3.61.0 + 小米手环 11，手动开关手机勿扰后手环均能同步。
- 补丁与应用版本绑定，运动健康升级后可能失效。

## 原理

运动健康本身已有勿扰同步功能，但在三星 + 手环 11 上不生效。补丁复用应用自身的设备连接，不另建蓝牙连接：

1. 对三星手机上的 `miwear.watch.q66u` 放宽勿扰功能判定；
2. 设备同步时注册 Android 勿扰状态监听；
3. 状态不一致时通过应用内部命令写入手环勿扰规则，并回读确认。

独立 App 直连手环会与运动健康争抢蓝牙连接，不可行。详见[调查与实现](docs/investigation.md)。

## 使用

1. 按[构建与测试](docs/build.md)打补丁、重签并安装（需先卸载官方版，注意备份数据）。
2. 运动健康中进入「设备 → 小米手环 11 → 勿扰模式同步」，开启「同步手机」。
3. 按提示授予勿扰访问权限（三星设置中位于「特殊应用权限」→「勿扰访问权限」，名称随 One UI 版本略有不同）。

## 目录

| 路径 | 内容 |
| --- | --- |
| [docs/investigation.md](docs/investigation.md) | 调查过程、协议、改动说明 |
| [docs/build.md](docs/build.md) | 构建、安装与测试 |
| [patches/](patches/mi-health-3.61.0/) | Smali 补丁 |
| [scripts/apply-patch.ps1](scripts/apply-patch.ps1) | 检查或应用补丁 |
| [probe/](probe/) | 蓝牙连接探针（调查用） |

## 社区

- 社区链接：[LINUX DO](https://linux.do/)

## 参考

- [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge)
- [miwear-ble-protocol](https://github.com/Zioove/miwear-ble-protocol)
