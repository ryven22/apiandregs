package com.example.belajaarnewproject.patch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

object GameLauncher {

    /**
     * Memeriksa apakah game terinstal di perangkat
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            // Coba cek via Intent
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }
    }

    /**
     * Meluncurkan game target (Auto Login / Launch)
     */
    fun launchGame(context: Context, packageName: String): Boolean {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            // Intent standar gagal, coba fallback Shizuku jika aktif
        }

        // Fallback via shell Shizuku jika Shizuku tersedia
        if (ShizukuHelper.hasPermission()) {
            val res = ShizukuHelper.launchAppViaShell(context, packageName)
            if (res) return true
        }

        return false
    }
}
