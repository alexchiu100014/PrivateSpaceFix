package com.example.privatespacefix

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class MainModule : XposedModule() {

    private val targetPackage = "com.nothing.launcher"

    override fun onModuleLoaded(param: ModuleLoadedParam) = Unit

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (!param.isFirstPackage) return
        if (param.packageName != targetPackage) return

        val cl = param.defaultClassLoader

        val appInfoClass = cl.loadClass("com.android.launcher3.model.data.AppInfo")
        val appInfoArrayClass = java.lang.reflect.Array
            .newInstance(appInfoClass, 0)
            .javaClass

        val containerClass = cl.loadClass(
            "com.nothing.launcher.privatespace.view.PrivateSpaceContainerView"
        )

        // Hook 1: Replace data whenever updateData is called (by us or by
        // Nothing's pipeline) with actual private space profile apps.
        val updateDataMethod = containerClass.getDeclaredMethod(
            "updateData", appInfoArrayClass
        )
        hook(updateDataMethod).intercept(UpdateDataHooker())

        // Hook 2: After the view inflates, trigger updateData ourselves
        // since Nothing's StateFlow won't emit when hidden-apps is empty.
        val onFinishInflateMethod = containerClass.getDeclaredMethod("onFinishInflate")
        hook(onFinishInflateMethod).intercept(ViewInitHooker(updateDataMethod, appInfoClass))
    }
}
