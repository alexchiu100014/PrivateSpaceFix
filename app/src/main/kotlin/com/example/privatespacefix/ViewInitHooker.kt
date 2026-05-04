package com.example.privatespacefix

import android.os.Build
import android.view.View
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

/**
 * Intercepts PrivateSpaceContainerView.onFinishInflate().
 *
 * Nothing Launcher's data pipeline (StateFlow) never emits when
 * its hidden-apps list is empty, so updateData() is never called.
 * After the view finishes inflating, we post a Runnable that
 * queries real private space apps and calls updateData() directly,
 * which then triggers UpdateDataHooker to ensure correct data.
 */
class ViewInitHooker(
    private val updateDataMethod: Method,
    private val appInfoClass: Class<*>,
) : XposedInterface.Hooker {

    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return result
        }

        val view = chain.thisObject as? View ?: return result

        view.post {
            val apps = PrivateSpaceHelper.queryPrivateSpaceApps(
                view.context,
                view.javaClass.classLoader,
            ) ?: return@post

            val array = PrivateSpaceHelper.buildAppInfoArray(apps, appInfoClass)
            runCatching { updateDataMethod.invoke(view, array) }
        }

        return result
    }
}
