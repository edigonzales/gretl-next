const state = {
  jobs: [],
  runs: [],
  workers: [],
  selectedRun: null
};

document.getElementById("refresh").addEventListener("click", refresh);

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || response.statusText);
  }
  const type = response.headers.get("content-type") || "";
  return type.includes("application/json") ? response.json() : response.text();
}

async function refresh() {
  const [jobs, runs, workers] = await Promise.all([
    api("/api/jobs"),
    api("/api/runs"),
    api("/api/workers")
  ]);
  state.jobs = jobs;
  state.runs = runs;
  state.workers = workers;
  renderJobs();
  renderRuns();
  renderWorkers();
}

function renderJobs() {
  const host = document.getElementById("jobs");
  host.innerHTML = "";
  state.jobs.forEach(job => {
    const item = document.createElement("div");
    item.className = "item";
    item.innerHTML = `
      <div class="item-title">
        <strong>${escapeHtml(job.name || job.id)}</strong>
        <span class="badge">${job.enabled === false ? "disabled" : "enabled"}</span>
      </div>
      <div class="meta">${escapeHtml(job.id)} · ${escapeHtml(job.projectDir)} · ${escapeHtml((job.tasks || []).join(" "))}</div>
      <div class="meta">${job.cron ? escapeHtml(job.cron) : "manual only"}</div>
    `;
    item.addEventListener("click", () => showJob(job));
    host.appendChild(item);
  });
}

function renderRuns() {
  const host = document.getElementById("runs");
  host.innerHTML = "";
  state.runs.forEach(run => {
    const item = document.createElement("div");
    item.className = "item";
    item.innerHTML = `
      <div class="item-title">
        <strong>${escapeHtml(run.jobId)}</strong>
        <span class="badge ${run.status.toLowerCase()}">${escapeHtml(run.status)}</span>
      </div>
      <div class="meta">${escapeHtml(run.id)}</div>
      <div class="meta">${escapeHtml(run.triggerType)} · ${escapeHtml(run.queuedAt || "")}</div>
    `;
    item.addEventListener("click", () => showRun(run.id));
    host.appendChild(item);
  });
}

function renderWorkers() {
  const host = document.getElementById("workers");
  host.innerHTML = "";
  state.workers.forEach(worker => {
    const item = document.createElement("div");
    item.className = "item";
    item.innerHTML = `
      <div class="item-title">
        <strong>${escapeHtml(worker.displayName || worker.id)}</strong>
        <span class="badge ${worker.status.toLowerCase()}">${escapeHtml(worker.status)}</span>
      </div>
      <div class="meta">${escapeHtml((worker.labels || []).join(", ")) || "no labels"}</div>
      <div class="meta">capacity ${worker.capacity}, active ${worker.activeRuns}</div>
    `;
    host.appendChild(item);
  });
}

function showJob(job) {
  state.selectedRun = null;
  document.getElementById("details").className = "details";
  document.getElementById("details").innerHTML = `
    <h2>${escapeHtml(job.name || job.id)}</h2>
    <p>${escapeHtml(job.id)}</p>
    <div class="meta">Project: ${escapeHtml(job.projectDir)}</div>
    <div class="meta">Tasks: ${escapeHtml((job.tasks || []).join(" "))}</div>
    <div class="meta">Labels: ${escapeHtml((job.workerLabels || []).join(", ")) || "any worker"}</div>
    <div class="meta">Secrets: ${escapeHtml((job.secretRefs || []).join(", ")) || "none"}</div>
    <div class="actions">
      <button id="start-run">Start</button>
    </div>
  `;
  document.getElementById("logs").textContent = "";
  document.getElementById("start-run").addEventListener("click", async () => {
    await api(`/api/jobs/${encodeURIComponent(job.id)}/runs`, {
      method: "POST",
      body: JSON.stringify({ parameters: {} })
    });
    await refresh();
  });
}

async function showRun(runId) {
  const run = await api(`/api/runs/${encodeURIComponent(runId)}`);
  state.selectedRun = run.id;
  document.getElementById("details").className = "details";
  document.getElementById("details").innerHTML = `
    <h2>${escapeHtml(run.jobId)}</h2>
    <p>${escapeHtml(run.id)}</p>
    <div class="meta">Status: ${escapeHtml(run.status)}</div>
    <div class="meta">Worker: ${escapeHtml(run.workerId || "")}</div>
    <div class="meta">Message: ${escapeHtml(run.message || "")}</div>
    <div class="actions">
      <button class="danger" id="cancel-run">Cancel</button>
      <button class="secondary" id="retry-run">Retry</button>
    </div>
  `;
  document.getElementById("cancel-run").addEventListener("click", async () => {
    await api(`/api/runs/${encodeURIComponent(run.id)}/cancel`, { method: "POST" });
    await refresh();
  });
  document.getElementById("retry-run").addEventListener("click", async () => {
    await api(`/api/runs/${encodeURIComponent(run.id)}/retry`, { method: "POST" });
    await refresh();
  });
  document.getElementById("logs").textContent = await api(`/api/runs/${encodeURIComponent(run.id)}/logs`);
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

refresh().catch(error => {
  document.getElementById("details").textContent = error.message;
});
