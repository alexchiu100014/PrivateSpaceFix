package com.example.privatespacefix

import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.view.View
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.annotations.XposedHooker

/**
 * 攔截 PrivateSpaceContainerView.updateData(AppInfo[])。
 *
 * 在陣列送入 AllAppsStore 之前（before 階段），重新過濾：
 * 只保留 ItemInfo.user == Private Space UserHandle 的 app，
 * 剔除因 isHiddenApp() 誤判而混入的 profile app。
 *
 * libxposed 規範重點：
 *   - Hooker 不可持有外部（非靜態）引用
 *   - before 必須是 companion object 的 @JvmStatic 方法
 *   - 透過 callback.thisObject (View) 取 context，避免依賴全域 AndroidAppHelper
 */
@XposedHooker
class UpdateDataHooker : XposedInterface.Hooker {

    companion object {

        /** Android 15 私人空間的使用者類型字串（UserManager 內部常數） */
        private const val PRIVATE_PROFILE_TYPE = "android.os.usertype.profile.PRIVATE"

        // ── Before Hook ──────────────────────────────────────────────────────

        @JvmStatic
        @XposedInterface.BeforeInvocation
        fun before(callback: BeforeHookCallback) {
            // 私人空間功能僅在 API 35（Android 15）以上存在
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

            // thisObject 是 PrivateSpaceContainerView，繼承自 View
            val view = callback.thisObject as? View ?: return

            // args[0] 是 AppInfo[]，型別在 hook 時已確認
            @Suppress("UNCHECKED_CAST")
            val apps = callback.args[0] as? Array<Any?> ?: return
            if (apps.isEmpty()) return

            // 取得 Private Space 的 UserHandle；若裝置未啟用私人空間則直接返回
            val privateUser = getPrivateSpaceUser(view.context) ?: return

            // ── 反射取得 ItemInfo.user 欄位 ──────────────────────────────────
            val userField = runCatching {
                Class.forName(
                    "com.android.launcher3.model.data.ItemInfo",
                    false,
                    apps[0]!!.javaClass.classLoader,
                ).getDeclaredField("user").also { it.isAccessible = true }
            }.getOrNull() ?: return

            // ── 過濾：只保留屬於 Private Space 使用者的 app ──────────────────
            val filtered = apps.filter { app ->
                app != null && (userField.get(app) as? UserHandle) == privateUser
            }

            // ── 建立同型別的替換陣列並寫回 args[0] ───────────────────────────
            val componentType = apps.javaClass.componentType ?: return

            @Suppress("UNCHECKED_CAST")
            val replacement = java.lang.reflect.Array
                .newInstance(componentType, filtered.size) as Array<Any?>
            filtered.forEachIndexed { i, app -> replacement[i] = app }

            callback.args[0] = replacement
        }

        // ── 工具函式 ─────────────────────────────────────────────────────────

        /**
         * 回傳 Private Space profile 的 [UserHandle]；若不存在則回傳 null。
         *
         * 使用 [UserManager.getUserProfiles]（不需要 MANAGE_USERS 權限），
         * 再以 [UserManager.getUserInfo] 比對 userType 字串。
         * userType 比對方式在 API 35 引入，因此搭配 @RequiresApi 標注。
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        private fun getPrivateSpaceUser(context: android.content.Context): UserHandle? {
            val um = context.getSystemService(UserManager::class.java) ?: return null
            return um.userProfiles.firstOrNull { handle ->
                runCatching {
                    um.getUserInfo(handle.identifier)?.userType == PRIVATE_PROFILE_TYPE
                }.getOrDefault(false)
            }
        }
    }
}
