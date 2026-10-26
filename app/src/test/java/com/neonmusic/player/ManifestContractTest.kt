package com.neonmusic.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManifestContractTest {
    @Test
    fun declaresRequiredMediaAndForegroundServicePermissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()

        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_AUDIO))
        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE))
        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK))
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun mainActivityAndPlaybackServiceAreDeclared() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES
        )

        assertNotNull(info.activities?.firstOrNull { it.name.endsWith("MainActivity") })
        assertNotNull(info.services?.firstOrNull { it.name.endsWith("MusicPlaybackService") })
    }
}
