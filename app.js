/**
 * REGS XD • Dashboard Controller & Real-Time Key Engine
 * Formats: Random 4x4 Blocks (XXXX-XXXX-XXXX-XXXX)
 * Integration: Supabase Database Cloud & Vercel API
 */

// Initial Seed Data matching the User's Screenshot exactly (10 keys)
const INITIAL_KEYS = [
    {
        id: 'seed-1',
        key_code: '0VAW-LPE4-XSHQ-QUHJ',
        type: 'Paid',
        duration_days: 1,
        duration_text: '1 Day',
        status: 'Active',
        created_at: '21 Sept 2026',
        expires_at: '22 Sept 2026',
        device: '00754884-38F7-42...',
        note: 'VIP user'
    },
    {
        id: 'seed-2',
        key_code: '8P53-C8VC-SZNV-1TF7',
        type: 'Paid',
        duration_days: 3,
        duration_text: '3 Days',
        status: 'Active',
        created_at: '20 Sept 2026',
        expires_at: '23 Sept 2026',
        device: '996AC412-5C42-49...',
        note: 'Telegram VIP'
    },
    {
        id: 'seed-3',
        key_code: 'DMMJ-5024-HGSE-QGUE',
        type: 'Paid',
        duration_days: 7,
        duration_text: '7 Days',
        status: 'Active',
        created_at: '20 Sept 2026',
        expires_at: '27 Sept 2026',
        device: 'D9C1322C-C41A-40...',
        note: 'Fast Turnament'
    },
    {
        id: 'seed-4',
        key_code: '1GTM-AZ48-BJW7-5ZH3',
        type: 'Paid',
        duration_days: 1,
        duration_text: '1 Day',
        status: 'Expired',
        created_at: '20 Sept 2026',
        expires_at: '21 Sept 2026',
        device: '0359D338-8CB7-4E...',
        note: 'Trial user'
    },
    {
        id: 'seed-5',
        key_code: '6HRM-FCZT-R3LH-YLTS',
        type: 'Owner',
        duration_days: 7,
        duration_text: '7 Days',
        status: 'Active',
        created_at: '20 Sept 2026',
        expires_at: '27 Sept 2026',
        device: '4E353C67-8377-4F...',
        note: 'Admin Regs'
    },
    {
        id: 'seed-6',
        key_code: 'G9GJ-2UBU-9KDT-VFCQ',
        type: 'Paid',
        duration_days: 30,
        duration_text: '30 Days',
        status: 'Active',
        created_at: '19 Sept 2026',
        expires_at: '19 Oct 2026',
        device: 'CC82ADFE-549B-46...',
        note: 'Monthly VIP'
    },
    {
        id: 'seed-7',
        key_code: '4TD0-ETUL-DRY9-5FU0',
        type: 'Paid',
        duration_days: 7,
        duration_text: '7 Days',
        status: 'Active',
        created_at: '18 Sept 2026',
        expires_at: '25 Sept 2026',
        device: 'DC29F3CF-13FC-45...',
        note: 'Streamer Regs'
    },
    {
        id: 'seed-8',
        key_code: 'ZS10-LH06-WEXX-CVKN',
        type: 'Owner',
        duration_days: 36500,
        duration_text: 'Lifetime',
        status: 'Active',
        created_at: '18 Sept 2026',
        expires_at: '25 Aug 2126',
        device: '4BF16D4D-D061-46...',
        note: 'Owner Master Key'
    },
    {
        id: 'seed-9',
        key_code: 'K3N9-8YRA-2PLM-90QW',
        type: 'Paid',
        duration_days: 15,
        duration_text: '15 Days',
        status: 'Active',
        created_at: '17 Sept 2026',
        expires_at: '02 Oct 2026',
        device: 'E8314F29-01BA-48...',
        note: 'Reseller Key'
    },
    {
        id: 'seed-10',
        key_code: '7XWQ-V92P-MM4T-LK91',
        type: 'Paid',
        duration_days: 30,
        duration_text: '30 Days',
        status: 'Active',
        created_at: '16 Sept 2026',
        expires_at: '16 Oct 2026',
        device: 'B1920834-55C1-39...',
        note: 'Pro Gamer VIP'
    }
];

// App State
let allKeys = [];
let currentFilter = 'all';
let currentSearch = '';
let isPausedAll = false;
let currentDurationMode = 'preset';
let supabaseClient = null;

// Initialize Supabase Client
try {
    if (window.supabase && window.SUPABASE_CONFIG) {
        supabaseClient = window.supabase.createClient(
            window.SUPABASE_CONFIG.URL,
            window.SUPABASE_CONFIG.ANON_KEY
        );
    }
} catch (e) {
    console.warn('Supabase JS Init Warn:', e);
}

// Generate Random 4x4 Alphanumeric Key: XXXX-XXXX-XXXX-XXXX
function generateRandomKey() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const seg = () => Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return `${seg()}-${seg()}-${seg()}-${seg()}`;
}

// Generate Masked Device ID
function generateRandomDeviceId() {
    const hex = () => Math.random().toString(16).substring(2, 6).toUpperCase();
    return `${hex()}${hex()}-${hex()}-${hex()}...`;
}

// Format Date e.g. "21 Sept 2026"
function formatDateDisplay(d) {
    const date = new Date(d);
    if (isNaN(date.getTime())) return '21 Sept 2026';
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sept', 'Okt', 'Nov', 'Des'];
    return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
}

// Add Activity Item
function logActivity(text) {
    const stream = document.getElementById('activityStream');
    if (!stream) return;
    const timeStr = new Date().toLocaleTimeString('id-ID');
    const div = document.createElement('div');
    div.className = 'activity-item';
    div.style.padding = '8px 0';
    div.style.borderBottom = '1px solid #1c1d24';
    div.style.fontSize = '12px';
    div.style.color = '#9ca3af';
    div.innerHTML = `<span style="color:#ffffff; font-weight:600;">[${timeStr}]</span> ${text}`;
    stream.prepend(div);
}

// Show Toast
function showToast(message, isSuccess = true) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${isSuccess ? 'toast-success' : ''}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 250);
    }, 2800);
}

// Set Duration Selection Mode
function setDurationMode(mode) {
    currentDurationMode = mode;
    document.getElementById('pillPreset').classList.toggle('active', mode === 'preset');
    document.getElementById('pillCustom').classList.toggle('active', mode === 'custom');
    document.getElementById('pillDate').classList.toggle('active', mode === 'date');

    document.getElementById('wrapPreset').classList.toggle('hidden', mode !== 'preset');
    document.getElementById('wrapCustom').classList.toggle('hidden', mode !== 'custom');
    document.getElementById('wrapDate').classList.toggle('hidden', mode !== 'date');
}

// Copy Key to Clipboard
function copyKey(keyText) {
    navigator.clipboard.writeText(keyText).then(() => {
        showToast(`📋 Key ${keyText} disalin ke clipboard!`, true);
        logActivity(`Key <b style="color:#dc2626">${keyText}</b> disalin.`);
    }).catch(() => {
        const temp = document.createElement('textarea');
        temp.value = keyText;
        document.body.appendChild(temp);
        temp.select();
        document.execCommand('copy');
        document.body.removeChild(temp);
        showToast(`📋 Key ${keyText} disalin ke clipboard!`, true);
    });
}

// Calculate & Update Stat Counters
function updateStats() {
    const total = allKeys.length;
    const paid = allKeys.filter(k => k.type === 'Paid').length;
    const free = allKeys.filter(k => k.type === 'Free').length;
    const valid = allKeys.filter(k => k.status === 'Active').length;
    const expired = allKeys.filter(k => k.status === 'Expired' || k.status === 'Revoked').length;

    document.getElementById('statTotal').textContent = total;
    document.getElementById('statPaid').textContent = paid;
    document.getElementById('statFree').textContent = free;
    document.getElementById('statValid').textContent = valid;
    document.getElementById('statExpired').textContent = expired;
    document.getElementById('tableHeading').textContent = `ALL KEYS (${total})`;

    const revPaid = document.getElementById('revPaidCount');
    if (revPaid) revPaid.textContent = paid;
    const revTotal = document.getElementById('revTotalRp');
    if (revTotal) {
        const estRp = paid * 145000;
        revTotal.textContent = `Rp ${estRp.toLocaleString('id-ID')}`;
    }
}

// Render Table Rows
function renderKeysTable() {
    const tbody = document.getElementById('keysTableBody');
    if (!tbody) return;

    let filtered = allKeys;

    if (currentFilter === 'Paid') {
        filtered = filtered.filter(k => k.type === 'Paid');
    } else if (currentFilter === 'Free') {
        filtered = filtered.filter(k => k.type === 'Free');
    }

    if (currentSearch) {
        const q = currentSearch.toLowerCase();
        filtered = filtered.filter(k => 
            k.key_code.toLowerCase().includes(q) ||
            (k.note && k.note.toLowerCase().includes(q)) ||
            (k.device && k.device.toLowerCase().includes(q))
        );
    }

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 36px; color: #6b7280;">
                    Tidak ada key yang sesuai dengan pencarian atau filter.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(item => {
        const isOwner = item.type === 'Owner';
        const typeClass = isOwner ? 'type-owner' : (item.type === 'Free' ? 'type-free' : 'type-paid');
        const statusClass = item.status === 'Active' ? 'status-active' : (item.status === 'Expired' ? 'status-expired' : 'status-paused');

        return `
            <tr>
                <td>
                    <div class="key-cell">
                        <div class="key-row">
                            <span class="key-code">${item.key_code}</span>
                            <button type="button" class="btn-copy" onclick="copyKey('${item.key_code}')">Copy</button>
                        </div>
                        ${isOwner ? '<span class="badge-owner-sub">OWNER KEY</span>' : ''}
                    </div>
                </td>
                <td>
                    <span class="badge-pill ${typeClass}">${item.type}</span>
                </td>
                <td>
                    <span>${item.duration_text || (item.duration_days >= 3650 ? 'Lifetime' : item.duration_days + ' Days')}</span>
                </td>
                <td>
                    <span class="badge-pill ${statusClass}">${item.status}</span>
                </td>
                <td>${item.created_at}</td>
                <td>${item.expires_at}</td>
                <td>
                    <span class="device-masked">${item.device || '-'}</span>
                </td>
                <td class="actions-cell">
                    <button type="button" class="btn-act-revoke" onclick="handleRevokeKey('${item.id}')">Revoke</button>
                    <button type="button" class="btn-act-delete" onclick="handleDeleteKey('${item.id}')">Delete</button>
                </td>
            </tr>
        `;
    }).join('');
}

// Generate New Keys Action
async function handleGenerateKeys(e) {
    e.preventDefault();

    const submitBtn = document.getElementById('btnGenSubmit');
    submitBtn.disabled = true;
    submitBtn.style.opacity = '0.7';

    let durationDays = 7;
    let durationText = '7 Days';

    if (currentDurationMode === 'preset') {
        durationDays = parseInt(document.getElementById('selDuration').value, 10);
        const selObj = document.getElementById('selDuration');
        durationText = selObj.options[selObj.selectedIndex].text;
    } else if (currentDurationMode === 'custom') {
        const val = parseInt(document.getElementById('inputCustomDays').value, 10);
        durationDays = isNaN(val) || val <= 0 ? 7 : val;
        durationText = `${durationDays} Days`;
    } else if (currentDurationMode === 'date') {
        const pickDateVal = document.getElementById('inputPickDate').value;
        if (pickDateVal) {
            const diffDays = Math.ceil((new Date(pickDateVal) - new Date()) / (1000 * 60 * 60 * 24));
            durationDays = Math.max(diffDays, 1);
            durationText = `${durationDays} Days`;
        }
    }

    const count = Math.min(Math.max(parseInt(document.getElementById('inputCount').value, 10) || 1, 1), 50);
    const note = document.getElementById('inputNote').value.trim();
    const typeRadios = document.getElementsByName('keyType');
    let keyType = 'Paid';
    for (const r of typeRadios) {
        if (r.checked) keyType = r.value;
    }

    const now = new Date();
    const nowStr = formatDateDisplay(now);
    const expDate = new Date(now.getTime() + (durationDays >= 3650 ? 36500 : durationDays) * 24 * 60 * 60 * 1000);
    const expStr = durationDays >= 3650 ? '25 Aug 2126' : formatDateDisplay(expDate);

    const generated = [];
    for (let i = 0; i < count; i++) {
        const keyCode = generateRandomKey();
        const newObj = {
            id: 'key-' + Date.now() + '-' + i,
            key_code: keyCode,
            type: keyType,
            duration_days: durationDays,
            duration_text: durationText,
            status: 'Active',
            created_at: nowStr,
            expires_at: expStr,
            device: '-',
            note: note || (keyType === 'Owner' ? 'Owner Master Key' : `Paket ${durationText}`)
        };
        generated.push(newObj);
        allKeys.unshift(newObj);
    }

    // Save to Supabase Cloud Database if connected
    if (supabaseClient) {
        try {
            await supabaseClient.from('license_keys').insert(
                generated.map(k => ({
                    key_code: k.key_code,
                    duration_days: k.duration_days,
                    note: `[${k.type}] ${k.note}`,
                    is_used: false
                }))
            );
        } catch (err) {
            console.error('Supabase key insert error:', err);
        }
    }

    // Call Vercel API asynchronously to keep backend in sync
    fetch('/api/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            duration: durationDays,
            count: count,
            type: keyType,
            note: note
        })
    }).catch(err => console.log('API sync background:', err));

    updateStats();
    renderKeysTable();

    submitBtn.disabled = false;
    submitBtn.style.opacity = '1';

    showToast(`✅ Berhasil membuat ${count} key lisensi (${keyType})!`, true);
    logActivity(`Membuat ${count} key baru (<b style="color:#10b981">${durationText}</b>, Tipe: ${keyType}).`);

    // Reset optional note
    document.getElementById('inputNote').value = '';
}

// Filter Selection
function setFilter(filterType) {
    currentFilter = filterType;
    document.querySelectorAll('.filter-pill').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-filter') === filterType);
    });
    renderKeysTable();
}

// Search Filter
function handleSearch(val) {
    currentSearch = val.trim();
    renderKeysTable();
}

// Toggle Pause All Keys
function handlePauseAllKeys() {
    isPausedAll = !isPausedAll;
    const btn = document.getElementById('btnPauseAll');
    if (isPausedAll) {
        btn.textContent = '▶️ Resume All Keys';
        btn.style.color = '#10b981';
        btn.style.borderColor = '#10b981';
        allKeys.forEach(k => {
            if (k.status === 'Active') k.status = 'Paused';
        });
        showToast('⏸️ Semua key aktif dijeda (Paused).', true);
        logActivity('Semua key dijeda (Paused).');
    } else {
        btn.textContent = 'Pause All Keys';
        btn.style.color = 'var(--amber-accent)';
        btn.style.borderColor = 'rgba(245, 158, 11, 0.4)';
        allKeys.forEach(k => {
            if (k.status === 'Paused') k.status = 'Active';
        });
        showToast('▶️ Semua key aktif dilanjutkan (Resumed).', true);
        logActivity('Semua key kembali aktif (Resumed).');
    }
    updateStats();
    renderKeysTable();
}

// Revoke Single Key
function handleRevokeKey(id) {
    const item = allKeys.find(k => k.id === id);
    if (!item) return;
    item.status = 'Revoked';
    updateStats();
    renderKeysTable();
    showToast(`⚠️ Key ${item.key_code} telah di-revoke.`, false);
    logActivity(`Key <b style="color:#ef4444">${item.key_code}</b> di-revoke.`);

    if (supabaseClient) {
        supabaseClient.from('license_keys')
            .update({ is_used: true, note: `[REVOKED] ${item.note || ''}` })
            .eq('key_code', item.key_code);
    }
}

// Delete Single Key
function handleDeleteKey(id) {
    const item = allKeys.find(k => k.id === id);
    if (!item) return;
    if (!confirm(`Hapus key lisensi ${item.key_code}?`)) return;

    allKeys = allKeys.filter(k => k.id !== id);
    updateStats();
    renderKeysTable();
    showToast(`🗑️ Key ${item.key_code} dihapus.`, true);
    logActivity(`Key <b>${item.key_code}</b> dihapus.`);

    if (supabaseClient) {
        supabaseClient.from('license_keys')
            .delete()
            .eq('key_code', item.key_code);
    }
}

// Switch Navigation View
function switchNav(viewName) {
    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-view') === viewName);
    });

    const panels = {
        keys: 'viewKeys',
        license: 'viewLicense',
        revenue: 'viewRevenue',
        analytics: 'viewAnalytics',
        activity: 'viewActivity',
        settings: 'viewSettings',
        owner: 'viewOwner'
    };

    document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));
    const target = document.getElementById(panels[viewName]);
    if (target) target.classList.add('active');

    // Close mobile sidebar if open
    document.getElementById('sidebar').classList.remove('open');
}

// Toggle Mobile Sidebar
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

// Lookup / Validate Key Form
async function handleValidateLookup() {
    const input = document.getElementById('lookupKeyInput');
    const resBox = document.getElementById('lookupResult');
    const val = input.value.trim().toUpperCase();
    if (!val) return;

    resBox.classList.remove('hidden');
    resBox.innerHTML = '<span style="color:#9ca3af;">Memeriksa database lisensi...</span>';

    try {
        const res = await fetch('/api/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key: val })
        });
        const data = await res.json();
        if (data.success) {
            resBox.innerHTML = `
                <div style="background:#13281d; border:1px solid #10b981; padding:14px; border-radius:8px; margin-top:14px;">
                    <div style="color:#10b981; font-weight:700; font-size:14px;">✅ LISENSI VALID & AKTIF</div>
                    <div style="margin-top:6px; font-size:13px; color:#ffffff;">Key: <b>${data.key}</b></div>
                    <div style="font-size:12px; color:#9ca3af; margin-top:2px;">Role: ${data.role} | Durasi: ${data.expiry}</div>
                </div>
            `;
        } else {
            resBox.innerHTML = `
                <div style="background:#281316; border:1px solid #ef4444; padding:14px; border-radius:8px; margin-top:14px;">
                    <div style="color:#ef4444; font-weight:700; font-size:14px;">❌ LISENSI TIDAK VALID</div>
                    <div style="margin-top:4px; font-size:12px; color:#d1d5db;">${data.message || 'Key tidak terdaftar.'}</div>
                </div>
            `;
        }
    } catch (e) {
        resBox.innerHTML = `<div style="color:#ef4444; margin-top:10px;">Gagal menghubungi server validasi.</div>`;
    }
}

// Export CSV
function exportKeysCSV() {
    let csv = 'Key,Type,Duration,Status,Created,Expires,Device,Note\n';
    allKeys.forEach(k => {
        csv += `"${k.key_code}","${k.type}","${k.duration_text}","${k.status}","${k.created_at}","${k.expires_at}","${k.device}","${k.note || ''}"\n`;
    });
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `REGS_XD_KEYS_${Date.now()}.csv`;
    a.click();
    showToast('💾 Berhasil mendownload backup CSV.', true);
}

// Export JSON
function exportKeysJSON() {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(allKeys, null, 2));
    const a = document.createElement('a');
    a.href = dataStr;
    a.download = `REGS_XD_KEYS_${Date.now()}.json`;
    a.click();
    showToast('💾 Berhasil mendownload backup JSON.', true);
}

// Modal Login Functions
function quickFillAdmin() {
    document.getElementById('loginUsername').value = 'regsxd18';
    document.getElementById('loginPass').value = 'leaaaimut1';
}

function handleModalLogin(e) {
    e.preventDefault();
    const u = document.getElementById('loginUsername').value.trim();
    const p = document.getElementById('loginPass').value.trim();

    if ((u === 'regsxd18' || u === 'regsxd18@cena-regs.com') && p === 'leaaaimut1') {
        localStorage.setItem('regs_owner_logged', 'true');
        document.getElementById('loginModal').classList.add('hidden');
        showToast('👑 Selamat datang kembali, Owner RegsXD!', true);
        logActivity('Owner login: <b>regsxd18</b>');
    } else {
        alert('Kredensial Owner Salah! Silakan gunakan akun Owner: regsxd18 / leaaaimut1');
    }
}

function handleLogout() {
    if (confirm('Apakah Anda ingin logout dari sesi Owner?')) {
        localStorage.removeItem('regs_owner_logged');
        document.getElementById('loginModal').classList.remove('hidden');
        showToast('🚪 Sesi Owner telah ditutup.', false);
    }
}

// Load real keys from Supabase Cloud
async function fetchSupabaseKeys() {
    if (!supabaseClient) return;
    try {
        const { data, error } = await supabaseClient
            .from('license_keys')
            .select('*')
            .order('created_at', { ascending: false });

        if (!error && data && data.length > 0) {
            const mapped = data.map(dbKey => {
                const note = dbKey.note || '';
                let type = 'Paid';
                if (note.includes('[Owner]') || note.toLowerCase().includes('owner')) type = 'Owner';
                else if (note.includes('[Free]') || note.toLowerCase().includes('free')) type = 'Free';

                const days = dbKey.duration_days || 7;
                let durText = `${days} Days`;
                if (days === 1) durText = '1 Day';
                else if (days >= 3650) durText = 'Lifetime';

                const created = formatDateDisplay(dbKey.created_at);
                const expDate = new Date(new Date(dbKey.created_at).getTime() + (days >= 3650 ? 36500 : days) * 86400000);
                const expires = days >= 3650 ? '25 Aug 2126' : formatDateDisplay(expDate);

                return {
                    id: dbKey.id,
                    key_code: dbKey.key_code,
                    type: type,
                    duration_days: days,
                    duration_text: durText,
                    status: dbKey.is_used ? 'Expired' : 'Active',
                    created_at: created,
                    expires_at: expires,
                    device: dbKey.used_by || '-',
                    note: note
                };
            });

            // Combine database keys with seed keys (avoiding duplicate codes)
            const existingCodes = new Set(mapped.map(m => m.key_code));
            const remainingSeeds = INITIAL_KEYS.filter(s => !existingCodes.has(s.key_code));
            allKeys = [...mapped, ...remainingSeeds];
            updateStats();
            renderKeysTable();
        }
    } catch (e) {
        console.warn('Supabase fetch note:', e);
    }
}

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    // Populate Initial Keys
    allKeys = [...INITIAL_KEYS];
    updateStats();
    renderKeysTable();

    // Check Login State
    const isLogged = localStorage.getItem('regs_owner_logged');
    if (isLogged !== 'true') {
        document.getElementById('loginModal').classList.remove('hidden');
    }

    // Fetch Cloud Keys
    fetchSupabaseKeys();
});
