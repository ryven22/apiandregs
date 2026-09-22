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
            message: 'Metode tidak diizinkan. Harap gunakan POST.'
        });
    }

    const { duration = 30, count = 1, note = '' } = req.body || {};
    const durationDays = parseInt(duration, 10);
    const numKeys = Math.min(Math.max(parseInt(count, 10) || 1, 1), 50);

    // Prefix tag durasi
    let tag = '30D';
    if (durationDays === 1) tag = '1D';
    else if (durationDays === 3) tag = '3D';
    else if (durationDays === 7) tag = '7D';
    else if (durationDays === 15) tag = '15D';
    else if (durationDays === 30) tag = '30D';
    else if (durationDays >= 3650) tag = 'LIFE';

    const newKeys = [];
    for (let i = 0; i < numKeys; i++) {
        const randStr = Math.random().toString(36).substring(2, 6).toUpperCase() +
                        Math.floor(100 + Math.random() * 900);
        newKeys.push({
            key_code: `REGSXD-${tag}-${randStr}`,
            duration_days: durationDays,
            note: note || `Paket ${durationDays >= 3650 ? 'LIFETIME' : durationDays + ' Hari'}`,
            is_used: false,
            created_at: new Date().toISOString()
        });
    }

    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_ANON_KEY;

    if (supabaseUrl && supabaseKey) {
        try {
            await fetch(`${supabaseUrl}/rest/v1/license_keys`, {
                method: 'POST',
                headers: {
                    'apikey': supabaseKey,
                    'Authorization': `Bearer ${supabaseKey}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(newKeys.map(k => ({
                    key_code: k.key_code,
                    duration_days: k.duration_days,
                    note: k.note,
                    is_used: false
                })))
            });
        } catch (err) {
            console.error('Error inserting keys to supabase:', err);
        }
    }

    return res.status(200).json({
        success: true,
        message: `Berhasil membuat ${newKeys.length} key lisensi.`,
        duration_days: durationDays,
        keys: newKeys.map(k => k.key_code),
        data: newKeys
    });
}
