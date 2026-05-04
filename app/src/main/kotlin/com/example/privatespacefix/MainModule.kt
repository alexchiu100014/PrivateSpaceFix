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

        val updateDataMethod = containerClass.getDeclaredMethod(
            "updateData",
            appInfoArrayClass
        )

        val onFinishInflateMethod = containerClass.getDeclaredMethod("onFinishInflate")

        // Hook 1: When updateData is called (by us or by the pipeline),
        // replace the data with actual private space apps
        hook(updateDataMethod)
            .intercept(UpdateDataHooker())

        // Hook 2: After the view is inflated, trigger loading of private
        // space apps since Nothing Launcher's StateFlow won't emit
        // when the hidden-apps list is empty
        hook(onFinishInflateMethod)
            .intercept(ViewInitHooker(updateDataMethod, appInfoArrayClass))
    }
}
