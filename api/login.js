export default async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'POST') {
        return res.status(405).json({
            success: false,
            message: 'Metode tidak diizinkan. Gunakan POST.'
        });
    }

    const { username, email, password } = req.body || {};
    const inputUser = (username || email || '').trim();
    const inputPass = String(password || '');

    // Kredensial Khusus Admin / Owner RegsXD
    if ((inputUser.toLowerCase() === 'regsxd18' || inputUser.toLowerCase() === 'regsxd18@admin.com') && 
        inputPass === 'leaaaimut1') {
        return res.status(200).json({
            success: true,
            message: 'Selamat datang, Owner RegsXD! Akses Admin penuh terbuka.',
            user: {
                username: 'regsxd18',
                email: 'regsxd18@cena-regs.com',
                role: 'OWNER / MASTER ADMIN',
                is_admin: true,
                expires_at: 'LIFETIME (Permanen)'
            },
            token: 'admin_token_regsxd18_master_access'
        });
    }

    // Jika menggunakan Supabase Auth untuk user biasa
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_ANON_KEY;

    if (supabaseUrl && supabaseKey) {
        try {
            const authRes = await fetch(`${supabaseUrl}/auth/v1/token?grant_type=password`, {
                method: 'POST',
                headers: {
                    'apikey': supabaseKey,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: inputUser,
                    password: inputPass
                })
            });

            const data = await authRes.json();
            if (authRes.ok) {
                return res.status(200).json({
                    success: true,
                    message: 'Login Berhasil.',
                    user: {
                        id: data.user?.id,
                        email: data.user?.email,
                        username: data.user?.user_metadata?.username || inputUser.split('@')[0],
                        role: 'VIP MEMBER'
                    },
                    token: data.access_token
                });
            } else {
                return res.status(401).json({
                    success: false,
                    message: data.error_description || 'Username / email atau kata sandi salah.'
                });
            }
        } catch (err) {
            return res.status(500).json({
                success: false,
                message: `Kesalahan server: ${err.message}`
            });
        }
    }

    // Fallback demo gagal
    return res.status(401).json({
        success: false,
        message: 'Username / Email atau Kata Sandi salah.'
    });
}
