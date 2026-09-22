# Panduan Deploy Web Portal ke Vercel & Setup Supabase

Portal ini digunakan untuk manajemen autentikasi akun & lisensi pengguna **CENA X REGS (External Android by RegsXD)**.

---

## 1. Setup Database Supabase (Gratis)

1. Buka [https://supabase.com](https://supabase.com) dan buat akun/login.
2. Buat project baru (misal diberi nama `regsxd-auth`).
3. Setelah project siap, buka menu **SQL Editor** di panel kiri.
4. Klik **New query**, lalu salin seluruh isi file [`schema.sql`](./schema.sql) dan klik **Run**.
5. Buka menu **Project Settings** (ikon gerigi di kiri bawah) -> **API**.
6. Salin dua nilai berikut:
   - **Project URL** (contoh: `https://xyzabc.supabase.co`)
   - **Project API Anon Key** (contoh: `eyJhbGciOi...`)
7. Buka file [`supabase_config.js`](./supabase_config.js) dan tempelkan URL serta Anon Key Anda di sana.

---

## 2. Deploy ke Vercel

### Cara 1: Lewat Vercel CLI (Paling Cepat dari Terminal)
1. Buka PowerShell di folder `web`:
   ```powershell
   cd web
   npx vercel
   ```
2. Ikuti petunjuk singkat di layar (cukup tekan Enter untuk setuju pada pengaturan default).
3. Dalam 1 menit, Vercel akan memberikan link website aktif Anda (misal: `https://external-android-regsxd-portal.vercel.app`).

### Cara 2: Lewat Dashboard Vercel (GitHub)
1. Push project Anda ke GitHub.
2. Buka [https://vercel.com](https://vercel.com), klik **Add New Project**.
3. Pilih repository GitHub Anda, pilih Root Directory ke folder `web`.
4. Klik **Deploy**!

---

## 3. Akun Khusus Master Owner (Admin):
Web portal dan API memiliki akun khusus Master Owner untuk membuat key lisensi:
- **Username:** `regsxd18`
- **Password:** `leaaaimut1`
- **Hak Akses:** Membuka tab **GENERATE KEY** (1D, 3D, 7D, 15D, 30D, LIFETIME), melihat daftar key, dan salin kode lisensi massal.

---

## 4. Vercel Serverless Web API Endpoints:
Web portal ini dilengkapi dengan backend Serverless Web API siap pakai di Vercel:

1. **`POST /api/login`**
   - Autentikasi user & login Owner Admin.
   - Body JSON:
     ```json
     { "username": "regsxd18", "password": "leaaaimut1" }
     ```

2. **`GET /api/status`**
   - Cek status server, versi, dan daftar tier lisensi yang didukung.
   - Contoh respon: `{ "status": "online", "version": "1.0.0", "supported_tiers": [...] }`

3. **`POST /api/validate`**
   - Validasi dan aktifkan kode lisensi dari aplikasi Android / bot / external client.
   - Body JSON:
     ```json
     { "key": "REGSXD-1D-XXXX" }
     ```
   - Contoh respon sukses:
     ```json
     { "success": true, "message": "Key Berhasil Diaktifkan (1 Hari)!", "duration_days": 1, "role": "VIP 1 DAY", "expiry": "1 Hari" }
     ```

4. **`POST /api/generate`**
   - Buat kode lisensi baru secara otomatis via API.
   - Body JSON:
     ```json
     { "duration": 7, "count": 5, "note": "Pembeli @user" }
     ```

*(Opsional) Masukkan `SUPABASE_URL` dan `SUPABASE_ANON_KEY` pada menu **Settings -> Environment Variables** di dashboard Vercel agar API terhubung langsung ke database cloud.*


