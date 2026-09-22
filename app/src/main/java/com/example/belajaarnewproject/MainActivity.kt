package com.example.belajaarnewproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.belajaarnewproject.patch.GameLauncher
import com.example.belajaarnewproject.patch.PatchCatalog
import com.example.belajaarnewproject.patch.PatchInstaller
import com.example.belajaarnewproject.patch.PatchItem
import com.example.belajaarnewproject.patch.SafAccess
import com.example.belajaarnewproject.patch.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import com.example.belajaarnewproject.auth.AuthManager
import com.example.belajaarnewproject.auth.SupabaseConfig
import com.example.belajaarnewproject.auth.UserSession
import com.example.belajaarnewproject.ui.theme.ExternalAndroidByRegsxdTheme
import com.example.belajaarnewproject.ui.theme.BlackBackground
import com.example.belajaarnewproject.ui.theme.BlackSurface
import com.example.belajaarnewproject.ui.theme.BlackSurfaceVariant
import com.example.belajaarnewproject.ui.theme.LogTextColor
import com.example.belajaarnewproject.ui.theme.RedDark
import com.example.belajaarnewproject.ui.theme.RedPrimary
import com.example.belajaarnewproject.ui.theme.SuccessGreen
import com.example.belajaarnewproject.ui.theme.TextGrey
import com.example.belajaarnewproject.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    private var permissionRefresh = mutableIntStateOf(0)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRefresh.intValue++
    }

    private val allFilesAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionRefresh.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExternalAndroidByRegsxdTheme {
                val context = LocalContext.current
                var userSession by remember { mutableStateOf(AuthManager.getSession(context)) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BlackBackground
                ) {
                    if (!userSession.isLoggedIn) {
                        LicenseLoginScreen(
                            onLoginSuccess = {
                                userSession = AuthManager.getSession(context)
                            }
                        )
                    } else {
                        PatchMenuScreen(
                            hasAllFiles = hasStorageAccess(),
                            refreshKey = permissionRefresh.intValue,
                            userSession = userSession,
                            onLogout = {
                                AuthManager.logout(context)
                                userSession = AuthManager.getSession(context)
                            },
                            onRequestPermission = { requestStorageAccess() },
                            onOpenAllFilesSettings = { openAllFilesSettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefresh.intValue++
    }

    private fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            openAllFilesSettings()
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun openAllFilesSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                allFilesAccessLauncher.launch(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            allFilesAccessLauncher.launch(intent)
        }
    }
}

@Composable
fun PatchMenuScreen(
    hasAllFiles: Boolean,
    refreshKey: Int,
    userSession: UserSession,
    onLogout: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAllFilesSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceBrand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val deviceModel = Build.MODEL
    val androidVer = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    var log by remember { mutableStateOf("Perangkat: $deviceBrand $deviceModel (Android $androidVer)\nSiap. Pilih INJECT di bawah.\n") }
    var busy by remember { mutableStateOf(false) }
    var granted by remember(refreshKey) { mutableStateOf(SafAccess.hasAccess(context)) }
    var safTick by remember { mutableIntStateOf(0) }
    var showGrantDialog by remember { mutableStateOf(false) }
    var shizukuOk by remember(refreshKey, safTick) { mutableStateOf(ShizukuHelper.hasPermission()) }
    var shellHits by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var selectedPkg by remember { mutableStateOf(PatchCatalog.items[0].packageName) }
    val scroll = rememberScrollState()

    fun appendLog(s: String) {
        log = (log + "\n" + s).takeLast(6000)
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val ok = grantResult == PackageManager.PERMISSION_GRANTED
            shizukuOk = ok
            if (ok) {
                appendLog("Terhubung.")
                safTick++
            } else {
                appendLog("Izin ditolak. Coba lagi.")
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    // Cek status folder game di background (tidak blokir UI)
    LaunchedEffect(shizukuOk, safTick) {
        if (shizukuOk) {
            shellHits = withContext(Dispatchers.IO) {
                PatchCatalog.items.associate { it.packageName to ShizukuHelper.isInjected(context, it) }
            }
        } else {
            shellHits = null
        }
    }

    // Minta izin otomatis sekali saat dibuka agar app muncul di daftar Shizuku
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ShizukuHelper.ensureRequested(context)
        }
    }

    // Saat aplikasi baru dibuka / diinstall, langsung arahkan ke menu perizinan jika belum diizinkan
    LaunchedEffect(Unit) {
        if (!hasAllFiles) {
            onRequestPermission()
        }
    }

    // Picker folder Android/data agar INJECT & BYPASS jalan dari HP tanpa PC
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            SafAccess.saveTreeUri(context, uri)
            granted = true
            safTick++
            appendLog("Izin folder tersimpan. Menjalankan INJECT untuk 2 game...")
            busy = true
            appendLog(SafAccess.injectAll(context))
            safTick++
            busy = false
        } else {
            appendLog("Pemilihan folder dibatalkan. Tetap bisa via PC / manual.")
        }
    }

    fun doInject(item: PatchItem) {
        if (!userSession.isLoggedIn) {
            appendLog("Akses ditolak: Harap login lisensi terlebih dahulu.")
            onLogout()
            return
        }
        selectedPkg = item.packageName
        busy = true
        appendLog("=== INJECT: ${item.title.uppercase()} ===")
        appendLog("Target: ${item.title}")

        scope.launch(Dispatchers.IO) {
            // [1/3] Cek ketersediaan game di HP
            val isInstalled = GameLauncher.isPackageInstalled(context, item.packageName)
            withContext(Dispatchers.Main) {
                if (isInstalled) {
                    appendLog("[1/3] Status: Game terdeteksi terpasang.")
                } else {
                    appendLog("[1/3] Peringatan: ${item.title} belum terpasang di HP.")
                }
            }

            // [2/3] Proses Injeksi Patch
            var injectLog = ""

            if (shizukuOk) {
                withContext(Dispatchers.Main) {
                    appendLog("[2/3] Menyuntikkan patch via Shizuku Shell...")
                }
                val res = ShizukuHelper.injectItem(context, item)
                injectLog = res.log
            } else if (granted) {
                withContext(Dispatchers.Main) {
                    appendLog("[2/3] Menyuntikkan patch via Akses Folder (SAF)...")
                }
                val res = SafAccess.injectItem(context, item)
                injectLog = res.log
            } else {
                withContext(Dispatchers.Main) {
                    appendLog("[2/3] Akses Android/data belum aktif, menyalin ke Download...")
                }
                val res = PatchInstaller.install(context, item)
                injectLog = res.log
            }

            withContext(Dispatchers.Main) {
                appendLog(injectLog)
                safTick++
            }

            // [3/3] Verifikasi file riil (real kebaca)
            val verify = withContext(Dispatchers.IO) {
                if (shizukuOk) {
                    ShizukuHelper.verifyFilesRead(context, item)
                } else if (granted) {
                    SafAccess.verifyFilesRead(context, item)
                } else {
                    val ready = PatchInstaller.isTargetInstalled(item)
                    ready to if (ready) "Salinan siap di Download/CenaRegs" else "Belum tersalin"
                }
            }

            withContext(Dispatchers.Main) {
                if (verify.first) {
                    appendLog("[3/3] REAL TERBACA: ${item.title} aktif.")
                    appendLog("  ${verify.second}")
                    appendLog("Injeksi selesai. Patch berhasil disuntikkan.")
                } else {
                    appendLog("[3/3] Verifikasi: ${verify.second}")
                    if (!shizukuOk && !granted) {
                        if (ShizukuHelper.isRunning()) {
                            ShizukuHelper.requestPermission()
                            appendLog("Aktifkan izin Shizuku agar patch tersuntik langsung.")
                        } else {
                            showGrantDialog = true
                        }
                    }
                }
                busy = false
            }
        }
    }

    fun doInjectAndLogin(item: PatchItem) = doInject(item)

    fun doBypassItem(item: PatchItem) {
        selectedPkg = item.packageName
        busy = true
        appendLog("=== BYPASS: ${item.title.uppercase()} ===")
        appendLog("Target: ${item.title}")

        scope.launch(Dispatchers.IO) {
            // [1/2] Bersihkan patch dari folder game
            var deletedLog = ""
            if (shizukuOk) {
                withContext(Dispatchers.Main) {
                    appendLog("[1/2] Membersihkan patch dari folder game...")
                }
                deletedLog = ShizukuHelper.bypassItem(context, item)
            } else if (granted) {
                withContext(Dispatchers.Main) {
                    appendLog("[1/2] Membersihkan patch via Akses Folder (SAF)...")
                }
                deletedLog = SafAccess.bypassItem(context, item)
            } else {
                withContext(Dispatchers.Main) {
                    appendLog("[1/2] Membersihkan patch dari direktori lokal...")
                }
                deletedLog = PatchInstaller.uninstallTarget(item)
            }

            // Bersihkan salinan download
            PatchInstaller.uninstallTarget(item)

            // [2/2] Verifikasi bahwa folder benar-benar bersih
            val isClean = if (shizukuOk) {
                !ShizukuHelper.isInjected(context, item)
            } else if (granted) {
                !SafAccess.isInjected(context, item.packageName)
            } else {
                !PatchInstaller.isTargetInstalled(item)
            }

            withContext(Dispatchers.Main) {
                appendLog("  ✓ $deletedLog")
                if (isClean) {
                    appendLog("[2/2] REAL BYPASS SUKSES: ${item.title} bersih.")
                } else {
                    appendLog("[2/2] Peringatan: Masih ada data tersisa.")
                }
                safTick++
                busy = false
            }
        }
    }

    fun doBypassAll() {
        if (shizukuOk) {
            busy = true
            scope.launch(Dispatchers.IO) {
                val msg = ShizukuHelper.bypassAll(context)
                withContext(Dispatchers.Main) {
                    appendLog(msg)
                    safTick++
                    busy = false
                }
            }
        } else if (granted) {
            busy = true
            appendLog(SafAccess.bypassAll(context))
            safTick++
            busy = false
        } else {
            busy = true
            appendLog(PatchInstaller.bypassProtect())
            busy = false
            if (ShizukuHelper.isRunning()) {
                ShizukuHelper.requestPermission()
                appendLog("Menunggu izin, ketuk BYPASS lagi setelah izinkan.")
            } else {
                showGrantDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBackground)
            .verticalScroll(scroll)
            .padding(bottom = 24.dp)
    ) {
        // Header logo CENA X REGS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cena),
                contentDescription = "CENA X REGS",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                                BlackBackground
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_cena),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, RedPrimary, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CENA X REGS",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "INJECT • ORI & MAX",
                            color = RedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "INJECT SYSTEM",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // DETEKSI PERANGKAT PENGGUNA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, RedDark.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BlackSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SuccessGreen, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PERANGKAT TERDETEKSI",
                            color = TextGrey,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$deviceBrand $deviceModel",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ANDROID $androidVer",
                        color = RedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "API $apiLevel • $abi",
                        color = TextGrey,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // INFO LOGIN & STATUS AKUN
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(
                    1.dp,
                    if (userSession.isLoggedIn) SuccessGreen.copy(alpha = 0.6f) else RedDark.copy(alpha = 0.7f),
                    RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = BlackSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (userSession.isLoggedIn) SuccessGreen else RedPrimary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (userSession.isLoggedIn) "AKUN TERHUBUNG" else "STATUS LOGIN",
                            color = TextGrey,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (userSession.isLoggedIn) userSession.username.uppercase() else "BELUM MASUK",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (userSession.isLoggedIn) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = userSession.role,
                            color = RedPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (userSession.expiry.isNotEmpty()) {
                            Text(
                                text = userSession.expiry,
                                color = TextGrey,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Keluar",
                            color = RedPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onLogout()
                                }
                                .padding(top = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Belum Masuk",
                        color = RedPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onLogout() }
                    )
                }
            }
        }

        // PILIH TARGET (ketuk icon FF / MAX)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, RedDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BlackSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "PILIH TARGET",
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PatchCatalog.items.forEach { target ->
                        val sel = selectedPkg == target.packageName
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (selectedPkg != target.packageName) {
                                    selectedPkg = target.packageName
                                    appendLog("Target: ${target.title}")
                                }
                            }
                        ) {
                            Image(
                                painter = painterResource(id = target.iconRes),
                                contentDescription = target.title,
                                modifier = Modifier
                                    .size(76.dp)
                                    .alpha(if (sel) 1f else 0.5f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        if (sel) 3.dp else 1.dp,
                                        if (sel) RedPrimary else RedDark,
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = target.shortName,
                                color = if (sel) TextWhite else TextGrey,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }


        // STATUS SHIZUKU
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, if (shizukuOk) SuccessGreen else RedDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BlackSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHIZUKU",
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(
                            if (shizukuOk) SuccessGreen else RedPrimary,
                            RoundedCornerShape(5.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (shizukuOk) "RUNNING" else "NOT RUNNING",
                    color = if (shizukuOk) SuccessGreen else RedPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // BYPASS PROTECT (Hanya untuk game yang sedang dipilih: FF MAX atau FF Biasa)
        val activeTarget = PatchCatalog.items.firstOrNull { it.packageName == selectedPkg } ?: PatchCatalog.items[0]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(1.dp, RedPrimary, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BlackSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BYPASS PROTECT • ${activeTarget.shortName}",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "TARGET: ${activeTarget.shortName}",
                        color = RedPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reset & bersihkan patch khusus ${activeTarget.title}",
                    color = TextGrey,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { doBypassItem(activeTarget) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("JALANKAN BYPASS ${activeTarget.shortName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Daftar menu INJECT (HANYA menampilkan game yang dipilih: jika MAX maka hanya MAX, jika FF maka hanya FF)
        val displayedItems = PatchCatalog.items.filter { it.packageName == selectedPkg }
        displayedItems.forEach { item ->
            refreshKey.let { }
            // AKTIF = terverifikasi di folder game (SHIZUKU / izin folder)
            // SIAP = file siap di Download • BELUM = klik INJECT
            val safInjected = remember(safTick, log, granted) {
                if (granted && !shizukuOk) SafAccess.isInjected(context, item.packageName) else false
            }
            val dlReady = remember(refreshKey, log) { PatchInstaller.isTargetInstalled(item) }
            val hits = shellHits
            val statusText: String
            val statusColor: Color
            when {
                shizukuOk && hits?.get(item.packageName) == true -> {
                    statusText = "● AKTIF"; statusColor = SuccessGreen
                }
                shizukuOk && hits != null -> {
                    statusText = "○ BELUM"; statusColor = RedPrimary
                }
                shizukuOk -> {
                    statusText = "…"; statusColor = TextGrey
                }
                granted && safInjected -> {
                    statusText = "● AKTIF"; statusColor = SuccessGreen
                }
                granted -> {
                    statusText = "○ BELUM"; statusColor = RedPrimary
                }
                dlReady -> {
                    statusText = "● SIAP"; statusColor = SuccessGreen
                }
                else -> {
                    statusText = "○ BELUM"; statusColor = RedPrimary
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .border(1.dp, statusColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = BlackSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        2.dp,
                                        if (selectedPkg == item.packageName) RedPrimary else BlackSurfaceVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedPkg = item.packageName },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.uppercase(),
                                    color = TextWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                if (selectedPkg == item.packageName) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "▼ TARGET AKTIF",
                                        color = RedPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = RedDark.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "PATCH SYSTEM • READY",
                        color = TextGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { doInject(item) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("INJECT", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            val launched = GameLauncher.launchGame(context, item.packageName)
                            if (!launched) appendLog("Game ${item.title} tidak dapat dibuka.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buka Game", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Log console
        val logScroll = rememberScrollState()
        LaunchedEffect(log) {
            logScroll.animateScrollTo(logScroll.maxValue)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG:",
                color = RedPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            TextButton(
                onClick = { log = "Perangkat: $deviceBrand $deviceModel (Android $androidVer)\nSiap. Pilih INJECT di atas.\n" }
            ) {
                Text("Bersihkan", color = TextGrey, fontSize = 11.sp)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 12.dp)
                .background(BlackSurface, RoundedCornerShape(8.dp))
                .border(1.dp, RedDark.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(10.dp)
                .verticalScroll(logScroll)
        ) {
            Text(
                text = log,
                color = LogTextColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    if (showGrantDialog) {
        AlertDialog(
            onDismissRequest = { showGrantDialog = false },
            containerColor = BlackSurfaceVariant,
            title = { Text("Aktifkan INJECT HP", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Agar INJECT & BYPASS jalan langsung dari HP tanpa PC:\n\n1. Ketuk PILIH FOLDER\n2. Ketuk folder Android → data\n3. Ketuk GUNAKAN FOLDER INI\n4. Ketuk IZINKAN\n\nSekali saja, izin tersimpan.",
                    color = TextGrey,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGrantDialog = false
                        try {
                            folderLauncher.launch(SafAccess.initialDataUri())
                        } catch (e: Exception) {
                            try {
                                folderLauncher.launch(null)
                            } catch (e2: Exception) {
                                appendLog("Picker tidak bisa dibuka di HP ini. Gunakan PC / manual.")
                            }
                        }
                    }
                ) {
                    Text("PILIH FOLDER", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGrantDialog = false }) {
                    Text("Nanti", color = TextGrey)
                }
            }
        )
    }

}

@Composable
fun LicenseLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var inputKey by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val whatsappChannelUrl = "https://www.whatsapp.com/channel/0029Vb800WiJkK74Ssu8Fx0i"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF160508),
                        Color(0xFF0C0D13),
                        Color(0xFF070709)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dev Watermark Pill
            Surface(
                color = Color(0xFF141622),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3348)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF10B981), shape = RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "</> REGS X CENA",
                        color = Color(0xFFE2E8F0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Glowing Logo Container
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                RedPrimary,
                                Color(0xFF4A000E),
                                Color(0xFF1A1C24)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFFF3366),
                                RedPrimary,
                                Color(0xFF262A38)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_cena),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(21.dp))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Subtitle
            Text(
                text = "CENA X REGS",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "EXTERNAL VIP INJECTOR",
                    color = RedPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Input Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF14151E),
                                Color(0xFF0F1017)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF25293A), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AKTIVASI LISENSI",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Surface(
                        color = RedPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SECURE",
                            color = RedPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Banner
                if (errorMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A0F12))
                            .border(1.dp, Color(0xFFE50914).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Input Key Field with PASTE Button
                androidx.compose.material3.OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it.uppercase()
                        errorMessage = ""
                    },
                    label = { Text("Kode Lisensi / License Key") },
                    placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    trailingIcon = {
                        Surface(
                            color = Color(0xFF212433),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF383D54)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        inputKey = clip.trim().uppercase()
                                        errorMessage = ""
                                    }
                                }
                        ) {
                            Text(
                                text = "TEMPEL",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = Color(0xFF2B3042),
                        focusedLabelColor = RedPrimary,
                        unfocusedLabelColor = TextGrey,
                        cursorColor = RedPrimary,
                        focusedContainerColor = Color(0xFF0B0C10),
                        unfocusedContainerColor = Color(0xFF0B0C10)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Format: XXXX-XXXX-XXXX-XXXX",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Button(
                    onClick = {
                        val trimmedKey = inputKey.trim()
                        if (trimmedKey.isEmpty()) {
                            errorMessage = "Silakan masukkan atau tempel kode lisensi Anda."
                            return@Button
                        }
                        busy = true
                        errorMessage = ""
                        scope.launch {
                            val (success, msg) = AuthManager.loginWithKey(context, trimmedKey)
                            busy = false
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        disabledContainerColor = RedPrimary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MEMVERIFIKASI LISENSI...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "AKTIFKAN & MASUK",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BUY KEY WHATSAPP CHANNEL CARD (Dedicated Feature)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0D1D16),
                                Color(0xFF09140F)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF25D366),
                                Color(0xFF128C7E),
                                Color(0xFF1A2F25)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappChannelUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF25D366),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "WHATSAPP CHANNEL",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUY KEY",
                                color = Color(0xFF25D366),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "BELUM PUNYA LISENSI? BUY DISINI",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Gabung ke Saluran WhatsApp </> REGS X CENA",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        color = Color(0xFF25D366),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "GABUNG >",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Web Portal Link
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SupabaseConfig.WEB_PORTAL_URL))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buka Web Portal Lisensi",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "↗",
                    color = RedPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Dev Credit
            Text(
                text = "DEVELOPER: </> REGS X CENA",
                color = Color(0xFF475569),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "EXTERNAL ANDROID VIP • v1.0",
                color = Color(0xFF334155),
                fontSize = 9.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}
