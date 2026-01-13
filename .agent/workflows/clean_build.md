---
description: Standard Robust Build Process (Clean & Assemble)
---
# Robust Build Workflow

Always use this workflow when compiling the project to ensure no stale cache issues affect the build.

1. Clean and Assemble Debug
   - Runs `clean` to remove old build artifacts.
   - Runs `assembleDebug` to create a fresh APK.
   - Sets correct JAVA_HOME.

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; ./gradlew clean assembleDebug
```

// turbo
2. Verify APK Timestamp and Size
   - Lists the output file to confirm creation time and size.

```powershell
ls -l app/build/outputs/apk/debug/app-debug.apk
```
