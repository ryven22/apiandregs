package com.example.belajaarnewproject.auth

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseConfig {
    // Kredensial Supabase Resmi Project Regsxit12:
    var SUPABASE_URL = "https://maghrxnyavkittygojnn.supabase.co"
    var SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1hZ2hyeG55YXZraXR0eWdvam5uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxNzE4MTIsImV4cCI6MjEwNDc0NzgxMn0.Vr1usXkl6jHzKujpEi8SxWPA2qV8mNrW5g6imXj-tso"
    
    // Link Web Login Vercel Resmi:
    var WEB_PORTAL_URL = "https://apiandregs.vercel.app"

    fun isConfigured(): Boolean {
        return !SUPABASE_URL.contains("your-project") && !SUPABASE_ANON_KEY.contains("your-anon")
    }
}

data class UserSession(
    val isLoggedIn: Boolean,
    val username: String = "",
    val email: String = "",
    val role: String = "USER",
    val expiry: String = "",
    val token: String = ""
)

object AuthManager {

    private const val PREFS = "auth_session"
    private const val KEY_LOGGED = "is_logged_in"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"
    private const val KEY_EXPIRY = "expiry"
    private const val KEY_TOKEN = "token"

    fun getSession(context: Context): UserSession {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val logged = sp.getBoolean(KEY_LOGGED, false)
        return UserSession(
            isLoggedIn = logged,
            username = sp.getString(KEY_USERNAME, "") ?: "",
            email = sp.getString(KEY_EMAIL, "") ?: "",
            role = sp.getString(KEY_ROLE, "USER") ?: "USER",
            expiry = sp.getString(KEY_EXPIRY, "30 Hari") ?: "30 Hari",
            token = sp.getString(KEY_TOKEN, "") ?: ""
        )
    }

    fun saveSession(context: Context, session: UserSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOGGED, session.isLoggedIn)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ROLE, session.role)
            .putString(KEY_EXPIRY, session.expiry)
            .putString(KEY_TOKEN, session.token)
            .apply()
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Login menggunakan Email / Username dan Password ke Supabase Auth
     */
    suspend fun loginWithEmail(context: Context, email: String, pass: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            // Mode Demo Lokal jika URL Supabase belum diisi
            if (pass.length >= 4) {
                val demoName = email.substringBefore("@")
                val session = UserSession(
                    isLoggedIn = true,
                    username = demoName,
                    email = email,
                    role = "VIP ACTIVE",
                    expiry = "30 Hari",
                    token = "demo_token_regsxd"
                )
                saveSession(context, session)
                return@withContext true to "Login Berhasil (Akun: $demoName)"
            } else {
                return@withContext false to "Password minimal 4 karakter."
            }
        }

        try {
            val endpoint = "${SupabaseConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val jsonReq = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(jsonReq.toString()) }

            val code = conn.responseCode
            if (code in 200..299) {
                val res = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val obj = JSONObject(res)
                val token = obj.optString("access_token")
                val userObj = obj.optJSONObject("user")
                val meta = userObj?.optJSONObject("user_metadata")
                val uname = meta?.optString("username", "") ?: ""
                val finalName = if (uname.isNotEmpty()) uname else email.substringBefore("@")

                val session = UserSession(
                    isLoggedIn = true,
                    username = finalName,
                    email = email,
                    role = "VIP ACTIVE",
                    expiry = "Aktif",
                    token = token
                )
                saveSession(context, session)
                true to "Login Sukses. Selamat datang, $finalName!"
            } else {
                val errStream = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                val errMsg = try {
                    JSONObject(errStream).optString("error_description", "Email atau kata sandi salah.")
                } catch (e: Exception) {
                    "Login gagal (kode $code)."
                }
                false to errMsg
            }
        } catch (e: Exception) {
            false to "Gagal terhubung: ${e.localizedMessage ?: "Periksa koneksi internet."}"
        }
    }

    /**
     * Login menggunakan License Key / Token Akses
     */
    suspend fun loginWithKey(context: Context, key: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = key.trim().uppercase()
        if (cleanKey.isEmpty()) return@withContext false to "Masukkan kode lisensi / key."

        val isFormattedKey = cleanKey.matches(Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")) || cleanKey.startsWith("REGSXD")
        if (!isFormattedKey) {
            return@withContext false to "Format key tidak valid. Contoh: XXXX-XXXX-XXXX-XXXX"
        }

        var lastErrorMessage = ""

        // 1. Verifikasi via Vercel Web API Resmi
        try {
            val apiEndpoint = "${SupabaseConfig.WEB_PORTAL_URL.trimEnd('/')}/api/validate"
            val url = URL(apiEndpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val reqObj = JSONObject().apply { put("key", cleanKey) }
            OutputStreamWriter(conn.outputStream).use { it.write(reqObj.toString()) }

            val code = conn.responseCode
            if (code in 200..299) {
                val res = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val resObj = JSONObject(res)
                if (resObj.optBoolean("success", false)) {
                    val roleText = resObj.optString("role", "VIP MEMBER")
                    val expiryText = resObj.optString("expiry", "Aktif")
                    val session = UserSession(
                        isLoggedIn = true,
                        username = cleanKey.take(14),
                        email = "license@regsxd.com",
                        role = roleText,
                        expiry = expiryText,
                        token = cleanKey
                    )
                    saveSession(context, session)
                    return@withContext true to resObj.optString("message", "Lisensi Berhasil Diaktifkan ($expiryText)!")
                }
            } else {
                val errStream = conn.errorStream
                if (errStream != null) {
                    val errRes = errStream.bufferedReader().use(BufferedReader::readText)
                    val errObj = JSONObject(errRes)
                    val msg = errObj.optString("message", "")
                    if (msg.isNotEmpty()) {
                        lastErrorMessage = msg
                    }
                }
            }
        } catch (_: Exception) {
            // Lanjut ke query Supabase langsung jika API Vercel tidak merespons
        }

        // 2. Verifikasi cadangan langsung ke Database Supabase Cloud
        try {
            val endpoint = "${SupabaseConfig.SUPABASE_URL}/rest/v1/license_keys?key_code=eq.$cleanKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            val code = conn.responseCode
            if (code in 200..299) {
                val res = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val arr = JSONArray(res)
                if (arr.length() > 0) {
                    val keyObj = arr.getJSONObject(0)

                    // Cek jika key dinonaktifkan (Revoked) atau dihapus
                    val note = keyObj.optString("note", "")
                    if (note.contains("[REVOKED]") || note.contains("[DELETED]")) {
                        return@withContext false to "Kode lisensi ini telah dinonaktifkan atau dihapus oleh Owner."
                    }

                    val duration = keyObj.optInt("duration_days", 1)
                    val isUsed = keyObj.optBoolean("is_used", false)
                    val usedAt = keyObj.optString("used_at", "")

                    val (expiryText, roleText) = when {
                        duration >= 3650 -> "LIFETIME (Permanen)" to "VIP LIFETIME"
                        duration == 1 -> "1 Hari" to "VIP 1 DAY"
                        duration == 3 -> "3 Hari" to "VIP 3 DAY"
                        duration == 7 -> "7 Hari" to "VIP 7 DAY"
                        duration == 15 -> "15 Hari" to "VIP 15 DAY"
                        duration == 30 -> "30 Hari" to "VIP 30 DAY"
                        else -> "$duration Hari" to "VIP ($duration Hari)"
                    }

                    // Cek masa aktif jika sudah pernah diaktifkan
                    if (isUsed && usedAt.isNotEmpty()) {
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val cleanDateStr = usedAt.substringBefore(".")
                            val activatedTime = sdf.parse(cleanDateStr)?.time ?: 0L
                            val maxDurationMs = if (duration >= 3650) 36500L * 86400000L else duration.toLong() * 86400000L
                            if (activatedTime > 0 && System.currentTimeMillis() > activatedTime + maxDurationMs) {
                                return@withContext false to "Kode lisensi telah kadaluarsa (Expired). Masa aktif $expiryText telah habis."
                            }
                        } catch (_: Exception) {}
                    }

                    // Tandai key sebagai terpakai di Supabase jika baru pertama kali
                    if (!isUsed) {
                        try {
                            val patchConn = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/license_keys?key_code=eq.$cleanKey").openConnection() as HttpURLConnection
                            patchConn.requestMethod = "POST"
                            patchConn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                            patchConn.setRequestProperty("Content-Type", "application/json")
                            patchConn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                            patchConn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                            patchConn.doOutput = true
                            patchConn.outputStream.bufferedWriter().use {
                                val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.format(java.util.Date())
                                it.write(JSONObject().apply {
                                    put("is_used", true)
                                    put("used_at", nowIso)
                                }.toString())
                            }
                            patchConn.responseCode
                        } catch (_: Exception) {}
                    }

                    val session = UserSession(
                        isLoggedIn = true,
                        username = cleanKey.take(14),
                        email = "license@regsxd.com",
                        role = roleText,
                        expiry = expiryText,
                        token = cleanKey
                    )
                    saveSession(context, session)
                    return@withContext true to "Lisensi Valid ($expiryText)! Selamat datang."
                } else {
                    // Key TIDAK ADA di database Supabase
                    return@withContext false to "Kode lisensi tidak terdaftar di database. Silakan buat atau beli key resmi di Web Portal."
                }
            }
        } catch (_: Exception) {
            // Lanjut ke pesan error akhir
        }

        if (lastErrorMessage.isNotEmpty()) {
            return@withContext false to lastErrorMessage
        }

        return@withContext false to "Kode lisensi tidak terdaftar di database. Silakan buat atau beli key resmi di Web Portal."
    }
}
