const SIZE_LABELS = {STARTUP: 'Startup', SME: 'SME', LARGE: 'Large', ENTERPRISE: 'Enterprise', UNKNOWN: 'Unknown'};

let editingCompanyId = null;
let debounceTimer = null;

document.addEventListener('DOMContentLoaded', () => {
    loadCompanies();
    setupSearch();
});

// ── LOAD ──────────────────────────────
async function loadCompanies(keyword = '') {
    showSkeleton();
    let url = '/api/v1/companies?size=100';
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    const res = await Auth.apiFetch(url);
    if (!res) return;
    const data = await res.json();
    renderGrid(data.data || []);
}

// ── RENDER ────────────────────────────
function renderGrid(list) {
    const grid = document.getElementById('companies-grid');

    if (!list.length) {
        grid.innerHTML = `
            <div class="companies-empty">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                          d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
                <strong>No companies yet</strong>
                <span>Add companies you've applied to</span>
            </div>`;
        return;
    }

    grid.innerHTML = list.map((c, i) => `
        <div class="company-card fade-up" style="animation-delay:${i * 0.04}s"
             onclick="openEditCompany('${c.id}')">
            <div class="company-card-head">
                <div style="display:flex;align-items:center;gap:10px;min-width:0;flex:1">
                    <div class="company-avatar">${c.name.charAt(0).toUpperCase()}</div>
                    <div class="company-name">${c.name}</div>
                </div>
                <div class="company-card-actions" onclick="event.stopPropagation()">
                    <button class="icon-btn" onclick="openEditCompany('${c.id}')" title="Edit">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                        </svg>
                    </button>
                    <button class="icon-btn danger" onclick="deleteCompany('${c.id}', '${c.name.replace(/'/g, "\\'")}')" title="Delete">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                        </svg>
                    </button>
                </div>
            </div>

            <div class="company-meta">
                ${c.size && c.size !== 'UNKNOWN' ? `<span class="company-tag size-tag">${SIZE_LABELS[c.size] || c.size}</span>` : ''}
                ${c.industry ? `<span class="company-tag">${c.industry}</span>` : ''}
                ${c.isOutsource ? `<span class="company-tag outsource">Outsource</span>` : ''}
            </div>

            ${c.notes ? `<div class="company-notes">${c.notes}</div>` : ''}

            <div class="company-footer">
                ${c.location ? `
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                    <span>${c.location}</span>
                ` : ''}
                ${c.website ? `
                    <a href="${c.website}" target="_blank" rel="noopener"
                       onclick="event.stopPropagation()"
                       style="color:var(--accent2);font-size:12px;text-decoration:none;margin-left:auto;transition:color 0.15s"
                       onmouseover="this.style.color='var(--accent-bright)'"
                       onmouseout="this.style.color='var(--accent2)'">
                       Website ↗
                    </a>
                ` : ''}
            </div>
        </div>
    `).join('');
}

function showSkeleton() {
    const grid = document.getElementById('companies-grid');
    grid.innerHTML = Array(6).fill(`
        <div class="company-card loading">
            <div style="display:flex;gap:10px;margin-bottom:14px">
                <div class="skel" style="width:40px;height:40px;border-radius:8px;flex-shrink:0"></div>
                <div style="flex:1">
                    <div class="skel" style="width:70%;height:15px;margin-bottom:6px"></div>
                    <div class="skel" style="width:45%;height:11px"></div>
                </div>
            </div>
            <div style="display:flex;gap:6px;margin-bottom:10px">
                <div class="skel" style="width:60px;height:20px;border-radius:4px"></div>
                <div class="skel" style="width:80px;height:20px;border-radius:4px"></div>
            </div>
            <div class="skel" style="width:100%;height:11px;margin-bottom:4px"></div>
            <div class="skel" style="width:75%;height:11px"></div>
        </div>
    `).join('');
}

// ── SEARCH ────────────────────────────
function setupSearch() {
    document.getElementById('search-input').addEventListener('input', function () {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => loadCompanies(this.value.trim()), 350);
    });
}

// ── ADD / EDIT ────────────────────────
function openAddCompany() {
    editingCompanyId = null;
    document.getElementById('modal-title').textContent = 'Add company';
    document.getElementById('company-form').reset();
    document.getElementById('c-size').value = 'UNKNOWN';
    clearError();
    openModal();
}

async function openEditCompany(id) {
    const res = await Auth.apiFetch(`/api/v1/companies/${id}`);
    if (!res) return;
    const data = await res.json();
    const c = data.data;
    if (!c) return;

    editingCompanyId = id;
    document.getElementById('modal-title').textContent = 'Edit company';

    document.getElementById('c-name').value = c.name || '';
    document.getElementById('c-industry').value = c.industry || '';
    document.getElementById('c-size').value = c.size || 'UNKNOWN';
    document.getElementById('c-location').value = c.location || '';
    document.getElementById('c-website').value = c.website || '';
    document.getElementById('c-outsource').checked = c.isOutsource || false;
    document.getElementById('c-notes').value = c.notes || '';

    clearError();
    openModal();
}

async function saveCompany() {
    const btn = document.getElementById('btn-save-company');
    const name = document.getElementById('c-name').value.trim();

    if (!name) {
        showError('Company name is required');
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Saving...';

    const payload = {
        name,
        industry: document.getElementById('c-industry').value || null,
        size: document.getElementById('c-size').value || 'UNKNOWN',
        location: document.getElementById('c-location').value || null,
        website: document.getElementById('c-website').value || null,
        isOutsource: document.getElementById('c-outsource').checked,
        notes: document.getElementById('c-notes').value || null,
    };

    const url = editingCompanyId ? `/api/v1/companies/${editingCompanyId}` : '/api/v1/companies';
    const method = editingCompanyId ? 'PUT' : 'POST';

    const res = await Auth.apiFetch(url, {method, body: JSON.stringify(payload)});
    btn.disabled = false;
    btn.textContent = 'Save';

    if (!res || !res.ok) {
        const err = await res?.json();
        showError(err?.detail || 'Failed to save');
        return;
    }

    closeModal();
    loadCompanies(document.getElementById('search-input').value.trim());
}

async function deleteCompany(id, name) {
    if (!confirm(`Delete "${name}"?\n\nThis won't delete job applications linked to this company.`)) return;
    const res = await Auth.apiFetch(`/api/v1/companies/${id}`, {method: 'DELETE'});
    if (res && res.ok) loadCompanies(document.getElementById('search-input').value.trim());
}

// ── MODAL HELPERS ─────────────────────
function openModal() {
    document.getElementById('company-modal').classList.add('open');
    document.body.style.overflow = 'hidden';
    setTimeout(() => document.getElementById('c-name').focus(), 150);
}

function closeModal() {
    document.getElementById('company-modal').classList.remove('open');
    document.body.style.overflow = '';
}

function showError(msg) {
    const el = document.getElementById('form-error');
    el.textContent = msg;
    el.style.display = 'block';
}

function clearError() {
    const el = document.getElementById('form-error');
    el.textContent = '';
    el.style.display = 'none';
}

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
});

document.getElementById('company-modal').addEventListener('click', e => {
    if (e.target === document.getElementById('company-modal')) closeModal();
});