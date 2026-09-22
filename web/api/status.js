export default function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    return res.status(200).json({
        status: 'online',
        service: 'CENA X REGS • VERCEL WEB API',
        version: '1.0.0',
        supported_tiers: [
            { tier: '1D', name: '1 Day (1 Hari)', days: 1 },
            { tier: '3D', name: '3 Days (3 Hari)', days: 3 },
            { tier: '7D', name: '7 Days (7 Hari)', days: 7 },
            { tier: '15D', name: '15 Days (15 Hari)', days: 15 },
            { tier: '30D', name: '30 Days (30 Hari)', days: 30 },
            { tier: 'LIFETIME', name: 'Lifetime (Permanen)', days: 36500 }
        ],
        endpoints: {
            status: 'GET /api/status',
            login: 'POST /api/login { username, password }',
            validate: 'POST /api/validate { key: "REGSXD-1D-XXXX" }',
            generate: 'POST /api/generate { duration: 1, count: 5, note: "..." }'
        },
        timestamp: new Date().toISOString()
    });
}
