const JOB_ID = window.JOB_ID;
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const STATUS_LABELS = {
    APPLIED: 'Applied',
    INTERVIEWING: 'Interviewing',
    OFFERED: 'Offered',
    REJECTED: 'Rejected',
    GHOSTED: 'Ghosted',
    WITHDRAWN: 'Withdrawn'
};
const INTERVIEW_TYPE_LABELS = {
    PHONE_SCREENING: 'Phone Screen',
    TECHNICAL: 'Technical',
    ONLINE: 'Online',
    ONSITE: 'Onsite',
    HR: 'HR',
    FINAL: 'Final'
};

let currentJob = null;
let currentOffer = null;
let editingInterviewId = null;
let editingReminderId = null;

// ── INIT ──────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    if (!JOB_ID) {
        console.error('JOB_ID is missing');
        return;
    }
    loadAll();
    setupModalListeners();
});

async function loadAll() {
    await loadJob();
    await Promise.all([loadInterviews(), loadOffer(), loadReminders(), loadHistory()]);
}

// ── JOB ──────────────────────────────
async function loadJob() {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}`);
    if (!res || !res.ok) return;
    const data = await res.json();
    if (!data.data) return;
    currentJob = data.data;
    renderJob(currentJob);
}

function renderJob(job) {
    document.getElementById('detail-position').textContent = job.position;
    document.getElementById('detail-company').textContent = job.companyName || 'No company';
    document.getElementById('page-title').textContent = job.position;
    renderStatusBadges(job.status, job.priority);
    renderInfoPanel(job);
    renderStatusSelector(job.status);
}

function renderStatusBadges(status, priority) {
    const wrap = document.getElementById('detail-badges');
    wrap.innerHTML = `
        <span class="badge ${status}"><span class="badge-dot"></span>${STATUS_LABELS[status] || status}</span>
        <span class="priority ${priority}" style="font-size:11px;font-weight:700;font-family:'IBM Plex Mono',monospace">${priority}</span>
    `;
}

function renderInfoPanel(job) {
    const fmt = d => {
        if (!d) return '—';
        const date = new Date(d);
        return `${MONTHS[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
    };

    document.getElementById('info-applied').textContent = fmt(job.appliedDate);
    document.getElementById('info-source').textContent = job.source ? job.source.replace(/_/g, ' ') : '—';
    document.getElementById('info-remote').textContent = job.isRemote ? '🏠 Remote' : '🏢 Onsite';

    const salaryEl = document.getElementById('info-salary');
    if (job.salaryMin || job.salaryMax) {
        const parts = [job.salaryMin, job.salaryMax].filter(Boolean).map(n => n.toLocaleString());
        salaryEl.innerHTML = `<span style="font-family:'IBM Plex Mono',monospace;color:var(--success)">${parts.join(' – ')} ${job.currency || 'VND'}</span>`;
    } else {
        salaryEl.textContent = '—';
    }

    const urlEl = document.getElementById('info-url');
    urlEl.innerHTML = job.jobUrl
        ? `<a href="${job.jobUrl}" target="_blank" rel="noopener">View posting ↗</a>`
        : '<span style="color:var(--text3)">—</span>';

    const notesEl = document.getElementById('detail-notes');
    if (job.notes) {
        notesEl.textContent = job.notes;
        notesEl.classList.remove('empty');
    } else {
        notesEl.textContent = 'No notes added.';
        notesEl.classList.add('empty');
    }
}

function renderStatusSelector(currentStatus) {
    document.querySelectorAll('.status-option').forEach(btn => {
        btn.classList.toggle('current', btn.dataset.status === currentStatus);
    });
}

// ── STATUS CHANGE ─────────────────────
async function changeStatus(newStatus) {
    if (!currentJob || currentJob.status === newStatus) return;

    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/status`, {
        method: 'PATCH',
        body: JSON.stringify({status: newStatus, note: null})
    });
    if (!res || !res.ok) return;
    await loadJob();
    await loadHistory();
}

// ── INTERVIEWS ───────────────────────
async function loadInterviews() {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/interviews`);
    if (!res || !res.ok) return;
    const data = await res.json();
    const list = data.data || [];
    renderInterviews(list);
    updateTabCount('tab-interviews', list.length);
}

function renderInterviews(list) {
    const container = document.getElementById('interview-list');
    const empty = document.getElementById('interview-empty');
    container.querySelectorAll('.interview-card').forEach(c => c.remove());

    if (!list.length) {
        empty.style.display = 'flex';
        return;
    }
    empty.style.display = 'none';

    const fmt = dt => {
        const d = new Date(dt);
        return `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()} · ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    };

    list.forEach(iv => {
        const card = document.createElement('div');
        card.className = 'interview-card';
        card.innerHTML = `
            <div class="interview-card-head">
                <span class="interview-round">Round ${iv.round} · ${INTERVIEW_TYPE_LABELS[iv.interviewType] || iv.interviewType}</span>
                <div class="interview-card-actions">
                    <span class="result-badge ${iv.result}">${iv.result}</span>
                    <button class="icon-btn" onclick="openEditInterview('${iv.id}')" title="Edit">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                    </button>
                    <button class="icon-btn danger" onclick="deleteInterview('${iv.id}')" title="Delete">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                </div>
            </div>
            <div class="interview-meta">
                <span class="meta-item">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                    ${fmt(iv.scheduledAt)}
                </span>
                <span class="meta-item">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                    ${iv.durationMinutes} min
                </span>
                ${iv.location ? `<span class="meta-item"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>${iv.location}</span>` : ''}
            </div>
            ${(iv.preparationNote || iv.questionsAsked || iv.feedback) ? `
            <div class="interview-notes">
                ${iv.preparationNote ? `<div><strong>Prep:</strong> ${iv.preparationNote}</div>` : ''}
                ${iv.questionsAsked ? `<div style="margin-top:4px"><strong>Q:</strong> ${iv.questionsAsked}</div>` : ''}
                ${iv.myAnswers ? `<div style="margin-top:4px"><strong>A:</strong> ${iv.myAnswers}</div>` : ''}
                ${iv.feedback ? `<div style="margin-top:4px"><strong>Feedback:</strong> ${iv.feedback}</div>` : ''}
            </div>` : ''}
        `;
        container.appendChild(card);
    });
}

async function saveInterview() {
    const btn = document.getElementById('btn-save-interview');
    btn.disabled = true;
    btn.textContent = 'Saving...';
    clearModalError('interview-modal');

    const scheduledAt = document.getElementById('iv-scheduled').value;
    if (!scheduledAt) {
        showModalError('interview-modal', 'Scheduled time is required');
        btn.disabled = false;
        btn.textContent = 'Save';
        return;
    }

    const payload = {
        round: parseInt(document.getElementById('iv-round').value) || 1,
        interviewType: document.getElementById('iv-type').value,
        scheduledAt: scheduledAt,
        durationMinutes: parseInt(document.getElementById('iv-duration').value) || 60,
        location: document.getElementById('iv-location').value || null,
        preparationNote: document.getElementById('iv-prep').value || null,
        questionsAsked: document.getElementById('iv-questions').value || null,
        myAnswers: document.getElementById('iv-answers').value || null,
        feedback: document.getElementById('iv-feedback').value || null,
        result: document.getElementById('iv-result').value || 'PENDING',
    };

    const url = editingInterviewId ? `/api/v1/jobs/${JOB_ID}/interviews/${editingInterviewId}` : `/api/v1/jobs/${JOB_ID}/interviews`;
    const method = editingInterviewId ? 'PUT' : 'POST';
    const res = await Auth.apiFetch(url, {method, body: JSON.stringify(payload)});

    btn.disabled = false;
    btn.textContent = 'Save';
    if (!res || !res.ok) {
        const err = await res?.json().catch(() => ({}));
        showModalError('interview-modal', err.detail || 'Failed to save interview');
        return;
    }
    closeModal('interview-modal');
    await loadInterviews();
}

async function openEditInterview(id) {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/interviews/${id}`);
    if (!res || !res.ok) return;
    const {data: iv} = await res.json();
    editingInterviewId = id;

    document.getElementById('interview-modal-title').textContent = 'Edit Interview';
    document.getElementById('iv-round').value = iv.round || 1;
    document.getElementById('iv-type').value = iv.interviewType || 'TECHNICAL';
    document.getElementById('iv-scheduled').value = iv.scheduledAt ? iv.scheduledAt.slice(0, 16) : '';
    document.getElementById('iv-duration').value = iv.durationMinutes || 60;
    document.getElementById('iv-location').value = iv.location || '';
    document.getElementById('iv-prep').value = iv.preparationNote || '';
    document.getElementById('iv-questions').value = iv.questionsAsked || '';
    document.getElementById('iv-answers').value = iv.myAnswers || '';
    document.getElementById('iv-feedback').value = iv.feedback || '';
    document.getElementById('iv-result').value = iv.result || 'PENDING';
    openModal('interview-modal');
}

async function deleteInterview(id) {
    if (!confirm('Delete this interview round?')) return;
    await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/interviews/${id}`, {method: 'DELETE'});
    await loadInterviews();
}

// ── OFFER ────────────────────────────
async function loadOffer() {
    try {
        const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/offer`);
        if (!res) return;

        if (res.status === 404 || !res.ok) {
            currentOffer = null;
            renderOffer(null);
            return;
        }

        const data = await res.json();
        currentOffer = data.data || null;
        renderOffer(currentOffer);
        updateTabCount('tab-offer', currentOffer ? 1 : 0);
    } catch {
        renderOffer(null);
    }
}

function renderOffer(offer) {
    const empty = document.getElementById('offer-empty');
    const card = document.getElementById('offer-card');
    const addBtn = document.getElementById('btn-add-offer');

    if (!offer) {
        empty.style.display = 'flex';
        card.style.display = 'none';
        if (addBtn) addBtn.style.display = 'flex';
        return;
    }

    empty.style.display = 'none';
    card.style.display = 'block';
    if (addBtn) addBtn.style.display = 'none';

    const fmt = d => d ? `${MONTHS[new Date(d).getMonth()]} ${new Date(d).getDate()}, ${new Date(d).getFullYear()}` : '—';

    // salary field từ OfferResponse DTO
    document.getElementById('offer-salary').textContent = offer.salary ? offer.salary.toLocaleString() : '—';
    document.getElementById('offer-currency').textContent = offer.currency || 'VND';
    document.getElementById('offer-start').textContent = fmt(offer.startDate);
    document.getElementById('offer-expires').textContent = fmt(offer.expiredAt);

    // benefits có thể là string JSON hoặc string thường
    let benefitsText = '—';
    if (offer.benefits) {
        try {
            const parsed = JSON.parse(offer.benefits);
            benefitsText = Array.isArray(parsed) ? parsed.join(', ') : String(parsed);
        } catch {
            benefitsText = offer.benefits;
        }
    }
    const benefitsEl = document.getElementById('offer-benefits');
    benefitsEl.textContent = benefitsText;
    benefitsEl.style.fontStyle = offer.benefits ? 'normal' : 'italic';

    document.getElementById('offer-note').textContent = offer.note || '—';

    document.querySelectorAll('.decision-btn').forEach(btn => {
        btn.classList.toggle('current', btn.classList.contains(offer.decision));
    });
}

async function saveOffer() {
    const btn = document.getElementById('btn-save-offer');
    btn.disabled = true;
    btn.textContent = 'Saving...';
    clearModalError('offer-modal');

    const payload = {
        salary: parseInt(document.getElementById('of-salary').value) || null,
        currency: document.getElementById('of-currency').value || 'VND',
        benefits: document.getElementById('of-benefits').value || null,
        startDate: document.getElementById('of-start').value || null,
        expiredAt: document.getElementById('of-expires').value || null,
        note: document.getElementById('of-note').value || null,
    };

    const url = `/api/v1/jobs/${JOB_ID}/offer`;
    const method = currentOffer ? 'PUT' : 'POST';
    const res = await Auth.apiFetch(url, {method, body: JSON.stringify(payload)});

    btn.disabled = false;
    btn.textContent = 'Save';
    if (!res || !res.ok) {
        const err = await res?.json().catch(() => ({}));
        showModalError('offer-modal', err.detail || 'Failed to save offer');
        return;
    }
    closeModal('offer-modal');
    await loadOffer();
}

async function updateOfferDecision(decision) {
    if (!currentOffer || currentOffer.decision === decision) return;
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/offer/decision`, {
        method: 'PATCH',
        body: JSON.stringify({decision})
    });
    if (res && res.ok) await loadOffer();
}

// ── REMINDERS ────────────────────────
async function loadReminders() {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/reminders`);
    if (!res || !res.ok) return;
    const data = await res.json();
    const list = data.data || [];
    renderReminders(list);
    updateTabCount('tab-reminders', list.filter(r => !r.isSent).length);
}

function renderReminders(list) {
    const container = document.getElementById('reminder-list');
    const empty = document.getElementById('reminder-empty');
    container.querySelectorAll('.reminder-card').forEach(c => c.remove());

    if (!list.length) {
        empty.style.display = 'flex';
        return;
    }
    empty.style.display = 'none';

    const now = new Date();
    list.forEach(r => {
        const dt = new Date(r.remindAt);
        const over = dt < now;
        const diff = Math.abs(now - dt);
        const days = Math.floor(diff / 86400000);
        const hrs = Math.floor((diff % 86400000) / 3600000);

        let timeStr, timeClass;
        if (r.isSent) {
            timeStr = 'Sent';
            timeClass = 'sent-lbl';
        } else if (over) {
            timeStr = days > 0 ? `${days}d overdue` : `${hrs}h overdue`;
            timeClass = 'overdue';
        } else {
            timeStr = days > 0 ? `in ${days}d` : hrs > 0 ? `in ${hrs}h` : 'soon';
            timeClass = 'upcoming';
        }

        const card = document.createElement('div');
        card.className = `reminder-card${r.isSent ? ' sent' : ''}`;
        card.innerHTML = `
            <span class="reminder-time-badge ${timeClass}">${timeStr}</span>
            <span class="reminder-msg-text${!r.message ? ' empty' : ''}">${r.message || 'No message'}</span>
            <div style="display:flex;gap:4px;flex-shrink:0">
                ${!r.isSent ? `<button class="icon-btn" onclick="openEditReminder('${r.id}')" title="Edit">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                </button>` : ''}
                <button class="icon-btn danger" onclick="deleteReminder('${r.id}')" title="Delete">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                </button>
            </div>
        `;
        container.appendChild(card);
    });
}

async function saveReminder() {
    const btn = document.getElementById('btn-save-reminder');
    btn.disabled = true;
    btn.textContent = 'Saving...';
    clearModalError('reminder-modal');

    const remindAt = document.getElementById('rm-time').value;
    if (!remindAt) {
        showModalError('reminder-modal', 'Remind time is required');
        btn.disabled = false;
        btn.textContent = 'Save';
        return;
    }

    const payload = {remindAt, message: document.getElementById('rm-message').value || null};
    const url = editingReminderId ? `/api/v1/jobs/${JOB_ID}/reminders/${editingReminderId}` : `/api/v1/jobs/${JOB_ID}/reminders`;
    const method = editingReminderId ? 'PUT' : 'POST';
    const res = await Auth.apiFetch(url, {method, body: JSON.stringify(payload)});

    btn.disabled = false;
    btn.textContent = 'Save';
    if (!res || !res.ok) {
        const err = await res?.json().catch(() => ({}));
        showModalError('reminder-modal', err.detail || 'Failed to save reminder');
        return;
    }
    closeModal('reminder-modal');
    await loadReminders();
}

async function openEditReminder(id) {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/reminders`);
    if (!res || !res.ok) return;
    const {data} = await res.json();
    const r = (data || []).find(x => x.id === id);
    if (!r) return;

    editingReminderId = id;
    document.getElementById('reminder-modal-title').textContent = 'Edit Reminder';
    document.getElementById('rm-time').value = r.remindAt ? r.remindAt.slice(0, 16) : '';
    document.getElementById('rm-message').value = r.message || '';
    openModal('reminder-modal');
}

async function deleteReminder(id) {
    if (!confirm('Delete this reminder?')) return;
    await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/reminders/${id}`, {method: 'DELETE'});
    await loadReminders();
}

// ── HISTORY ──────────────────────────
async function loadHistory() {
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}/status-history`);
    if (!res || !res.ok) return;
    const data = await res.json();
    renderHistory(data.data || []);
}

function renderHistory(list) {
    const container = document.getElementById('history-timeline');
    container.innerHTML = '';

    if (!list.length) {
        container.innerHTML = `<div class="tab-empty"><strong>No history yet</strong></div>`;
        return;
    }

    const fmt = dt => {
        const d = new Date(dt);
        return `${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
    };

    [...list].reverse().forEach(h => {
        const item = document.createElement('div');
        item.className = 'history-item';
        item.innerHTML = `
            <div class="history-dot"></div>
            <div class="history-meta">
                <div class="history-arrow">
                    ${h.oldStatus ? `<span class="badge ${h.oldStatus}" style="font-size:9px;padding:2px 6px">${STATUS_LABELS[h.oldStatus]}</span>
                    <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>` : ''}
                    <span class="badge ${h.newStatus}" style="font-size:9px;padding:2px 6px">${STATUS_LABELS[h.newStatus]}</span>
                </div>
                <span class="history-time">${fmt(h.changedAt)}</span>
            </div>
            ${h.note ? `<div class="history-note">"${h.note}"</div>` : ''}
        `;
        container.appendChild(item);
    });
}

// ── EDIT JOB ─────────────────────────
function openEditJob() {
    if (!currentJob) return;
    document.getElementById('ej-position').value = currentJob.position || '';
    document.getElementById('ej-source').value = currentJob.source || 'OTHER';
    document.getElementById('ej-priority').value = currentJob.priority || 'MEDIUM';
    document.getElementById('ej-salary-min').value = currentJob.salaryMin || '';
    document.getElementById('ej-salary-max').value = currentJob.salaryMax || '';
    document.getElementById('ej-currency').value = currentJob.currency || 'VND';
    document.getElementById('ej-url').value = currentJob.jobUrl || '';
    document.getElementById('ej-remote').checked = currentJob.isRemote || false;
    document.getElementById('ej-notes').value = currentJob.notes || '';
    openModal('edit-job-modal');
}

async function saveJob() {
    const btn = document.getElementById('btn-save-job');
    const position = document.getElementById('ej-position').value.trim();
    if (!position) {
        showModalError('edit-job-modal', 'Position is required');
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Saving...';
    clearModalError('edit-job-modal');

    const payload = {
        position,
        companyId: currentJob.companyId || null,
        source: document.getElementById('ej-source').value,
        priority: document.getElementById('ej-priority').value,
        salaryMin: parseInt(document.getElementById('ej-salary-min').value) || null,
        salaryMax: parseInt(document.getElementById('ej-salary-max').value) || null,
        currency: document.getElementById('ej-currency').value || 'VND',
        jobUrl: document.getElementById('ej-url').value || null,
        isRemote: document.getElementById('ej-remote').checked,
        appliedDate: currentJob.appliedDate || null,
        jobDescription: currentJob.jobDescription || null,
        notes: document.getElementById('ej-notes').value || null,
    };

    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}`, {method: 'PUT', body: JSON.stringify(payload)});
    btn.disabled = false;
    btn.textContent = 'Save';

    if (!res || !res.ok) {
        const err = await res?.json().catch(() => ({}));
        showModalError('edit-job-modal', err.detail || 'Failed to update');
        return;
    }
    closeModal('edit-job-modal');
    await loadJob();
}

async function deleteJob() {
    if (!confirm(`Delete "${currentJob?.position}"? This cannot be undone.`)) return;
    const res = await Auth.apiFetch(`/api/v1/jobs/${JOB_ID}`, {method: 'DELETE'});
    if (res && res.ok) window.location.href = '/jobs';
}

// ── TABS ──────────────────────────────
function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.getElementById('tab-btn-' + tabId).classList.add('active');
    document.getElementById('tab-' + tabId).classList.add('active');
}

function updateTabCount(tabId, count) {
    const el = document.getElementById(tabId + '-count');
    if (el) el.textContent = count > 0 ? count : '';
}

// ── MODAL HELPERS ─────────────────────
function openModal(id) {
    document.getElementById(id).classList.add('open');
    document.body.style.overflow = 'hidden';

    if (id === 'interview-modal' && !editingInterviewId) {
        document.getElementById('interview-modal-title').textContent = 'Add Interview';
        document.getElementById('interview-modal-form').reset();
        document.getElementById('iv-round').value = 1;
        document.getElementById('iv-type').value = 'TECHNICAL';
        document.getElementById('iv-duration').value = 60;
        document.getElementById('iv-result').value = 'PENDING';
    }
    if (id === 'reminder-modal' && !editingReminderId) {
        document.getElementById('reminder-modal-title').textContent = 'Add Reminder';
        document.getElementById('reminder-modal-form').reset();
    }
    if (id === 'offer-modal' && currentOffer) {
        document.getElementById('of-salary').value = currentOffer.salary || '';
        document.getElementById('of-currency').value = currentOffer.currency || 'VND';
        document.getElementById('of-benefits').value = currentOffer.benefits || '';
        document.getElementById('of-start').value = currentOffer.startDate || '';
        document.getElementById('of-expires').value = currentOffer.expiredAt || '';
        document.getElementById('of-note').value = currentOffer.note || '';
    }
}

function closeModal(id) {
    document.getElementById(id).classList.remove('open');
    document.body.style.overflow = '';
    if (id === 'interview-modal') editingInterviewId = null;
    if (id === 'reminder-modal') editingReminderId = null;
    clearModalError(id);
}

function showModalError(modalId, msg) {
    const el = document.getElementById(modalId + '-error');
    if (el) {
        el.textContent = msg;
        el.style.display = 'block';
    }
}

function clearModalError(modalId) {
    const el = document.getElementById(modalId + '-error');
    if (el) {
        el.textContent = '';
        el.style.display = 'none';
    }
}

// Setup event listeners sau khi DOM ready
function setupModalListeners() {
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay.open').forEach(m => closeModal(m.id));
        }
    });

    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', e => {
            if (e.target === overlay) closeModal(overlay.id);
        });
    });
}