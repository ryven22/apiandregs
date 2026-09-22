package com.example.belajaarnewproject.patch

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object SafInject {

    /** Uri awal agar picker langsung buka folder game. */
    fun initialGameFolderUri(packageName: String): Uri? {
        return try {
            val path = "primary:Android/data/$packageName/files"
            Uri.parse(
                "content://com.android.externalstorage.documents/tree/" +
                    Uri.encode(path)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Salin 2 file external dari assets ke folder yang dipilih user via SAF.
     * Return log siap tampil (tanpa kata terlarang).
     */
    fun copyToTree(context: Context, treeUri: Uri, item: PatchItem): String {
        val sb = StringBuilder()
        return try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // abaikan, lanjut coba salin
            null
        }.let {
            try {
                val dir = DocumentFile.fromTreeUri(context, treeUri)
                    ?: return "Folder tidak terbaca. Coba pilih ulang folder files game."
                var okCount = 0
                for (name in item.files) {
                    try {
                        context.assets.open("${item.assetFolder}/$name").use { input ->
                            val bytes = input.readBytes()
                            // hapus file lama bila ada agar replace bersih
                            dir.findFile(name)?.delete()
                            val mime = if (name.endsWith(".json")) "application/json" else "application/octet-stream"
                            val newFile = dir.createFile(mime, name)
                            if (newFile != null) {
                                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                                    out.write(bytes)
                                }
                                okCount++
                                sb.appendLine("Data $okCount -> OK (langsung masuk)")
                            } else {
                                sb.appendLine("Data belum masuk, coba lagi")
                            }
                        }
                    } catch (e: Exception) {
                        sb.appendLine("Data tertunda: ${e.message}")
                    }
                }
                if (okCount == item.files.size) {
                    sb.appendLine("INJECT ${item.title} BERHASIL.")
                } else {
                    sb.appendLine("INJECT selesai sebagian ($okCount/${item.files.size}). Coba lagi / pindah manual dari Download/CenaRegs.")
                }
                sb.toString()
            } catch (e: Exception) {
                "INJECT tertunda: ${e.message}. Pindah manual dari Download/CenaRegs."
            }
        }
    }
}
