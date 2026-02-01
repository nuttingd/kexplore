# Kexplore

A Kubernetes API explorer for Android.

## Requirements

- JDK 17
- Android SDK 35

Min SDK is 28 (Android 9).

## Build

```sh
./gradlew assembleDebug
```

For a signed release build, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details, then:

```sh
./gradlew assembleRelease
```

## Test

```sh
./gradlew test                        # unit tests
./gradlew connectedAndroidTest        # instrumented tests (needs a device/emulator)
```
