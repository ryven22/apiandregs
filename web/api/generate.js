// Vercel Serverless Function: POST /api/generate
// Menghasilkan random key lisensi dengan format: XXXX-XXXX-XXXX-XXXX (contoh: 0VAW-LPE4-XSHQ-QUHJ)

function generateRandomKey() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const seg = () => Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return `${seg()}-${seg()}-${seg()}-${seg()}`;
}

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

    const { 
        duration = 1, 
        count = 1, 
        note = '', 
        type = 'Paid' 
    } = req.body || {};

    const durationDays = parseInt(duration, 10) || 1;
    const numKeys = Math.min(Math.max(parseInt(count, 10) || 1, 1), 50);
    const keyType = (['Paid', 'Free', 'Owner'].includes(type)) ? type : 'Paid';

    const now = new Date();
    const expiryDate = new Date(now.getTime() + (durationDays >= 3650 ? 36500 : durationDays) * 24 * 60 * 60 * 1000);

    const newKeys = [];
    for (let i = 0; i < numKeys; i++) {
        const keyCode = generateRandomKey();
        const formattedNote = `[${keyType}] ${note ? note : (keyType === 'Owner' ? 'Owner Master Key' : `Paket ${durationDays >= 3650 ? 'Lifetime' : durationDays + ' Days'}`)}`.trim();
        
        newKeys.push({
            key_code: keyCode,
            duration_days: durationDays,
            type: keyType,
            note: formattedNote,
            is_used: false,
            created_at: now.toISOString(),
            expires_at: expiryDate.toISOString()
        });
    }

    // Supabase Configuration
    const supabaseUrl = process.env.SUPABASE_URL || 'https://maghrxnyavkittygojnn.supabase.co';
    const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || 
                        process.env.SUPABASE_ANON_KEY || 
                        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1hZ2hyeG55YXZraXR0eWdvam5uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxNzE4MTIsImV4cCI6MjEwNDc0NzgxMn0.Vr1usXkl6jHzKujpEi8SxWPA2qV8mNrW5g6imXj-tso';

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
            console.error('Error inserting random keys to supabase:', err);
        }
    }

    return res.status(200).json({
        success: true,
        message: `Berhasil membuat ${newKeys.length} key lisensi (${keyType}).`,
        duration_days: durationDays,
        type: keyType,
        keys: newKeys.map(k => k.key_code),
        data: newKeys
    });
}
