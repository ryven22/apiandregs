// Vercel Serverless Function: POST/GET /api/validate
// STRICT VALIDATION: Hanya key yang benar-benar TERDAFTAR di Supabase Cloud yang diizinkan!

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
            message: 'Parameter "key" wajib disertakan.'
        });
    }

    const supabaseUrl = process.env.SUPABASE_URL || 'https://maghrxnyavkittygojnn.supabase.co';
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || 
                        process.env.SUPABASE_ANON_KEY || 
                        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1hZ2hyeG55YXZraXR0eWdvam5uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxNzE4MTIsImV4cCI6MjEwNDc0NzgxMn0.Vr1usXkl6jHzKujpEi8SxWPA2qV8mNrW5g6imXj-tso';

    try {
        // Query key strictly ke Supabase REST
        const queryUrl = `${supabaseUrl}/rest/v1/license_keys?key_code=eq.${encodeURIComponent(cleanKey)}&select=*`;
        const fetchRes = await fetch(queryUrl, {
            headers: {
                'apikey': supabaseKey,
                'Authorization': `Bearer ${supabaseKey}`
            }
        });

        if (fetchRes.ok) {
            const rows = await fetchRes.json();
            if (rows && rows.length > 0) {
                const keyData = rows[0];

                // Cek jika key di-revoke
                if (keyData.note && keyData.note.includes('[REVOKED]')) {
                    return res.status(403).json({
                        success: false,
                        message: 'Kode lisensi ini telah dinonaktifkan (Revoked) oleh Owner.'
                    });
                }

                const duration = (keyData.duration_days !== null && keyData.duration_days !== undefined) ? parseInt(keyData.duration_days, 10) : 1;
                let expiryText = `${duration} Hari`;
                let role = `VIP ${duration} DAY`;

                if (duration >= 3650) {
                    expiryText = 'LIFETIME (Permanen)';
                    role = 'VIP LIFETIME';
                } else if (duration === 1) {
                    expiryText = '1 Hari';
                    role = 'VIP 1 DAY';
                }

                // Cek masa aktif jika key sudah pernah diaktifkan sebelumnya
                if (keyData.is_used && keyData.used_at) {
                    const activatedAt = new Date(keyData.used_at).getTime();
                    const expireTimestamp = activatedAt + (duration >= 3650 ? 36500 : duration) * 86400000;
                    if (Date.now() > expireTimestamp) {
                        return res.status(410).json({
                            success: false,
                            message: `Kode lisensi telah kadaluarsa (Expired). Masa aktif ${expiryText} telah habis.`
                        });
                    }
                }

                // Jika pertama kali dipakai, tandai is_used = true dan used_at
                if (!keyData.is_used) {
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
                    }).catch(() => {});
                }

                return res.status(200).json({
                    success: true,
                    message: `Lisensi Resmi Aktif (${expiryText})!`,
                    key: cleanKey,
                    duration_days: duration,
                    role: role,
                    expiry: expiryText,
                    note: keyData.note || '',
                    verified_by: 'supabase_cloud'
                });
            }
        }
    } catch (err) {
        console.error('Error querying Supabase:', err);
    }

    // STRICT: Jika key TIDAK ADA di database Supabase, WAJIB DITOLAK!
    return res.status(404).json({
        success: false,
        message: 'Kode lisensi tidak terdaftar di database. Silakan buat atau beli key resmi di Web Portal.'
    });
}
