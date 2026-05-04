package com.example.privatespacefix

import android.os.Build
import android.view.View
import io.github.libxposed.api.XposedInterface

class UpdateDataHooker : XposedInterface.Hooker {

    override fun intercept(chain: XposedInterface.Chain): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return chain.proceed()
        }

        val view = chain.thisObject as? View ?: return chain.proceed()

        @Suppress("UNCHECKED_CAST")
        val originalApps = chain.getArg(0) as? Array<Any?> ?: return chain.proceed()

        val privateApps = PrivateSpaceHelper.queryPrivateSpaceApps(
            view.context,
            view.javaClass.classLoader,
        ) ?: return chain.proceed()

        val componentType = originalApps.javaClass.componentType ?: return chain.proceed()

        @Suppress("UNCHECKED_CAST")
        val replacement = java.lang.reflect.Array
            .newInstance(componentType, privateApps.size) as Array<Any?>
        privateApps.forEachIndexed { i, app -> replacement[i] = app }

        return chain.proceed(arrayOf(replacement))
    }
}
