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

        hookUpdateData(cl)
    }

    private fun hookUpdateData(cl: ClassLoader) {
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

        hook(updateDataMethod)
            .intercept(UpdateDataHooker())
    }
}
