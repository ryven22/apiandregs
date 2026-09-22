-- ==============================================================================
-- CENA X REGS / EXTERNAL ANDROID BY REGSXD - SUPABASE DATABASE SCHEMA
-- ==============================================================================
-- Petunjuk:
-- 1. Buka dashboard Supabase (https://supabase.com/dashboard)
-- 2. Buka menu "SQL Editor" -> "New query"
-- 3. Tempel seluruh isi script ini dan klik "Run"
-- ==============================================================================

-- 1. Tabel Profil Pengguna (Terkoneksi dengan auth.users Supabase)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'vip', 'admin')),
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '30 days'),
    device_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Tabel Lisensi / Access Keys (Untuk model aktivasi key)
CREATE TABLE IF NOT EXISTS public.license_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_code TEXT UNIQUE NOT NULL,
    duration_days INT DEFAULT 30,
    is_used BOOLEAN DEFAULT false,
    used_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    used_at TIMESTAMPTZ,
    note TEXT
);

-- 3. Buat Trigger Otomatis saat Pengguna Baru Mendaftar di Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, role, is_active, expires_at)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
        'user',
        true,
        NOW() + INTERVAL '30 days'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 4. Aktifkan Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.license_keys ENABLE ROW LEVEL SECURITY;

-- 5. Kebijakan Keamanan (Policies)
-- Pengguna dapat membaca profil mereka sendiri
CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

-- Pengguna dapat mengupdate profil mereka sendiri
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- Publik / App dapat membaca dan memvalidasi key yang belum terpakai
CREATE POLICY "Anyone can view unused keys"
    ON public.license_keys FOR SELECT
    USING (true);

-- Admin atau fungsi dapat mengubah status key
CREATE POLICY "Anyone can update keys"
    ON public.license_keys FOR UPDATE
    USING (true);

-- Portal web dapat membuat / generate key baru
CREATE POLICY "Anyone can insert keys"
    ON public.license_keys FOR INSERT
    WITH CHECK (true);

-- 6. Tambahkan Contoh Beberapa License Key Awal untuk Uji Coba (Semua 6 Paket)
INSERT INTO public.license_keys (key_code, duration_days, note)
VALUES 
    ('REGSXD-1D-TEST1', 1, 'Key Uji Coba 1 Hari'),
    ('REGSXD-3D-TEST1', 3, 'Key Uji Coba 3 Hari'),
    ('REGSXD-7D-TEST1', 7, 'Key Uji Coba 7 Hari'),
    ('REGSXD-15D-TEST1', 15, 'Key Uji Coba 15 Hari'),
    ('REGSXD-30D-TEST1', 30, 'Key Uji Coba 30 Hari'),
    ('REGSXD-LIFE-TEST1', 36500, 'Key Uji Coba Lifetime')
ON CONFLICT (key_code) DO NOTHING;

