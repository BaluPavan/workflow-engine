const DEMO_WORKFLOW = {
  name: 'order-fulfillment',
  description: 'Validate, charge, reserve, ship, and notify',
  steps: [
    { stepName: 'validate_order', stepOrder: 1, critical: true, maxRetries: 3, timeoutSeconds: 30 },
    { stepName: 'charge_payment', stepOrder: 2, critical: true, maxRetries: 3, timeoutSeconds: 30 },
    { stepName: 'reserve_inventory', stepOrder: 3, critical: false, maxRetries: 1, timeoutSeconds: 30 },
    { stepName: 'ship_order', stepOrder: 4, critical: true, maxRetries: 3, timeoutSeconds: 30 },
    { stepName: 'send_notification', stepOrder: 5, critical: false, maxRetries: 1, timeoutSeconds: 30 },
  ],
};

const SCENARIOS = {
  happy: { orderId: 'ORD-123', amount: 99.99 },
  skip: { orderId: 'ORD-456', simulateFailure: 'reserve_inventory' },
  retry: { orderId: 'ORD-789', simulateFailure: 'charge_payment' },
};

let selectedId = null;
let pollTimer = null;

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const error = new Error(body?.error || response.statusText);
    error.status = response.status;
    error.body = body;
    throw error;
  }
  return body;
}

function statusBadgeClass(status) {
  switch (status) {
    case 'RUNNING': return 'badge-running';
    case 'COMPLETED': return 'badge-completed';
    case 'FAILED': return 'badge-failed';
    case 'RETRYING': return 'badge-retrying';
    default: return 'badge-pending';
  }
}

function formatTime(value) {
  if (!value) return '—';
  return new Date(value).toLocaleTimeString();
}

function shortId(id) {
  return id ? `${id.slice(0, 8)}…` : '';
}

async function checkHealth() {
  try {
    const health = await api('/actuator/health');
    $('health-badge').textContent = health.status === 'UP' ? 'Engine UP' : 'Engine DOWN';
    $('health-badge').className = `badge ${health.status === 'UP' ? 'badge-completed' : 'badge-failed'}`;
  } catch {
    $('health-badge').textContent = 'Engine DOWN';
    $('health-badge').className = 'badge badge-failed';
  }
}

async function setupDemo() {
  const statusEl = $('setup-status');
  statusEl.textContent = 'Registering…';
  statusEl.className = 'status-msg';
  try {
    await api('/api/workflows', { method: 'POST', body: JSON.stringify(DEMO_WORKFLOW) });
    statusEl.textContent = 'Demo workflow registered.';
    statusEl.className = 'status-msg success';
  } catch (err) {
    if (err.status === 409) {
      statusEl.textContent = 'Demo workflow already registered.';
      statusEl.className = 'status-msg success';
      return;
    }
    statusEl.textContent = err.message;
    statusEl.className = 'status-msg error';
  }
}

async function loadInstanceList() {
  const instances = await api('/api/instances');
  const list = $('instance-list');
  list.innerHTML = '';

  if (!instances.length) {
    list.innerHTML = '<li class="hint">No instances yet.</li>';
    return;
  }

  instances.forEach((instance) => {
    const li = document.createElement('li');
    li.className = `instance-item${instance.id === selectedId ? ' active' : ''}`;
    li.innerHTML = `
      <div class="name">${instance.workflowName}</div>
      <div class="meta">${shortId(instance.id)} · ${instance.status}${instance.currentStepName ? ` · ${instance.currentStepName}` : ''}</div>
    `;
    li.onclick = () => selectInstance(instance.id);
    list.appendChild(li);
  });
}

async function selectInstance(id) {
  selectedId = id;
  await loadInstanceList();
  await renderDetail(id);
  schedulePoll();
}

async function renderDetail(id) {
  const instance = await api(`/api/instances/${id}`);

  $('empty-state').classList.add('hidden');
  $('detail').classList.remove('hidden');

  $('detail-title').textContent = instance.workflowName;
  $('detail-meta').textContent = `Instance ${instance.id}`;
  $('detail-status').textContent = instance.status;
  $('detail-status').className = `badge ${statusBadgeClass(instance.status)}`;

  let contextPretty = instance.context;
  try {
    contextPretty = JSON.stringify(JSON.parse(instance.context), null, 2);
  } catch { /* keep raw */ }
  $('detail-context').textContent = contextPretty;

  renderPipeline(instance.steps);
  renderStepsTable(instance.steps);

  if (!['RUNNING', 'RETRYING'].includes(instance.status)) {
    clearPoll();
  }
}

function renderPipeline(steps) {
  const pipeline = $('pipeline');
  pipeline.innerHTML = '';

  steps.forEach((step, index) => {
    if (index > 0) {
      const arrow = document.createElement('span');
      arrow.className = 'step-arrow';
      arrow.textContent = '→';
      pipeline.appendChild(arrow);
    }

    const card = document.createElement('div');
    card.className = `step-card ${step.status.toLowerCase()}`;
    card.innerHTML = `
      <div class="step-name">${step.stepName.replace(/_/g, ' ')}</div>
      <div class="step-status">${step.status}${step.retryCount ? ` (${step.retryCount})` : ''}</div>
    `;
    pipeline.appendChild(card);
  });
}

function renderStepsTable(steps) {
  const body = $('steps-body');
  body.innerHTML = steps.map((step) => `
    <tr>
      <td>${step.stepName}</td>
      <td><span class="badge ${statusBadgeClass(step.status)}">${step.status}</span></td>
      <td>${step.retryCount}</td>
      <td>${formatTime(step.startedAt)}</td>
      <td>${formatTime(step.completedAt)}</td>
      <td>${step.failureReason || '—'}</td>
    </tr>
  `).join('');
}

async function startInstance() {
  const scenario = $('scenario').value;
  const context = JSON.stringify(SCENARIOS[scenario]);

  const instance = await api('/api/workflows/order-fulfillment/instances', {
    method: 'POST',
    body: JSON.stringify({ context }),
  });

  await selectInstance(instance.id);
}

function schedulePoll() {
  clearPoll();
  pollTimer = setInterval(async () => {
    if (!selectedId) return;
    try {
      await renderDetail(selectedId);
      await loadInstanceList();
    } catch (err) {
      console.error(err);
    }
  }, 2000);
}

function clearPoll() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

$('btn-setup').addEventListener('click', setupDemo);
$('btn-start').addEventListener('click', async () => {
  $('btn-start').disabled = true;
  try {
    await startInstance();
  } catch (err) {
    alert(err.message || 'Failed to start instance. Register the demo workflow first.');
  } finally {
    $('btn-start').disabled = false;
  }
});

(async function init() {
  await checkHealth();
  await loadInstanceList();
  setInterval(checkHealth, 15000);
  setInterval(loadInstanceList, 5000);
})();
