package com.example.privatespacefix

import android.view.View
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

class ViewInitHooker(
    private val updateDataMethod: Method,
    private val appInfoArrayClass: Class<*>,
) : XposedInterface.Hooker {

    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()

        val view = chain.thisObject as? View ?: return result

        // Post to ensure the view is fully initialized before we push data
        view.post {
            val apps = PrivateSpaceHelper.queryPrivateSpaceApps(
                view.context,
                view.javaClass.classLoader,
            )
            if (apps != null) {
                val array = java.lang.reflect.Array
                    .newInstance(appInfoArrayClass.componentType, apps.size)
                apps.forEachIndexed { i, app ->
                    java.lang.reflect.Array.set(array, i, app)
                }
                updateDataMethod.invoke(view, array)
            }
        }

        return result
    }
}
