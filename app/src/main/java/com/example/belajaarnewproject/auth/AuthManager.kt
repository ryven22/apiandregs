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
    
    // Link Web Login Vercel Anda:
    var WEB_PORTAL_URL = "https://external-android-regsxd-portal.vercel.app"

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

        if (!SupabaseConfig.isConfigured()) {
            // Coba validasi via Vercel Web API jika URL web portal aktif
            try {
                val apiEndpoint = "${SupabaseConfig.WEB_PORTAL_URL.trimEnd('/')}/api/validate"
                val url = URL(apiEndpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

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
                            username = cleanKey.take(12),
                            email = "key-auth@regsxd.com",
                            role = roleText,
                            expiry = expiryText,
                            token = cleanKey
                        )
                        saveSession(context, session)
                        return@withContext true to resObj.optString("message", "Key Berhasil Diaktifkan ($expiryText)!")
                    }
                }
            } catch (_: Exception) {
                // Lanjut ke fallback offline jika Vercel belum di-deploy / koneksi gagal
            }

            // Mode Demo Lokal / Offline Fallback
            val (durationDays, expiryText, roleText) = when {
                cleanKey.contains("1D") || cleanKey.contains("-1-") -> Triple(1, "1 Hari", "VIP 1 DAY")
                cleanKey.contains("3D") || cleanKey.contains("-3-") -> Triple(3, "3 Hari", "VIP 3 DAY")
                cleanKey.contains("7D") || cleanKey.contains("-7-") -> Triple(7, "7 Hari", "VIP 7 DAY")
                cleanKey.contains("15D") || cleanKey.contains("-15-") -> Triple(15, "15 Hari", "VIP 15 DAY")
                cleanKey.contains("30D") || cleanKey.contains("-30-") -> Triple(30, "30 Hari", "VIP 30 DAY")
                cleanKey.contains("LIFE") -> Triple(36500, "LIFETIME (Permanen)", "VIP LIFETIME")
                cleanKey.startsWith("REGSXD") -> Triple(30, "30 Hari", "VIP MEMBER")
                else -> Triple(0, "", "")
            }

            if (durationDays > 0) {
                val session = UserSession(
                    isLoggedIn = true,
                    username = cleanKey.take(12),
                    email = "key-auth@regsxd.com",
                    role = roleText,
                    expiry = expiryText,
                    token = cleanKey
                )
                saveSession(context, session)
                return@withContext true to "Key Berhasil Diaktifkan ($expiryText)!"
            } else {
                return@withContext false to "Key tidak valid. Contoh: REGSXD-1D-XXXX atau REGSXD-LIFE-XXXX"
            }
        }

        try {
            val endpoint = "${SupabaseConfig.SUPABASE_URL}/rest/v1/license_keys?key_code=eq.$cleanKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val code = conn.responseCode
            if (code in 200..299) {
                val res = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val arr = JSONArray(res)
                if (arr.length() > 0) {
                    val keyObj = arr.getJSONObject(0)
                    val isUsed = keyObj.optBoolean("is_used", false)
                    val duration = keyObj.optInt("duration_days", 30)

                    if (isUsed) {
                        false to "Key ini sudah pernah digunakan."
                    } else {
                        val (expiryText, roleText) = when {
                            duration >= 3650 -> "LIFETIME (Permanen)" to "VIP LIFETIME"
                            duration == 1 -> "1 Hari" to "VIP 1 DAY"
                            duration == 3 -> "3 Hari" to "VIP 3 DAY"
                            duration == 7 -> "7 Hari" to "VIP 7 DAY"
                            duration == 15 -> "15 Hari" to "VIP 15 DAY"
                            duration == 30 -> "30 Hari" to "VIP 30 DAY"
                            else -> "$duration Hari" to "VIP ($duration Hari)"
                        }

                        // Tandai key sebagai sudah dipakai di Supabase
                        try {
                            val updateUrl = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/license_keys?key_code=eq.$cleanKey")
                            val patchConn = updateUrl.openConnection() as HttpURLConnection
                            patchConn.requestMethod = "POST"
                            patchConn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                            patchConn.setRequestProperty("Content-Type", "application/json")
                            patchConn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                            patchConn.setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                            patchConn.doOutput = true
                            val patchBody = JSONObject().apply {
                                put("is_used", true)
                            }
                            patchConn.outputStream.bufferedWriter().use { it.write(patchBody.toString()) }
                            patchConn.responseCode
                        } catch (_: Exception) {}

                        val session = UserSession(
                            isLoggedIn = true,
                            username = cleanKey.take(12),
                            email = "key-$cleanKey",
                            role = roleText,
                            expiry = expiryText,
                            token = cleanKey
                        )
                        saveSession(context, session)
                        true to "Key Berhasil Diaktifkan ($expiryText)!"
                    }
                } else {
                    false to "Kode lisensi tidak ditemukan di database."
                }
            } else {
                false to "Gagal memverifikasi key (kode $code)."
            }
        } catch (e: Exception) {
            false to "Gagal terhubung ke database: ${e.localizedMessage}"
        }
    }
}
