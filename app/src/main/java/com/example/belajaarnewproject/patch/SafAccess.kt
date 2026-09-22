package com.example.belajaarnewproject.patch

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Akses folder game via Storage Access Framework agar INJECT & BYPASS
 * bisa jalan langsung dari HP tanpa PC.
 * User memberi izin sekali ke folder Android/data (atau folder game), app menyimpan izinnya.
 */
object SafAccess {

    private const val PREFS = "saf_access"
    private const val KEY_URI = "tree_uri"

    data class SafInjectResult(val ok: Boolean, val log: String) {
        override fun toString(): String = log
    }

    /** Uri awal agar picker langsung buka folder data. */
    fun initialDataUri(): Uri? {
        return try {
            Uri.parse(
                "content://com.android.externalstorage.documents/tree/" +
                    Uri.encode("primary:Android/data")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveTreeUri(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // lanjut simpan walau persist gagal
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun loadTreeUri(context: Context): Uri? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URI, null) ?: return null
        return try {
            Uri.parse(s)
        } catch (e: Exception) {
            null
        }
    }

    fun hasAccess(context: Context): Boolean {
        val uri = loadTreeUri(context) ?: return false
        val persisted = context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isWritePermission }
        if (!persisted) return false
        return try {
            DocumentFile.fromTreeUri(context, uri)?.exists() == true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAccess(context: Context) {
        loadTreeUri(context)?.let { uri ->
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_URI).apply()
    }

    /**
     * Cari folder files milik game dari tree yang diberi user.
     * Mendukung tree = Android/data, Android, paket game langsung, atau files.
     */
    fun resolveFilesDir(context: Context, packageName: String): DocumentFile? {
        val uri = loadTreeUri(context) ?: return null
        val root = DocumentFile.fromTreeUri(context, uri) ?: return null

        // Kasus 1: User memilih langsung folder files
        if (root.name == "files") return root

        // Kasus 2: User memilih langsung folder paket (contoh: com.dts.freefiremax)
        if (root.name == packageName) {
            return root.findFile("files") ?: root.createDirectory("files")
        }

        // Kasus 3: Tree = Android/data -> cari paket -> cari files
        root.findFile(packageName)?.let { pkg ->
            return pkg.findFile("files") ?: pkg.createDirectory("files")
        }

        // Kasus 4: Tree = Android atau root -> turun ke data -> cari paket -> cari files
        root.findFile("data")?.let { data ->
            data.findFile(packageName)?.let { pkg ->
                return pkg.findFile("files") ?: pkg.createDirectory("files")
            }
        }

        return null
    }

    private fun ensureFilesDir(context: Context, packageName: String): DocumentFile? {
        resolveFilesDir(context, packageName)?.let { return it }
        return try {
            val uri = loadTreeUri(context) ?: return null
            val root = DocumentFile.fromTreeUri(context, uri) ?: return null

            if (root.name == packageName) {
                return root.findFile("files") ?: root.createDirectory("files")
            }
            if (root.name == "data") {
                val pkg = root.findFile(packageName) ?: root.createDirectory(packageName) ?: return null
                return pkg.findFile("files") ?: pkg.createDirectory("files")
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Cek via izin folder: kedua file benar-benar ada dan tidak kosong. */
    fun isInjected(context: Context, packageName: String): Boolean {
        return try {
            val dir = resolveFilesDir(context, packageName) ?: return false
            val p = dir.findFile(PatchCatalog.PATCH_FILE)
            val c = dir.findFile(PatchCatalog.CONFIG_FILE)
            p != null && p.length() > 0 && c != null && c.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /** Verifikasi pembacaan file via SAF untuk logging */
    fun verifyFilesRead(context: Context, item: PatchItem): Pair<Boolean, String> {
        val dir = resolveFilesDir(context, item.packageName) ?: return false to "Folder game tidak ditemukan"
        val patch = dir.findFile(PatchCatalog.PATCH_FILE)
        val config = dir.findFile(PatchCatalog.CONFIG_FILE)
        val ok = patch != null && patch.length() > 0 && config != null && config.length() > 0
        val details = if (ok) {
            "✓ Patch: ${patch?.length() ?: 0} bytes | Config: ${config?.length() ?: 0} bytes"
        } else {
            "Patch belum lengkap / belum terpasang"
        }
        return ok to details
    }

    /** Tulis patch langsung ke folder game dari HP via SAF. */
    fun injectItem(context: Context, item: PatchItem): SafInjectResult {
        val sb = StringBuilder()
        val dir = ensureFilesDir(context, item.packageName)
        if (dir == null) {
            return SafInjectResult(false, "${item.title}: Folder data tidak ditemukan via SAF.")
        }
        var ok = 0
        for (name in item.files) {
            try {
                context.assets.open("${item.assetFolder}/$name").use { input ->
                    val bytes = input.readBytes()
                    dir.findFile(name)?.delete()
                    val mime = if (name.endsWith(".json")) "application/json" else "application/octet-stream"
                    val nf = dir.createFile(mime, name)
                    if (nf != null) {
                        context.contentResolver.openOutputStream(nf.uri)?.use { it.write(bytes) }
                        if (nf.length() > 0) {
                            ok++
                            sb.appendLine("  ✓ $name: ${bytes.size} bytes (TERPASANG)")
                        }
                    }
                }
            } catch (e: Exception) {
                sb.appendLine("  ✗ $name: gagal (${e.message})")
            }
        }
        val allOk = (ok == item.files.size)
        if (allOk) {
            sb.appendLine("${item.title}: INJECT BERHASIL & AKTIF!")
        } else {
            sb.appendLine("${item.title}: Masuk $ok/${item.files.size}.")
        }
        return SafInjectResult(allOk, sb.toString())
    }

    fun injectAll(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("=== INJECT DARI HP ===")
        PatchCatalog.items.forEach { sb.append(injectItem(context, it).log) }
        return sb.toString()
    }

    private fun deleteCount(context: Context, item: PatchItem): Int {
        var n = 0
        try {
            val dir = resolveFilesDir(context, item.packageName)
            for (name in item.files) {
                try {
                    if (dir?.findFile(name)?.delete() == true) n++
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        return n
    }

    /** Hapus patch satu game langsung dari HP. */
    fun bypassItem(context: Context, item: PatchItem): String {
        return "${item.title} -> ${deleteCount(context, item)} data dibersihkan"
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
