package com.example.privatespacefix

import android.os.Build
import android.view.View
import io.github.libxposed.api.XposedInterface

/**
 * Intercepts PrivateSpaceContainerView.updateData(AppInfo[]).
 *
 * Replaces whatever Nothing Launcher passes (hidden main-user apps)
 * with actual apps from the Android 15 private space profile.
 * If no private space profile exists or it's locked (quiet mode),
 * falls through to the original behavior.
 */
class UpdateDataHooker : XposedInterface.Hooker {

    override fun intercept(chain: XposedInterface.Chain): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return chain.proceed()
        }

        val view = chain.thisObject as? View ?: return chain.proceed()

        @Suppress("UNCHECKED_CAST")
        val originalApps = chain.getArg(0) as? Array<Any?> ?: return chain.proceed()
        val componentType = originalApps.javaClass.componentType ?: return chain.proceed()

        val privateApps = PrivateSpaceHelper.queryPrivateSpaceApps(
            view.context,
            view.javaClass.classLoader,
        ) ?: return chain.proceed()

        val replacement = PrivateSpaceHelper.buildAppInfoArray(privateApps, componentType)
        return chain.proceed(arrayOf(replacement))
    }
}
