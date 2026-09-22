package com.example.belajaarnewproject.shizuku

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Berjalan di proses shell milik Shizuku (bukan proses app),
 * sehingga perintah file bisa menyentuh folder game.
 * WAJIB extend Stub (IBinder), bukan Service.
 */
class ShellUserService : IShellService.Stub {

    constructor() : super()

    constructor(context: Context) : super()

    override fun exec(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = StringBuilder()
            val t1 = Thread {
                try {
                    p.inputStream.bufferedReader().forEachLine { out.appendLine(it) }
                } catch (e: Exception) {
                }
            }
            val t2 = Thread {
                try {
                    p.errorStream.bufferedReader().forEachLine { out.appendLine(it) }
                } catch (e: Exception) {
                }
            }
            t1.start()
            t2.start()
            val finished = p.waitFor(25, TimeUnit.SECONDS)
            if (!finished) {
                try {
                    p.destroy()
                } catch (e: Exception) {
                }
            }
            t1.join(2000)
            t2.join(2000)
            "${if (finished) p.exitValue() else -1}\n$out"
        } catch (e: Exception) {
            "-1\n${e.message}"
        }
    }
}
