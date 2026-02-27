# Workaround: R8 obfuscates the non-incubator OTel API interfaces while the EDOT
# agent's consumer rules only keep io.opentelemetry.api.incubator.**. On API <= 29,
# lambda desugaring + interface obfuscation causes IllegalAccessError at runtime.
# TODO: Remove once fixed upstream in the EDOT Android SDK's shared-rules.pro.
-keep interface io.opentelemetry.api.** { *; }
