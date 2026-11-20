# 构建与测试

## 准备

- 中国版 Mi Fitness `com.mi.health` 3.61.0（`versionCode=361000`）APK，自行合法获取
  - 已验证文件的 SHA-256：`0a741c885939d264aeaa2f0fd3f412f381211eb08e43f8b585e35280777bc68a`
  - 其他渠道的同版本包需通过补丁预检查
- Apktool 3.0.3、Java 21
- Git
- Android SDK Build Tools 36.1.0（`zipalign`、`apksigner`，需加入 PATH）
- 自己的签名密钥

## 打补丁

在仓库根目录执行（PowerShell），APK 放在 `input/`：

```powershell
New-Item -ItemType Directory -Force input, build | Out-Null
Get-FileHash input\mi-health-3.61.0.apk -Algorithm SHA256
apktool d -r input\mi-health-3.61.0.apk -o build\decoded
.\scripts\apply-patch.ps1 -DecodedApk build\decoded -CheckOnly
.\scripts\apply-patch.ps1 -DecodedApk build\decoded
```

`-r` 表示不解码资源，补丁只改 Smali。预检查失败时先核对 APK 哈希和 Apktool 版本。

## 重建与签名

```powershell
apktool b build\decoded -o build\unsigned.apk
zipalign -P 16 -f 4 build\unsigned.apk build\aligned.apk
apksigner sign --ks .\local-debug.keystore --out build\mi-health-patched.apk build\aligned.apk
apksigner verify --verbose build\mi-health-patched.apk
```

后续更新须使用同一密钥。

## 安装

重签包与官方签名不同，不能覆盖安装。**先确认数据已同步到云端**，再卸载官方版：

```powershell
adb uninstall com.mi.health
adb install build\mi-health-patched.apk
```

恢复官方版：卸载补丁版，再从原渠道安装。

## 测试

1. 运动健康中开启「同步手机」，并授予勿扰访问权限（见 [README](../README.md#使用)）。
2. 手机手动开启勿扰，确认手环进入勿扰；关闭后确认手环退出。
3. 不生效时先核对应用版本、设备型号、应用内开关和系统权限，再查看日志：

```powershell
adb logcat -s Q66ZenRules:I zenMode:I
```

正常写入时依次出现 `109 result=`、`110 requested manual state=`、`verified manual state match=1`。状态本已一致时会跳过写入。

分享日志前请去除账号、通知内容和设备标识。
