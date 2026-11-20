# Mi Fitness 3.61.0 补丁

`patch.diff` 基于 3.61.0 原版 APK 的 `apktool d -r` 解包结果，修改 3 个 Smali 文件、新增 1 个：

```
smali_classes11/com/xiaomi/fitness/devicesettings/
├── DeviceSettingsSyncer.smali
└── utils/
    ├── ZenUtils.smali
    ├── ZenUtils$postSyncZenMode$2.smali
    └── Q66ZenRules.smali   (新增)
```

用 [`scripts/apply-patch.ps1`](../../scripts/apply-patch.ps1) 应用，步骤见[构建与测试](../../docs/build.md)。改动说明见[调查与实现](../../docs/investigation.md#改动文件)。其他版本的类名和字段可能不同，不保证兼容。
