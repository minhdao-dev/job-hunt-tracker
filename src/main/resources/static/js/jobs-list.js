const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const STATUS_LABELS = {
    APPLIED: 'Applied',
    INTERVIEWING: 'Interviewing',
    OFFERED: 'Offered',
    REJECTED: 'Rejected',
    GHOSTED: 'Ghosted',
    WITHDRAWN: 'Withdrawn'
};
const SOURCE_LABELS = {
    LINKEDIN: 'LinkedIn',
    ITVIEC: 'ITViec',
    TOPCV: 'TopCV',
    VIETNAMWORKS: 'VietnamWorks',
    CAREERVIET: 'CareerViet',
    COMPANY_WEBSITE: 'Company Site',
    REFERRAL: 'Referral',
    FACEBOOK: 'Facebook',
    OTHER: 'Other'
};

let currentPage = 0;
let totalPages = 0;
let currentStatus = '';
let currentKeyword = '';
let debounceTimer = null;
let editingJobId = null;
let companies = [];

document.addEventListener('DOMContentLoaded', () => {
    loadCompanies();
    loadJobs();
    loadStatusCounts();
    setupSearch();
});

// ── LOAD ──────────────────────────────
async function loadJobs(page = 0) {
    showSkeleton();
    currentPage = page;

    let url = `/api/v1/jobs?page=${page}&size=10`;
    if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;
    if (currentStatus) url += `&status=${currentStatus}`;

    const res = await Auth.apiFetch(url);
    if (!res) return;
    const data = await res.json();

    renderTable(data.data || []);
    renderPagination(data.metadata);
}

async function loadStatusCounts() {
    const res = await Auth.apiFetch('/api/v1/stats/jobs');
    if (!res) return;
    const data = await res.json();
    if (!data.data?.byStatus) return;

    const counts = data.data.byStatus;
    const total = Object.values(counts).reduce((a, b) => a + b, 0);

    document.getElementById('chip-all-count').textContent = total;

    Object.entries(counts).forEach(([status, count]) => {
        const el = document.getElementById(`chip-${status.toLowerCase()}-count`);
        if (el) el.textContent = count;
    });
}

async function loadCompanies() {
    const res = await Auth.apiFetch('/api/v1/companies?size=100');
    if (!res) return;
    const data = await res.json();
    companies = data.data || [];

    const sel = document.getElementById('j-company');
    sel.innerHTML = '<option value="">No company</option>';
    companies.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.name;
        sel.appendChild(opt);
    });
}

// ── RENDER TABLE ──────────────────────
function renderTable(jobs) {
    const tbody = document.getElementById('jobs-tbody');

    if (!jobs.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6">
                    <div class="jobs-empty">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                        </svg>
                        <strong>No applications found</strong>
                        <span>${currentKeyword || currentStatus ? 'Try adjusting your filters' : 'Add your first job application'}</span>
                    </div>
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = jobs.map(j => {
        const d = j.appliedDate ? new Date(j.appliedDate) : null;
        const dateStr = d ? `${MONTHS[d.getMonth()]} ${d.getDate()}` : '—';
        const company = j.companyName || '<span style="color:var(--text3)">—</span>';

        return `
            <tr onclick="goToDetail('${j.id}')">
                <td>
                    <div class="job-position">${j.position}</div>
                    <div class="job-company">${company}</div>
                </td>
                <td>
                    <span class="badge ${j.status}">
                        <span class="badge-dot"></span>
                        ${STATUS_LABELS[j.status] || j.status}
                    </span>
                </td>
                <td><span class="priority ${j.priority}">${j.priority}</span></td>
                <td>${j.source ? (SOURCE_LABELS[j.source] || j.source) : '—'}</td>
                <td class="date-cell">${dateStr}</td>
                <td onclick="event.stopPropagation()">
                    <div class="row-actions">
                        <button class="icon-btn" onclick="openEditJob('${j.id}')" title="Edit">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                            </svg>
                        </button>
                        <button class="icon-btn danger" onclick="deleteJob('${j.id}', '${j.position.replace(/'/g, "\\'")}')" title="Delete">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                            </svg>
                        </button>
                    </div>
                </td>
            </tr>`;
    }).join('');
}

function showSkeleton() {
    const tbody = document.getElementById('jobs-tbody');
    tbody.innerHTML = Array(5).fill(`
        <tr>
            <td><div class="skel" style="width:60%;height:14px;margin-bottom:5px"></div><div class="skel" style="width:40%;height:11px"></div></td>
            <td><div class="skel" style="width:80px;height:20px;border-radius:5px"></div></td>
            <td><div class="skel" style="width:50px;height:13px"></div></td>
            <td><div class="skel" style="width:70px;height:13px"></div></td>
            <td><div class="skel" style="width:55px;height:13px"></div></td>
            <td></td>
        </tr>`).join('');
}

// ── PAGINATION ────────────────────────
function renderPagination(meta) {
    if (!meta) return;
    totalPages = meta.totalPages;

    document.getElementById('page-info').textContent =
        `${meta.totalElements} jobs · Page ${meta.page + 1} of ${Math.max(meta.totalPages, 1)}`;

    const wrap = document.getElementById('page-btns');
    wrap.innerHTML = '';

    const addBtn = (label, page, disabled, active) => {
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (active ? ' active' : '');
        btn.textContent = label;
        btn.disabled = disabled;
        btn.onclick = () => loadJobs(page);
        wrap.appendChild(btn);
    };

    addBtn('←', currentPage - 1, currentPage === 0, false);

    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages - 1, currentPage + 2);
    for (let i = start; i <= end; i++) {
        addBtn(i + 1, i, false, i === currentPage);
    }

    addBtn('→', currentPage + 1, currentPage >= totalPages - 1, false);
}

// ── SEARCH & FILTER ───────────────────
function setupSearch() {
    const input = document.getElementById('search-input');
    input.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            currentKeyword = input.value.trim();
            currentPage = 0;
            loadJobs(0);
        }, 350);
    });
}

function filterStatus(status) {
    currentStatus = status;
    currentPage = 0;

    document.querySelectorAll('.stat-chip').forEach(c => c.classList.remove('active'));
    document.getElementById(`chip-${status ? status.toLowerCase() : 'all'}`).classList.add('active');

    const sel = document.getElementById('status-filter');
    if (sel) sel.value = status;

    loadJobs(0);
}

// ── NAVIGATION ────────────────────────
function goToDetail(id) {
    window.location.href = `/jobs/${id}`;
}

// ── ADD / EDIT JOB ────────────────────
function openAddJob() {
    editingJobId = null;
    document.getElementById('modal-title').textContent = 'Add application';
    document.getElementById('job-form').reset();
    document.getElementById('j-priority').value = 'MEDIUM';
    document.getElementById('j-source').value = 'OTHER';
    document.getElementById('j-currency').value = 'VND';
    clearError();
    document.getElementById('job-modal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

async function openEditJob(id) {
    const res = await Auth.apiFetch(`/api/v1/jobs/${id}`);
    if (!res) return;
    const data = await res.json();
    const j = data.data;
    if (!j) return;

    editingJobId = id;
    document.getElementById('modal-title').textContent = 'Edit application';

    document.getElementById('j-position').value = j.position || '';
    document.getElementById('j-company').value = j.companyId || '';
    document.getElementById('j-source').value = j.source || 'OTHER';
    document.getElementById('j-priority').value = j.priority || 'MEDIUM';
    document.getElementById('j-salary-min').value = j.salaryMin || '';
    document.getElementById('j-salary-max').value = j.salaryMax || '';
    document.getElementById('j-currency').value = j.currency || 'VND';
    document.getElementById('j-url').value = j.jobUrl || '';
    document.getElementById('j-remote').checked = j.isRemote || false;
    document.getElementById('j-applied').value = j.appliedDate || '';
    document.getElementById('j-notes').value = j.notes || '';

    clearError();
    document.getElementById('job-modal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

async function saveJob() {
    const btn = document.getElementById('btn-save-job');
    const position = document.getElementById('j-position').value.trim();

    if (!position) {
        showError('Position is required');
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Saving...';

    const payload = {
        position,
        companyId: document.getElementById('j-company').value || null,
        source: document.getElementById('j-source').value,
        priority: document.getElementById('j-priority').value,
        salaryMin: parseInt(document.getElementById('j-salary-min').value) || null,
        salaryMax: parseInt(document.getElementById('j-salary-max').value) || null,
        currency: document.getElementById('j-currency').value || 'VND',
        jobUrl: document.getElementById('j-url').value || null,
        isRemote: document.getElementById('j-remote').checked,
        appliedDate: document.getElementById('j-applied').value || null,
        notes: document.getElementById('j-notes').value || null,
        jobDescription: null,
    };

    const url = editingJobId ? `/api/v1/jobs/${editingJobId}` : '/api/v1/jobs';
    const method = editingJobId ? 'PUT' : 'POST';

    const res = await Auth.apiFetch(url, {method, body: JSON.stringify(payload)});
    btn.disabled = false;
    btn.textContent = 'Save';

    if (!res || !res.ok) {
        const err = await res?.json();
        showError(err?.detail || 'Failed to save');
        return;
    }

    closeModal();
    loadJobs(currentPage);
    loadStatusCounts();
}

async function deleteJob(id, position) {
    if (!confirm(`Delete "${position}"?`)) return;
    const res = await Auth.apiFetch(`/api/v1/jobs/${id}`, {method: 'DELETE'});
    if (res && res.ok) {
        loadJobs(currentPage);
        loadStatusCounts();
    }
}

function closeModal() {
    document.getElementById('job-modal').classList.remove('open');
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

document.getElementById('job-modal').addEventListener('click', e => {
    if (e.target === document.getElementById('job-modal')) closeModal();
});