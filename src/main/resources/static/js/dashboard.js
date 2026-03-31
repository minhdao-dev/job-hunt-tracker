const STATUS_COLORS = {
    APPLIED:      '#a5b4fc',
    INTERVIEWING: '#fcd34d',
    OFFERED:      '#86efac',
    REJECTED:     '#fca5a5',
    GHOSTED:      '#9ca3af',
    WITHDRAWN:    '#6b7280',
};

const STATUS_LABELS = {
    APPLIED:      'Applied',
    INTERVIEWING: 'Interviewing',
    OFFERED:      'Offered',
    REJECTED:     'Rejected',
    GHOSTED:      'Ghosted',
    WITHDRAWN:    'Withdrawn',
};

let donutChart = null;
let lineChart  = null;

async function loadDashboard() {
    try {
        const [overviewRes, jobsRes, remindersRes] = await Promise.all([
            Auth.apiFetch('/api/v1/stats/overview'),
            Auth.apiFetch('/api/v1/jobs?page=0&size=5'),
            Auth.apiFetch('/api/v1/jobs?page=0&size=100'),
        ]);

        if (!overviewRes || !jobsRes) return;

        const overview   = await overviewRes.json();
        const jobsData   = await jobsRes.json();

        renderMetrics(overview.data);
        renderRecentJobs(jobsData.data || []);
        await loadJobStats();
        await loadReminders();
        setGreeting();
    } catch (e) {
        console.error('Dashboard load error:', e);
    }
}

function setGreeting() {
    const hour = new Date().getHours();
    const greet = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    const name = document.getElementById('user-name')?.textContent || '';
    const firstName = name.split(' ').pop() || 'there';
    document.getElementById('greeting-hello').textContent = `${greet}, ${firstName} 👋`;

    const days = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
    const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    const now = new Date();
    document.getElementById('greeting-sub').textContent =
        `${days[now.getDay()]}, ${months[now.getMonth()]} ${now.getDate()} · Here's your job search overview`;
}

function renderMetrics(data) {
    if (!data) return;

    document.getElementById('m-total').textContent   = data.totalJobs ?? 0;
    document.getElementById('m-active').textContent  = data.activeJobs ?? 0;
    document.getElementById('m-interviews').textContent = data.totalInterviews ?? 0;
    document.getElementById('m-offers').textContent  = data.totalOffers ?? 0;

    document.getElementById('m-response').textContent  = `${data.responseRate ?? 0}% response rate`;
    document.getElementById('m-offer-rate').textContent = `${data.offerRate ?? 0}% offer rate`;
    document.getElementById('m-reminders').textContent  = `${data.pendingReminders ?? 0} pending`;
}

async function loadJobStats() {
    const res = await Auth.apiFetch('/api/v1/stats/jobs');
    if (!res) return;
    const data = await res.json();
    renderDonut(data.data);
}

function renderDonut(data) {
    if (!data?.byStatus) return;

    const entries = Object.entries(data.byStatus).filter(([, v]) => v > 0);
    if (!entries.length) return;

    const labels = entries.map(([k]) => STATUS_LABELS[k] || k);
    const values = entries.map(([, v]) => v);
    const colors = entries.map(([k]) => STATUS_COLORS[k] || '#888');
    const total  = values.reduce((a, b) => a + b, 0);

    document.getElementById('donut-total').textContent = total;

    const legend = document.getElementById('donut-legend');
    legend.innerHTML = entries.map(([k, v]) => `
        <div class="legend-item">
            <div class="legend-dot" style="background:${STATUS_COLORS[k]}"></div>
            <span>${STATUS_LABELS[k] || k} (${v})</span>
        </div>
    `).join('');

    const ctx = document.getElementById('donut-chart').getContext('2d');

    if (donutChart) donutChart.destroy();

    donutChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data: values,
                backgroundColor: colors,
                borderColor: 'transparent',
                borderWidth: 0,
                hoverOffset: 6,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '72%',
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#0e0e1a',
                    titleColor: '#f0efff',
                    bodyColor: '#7b7a9e',
                    borderColor: '#1c1c30',
                    borderWidth: 1,
                    padding: 10,
                    callbacks: {
                        label: ctx => ` ${ctx.parsed} jobs (${Math.round(ctx.parsed / total * 100)}%)`
                    }
                }
            },
            animation: { animateScale: true, duration: 600 }
        }
    });
}

async function loadReminders() {
    const res = await Auth.apiFetch('/api/v1/jobs?page=0&size=50');
    if (!res) return;

    const jobsData = await res.json();
    const jobs = jobsData.data || [];

    const allReminders = [];

    await Promise.all(jobs.map(async job => {
        const r = await Auth.apiFetch(`/api/v1/jobs/${job.id}/reminders`);
        if (!r) return;
        const rd = await r.json();
        const list = rd.data || [];
        list.forEach(rem => {
            if (!rem.isSent) allReminders.push({ ...rem, jobPosition: job.position });
        });
    }));

    allReminders.sort((a, b) => new Date(a.remindAt) - new Date(b.remindAt));
    renderReminders(allReminders.slice(0, 6));
}

function renderReminders(list) {
    const container = document.getElementById('reminder-list');
    if (!list.length) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">🔔</div>
                <div>No pending reminders</div>
            </div>`;
        return;
    }

    const now = new Date();
    container.innerHTML = list.map(r => {
        const dt   = new Date(r.remindAt);
        const over = dt < now;
        const diff = Math.abs(now - dt);
        const days = Math.floor(diff / 86400000);
        const hrs  = Math.floor((diff % 86400000) / 3600000);
        const timeStr = over
            ? (days > 0 ? `${days}d overdue` : `${hrs}h overdue`)
            : (days > 0 ? `in ${days}d`      : `in ${hrs}h`);

        return `
            <div class="reminder-item">
                <div class="reminder-pos">${r.jobPosition}</div>
                <div class="reminder-msg">${r.message || 'Follow up'}</div>
                <div class="reminder-time ${over ? 'overdue' : ''}">${timeStr}</div>
            </div>`;
    }).join('');
}

function renderRecentJobs(jobs) {
    const tbody = document.getElementById('jobs-tbody');
    if (!jobs.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="empty-state">
                    <div class="icon">📋</div>
                    <div>No job applications yet</div>
                    <a href="/jobs" style="color:var(--accent2);font-size:12px;margin-top:6px;display:inline-block;">Add your first job →</a>
                </td>
            </tr>`;
        return;
    }

    const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    tbody.innerHTML = jobs.map(j => {
        const d = j.appliedDate ? new Date(j.appliedDate) : null;
        const dateStr = d ? `${months[d.getMonth()]} ${d.getDate()}` : '—';

        return `
            <tr>
                <td>
                    <div style="font-weight:500">${j.position}</div>
                    <div class="company">${j.companyName || '—'}</div>
                </td>
                <td>
                    <span class="badge ${j.status}">
                        <span class="badge-dot"></span>
                        ${STATUS_LABELS[j.status] || j.status}
                    </span>
                </td>
                <td><span class="priority ${j.priority}">${j.priority}</span></td>
                <td style="color:var(--muted);font-family:'IBM Plex Mono',monospace;font-size:12px">${dateStr}</td>
            </tr>`;
    }).join('');
}

document.addEventListener('DOMContentLoaded', loadDashboard);