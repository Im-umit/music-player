# NeonMusic — Full source package with CI test-runner fix

This ZIP contains the complete source tree from `NeonMusic-Audit-Fix13.zip`, with a targeted fix for the known GitHub Actions unit-test failures.

## Included fix
- `ManifestContractTest` now runs under Robolectric at SDK 36.
- `SongDaoTest` now runs under Robolectric at SDK 36.
- The project already declares Robolectric and Android resource inclusion for local unit tests.

## Build / verification status
This package is not certified as fully build-clean. The known CI failure suggested these two test classes were being run as plain JVM tests despite using Android `ApplicationProvider`, PackageManager, and Room. The runner fix addresses that likely root cause, but a full Gradle run still needs to confirm it. This environment has JDK 21 but no configured Android SDK, so APK assembly and complete Android tests could not be verified here.

From the project root, run:
```bash
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon
```

If those pass, upload/push the **contents of this source package** to a new branch of your GitHub repository. Do not upload this ZIP as a single file if you want GitHub Actions to build the project.
