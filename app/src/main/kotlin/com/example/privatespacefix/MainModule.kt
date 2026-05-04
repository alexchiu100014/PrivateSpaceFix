package com.example.privatespacefix

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * LSPosed 模組主入口。
 *
 * 目標：攔截 Nothing Launcher 的 PrivateSpaceContainerView.updateData(AppInfo[])，
 * 在資料送入 AllAppsStore 之前，過濾掉非 Private Space 使用者的 app，
 * 修正私人空間顯示成「設定為隱藏的 app」而非「私人空間 app」的問題。
 *
 * 驗證目標 package name：
 *   adb shell dumpsys window | grep mCurrentFocus   (Launcher 開著時執行)
 */
class MainModule(
    base: XposedModuleInterface,
    host: XposedModuleInterface.HostInfo,
) : XposedModule(base, host) {

    // TODO: 若實際 package name 不同請修改此處
    private val targetPackage = "com.nothing.launcher"

    override fun onModuleLoaded(param: ModuleLoadedParam) = Unit

    override fun onPackageLoaded(param: PackageLoadedParam) {
        // 只處理目標 package 的第一次載入
        if (!param.isFirstPackage) return
        if (param.packageName != targetPackage) return

        val cl = param.classLoader

        // ── 1. 取得 AppInfo class 並建立對應陣列 class ──────────────────────
        val appInfoClass = cl.loadClass("com.android.launcher3.model.data.AppInfo")
        val appInfoArrayClass = java.lang.reflect.Array
            .newInstance(appInfoClass, 0)
            .javaClass

        // ── 2. 取得 PrivateSpaceContainerView.updateData(AppInfo[]) ─────────
        //    類名未混淆，是資料進入 UI 的最後一關。
        val containerClass = cl.loadClass(
            "com.nothing.launcher.privatespace.view.PrivateSpaceContainerView"
        )
        val updateDataMethod = containerClass.getDeclaredMethod(
            "updateData",
            appInfoArrayClass
        )

        // ── 3. 掛鉤，交由 UpdateDataHooker 在 before 階段過濾陣列 ──────────
        hook(updateDataMethod, UpdateDataHooker::class.java)
    }
}
