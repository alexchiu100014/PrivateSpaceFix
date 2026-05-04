package com.example.privatespacefix

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

        @Suppress("UNCHECKED_CAST")
        val apps = chain.getArg(0) as? Array<Any?> ?: return chain.proceed()
        if (apps.isEmpty()) return chain.proceed()

        val privateUser = getPrivateSpaceUser(view.context) ?: return chain.proceed()

        val userField = runCatching {
            Class.forName(
                "com.android.launcher3.model.data.ItemInfo",
                false,
                apps[0]!!.javaClass.classLoader,
            ).getDeclaredField("user").also { it.isAccessible = true }
        }.getOrNull() ?: return chain.proceed()

        val filtered = apps.filter { app ->
            app != null && (userField.get(app) as? UserHandle) == privateUser
        }

        val componentType = apps.javaClass.componentType ?: return chain.proceed()

        @Suppress("UNCHECKED_CAST")
        val replacement = java.lang.reflect.Array
            .newInstance(componentType, filtered.size) as Array<Any?>
        filtered.forEachIndexed { i, app -> replacement[i] = app }

        return chain.proceed(arrayOf(replacement))
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun getPrivateSpaceUser(context: android.content.Context): UserHandle? {
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
