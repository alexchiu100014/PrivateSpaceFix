# LSPosed 模組 ProGuard 規則

# 保留所有 Hooker 類（libxposed 透過反射呼叫）
-keep class com.example.privatespacefix.** { *; }

# 保留 libxposed annotations
-keepattributes *Annotation*

# 保留 XposedHooker / BeforeInvocation 標注
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
