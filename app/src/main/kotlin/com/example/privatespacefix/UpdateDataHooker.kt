package com.example.privatespacefix

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.view.View
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedInterface

class UpdateDataHooker : XposedInterface.Hooker {

    companion object {
        private const val PRIVATE_PROFILE_TYPE = "android.os.usertype.profile.PRIVATE"
    }

    override fun intercept(chain: XposedInterface.Chain): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return chain.proceed()
        }

        val view = chain.thisObject as? View ?: return chain.proceed()
        val context = view.context

        @Suppress("UNCHECKED_CAST")
        val originalApps = chain.getArg(0) as? Array<Any?> ?: return chain.proceed()

        val privateUser = getPrivateSpaceUser(context) ?: return chain.proceed()
        val privateApps = queryPrivateSpaceApps(context, privateUser, view.javaClass.classLoader)
            ?: return chain.proceed()

        val componentType = originalApps.javaClass.componentType ?: return chain.proceed()

        @Suppress("UNCHECKED_CAST")
        val replacement = java.lang.reflect.Array
            .newInstance(componentType, privateApps.size) as Array<Any?>
        privateApps.forEachIndexed { i, app -> replacement[i] = app }

        return chain.proceed(arrayOf(replacement))
    }

    /**
     * Query actual apps installed in the Android 15 private space profile
     * via LauncherApps, then construct AppInfo objects using the launcher's
     * own constructor: AppInfo(Context, LauncherActivityInfo, UserHandle).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun queryPrivateSpaceApps(
        context: Context,
        privateUser: UserHandle,
        classLoader: ClassLoader,
    ): List<Any>? {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val activities = launcherApps.getActivityList(null, privateUser)
        if (activities.isEmpty()) return emptyList()

        val appInfoClass = classLoader.loadClass("com.android.launcher3.model.data.AppInfo")
        val launcherActivityInfoClass = Class.forName("android.content.pm.LauncherActivityInfo")

        // Nothing Launcher's AppInfo(Context, LauncherActivityInfo, UserHandle)
        val constructor = runCatching {
            appInfoClass.getDeclaredConstructor(
                Context::class.java,
                launcherActivityInfoClass,
                UserHandle::class.java,
            ).also { it.isAccessible = true }
        }.getOrNull() ?: return null

        return activities.mapNotNull { activityInfo ->
            runCatching {
                constructor.newInstance(context, activityInfo, privateUser)
            }.getOrNull()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPrivateSpaceUser(context: Context): UserHandle? {
        val um = context.getSystemService(UserManager::class.java) ?: return null
        val getIdentifier = UserHandle::class.java.getMethod("getIdentifier")
        val getUserInfo = UserManager::class.java.getMethod("getUserInfo", Int::class.javaPrimitiveType)
        return um.userProfiles.firstOrNull { handle ->
            runCatching {
                val userId = getIdentifier.invoke(handle) as Int
                val userInfo = getUserInfo.invoke(um, userId) ?: return@firstOrNull false
                val userType = userInfo.javaClass.getField("userType").get(userInfo) as? String
                userType == PRIVATE_PROFILE_TYPE
            }.getOrDefault(false)
        }
    }
}
