# PrivateSpaceFix — LSPosed Module

修正 Nothing Launcher 私人空間（Private Space）顯示錯誤 App 的 LSPosed 模組。

## 問題描述

Nothing Launcher 在私人空間頁面顯示的是「被標記為隱藏的 app」，
而非「實際屬於 Private Space 使用者的 app」。

## 修復策略

Hook `PrivateSpaceContainerView.updateData(AppInfo[])`，
在資料進入 UI 之前，過濾陣列只保留 `ItemInfo.user == PrivateSpaceUserHandle` 的項目。

### 為何選此掛鉤點

| 考量 | 說明 |
|------|------|
| 類名未混淆 | `PrivateSpaceContainerView` 可直接載入，不需 DexKit |
| 資料最後一關 | 在送入 AllAppsStore 前攔截，影響範圍最小 |
| Context 來源 | `thisObject as View` → `getContext()`，無需全域 helper |

## 環境需求

- Android 15（API 35）以上
- LSPosed（Zygisk 版）已安裝並啟用
- Nothing Launcher 已安裝

## 建置步驟

```bash
# 1. 用 Android Studio 開啟本專案
# 2. Build → Build Bundle(s) / APK(s) → Build APK(s)
# 3. 或直接執行：
./gradlew assembleRelease
```

產出位置：`app/build/outputs/apk/release/app-release.apk`

## 安裝步驟

```bash
# 安裝 APK
adb install app/build/outputs/apk/release/app-release.apk

# 在 LSPosed Manager 中啟用本模組，並勾選作用域：Nothing Launcher
# 重啟裝置
adb reboot
```

## 確認 Package Name

若 Nothing Launcher 的 package name 有誤，請執行：

```bash
# 確認 package name
adb shell pm list packages | grep nothing

# 或在 Launcher 開啟時
adb shell dumpsys window | grep mCurrentFocus
```

確認後修改 `MainModule.kt` 的 `targetPackage` 欄位。

## 專案結構

```
PrivateSpaceFix/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/example/privatespacefix/
│   │   │   ├── MainModule.kt          # 模組入口，掛鉤邏輯
│   │   │   └── UpdateDataHooker.kt   # Before Hook 實作
│   │   ├── res/values/
│   │   │   └── arrays.xml            # xposed_scope 宣告
│   │   ├── assets/
│   │   │   └── xposed_init           # 模組類名宣告
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 授權

MIT License
