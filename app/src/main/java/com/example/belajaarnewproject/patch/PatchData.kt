package com.example.belajaarnewproject.patch

import com.example.belajaarnewproject.R

data class PatchItem(
    val packageName: String,
    val title: String,
    val shortName: String,
    val iconRes: Int,
    val assetFolder: String,
    val files: List<String>
)

object PatchCatalog {
    const val PATCH_FILE = "Assembly-CSharp-patch.bytes"
    const val CONFIG_FILE = "localConfig.json"

    val items = listOf(
        PatchItem(
            packageName = "com.dts.freefiremax",
            title = "FREE FIRE MAX",
            shortName = "FREE FIRE MAX",
            iconRes = R.drawable.ff_max,
            assetFolder = "patches/com.dts.freefiremax/files",
            files = listOf(PATCH_FILE, CONFIG_FILE)
        ),
        PatchItem(
            packageName = "com.dts.freefireth",
            title = "FREE FIRE ORI",
            shortName = "FREE FIRE ORI",
            iconRes = R.drawable.ff_th,
            assetFolder = "patches/com.dts.freefireth/files",
            files = listOf(PATCH_FILE, CONFIG_FILE)
        )
    )

    // Target resmi game di HP: /storage/emulated/0/Android/data/<package>/files/
    fun targetDir(item: PatchItem): java.io.File {
        return java.io.File(
            android.os.Environment.getExternalStorageDirectory(),
            "Android/data/${item.packageName}/files"
        )
    }
}
