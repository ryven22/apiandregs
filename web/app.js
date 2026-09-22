/**
 * REGS XD • Dashboard Controller
 * Robust Real-time Cloud Sync with Supabase & Local Cache (Never Disappears on Refresh)
 */

const SUPABASE_URL = (window.SUPABASE_CONFIG && window.SUPABASE_CONFIG.URL) || "https://maghrxnyavkittygojnn.supabase.co";
const SUPABASE_ANON_KEY = (window.SUPABASE_CONFIG && window.SUPABASE_CONFIG.ANON_KEY) || "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1hZ2hyeG55YXZraXR0eWdvam5uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkxNzE4MTIsImV4cCI6MjEwNDc0NzgxMn0.Vr1usXkl6jHzKujpEi8SxWPA2qV8mNrW5g6imXj-tso";

let allKeys = [];
let currentFilter = 'all';
let currentSearch = '';
let isPausedAll = false;
let currentDurationMode = 'preset';
let supabaseClient = null;

// Initialize Supabase Client
try {
    if (window.supabase) {
        supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
    }
} catch (e) {
    console.warn('Supabase JS Client Warn:', e);
}

// Generate Random 4x4 Alphanumeric Key: XXXX-XXXX-XXXX-XXXX
function generateRandomKey() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const seg = () => Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    return `${seg()}-${seg()}-${seg()}-${seg()}`;
}

// Format Date e.g. "22 Sept 2026"
function formatDateDisplay(d) {
    const date = new Date(d);
    if (isNaN(date.getTime())) return '-';
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul', 'Agu', 'Sept', 'Okt', 'Nov', 'Des'];
    return `${date.getDate()} ${months[date.getMonth()]} ${date.getFullYear()}`;
}

// Add Activity Item
function logActivity(text) {
    const stream = document.getElementById('activityStream');
    if (!stream) return;
    const emptyMsg = stream.querySelector('.empty-activity');
    if (emptyMsg) emptyMsg.remove();

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

// Show Toast Notification
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
        showToast(`Key ${keyText} disalin!`, true);
        logActivity(`Key <b style="color:#dc2626">${keyText}</b> disalin.`);
    }).catch(() => {
        const temp = document.createElement('textarea');
        temp.value = keyText;
        document.body.appendChild(temp);
        temp.select();
        document.execCommand('copy');
        document.body.removeChild(temp);
        showToast(`Key ${keyText} disalin!`, true);
    });
}

// Save Local Cache so Keys Never Disappear
function saveLocalCache() {
    try {
        localStorage.setItem('regs_cached_keys', JSON.stringify(allKeys));
    } catch (e) {}
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

    // Revenue Update
    const revPaid = document.getElementById('revPaidCount');
    if (revPaid) revPaid.textContent = paid;
    const revTotal = document.getElementById('revTotalRp');
    if (revTotal) {
        const estRp = paid * 145000;
        revTotal.textContent = `Rp ${estRp.toLocaleString('id-ID')}`;
    }
    const revRate = document.getElementById('revRate');
    if (revRate) {
        const rate = total > 0 ? Math.round((paid / total) * 100) : 0;
        revRate.textContent = `${rate}%`;
    }

    // Analytics Breakdown Update
    updateAnalytics();
}

function updateAnalytics() {
    const total = allKeys.length;
    const count1D = allKeys.filter(k => k.duration_days === 1).length;
    const count3D = allKeys.filter(k => k.duration_days === 3).length;
    const count7D = allKeys.filter(k => k.duration_days === 7).length;
    const count15D = allKeys.filter(k => k.duration_days === 15).length;
    const count30D = allKeys.filter(k => k.duration_days === 30).length;
    const countLife = allKeys.filter(k => k.duration_days >= 3650).length;

    const setBar = (barId, valId, count) => {
        const bar = document.getElementById(barId);
        const val = document.getElementById(valId);
        if (bar && val) {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            bar.style.width = `${pct}%`;
            val.textContent = `${count} Keys (${pct}%)`;
        }
    };

    setBar('bar1D', 'val1D', count1D);
    setBar('bar3D', 'val3D', count3D);
    setBar('bar7D', 'val7D', count7D);
    setBar('bar15D', 'val15D', count15D);
    setBar('bar30D', 'val30D', count30D);
    setBar('barLife', 'valLife', countLife);
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
                <td colspan="8" class="empty-keys-msg">
                    Belum ada key lisensi. Silakan buat key baru pada form di atas.
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

    let durationDays = 1;
    let durationText = '1 Day';

    if (currentDurationMode === 'preset') {
        durationDays = parseInt(document.getElementById('selDuration').value, 10) || 1;
        const selObj = document.getElementById('selDuration');
        durationText = selObj.options[selObj.selectedIndex].text;
    } else if (currentDurationMode === 'custom') {
        const val = parseInt(document.getElementById('inputCustomDays').value, 10);
        durationDays = isNaN(val) || val <= 0 ? 1 : val;
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

    // Save locally first so keys NEVER disappear
    saveLocalCache();
    updateStats();
    renderKeysTable();

    // Save to Supabase Cloud Database
    const payload = generated.map(k => ({
        key_code: k.key_code,
        duration_days: k.duration_days,
        note: `[${k.type}] ${k.note}`,
        is_used: false
    }));

    if (supabaseClient) {
        try {
            await supabaseClient.from('license_keys').insert(payload);
        } catch (err) {
            console.error('Supabase client insert error:', err);
        }
    } else {
        // Fallback: Direct REST fetch to Supabase
        try {
            await fetch(`${SUPABASE_URL}/rest/v1/license_keys`, {
                method: 'POST',
                headers: {
                    'apikey': SUPABASE_ANON_KEY,
                    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });
        } catch (err) {
            console.error('Direct fetch insert error:', err);
        }
    }

    submitBtn.disabled = false;
    submitBtn.style.opacity = '1';

    showToast(`Berhasil membuat ${count} key lisensi (${keyType}, ${durationText})!`, true);
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
        btn.textContent = 'Resume All Keys';
        btn.style.color = '#10b981';
        btn.style.borderColor = '#10b981';
        allKeys.forEach(k => {
            if (k.status === 'Active') k.status = 'Paused';
        });
        showToast('Semua key aktif dijeda (Paused).', true);
        logActivity('Semua key dijeda (Paused).');
    } else {
        btn.textContent = 'Pause All Keys';
        btn.style.color = 'var(--amber-accent)';
        btn.style.borderColor = 'rgba(245, 158, 11, 0.4)';
        allKeys.forEach(k => {
            if (k.status === 'Paused') k.status = 'Active';
        });
        showToast('Semua key aktif dilanjutkan (Resumed).', true);
        logActivity('Semua key kembali aktif (Resumed).');
    }
    saveLocalCache();
    updateStats();
    renderKeysTable();
}

// Revoke Single Key
function handleRevokeKey(id) {
    const item = allKeys.find(k => k.id === id);
    if (!item) return;
    item.status = 'Revoked';
    saveLocalCache();
    updateStats();
    renderKeysTable();
    showToast(`Key ${item.key_code} telah di-revoke.`, false);
    logActivity(`Key <b style="color:#ef4444">${item.key_code}</b> di-revoke.`);

    const updatePayload = { is_used: true, note: `[REVOKED] ${item.note || ''}` };
    if (supabaseClient) {
        supabaseClient.from('license_keys').update(updatePayload).eq('key_code', item.key_code);
    } else {
        fetch(`${SUPABASE_URL}/rest/v1/license_keys?key_code=eq.${item.key_code}`, {
            method: 'PATCH',
            headers: {
                'apikey': SUPABASE_ANON_KEY,
                'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updatePayload)
        }).catch(() => {});
    }
}

// Delete Single Key
function handleDeleteKey(id) {
    const item = allKeys.find(k => k.id === id);
    if (!item) return;
    if (!confirm(`Hapus key lisensi ${item.key_code}?`)) return;

    allKeys = allKeys.filter(k => k.id !== id);
    saveLocalCache();
    updateStats();
    renderKeysTable();
    showToast(`Key ${item.key_code} dihapus.`, true);
    logActivity(`Key <b>${item.key_code}</b> dihapus.`);

    if (supabaseClient) {
        supabaseClient.from('license_keys').delete().eq('key_code', item.key_code);
    } else {
        fetch(`${SUPABASE_URL}/rest/v1/license_keys?key_code=eq.${item.key_code}`, {
            method: 'DELETE',
            headers: {
                'apikey': SUPABASE_ANON_KEY,
                'Authorization': `Bearer ${SUPABASE_ANON_KEY}`
            }
        }).catch(() => {});
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
                    <div style="color:#10b981; font-weight:700; font-size:14px;">LISENSI VALID & AKTIF</div>
                    <div style="margin-top:6px; font-size:13px; color:#ffffff;">Key: <b>${data.key}</b></div>
                    <div style="font-size:12px; color:#9ca3af; margin-top:2px;">Role: ${data.role} | Durasi: ${data.expiry}</div>
                </div>
            `;
        } else {
            resBox.innerHTML = `
                <div style="background:#281316; border:1px solid #ef4444; padding:14px; border-radius:8px; margin-top:14px;">
                    <div style="color:#ef4444; font-weight:700; font-size:14px;">LISENSI TIDAK VALID</div>
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
    showToast('Berhasil mendownload backup CSV.', true);
}

// Export JSON
function exportKeysJSON() {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(allKeys, null, 2));
    const a = document.createElement('a');
    a.href = dataStr;
    a.download = `REGS_XD_KEYS_${Date.now()}.json`;
    a.click();
    showToast('Berhasil mendownload backup JSON.', true);
}

// Modal Login: Clean Username & Password
function handleModalLogin(e) {
    e.preventDefault();
    const u = document.getElementById('loginUsername').value.trim();
    const p = document.getElementById('loginPass').value.trim();

    if ((u === 'regsxd18' || u === 'regsxd18@cena-regs.com') && p === 'leaaaimut1') {
        localStorage.setItem('regs_owner_logged', 'true');
        document.getElementById('loginModal').classList.add('hidden');
        showToast('Selamat datang, regsxd18!', true);
        logActivity('Login: <b>regsxd18</b>');
    } else {
        alert('Username atau Password salah!');
    }
}

function handleLogout() {
    if (confirm('Keluar dari sesi?')) {
        localStorage.removeItem('regs_owner_logged');
        document.getElementById('loginUsername').value = '';
        document.getElementById('loginPass').value = '';
        document.getElementById('loginModal').classList.remove('hidden');
        showToast('Sesi telah ditutup.', false);
    }
}

// Fetch Cloud Keys from Supabase (Never Disappears on Refresh)
async function fetchSupabaseKeys() {
    let data = null;

    if (supabaseClient) {
        try {
            const res = await supabaseClient
                .from('license_keys')
                .select('*')
                .order('created_at', { ascending: false });
            data = res.data;
        } catch (e) {
            console.warn('Supabase client select warn:', e);
        }
    }

    // Direct HTTP fetch fallback if client was empty or failed
    if (!data) {
        try {
            const res = await fetch(`${SUPABASE_URL}/rest/v1/license_keys?select=*&order=created_at.desc`, {
                headers: {
                    'apikey': SUPABASE_ANON_KEY,
                    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`
                }
            });
            if (res.ok) {
                data = await res.json();
            }
        } catch (e) {
            console.warn('Direct fetch select warn:', e);
        }
    }

    if (data && Array.isArray(data)) {
        // Filter out old seed test keys from database setup
        const freshData = data.filter(k => 
            !k.key_code.includes('TEST1') && 
            !k.key_code.includes('PRB5956') &&
            !k.key_code.includes('TEST-SCHEMA')
        );

        if (freshData.length > 0) {
            allKeys = freshData.map(dbKey => {
                const note = dbKey.note || '';
                let type = 'Paid';
                if (note.includes('[Owner]') || note.toLowerCase().includes('owner')) type = 'Owner';
                else if (note.includes('[Free]') || note.toLowerCase().includes('free')) type = 'Free';

                const days = (dbKey.duration_days !== null && dbKey.duration_days !== undefined) ? parseInt(dbKey.duration_days, 10) : 1;
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
        }
        saveLocalCache();
        updateStats();
        renderKeysTable();
    }
}

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    // 1. Instantly restore from Local Cache if exists (Zero blink on refresh!)
    try {
        const cached = localStorage.getItem('regs_cached_keys');
        if (cached) {
            const parsed = JSON.parse(cached);
            if (Array.isArray(parsed) && parsed.length > 0) {
                allKeys = parsed;
            }
        }
    } catch (e) {}

    updateStats();
    renderKeysTable();

    // 2. Check Login State
    const isLogged = localStorage.getItem('regs_owner_logged');
    if (isLogged !== 'true') {
        document.getElementById('loginModal').classList.remove('hidden');
    }

    // 3. Sync live keys with Supabase Cloud
    fetchSupabaseKeys();
});
