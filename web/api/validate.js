export default async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    const key = (req.method === 'POST' ? req.body?.key : req.query?.key) || '';
    const cleanKey = String(key).trim().toUpperCase();

    if (!cleanKey) {
        return res.status(400).json({
            success: false,
            message: 'Parameter "key" wajib disertakan. Contoh: POST { "key": "REGSXD-1D-XXXX" }'
        });
    }

    // Ambil kredensial Supabase dari Environment Variables Vercel
    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_ANON_KEY;

    // Jika Supabase belum diset di Vercel Env, gunakan mode verifikasi cerdas (demo)
    if (!supabaseUrl || !supabaseKey) {
        let duration = 30;
        let role = 'VIP 30 DAY';
        let expiry = '30 Hari';

        if (cleanKey.includes('1D') || cleanKey.includes('-1-')) {
            duration = 1; role = 'VIP 1 DAY'; expiry = '1 Hari';
        } else if (cleanKey.includes('3D') || cleanKey.includes('-3-')) {
            duration = 3; role = 'VIP 3 DAY'; expiry = '3 Hari';
        } else if (cleanKey.includes('7D') || cleanKey.includes('-7-')) {
            duration = 7; role = 'VIP 7 DAY'; expiry = '7 Hari';
        } else if (cleanKey.includes('15D') || cleanKey.includes('-15-')) {
            duration = 15; role = 'VIP 15 DAY'; expiry = '15 Hari';
        } else if (cleanKey.includes('30D') || cleanKey.includes('-30-')) {
            duration = 30; role = 'VIP 30 DAY'; expiry = '30 Hari';
        } else if (cleanKey.includes('LIFE')) {
            duration = 36500; role = 'VIP LIFETIME'; expiry = 'LIFETIME (Permanen)';
        }

        if (cleanKey.startsWith('REGSXD')) {
            return res.status(200).json({
                success: true,
                message: `Key Valid! Paket ${expiry} aktif.`,
                key: cleanKey,
                duration_days: duration,
                role: role,
                expiry: expiry,
                mode: 'demo_fallback'
            });
        }

        return res.status(404).json({
            success: false,
            message: 'Format key tidak dikenali. Contoh: REGSXD-1D-XXXX atau REGSXD-LIFE-XXXX'
        });
    }

    try {
        // Query key ke Supabase REST
        const queryUrl = `${supabaseUrl}/rest/v1/license_keys?key_code=eq.${encodeURIComponent(cleanKey)}&select=*`;
        const fetchRes = await fetch(queryUrl, {
            headers: {
                'apikey': supabaseKey,
                'Authorization': `Bearer ${supabaseKey}`
            }
        });

        if (!fetchRes.ok) {
            return res.status(502).json({
                success: false,
                message: `Gagal menghubungi database Supabase (Status ${fetchRes.status})`
            });
        }

        const rows = await fetchRes.json();
        if (!rows || rows.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'Kode lisensi tidak ditemukan di database.'
            });
        }

        const keyData = rows[0];
        if (keyData.is_used) {
            return res.status(409).json({
                success: false,
                message: 'Key lisensi ini sudah pernah digunakan sebelumnya.'
            });
        }

        const duration = keyData.duration_days || 30;
        let expiry = `${duration} Hari`;
        let role = `VIP ${duration} DAY`;

        if (duration >= 3650) {
            expiry = 'LIFETIME (Permanen)';
            role = 'VIP LIFETIME';
        }

        // Tandai key sebagai terpakai (is_used = true)
        await fetch(`${supabaseUrl}/rest/v1/license_keys?id=eq.${keyData.id}`, {
            method: 'PATCH',
            headers: {
                'apikey': supabaseKey,
                'Authorization': `Bearer ${supabaseKey}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                is_used: true,
                used_at: new Date().toISOString()
            })
        });

        return res.status(200).json({
            success: true,
            message: `Key Berhasil Diaktifkan (${expiry})!`,
            key: cleanKey,
            duration_days: duration,
            role: role,
            expiry: expiry
        });

    } catch (err) {
        return res.status(500).json({
            success: false,
            message: `Terjadi kesalahan internal server: ${err.message}`
        });
    }
}
