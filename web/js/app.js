// app.js
// The shell of the web app: talking to the Java server, page switching, the sidebar
// numbers, toasts, dialogs and the Ctrl+K search. Each page file (overview.js, ...)
// registers itself in App.pages and this file calls its init() once and onShow() on every visit.

const App = {
  pages: {},       // App.pages.overview = { title, eyebrow, actions, init, onShow }
  meta: null,      // categories, conditions, zones from /api/meta
  status: null,    // latest /api/status
  current: null,
};

// ---------- Talking to Java ----------
// GET:  await api("/api/status")
// POST: await api("/api/maintenance/flag", { id: "EQ-00042" })  (sent as form fields)
async function api(path, postData) {
  const options = postData === undefined ? {} : {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(postData).toString(),
  };
  try {
    const res = await fetch(path, options);
    return await res.json();
  } catch (e) {
    return { ok: false, message: "Can't reach the server. Is web.WebServer running?" };
  }
}

// A POST that changes data: shows its message as a toast and refreshes the sidebar.
async function action(path, postData, button) {
  if (button) button.disabled = true;           // no double clicks while waiting
  const r = await api(path, postData || {});
  if (button) button.disabled = false;
  if (r.message) toast(r.message, !r.ok);
  refreshStatus();
  return r;
}

// ---------- Small helpers ----------
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
const fmt = (n) => Number(n).toLocaleString("en-US");
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

// "STRENGTH_MACHINE" -> "Strength machine"
function pretty(name) {
  const s = String(name).replace(/_/g, " ").toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}
const condPill = (c) => `<span class="pill cond cond-${c}">${pretty(c)}</span>`;
const bar = (pct, red) => `<div class="bar ${red ? "red" : ""}"><i style="width:${Math.max(2, Math.min(100, pct))}%"></i></div>`;

function options(items, selected) {
  return items.map((it) => {
    const [v, label] = Array.isArray(it) ? it : [it, it];
    return `<option value="${esc(v)}" ${v === selected ? "selected" : ""}>${esc(label)}</option>`;
  }).join("");
}

// Line icons used by the pages (all stroke = currentColor).
const ICON = {
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
  arrow: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
  wrench: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z"/></svg>',
  flag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 21V4h11l-1.5 4L16 12H5"/></svg>',
  check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6L9 17l-5-5"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 20h4L19 9l-4-4L4 16z"/></svg>',
  trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/></svg>',
  clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>',
  box: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 7l9-4 9 4-9 4zM3 7v10l9 4 9-4V7"/></svg>',
  play: '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M7 4v16l13-8z"/></svg>',
};

// ---------- Toasts ----------
function toast(message, isError = false) {
  const t = document.createElement("div");
  t.className = "toast glass" + (isError ? " err" : "");
  t.setAttribute("role", isError ? "alert" : "status");
  t.innerHTML = `<span class="dot"></span><span>${esc(message)}</span>`;
  const box = $("#toasts");
  box.appendChild(t);
  while (box.children.length > 3) box.firstElementChild.remove();
  setTimeout(() => { t.classList.add("out"); setTimeout(() => t.remove(), 260); }, 3800);
}

// ---------- Dialogs ----------
// confirmDialog() resolves true/false. infoDialog() just shows content with a Close button.
let lastFocus = null;
function openDialog(title, bodyHtml, actionsHtml) {
  lastFocus = document.activeElement;
  $("#dialogTitle").textContent = title;
  $("#dialogBody").innerHTML = bodyHtml;
  $("#dialogActions").innerHTML = actionsHtml;
  $("#dialog").hidden = false;
  const first = $("#dialogActions .btn:last-child");
  if (first) first.focus();
}
function closeDialog() {
  $("#dialog").hidden = true;
  if (lastFocus) lastFocus.focus();
}
function confirmDialog(title, bodyHtml, okLabel = "Confirm", danger = false) {
  return new Promise((resolve) => {
    openDialog(title, bodyHtml,
      `<button class="btn ghost" data-r="0">Cancel</button><button class="btn ${danger ? "danger" : "lime"}" data-r="1">${esc(okLabel)}</button>`);
    $("#dialog").onclick = (e) => {
      const r = e.target.closest("[data-r]");
      if (e.target.id === "dialog" || r) { closeDialog(); resolve(!!r && r.dataset.r === "1"); }
    };
  });
}
function infoDialog(title, bodyHtml) {
  openDialog(title, bodyHtml, `<button class="btn lime" data-r="1">Close</button>`);
  $("#dialog").onclick = (e) => { if (e.target.id === "dialog" || e.target.closest("[data-r]")) closeDialog(); };
}

// ---------- Ctrl+K search ----------
let paletteRows = [], paletteIndex = 0, paletteTimer;
function openPalette() {
  lastFocus = document.activeElement;
  $("#palette").hidden = false;
  $("#paletteInput").value = "";
  $("#paletteList").innerHTML = `<div class="empty-state">Type an ID, a machine name or a zone.</div>`;
  $("#paletteInput").focus();
}
function closePalette() { $("#palette").hidden = true; if (lastFocus) lastFocus.focus(); }
async function searchPalette() {
  const q = $("#paletteInput").value.trim();
  if (!q) return;
  const r = await api(`/api/equipment?q=${encodeURIComponent(q)}&limit=8`);
  if (!r.ok) return;
  paletteRows = r.rows; paletteIndex = 0;
  $("#paletteList").innerHTML = r.rows.length ? r.rows.map((e, i) => `
    <button class="palette-item ${i === 0 ? "on" : ""}" role="option" data-i="${i}">
      <span class="mono" style="color:var(--lime)">${esc(e.id)}</span>
      <span><b style="font-weight:600">${esc(e.name)}</b><br><span class="muted" style="font-size:13px">${esc(e.location)}</span></span>
      ${condPill(e.condition)}
    </button>`).join("") + `<div class="hint" style="padding:8px 12px">${fmt(r.matched)} match${r.matched === 1 ? "" : "es"}</div>`
    : `<div class="empty-state">${ICON.box}No equipment matches “${esc(q)}”.</div>`;
}
function pickPalette(i) {
  const e = paletteRows[i];
  if (!e) return;
  closePalette();
  location.hash = "equipment";
  App.pages.equipment.focusOn(e.id);
}

// ---------- Sidebar numbers + database status ----------
async function refreshStatus() {
  const s = await api("/api/status");
  if (!s.ok) return null;
  App.status = s;
  $("#navCount").textContent = fmt(s.count);
  $("#navHeap").textContent = fmt(s.heap);
  $("#navHeap").hidden = s.heap === 0;
  $("#sideQueue").textContent = fmt(s.queue);
  $("#sideHeap").textContent = fmt(s.heap);
  $("#sideServiced").textContent = fmt(s.serviced ?? 0);
  $("#dbStatus").classList.toggle("online", s.online);
  $("#dbText").textContent = s.online ? "MySQL connected" : "Offline · CSV data";
  return s;
}

// ---------- Page switching ----------
function showPage(key) {
  if (!App.pages[key]) key = "overview";
  const page = App.pages[key];
  App.current = key;
  $("#dialog").hidden = true;
  $$(".page").forEach((p) => p.classList.toggle("active", p.id === "page-" + key));
  $$("[data-page]").forEach((a) => {
    const on = a.dataset.page === key;
    a.classList.toggle("active", on);
    if (on) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current");
  });
  $("#pageTitle").textContent = page.title;
  $("#pageEyebrow").textContent = typeof page.eyebrow === "function" ? page.eyebrow() : page.eyebrow;
  $("#headActions").innerHTML = page.actions ? page.actions() : "";
  if (page.bindActions) page.bindActions();
  document.title = page.title + " · Lime Gym";
  if (page.onShow) page.onShow();
}

function today() {
  return new Date().toLocaleDateString("en-US", { weekday: "short", day: "numeric", month: "short" });
}

// ---------- Start ----------
document.addEventListener("DOMContentLoaded", async () => {
  window.addEventListener("hashchange", () => showPage(location.hash.slice(1)));

  // Ctrl+K / Cmd+K opens search; Esc closes overlays.
  document.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") { e.preventDefault(); openPalette(); }
    if (e.key === "Escape") { if (!$("#palette").hidden) closePalette(); else if (!$("#dialog").hidden) $("#dialog").click(); }
  });
  $("#openPalette").addEventListener("click", openPalette);
  $("#palette").addEventListener("click", (e) => {
    if (e.target.id === "palette") closePalette();
    const item = e.target.closest(".palette-item");
    if (item) pickPalette(Number(item.dataset.i));
  });
  $("#paletteInput").addEventListener("input", () => { clearTimeout(paletteTimer); paletteTimer = setTimeout(searchPalette, 150); });
  $("#paletteInput").addEventListener("keydown", (e) => {
    const items = $$(".palette-item");
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      paletteIndex = (paletteIndex + (e.key === "ArrowDown" ? 1 : -1) + items.length) % Math.max(1, items.length);
      items.forEach((it, i) => it.classList.toggle("on", i === paletteIndex));
    }
    if (e.key === "Enter") pickPalette(paletteIndex);
  });

  App.meta = await api("/api/meta");
  if (!App.meta.ok) { toast(App.meta.message, true); return; }
  Object.values(App.pages).forEach((p) => p.init && p.init());
  await refreshStatus();
  showPage(location.hash.slice(1) || "overview");
});
