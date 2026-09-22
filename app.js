import { createClient } from 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/+esm';
import { SUPABASE_CONFIG } from './supabase_config.js';

// Setup Supabase Client
const isConfigured = SUPABASE_CONFIG.url && !SUPABASE_CONFIG.url.includes("your-project") && 
                     SUPABASE_CONFIG.anonKey && !SUPABASE_CONFIG.anonKey.includes("your-anon");

export const supabase = isConfigured ? createClient(SUPABASE_CONFIG.url, SUPABASE_CONFIG.anonKey) : null;

// DOM Elements
const alertBox = document.getElementById('alertBox');
const tabButtons = document.querySelectorAll('.tab-btn');
const formSections = document.querySelectorAll('.form-section');

// Helper Isi Cepat Kredensial Owner
window.fillAdminCredentials = function() {
    const emailInput = document.getElementById('loginEmail');
    const passInput = document.getElementById('loginPassword');
    if (emailInput && passInput) {
        emailInput.value = 'regsxd18';
        passInput.value = 'leaaaimut1';
        showAlert('Kredensial Owner terisi. Klik "LOGIN SEKARANG" untuk masuk.', false);
    }
};

window.quickSwitchToAdminLogin = function() {
    window.switchTab('login');
    window.fillAdminCredentials();
};

// Tab Navigation
window.switchTab = function(tabName) {
    tabButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabName);
    });
    formSections.forEach(section => {
        section.classList.toggle('active', section.id === `${tabName}Section`);
    });

    // Cek Akses Generator (Hanya Admin / Owner regsxd18)
    if (tabName === 'gen') {
        const isAdmin = localStorage.getItem('admin_user') === 'regsxd18';
        const lockNotice = document.getElementById('genLockNotice');
        const genContent = document.getElementById('genContent');
        if (lockNotice && genContent) {
            lockNotice.style.display = isAdmin ? 'none' : 'block';
            genContent.style.display = isAdmin ? 'block' : 'none';
        }
    }

    hideAlert();
};

function showAlert(message, isError = true) {
    alertBox.className = `alert-box show ${isError ? 'alert-error' : 'alert-success'}`;
    alertBox.textContent = message;
}

function hideAlert() {
    alertBox.className = 'alert-box';
    alertBox.textContent = '';
}

// Initial session check
async function initSession() {
    // 1. Cek Sesi Owner / Master Admin
    const adminUser = localStorage.getItem('admin_user');
    if (adminUser === 'regsxd18') {
        renderDashboard({
            email: 'regsxd18@cena-regs.com',
            username: 'regsxd18',
            role: '👑 OWNER / MASTER ADMIN',
            expires_at: 'LIFETIME (Permanen)',
            is_admin: true
        });
        return;
    }

    if (!supabase) {
        // Mode Demo Pengguna Biasa
        const demoUser = localStorage.getItem('demo_user');
        if (demoUser) {
            renderDashboard({
                email: demoUser,
                username: demoUser.split('@')[0],
                role: 'VIP MEMBER',
                expires_at: '30 Hari Aktif'
            });
        }
        return;
    }

    const { data: { session } } = await supabase.auth.getSession();
    if (session) {
        fetchUserProfile(session.user);
    }
}

async function fetchUserProfile(user) {
    try {
        const { data: profile } = await supabase
            .from('profiles')
            .select('*')
            .eq('id', user.id)
            .single();

        renderDashboard({
            email: user.email,
            username: profile?.username || user.user_metadata?.username || user.email.split('@')[0],
            role: (profile?.role || 'VIP User').toUpperCase(),
            expires_at: profile?.expires_at ? new Date(profile.expires_at).toLocaleDateString() : 'Active'
        });
    } catch (e) {
        renderDashboard({
            email: user.email,
            username: user.email.split('@')[0],
            role: 'MEMBER',
            expires_at: 'Active'
        });
    }
}

function renderDashboard(data) {
    document.getElementById('dashUsername').textContent = data.username || data.email;
    document.getElementById('dashEmail').textContent = data.email;
    document.getElementById('dashRole').textContent = data.role;
    document.getElementById('dashExpiry').textContent = data.expires_at;

    // Tampilkan tombol Buka Generator jika role Owner / Admin
    const adminBox = document.getElementById('adminActionBox');
    if (adminBox) {
        const isAdmin = data.is_admin || localStorage.getItem('admin_user') === 'regsxd18';
        adminBox.style.display = isAdmin ? 'block' : 'none';
    }

    document.getElementById('authTabs').style.display = 'none';
    formSections.forEach(s => s.classList.remove('active'));
    document.getElementById('dashboardSection').classList.add('active');
}

// 1. Handle Login
window.handleLogin = async function(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const pass = document.getElementById('loginPassword').value;
    const btn = document.getElementById('btnLogin');

    btn.disabled = true;
    btn.textContent = 'MEMPROSES...';
    hideAlert();

    // ⚡ Autentikasi Khusus Owner / Master Admin: regsxd18 : leaaaimut1
    if ((email.toLowerCase() === 'regsxd18' || email.toLowerCase() === 'regsxd18@admin.com') && pass === 'leaaaimut1') {
        setTimeout(() => {
            btn.disabled = false;
            btn.textContent = 'LOGIN SEKARANG';
            localStorage.setItem('admin_user', 'regsxd18');
            renderDashboard({
                email: 'regsxd18@cena-regs.com',
                username: 'regsxd18',
                role: '👑 OWNER / MASTER ADMIN',
                expires_at: 'LIFETIME (Permanen)',
                is_admin: true
            });
            showAlert('Selamat datang Owner RegsXD! Akses Admin Penuh Aktif.', false);
        }, 400);
        return;
    }

    if (!supabase) {
        // Fallback demo login
        setTimeout(() => {
            btn.disabled = false;
            btn.textContent = 'LOGIN SEKARANG';
            localStorage.setItem('demo_user', email);
            renderDashboard({
                email: email,
                username: email.split('@')[0],
                role: 'VIP MEMBER',
                expires_at: '30 Hari Aktif'
            });
            showAlert('Login Berhasil (Mode Demo Supabase)', false);
        }, 500);
        return;
    }

    const { data, error } = await supabase.auth.signInWithPassword({
        email: email,
        password: pass
    });

    btn.disabled = false;
    btn.textContent = 'LOGIN SEKARANG';

    if (error) {
        showAlert(error.message);
    } else {
        showAlert('Login Berhasil!', false);
        fetchUserProfile(data.user);
    }
};

// 2. Handle Register
window.handleRegister = async function(e) {
    e.preventDefault();
    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const pass = document.getElementById('regPassword').value;
    const btn = document.getElementById('btnRegister');

    if (pass.length < 6) {
        showAlert('Password minimal 6 karakter!');
        return;
    }

    btn.disabled = true;
    btn.textContent = 'MENDAFTAR...';
    hideAlert();

    if (!supabase) {
        setTimeout(() => {
            btn.disabled = false;
            btn.textContent = 'DAFTAR AKUN BARU';
            showAlert('Registrasi berhasil! Silakan login di tab Login.', false);
            window.switchTab('login');
        }, 600);
        return;
    }

    const { data, error } = await supabase.auth.signUp({
        email: email,
        password: pass,
        options: {
            data: { username: username }
        }
    });

    btn.disabled = false;
    btn.textContent = 'DAFTAR AKUN BARU';

    if (error) {
        showAlert(error.message);
    } else {
        showAlert('Akun berhasil dibuat! Silakan masuk.', false);
        window.switchTab('login');
    }
};

// Helper untuk format nama durasi
function getDurationLabel(days) {
    const d = parseInt(days, 10);
    if (d >= 3650) return 'LIFETIME (Permanen)';
    if (d === 1) return '1 Hari';
    if (d === 3) return '3 Hari';
    if (d === 7) return '7 Hari';
    if (d === 15) return '15 Hari';
    if (d === 30) return '30 Hari';
    return `${d} Hari`;
}

// 3. Handle Redeem Key
window.handleRedeemKey = async function(e) {
    e.preventDefault();
    const keyInput = document.getElementById('licenseKey');
    const key = keyInput.value.trim().toUpperCase();
    const btn = document.getElementById('btnRedeem');

    if (!key) return;

    btn.disabled = true;
    btn.textContent = 'MEMVALIDASI...';
    hideAlert();

    if (!supabase) {
        // Mode Demo Lokal
        setTimeout(() => {
            btn.disabled = false;
            btn.textContent = 'AKTIFKAN LISENSI';

            // Cek di penyimpanan demo local storage atau deteksi format
            const demoKeys = JSON.parse(localStorage.getItem('demo_license_keys') || '[]');
            const foundIndex = demoKeys.findIndex(k => k.key_code === key && !k.is_used);

            let durationDays = 30;
            if (foundIndex !== -1) {
                durationDays = demoKeys[foundIndex].duration_days;
                demoKeys[foundIndex].is_used = true;
                localStorage.setItem('demo_license_keys', JSON.stringify(demoKeys));
            } else if (key.includes('1D')) {
                durationDays = 1;
            } else if (key.includes('3D')) {
                durationDays = 3;
            } else if (key.includes('7D')) {
                durationDays = 7;
            } else if (key.includes('15D')) {
                durationDays = 15;
            } else if (key.includes('30D')) {
                durationDays = 30;
            } else if (key.includes('LIFE')) {
                durationDays = 36500;
            } else if (!key.startsWith('REGSXD')) {
                showAlert('Format key tidak valid. Contoh: REGSXD-1D-XXXX atau REGSXD-LIFE-XXXX');
                return;
            }

            const durationText = getDurationLabel(durationDays);
            showAlert(`Sukses! Lisensi ${durationText} berhasil diaktifkan.`, false);
            keyInput.value = '';

            // Update demo session jika ada
            const demoUser = localStorage.getItem('demo_user');
            if (demoUser) {
                renderDashboard({
                    email: demoUser,
                    username: demoUser.split('@')[0],
                    role: durationDays >= 3650 ? 'VIP LIFETIME' : 'VIP MEMBER',
                    expires_at: durationText
                });
            }
        }, 600);
        return;
    }

    const { data, error } = await supabase
        .from('license_keys')
        .select('*')
        .eq('key_code', key)
        .eq('is_used', false)
        .single();

    btn.disabled = false;
    btn.textContent = 'AKTIFKAN LISENSI';

    if (error || !data) {
        showAlert('Key tidak valid atau sudah pernah digunakan!');
    } else {
        const durationText = getDurationLabel(data.duration_days);

        // Ambil session user saat ini jika ada
        const { data: { session } } = await supabase.auth.getSession();
        const userId = session?.user?.id || null;

        // Tandai key sebagai terpakai
        await supabase
            .from('license_keys')
            .update({ 
                is_used: true, 
                used_at: new Date(),
                used_by: userId
            })
            .eq('id', data.id);

        // Jika user sedang login, update profile expires_at
        if (userId) {
            const expDate = new Date();
            expDate.setDate(expDate.getDate() + data.duration_days);

            await supabase
                .from('profiles')
                .update({
                    expires_at: expDate.toISOString(),
                    role: data.duration_days >= 3650 ? 'vip' : 'vip'
                })
                .eq('id', userId);

            fetchUserProfile(session.user);
        }

        keyInput.value = '';
        showAlert(`Sukses! Lisensi ${durationText} berhasil diaktifkan.`, false);
    }
};

// 4. Handle Generate Key (Admin / Owner Generator)
let currentGeneratedKeys = [];

window.handleGenerateKeys = async function(e) {
    e.preventDefault();
    const durationRadio = document.querySelector('input[name="genDuration"]:checked');
    const durationDays = parseInt(durationRadio?.value || '30', 10);
    const count = parseInt(document.getElementById('genCount').value || '1', 10);
    const note = document.getElementById('genNote').value.trim();
    const btn = document.getElementById('btnGen');

    btn.disabled = true;
    btn.textContent = 'MEMBUAT KEY...';
    hideAlert();

    // Tentukan prefix tag durasi
    let tag = '30D';
    if (durationDays === 1) tag = '1D';
    else if (durationDays === 3) tag = '3D';
    else if (durationDays === 7) tag = '7D';
    else if (durationDays === 15) tag = '15D';
    else if (durationDays === 30) tag = '30D';
    else if (durationDays >= 3650) tag = 'LIFE';

    const newKeys = [];
    for (let i = 0; i < count; i++) {
        // Buat random string 6 karakter alfanumerik unik
        const randStr = Math.random().toString(36).substring(2, 6).toUpperCase() + 
                        Math.floor(100 + Math.random() * 900);
        const keyCode = `REGSXD-${tag}-${randStr}`;
        newKeys.push({
            key_code: keyCode,
            duration_days: durationDays,
            note: note || `Paket ${getDurationLabel(durationDays)}`,
            is_used: false,
            created_at: new Date().toISOString()
        });
    }

    if (!supabase) {
        // Simpan di demo local storage
        setTimeout(() => {
            btn.disabled = false;
            btn.textContent = 'BUAT LICENSE KEY';

            const demoKeys = JSON.parse(localStorage.getItem('demo_license_keys') || '[]');
            demoKeys.push(...newKeys);
            localStorage.setItem('demo_license_keys', JSON.stringify(demoKeys));

            currentGeneratedKeys = newKeys;
            renderGeneratedKeysList(newKeys);
            showAlert(`Berhasil membuat ${newKeys.length} License Key (${getDurationLabel(durationDays)})!`, false);
        }, 500);
        return;
    }

    // Simpan ke Supabase license_keys
    const insertPayload = newKeys.map(k => ({
        key_code: k.key_code,
        duration_days: k.duration_days,
        note: k.note,
        is_used: false
    }));

    const { error } = await supabase
        .from('license_keys')
        .insert(insertPayload);

    btn.disabled = false;
    btn.textContent = 'BUAT LICENSE KEY';

    if (error) {
        showAlert(`Gagal menyimpan key ke Supabase: ${error.message}`);
    } else {
        currentGeneratedKeys = newKeys;
        renderGeneratedKeysList(newKeys);
        showAlert(`Berhasil membuat ${newKeys.length} License Key (${getDurationLabel(durationDays)})!`, false);
    }
};

function renderGeneratedKeysList(keys) {
    const box = document.getElementById('genResultBox');
    const list = document.getElementById('genKeysList');
    box.style.display = 'block';
    list.innerHTML = '';

    keys.forEach(k => {
        const item = document.createElement('div');
        item.className = 'key-item-row';
        item.innerHTML = `
            <div class="key-info">
                <span class="key-tier-tag">${getDurationLabel(k.duration_days)}</span>
                <span class="key-code-text">${k.key_code}</span>
            </div>
            <button type="button" class="btn-copy-item" onclick="copyKeyToClipboard('${k.key_code}', this)">Salin</button>
        `;
        list.appendChild(item);
    });
}

window.copyKeyToClipboard = function(text, btn) {
    navigator.clipboard.writeText(text).then(() => {
        const originalText = btn.textContent;
        btn.textContent = 'Tersalin!';
        btn.style.background = 'var(--green-success)';
        setTimeout(() => {
            btn.textContent = originalText;
            btn.style.background = '';
        }, 1500);
    });
};

window.copyAllGeneratedKeys = function() {
    if (!currentGeneratedKeys.length) return;
    const textAll = currentGeneratedKeys.map(k => `${k.key_code} (${getDurationLabel(k.duration_days)})`).join('\n');
    navigator.clipboard.writeText(textAll).then(() => {
        showAlert('Semua key berhasil disalin ke clipboard!', false);
    });
};

// 5. Logout
window.handleLogout = async function() {
    if (supabase) {
        await supabase.auth.signOut();
    }
    localStorage.removeItem('demo_user');
    localStorage.removeItem('admin_user');
    document.getElementById('authTabs').style.display = 'flex';
    document.getElementById('dashboardSection').classList.remove('active');
    window.switchTab('login');
    showAlert('Anda telah keluar.', false);
};

// Init
initSession();

