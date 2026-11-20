# 蓝牙连接探针

调查用的最小 Android 应用，用于验证能否绕开运动健康直连手环。它查找已配对且名称含「Smart Band 11」的设备，**不发送任何控制数据**。

- `spp` 模式（默认）：打开 SPP socket，成功后约 3 秒关闭。
- `ble` 模式：扫描 15 秒，匹配到手环则尝试 GATT 连接和服务发现。

## 构建

JDK 17/21、Android SDK 36（AGP 9.0.1、Kotlin 2.3.20、Gradle 9.1）：

```powershell
gradle -p probe :app:assembleDebug
adb install -r probe/app/build/outputs/apk/debug/app-debug.apk
```

## 运行

首次启动需授予「附近的设备」权限。

```powershell
adb shell am force-stop com.example.banddndprobe
adb shell am start -n com.example.banddndprobe/.MainActivity --es mode spp   # 或 ble
adb logcat -s BandDndProbe:I
```

运动健康在线时运行 `spp` 模式会使其短暂断连。结果见[调查与实现](../docs/investigation.md#结论)。
