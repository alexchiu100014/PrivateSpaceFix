package com.example.privatespacefix

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi

object PrivateSpaceHelper {

    private const val PRIVATE_PROFILE_TYPE = "android.os.usertype.profile.PRIVATE"

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getPrivateSpaceUser(context: Context): UserHandle? {
        val um = context.getSystemService(UserManager::class.java) ?: return null
        val getIdentifier = UserHandle::class.java.getMethod("getIdentifier")
        val getUserInfo = UserManager::class.java
            .getMethod("getUserInfo", Int::class.javaPrimitiveType)
        return um.userProfiles.firstOrNull { handle ->
            runCatching {
                val userId = getIdentifier.invoke(handle) as Int
                val userInfo = getUserInfo.invoke(um, userId) ?: return@firstOrNull false
                val userType = userInfo.javaClass.getField("userType").get(userInfo) as? String
                userType == PRIVATE_PROFILE_TYPE
            }.getOrDefault(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun queryPrivateSpaceApps(context: Context, classLoader: ClassLoader): List<Any>? {
        val privateUser = getPrivateSpaceUser(context) ?: return null

        val um = context.getSystemService(UserManager::class.java) ?: return null
        if (um.isQuietModeEnabled(privateUser)) return null

        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return null
        val activities = launcherApps.getActivityList(null, privateUser)
        if (activities.isEmpty()) return emptyList()

        val appInfoClass = classLoader.loadClass("com.android.launcher3.model.data.AppInfo")
        val launcherActivityInfoClass = Class.forName("android.content.pm.LauncherActivityInfo")

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

    fun buildAppInfoArray(
        apps: List<Any>,
        componentType: Class<*>,
    ): Array<Any?> {
        @Suppress("UNCHECKED_CAST")
        val array = java.lang.reflect.Array
            .newInstance(componentType, apps.size) as Array<Any?>
        apps.forEachIndexed { i, app -> array[i] = app }
        return array
    }
}
