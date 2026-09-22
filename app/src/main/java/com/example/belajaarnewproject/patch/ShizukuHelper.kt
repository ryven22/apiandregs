package com.example.belajaarnewproject.patch

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.example.belajaarnewproject.shizuku.IShellService
import com.example.belajaarnewproject.shizuku.ShellUserService
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * INJECT & BYPASS langsung dari HP via Shizuku (hak shell, tanpa pilih folder).
 * Syarat sekali saja: install app Shizuku → pairing wireless debugging → start.
 */
object ShizukuHelper {

    const val REQUEST_CODE = 1001
    private const val TAG = "CenaRegs"

    data class ShellInjectResult(
        val ok: Boolean,
        val log: String,
        val fileSizes: Map<String, Long> = emptyMap()
    )

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        if (!isRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
        }
    }

    /**
     * Minta izin otomatis sekali agar app muncul di daftar Shizuku.
     * Dipanggil tiap buka app: hanya benar-benar request bila belum pernah.
     */
    fun ensureRequested(context: Context): Boolean {
        return try {
            if (!isRunning() || hasPermission()) return hasPermission()
            val prefs = context.getSharedPreferences("saf_access", Context.MODE_PRIVATE)
            if (prefs.getBoolean("shizuku_asked", false)) return false
            prefs.edit().putBoolean("shizuku_asked", true).apply()
            Shizuku.requestPermission(REQUEST_CODE)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Jalankan perintah di proses shell. Return Pair(kodeExit, output). */
    fun execShell(context: Context, cmd: String): Pair<Int, String> {
        if (!hasPermission()) return -1 to "Belum terhubung."
        var service: IShellService? = null
        val latch = CountDownLatch(1)
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                try {
                    service = IShellService.Stub.asInterface(binder)
                } catch (e: Exception) {
                }
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                latch.countDown()
            }

            override fun onBindingDied(name: ComponentName?) {
                latch.countDown()
            }

            override fun onNullBinding(name: ComponentName?) {
                latch.countDown()
            }
        }
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .version(1)
        return try {
            Shizuku.bindUserService(args, conn)
            if (!latch.await(20, TimeUnit.SECONDS)) {
                Log.d(TAG, "bind timeout")
                return -1 to "Service lambat (timeout), coba lagi."
            }
            val svc = service ?: run {
                Log.d(TAG, "service null")
                return -1 to "Service tidak jalan."
            }
            val raw = try {
                svc.exec(cmd)
            } catch (e: Exception) {
                Log.d(TAG, "exec fail: ${e.message}")
                return -1 to "Tertunda (${e.message})."
            }
            val code = raw.lineSequence().firstOrNull()?.trim()?.toIntOrNull() ?: -1
            val out = raw.lineSequence().drop(1).joinToString("\n")
            Log.d(TAG, "exec code=$code cmd=$cmd out=$out")
            code to out
        } catch (e: Exception) {
            -1 to "Tertunda, coba lagi."
        } finally {
            try {
                Shizuku.unbindUserService(args, conn, false)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Hentikan proses game yang sedang berjalan agar saat di-inject
     * dan dibuka kembali, game membaca patch dari nol (cold boot).
     */
    fun forceStopGame(context: Context, packageName: String): Boolean {
        if (!hasPermission()) return false
        val r = execShell(context, "am force-stop $packageName")
        return r.first == 0
    }

    /**
     * Cek apakah paket game terpasang via shell pm path
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        if (!hasPermission()) return false
        val r = execShell(context, "pm path $packageName")
        return r.first == 0 && r.second.contains("package:")
    }

    /**
     * Buka game langsung dari shell (auto login / launch)
     */
    fun launchAppViaShell(context: Context, packageName: String): Boolean {
        if (!hasPermission()) return false
        val r = execShell(context, "monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        return r.first == 0
    }

    private fun stageFile(context: Context, item: PatchItem, name: String): File {
        val dir = File(context.getExternalFilesDir(null), "stage/${item.packageName}")
        dir.mkdirs()
        val dest = File(dir, name)
        context.assets.open("${item.assetFolder}/$name").use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.setReadable(true, false)
        return dest
    }

    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    /** Cek jujur via shell: file benar ada dan tidak kosong di folder game. */
    fun isInjected(context: Context, item: PatchItem): Boolean {
        val patch = "/sdcard/Android/data/${item.packageName}/files/${PatchCatalog.PATCH_FILE}"
        val config = "/sdcard/Android/data/${item.packageName}/files/${PatchCatalog.CONFIG_FILE}"
        val cmd = "[ -s ${q(patch)} ] && [ -s ${q(config)} ]"
        return execShell(context, cmd).first == 0
    }

    /**
     * Verifikasi pembacaan file di folder game dan ambil ukuran riil
     */
    fun verifyFilesRead(context: Context, item: PatchItem): Pair<Boolean, String> {
        val dir = "/sdcard/Android/data/${item.packageName}/files"
        val patch = "$dir/${PatchCatalog.PATCH_FILE}"
        val config = "$dir/${PatchCatalog.CONFIG_FILE}"
        val cmd = """
            if [ -s ${q(patch)} ] && [ -s ${q(config)} ]; then
                echo "STATUS=OK"
                wc -c < ${q(patch)}
                wc -c < ${q(config)}
            else
                echo "STATUS=FAIL"
            fi
        """.trimIndent()
        val r = execShell(context, cmd)
        val lines = r.second.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val ok = lines.any { it.contains("STATUS=OK") }
        val details = if (ok) {
            val sPatch = lines.getOrNull(1)?.toLongOrNull() ?: -1L
            val sConfig = lines.getOrNull(2)?.toLongOrNull() ?: -1L
            "✓ Patch: ${sPatch} bytes | Config: ${sConfig} bytes"
        } else {
            "Patch belum lengkap / belum terpasang"
        }
        return ok to details
    }

    /**
     * Tulis patch langsung ke folder game dari HP,
     * pastikan folder dibuat (mkdir -p), izin diatur (chmod 666),
     * dan diverifikasi secara real.
     */
    fun injectItem(context: Context, item: PatchItem): ShellInjectResult {
        val sb = StringBuilder()
        val staged = try {
            item.files.map { stageFile(context, item, it) }
        } catch (e: Exception) {
            return ShellInjectResult(false, "${item.title}: Gagal mempersiapkan asset lokal.")
        }

        val targetDir = "/sdcard/Android/data/${item.packageName}/files"
        // Buat folder game jika belum ada
        val mkdirResult = execShell(context, "mkdir -p ${q(targetDir)} && chmod 777 ${q(targetDir)}")
        if (mkdirResult.first != 0) {
            sb.appendLine("Peringatan mkdir: ${mkdirResult.second.trim()}")
        }

        var ok = 0
        val fileSizes = mutableMapOf<String, Long>()

        for ((i, name) in item.files.withIndex()) {
            val src = staged[i].absolutePath
            val dst = "$targetDir/$name"
            val cmd = """
                cp -f ${q(src)} ${q(dst)} && chmod 666 ${q(dst)} && if [ -s ${q(dst)} ]; then wc -c < ${q(dst)}; else echo "0"; fi
            """.trimIndent()
            val r = execShell(context, cmd)
            val size = r.second.lines().firstOrNull { it.trim().toLongOrNull() != null }?.trim()?.toLongOrNull() ?: 0L

            if (r.first == 0 && size > 0L) {
                ok++
                fileSizes[name] = size
                sb.appendLine("  ✓ $name: $size bytes (TERPASANG)")
            } else {
                val detail = r.second.trim().take(120)
                sb.appendLine("  ✗ $name: gagal disuntikkan (${if (detail.isNotEmpty()) detail else "kode ${r.first}"})")
            }
        }

        // Set hak akses menyeluruh ke folder files
        execShell(context, "chmod 777 ${q(targetDir)}")

        val allOk = (ok == item.files.size)
        if (allOk) {
            sb.appendLine("${item.title}: INJECT SUKSES & AKTIF!")
        } else {
            sb.appendLine("${item.title}: Masuk $ok/${item.files.size}. Coba ulangi.")
        }
        return ShellInjectResult(allOk, sb.toString(), fileSizes)
    }

    private fun deleteCount(context: Context, item: PatchItem): Int {
        var n = 0
        for (name in item.files) {
            val dst = "/sdcard/Android/data/${item.packageName}/files/$name"
            val r = execShell(context, "rm -f ${q(dst)}; [ -e ${q(dst)} ] && echo ADA || echo HILANG")
            if (r.second.contains("HILANG")) n++
        }
        return n
    }

    /** Hapus patch satu game langsung dari HP. */
    fun bypassItem(context: Context, item: PatchItem): String {
        deleteCount(context, item)
        return "${item.title} -> Patch dibersihkan"
    }

    /** Hapus patch langsung dari HP. */
    fun bypassAll(context: Context): String {
        val sb = StringBuilder("=== BYPASS PROTECT ===")
        var total = 0
        for (item in PatchCatalog.items) {
            val n = deleteCount(context, item)
            total += n
            sb.appendLine("${item.title} -> $n item dibersihkan")
        }
        sb.appendLine("Total dibersihkan: $total item. BYPASS selesai.")
        return sb.toString()
    }
}
