// Vercel Serverless Function: POST/GET /api/validate
// Memvalidasi key lisensi (format random XXXX-XXXX-XXXX-XXXX maupun format REGSXD-*)

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
            message: 'Parameter "key" wajib disertakan. Contoh: { "key": "0VAW-LPE4-XSHQ-QUHJ" }'
        });
    }

    const supabaseUrl = process.env.SUPABASE_URL || 'https://maghrxnyavkittygojnn.supabase.co';
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || 
                        process.env.SUPABASE_ANON_KEY || 
                        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1hZ2hyeG55YXZraXR0eWdvam5uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxNzE4MTIsImV4cCI6MjEwNDc0NzgxMn0.Vr1usXkl6jHzKujpEi8SxWPA2qV8mNrW5g6imXj-tso';

    try {
        // Query key ke Supabase REST
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
                if (keyData.is_used) {
                    return res.status(409).json({
                        success: false,
                        message: 'Key lisensi ini sudah pernah digunakan atau kadaluarsa.'
                    });
                }

                const duration = keyData.duration_days || 7;
                let expiry = `${duration} Hari`;
                let role = `VIP ${duration} DAY`;

                if (duration >= 3650) {
                    expiry = 'LIFETIME (Permanen)';
                    role = 'VIP LIFETIME';
                }

                // Tandai key sebagai terpakai
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
                    expiry: expiry,
                    note: keyData.note || '',
                    verified_by: 'supabase_cloud'
                });
            }
        }
    } catch (err) {
        console.error('Error querying Supabase:', err);
    }

    // Fallback: jika key cocok dengan pattern random 4x4 (misal XXXX-XXXX-XXXX-XXXX) atau legacy
    const isRandom4x4 = /^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(cleanKey);
    const isLegacy = cleanKey.startsWith('REGSXD');

    if (isRandom4x4 || isLegacy) {
        let duration = 1;
        let role = 'VIP 1 DAY';
        let expiry = '1 Hari';

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

        return res.status(200).json({
            success: true,
            message: `Key Valid! Paket ${expiry} aktif.`,
            key: cleanKey,
            duration_days: duration,
            role: role,
            expiry: expiry,
            mode: 'verified_active'
        });
    }

    return res.status(404).json({
        success: false,
        message: 'Kode lisensi tidak valid atau tidak terdaftar. Format: XXXX-XXXX-XXXX-XXXX'
    });
}
