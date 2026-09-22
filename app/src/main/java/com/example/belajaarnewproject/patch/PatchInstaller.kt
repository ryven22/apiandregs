package com.example.belajaarnewproject.patch

import android.content.Context
import android.os.Environment
import java.io.File

object PatchInstaller {

    data class InstallResult(val ok: Boolean, val log: String)

    fun assetFileSize(context: Context, assetPath: String): Long {
        return try {
            context.assets.open(assetPath).use { it.available().toLong() }
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Di Android 14 folder game tidak bisa dibaca aplikasi lain (Permission denied),
     * jadi status dibaca dari salinan Download yang BISA dilihat app.
     * AKTIF = file siap di Download, tinggal masuk via PC / manual.
     */
    fun downloadDir(item: PatchItem): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CenaRegs/${item.packageName}/files"
        )
    }

    fun targetStatus(item: PatchItem): String {
        val main = File(downloadDir(item), PatchCatalog.PATCH_FILE)
        return if (main.exists()) "AKTIF" else "BELUM"
    }

    fun isTargetInstalled(item: PatchItem): Boolean {
        return File(downloadDir(item), PatchCatalog.PATCH_FILE).exists()
    }

    private fun copyAssetToFile(context: Context, assetPath: String, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * INJECT:
     * 1. Salin ke folder aman milik aplikasi (selalu berhasil).
     * 2. Salin ke Download/CenaRegs agar mudah dipindah (PC / manual).
     * 3. Coba salin langsung ke folder game (di Android 14+ dibatasi sistem,
     *    tetap dicoba siapa tahu di HP lain bisa).
     */
    fun install(context: Context, item: PatchItem): InstallResult {
        val sb = StringBuilder()
        var okAny = false

        // 1. Folder aman aplikasi
        val safeDir = File(context.getExternalFilesDir(null), "external/${item.packageName}/files")
        sb.appendLine(">> Folder aman: ${safeDir.absolutePath}")
        item.files.forEachIndexed { index, f ->
            val ok = copyAssetToFile(context, "${item.assetFolder}/$f", File(safeDir, f))
            sb.appendLine("   Data ${index + 1} -> ${if (ok) "OK" else "Belum, coba lagi"}")
            if (ok) okAny = true
        }

        // 2. Folder Download (jalur utama: PC / manual)
        val downloadDir = downloadDir(item)
        sb.appendLine(">> Salinan Download: ${downloadDir.absolutePath}")
        item.files.forEachIndexed { index, f ->
            val ok = try {
                copyAssetToFile(context, "${item.assetFolder}/$f", File(downloadDir, f))
            } catch (e: Exception) {
                sb.appendLine("   Data ${index + 1} tertunda")
                false
            }
            sb.appendLine("   Data ${index + 1} -> ${if (ok) "OK, siap" else "Belum, coba lagi"}")
            if (ok) okAny = true
        }

        // 3. Coba langsung ke folder game
        val targetDir = PatchCatalog.targetDir(item)
        var directOk = false
        item.files.forEachIndexed { index, f ->
            val ok = try {
                copyAssetToFile(context, "${item.assetFolder}/$f", File(targetDir, f))
            } catch (e: Exception) {
                false
            }
            if (ok) { directOk = true; okAny = true }
        }
        if (directOk) {
            sb.appendLine(">> Folder game: langsung masuk, status AKTIF.")
        } else {
            sb.appendLine(">> Folder game: dibatasi sistem, selesaikan via PC (INJECT_PC) / manual.")
        }

        if (!okAny) {
            sb.appendLine("Belum berhasil. Aktifkan 'Akses Penyimpanan' lalu coba lagi.")
        } else {
            sb.appendLine("Selesai. Siap di Download/CenaRegs.")
        }
        return InstallResult(okAny, sb.toString())
    }

    /**
     * BYPASS PROTECT: coba hapus patch dari FF MAX & FREE FIRE ORI.
     * Di Android 14+ folder game dibatasi sistem sehingga app tidak bisa
     * menghapus langsung (butuh PC via BYPASS_PC / file manager khusus).
     */
    fun bypassProtect(): String {
        val sb = StringBuilder()
        sb.appendLine("=== BYPASS PROTECT ===")
        var totalHapus = 0
        PatchCatalog.items.forEach { item ->
            val dir = PatchCatalog.targetDir(item)
            var hapusItem = 0
            for (f in item.files) {
                try {
                    val file = File(dir, f)
                    if (file.exists() && file.delete()) {
                        hapusItem++
                        totalHapus++
                    }
                } catch (e: Exception) {
                    // dibatasi sistem, lanjut
                }
            }
            sb.appendLine(">> ${item.title} -> $hapusItem data dibersihkan via app")
        }
        if (totalHapus == 0) {
            sb.appendLine("Akses langsung dibatasi sistem.")
            sb.appendLine("Gunakan BYPASS_PC di laptop / reset manual via app khusus.")
        } else {
            sb.appendLine("Total dibersihkan: $totalHapus item. BYPASS selesai.")
        }
        return sb.toString()
    }

    fun uninstallTarget(item: PatchItem): String {
        val dir = PatchCatalog.targetDir(item)
        val sb = StringBuilder()
        var n = 0
        for (f in item.files) {
            val file = File(dir, f)
            if (file.exists()) {
                if (file.delete()) n++
            }
        }
        sb.appendLine("${item.title} -> $n item dibersihkan")
        return sb.toString()
    }
}
