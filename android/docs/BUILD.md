# Build and install

## Requirements

- JDK 21
- Android SDK 35
- Android NDK 27
- CMake 3.22.1

The Codespace build environment used:

```bash
export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.13.0+2-ms
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
cd android
./gradlew assembleDebug
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected Android device:

```bash
/opt/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
