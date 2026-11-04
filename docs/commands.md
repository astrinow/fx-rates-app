# Commands

All modules except `:androidApp` are Kotlin Multiplatform, so the Android task names differ
from a plain Android project: there is no `testDebugUnitTest` in `:shared`.

| What                         | Command                                         |
|------------------------------|-------------------------------------------------|
| Compile the shared code      | `./gradlew :shared:compileAndroidMain`          |
| Unit tests                   | `./gradlew :shared:testAndroidHostTest`         |
| Static analysis              | `./gradlew detekt`                              |
| Build the APK                | `./gradlew :androidApp:assembleDebug`           |
| Install on a device/emulator | `./gradlew :androidApp:installDebug`            |

Test reports: `shared/build/reports/tests/testAndroidHostTest/index.html`.
Detekt report: `shared/build/reports/detekt/detekt.html`.

## Before calling a change done

1. `./gradlew :shared:testAndroidHostTest detekt :androidApp:assembleDebug` is green.
2. The app was launched and the changed screen was looked at, including the loading and error
   states (airplane mode is enough for the error).
