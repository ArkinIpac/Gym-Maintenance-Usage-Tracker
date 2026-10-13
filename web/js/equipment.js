// equipment.js
// Equipment page: category folder tabs, search, the inventory table, and a side panel
// that shows the selected machine (flag / edit / delete / history) or the register form.

App.pages.equipment = (() => {
  const PAGE = 25;
  let category = "", offset = 0, rows = [], matched = 0;
  let selected = null;     // the equipment object shown in the side panel
  let mode = "empty";      // side panel: "empty" | "detail" | "form"
  let editing = false;     // form is editing (true) or registering (false)
  let timer;

  function init() {
    const cats = App.meta.categories;
    $("#page-equipment").innerHTML = `
      <div class="bento">
        <div class="s8" style="min-width:0">
          <div class="folder-tabs" role="tablist" aria-label="Category" id="eqTabs">
            <button class="ftab active" role="tab" data-cat="">All <span class="mono" id="eqAllCount"></span></button>
            ${cats.map((c) => `<button class="ftab" role="tab" data-cat="${c}">${pretty(c)}</button>`).join("")}
          </div>
          <div class="flat" style="border-radius:0 var(--r-xl) var(--r-xl) var(--r-xl);padding:20px">
            <div style="display:flex;flex-wrap:wrap;gap:10px;margin-bottom:14px">
              <label class="inline-input flat" style="flex:1 1 260px;padding-right:16px">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" style="color:var(--text-3)"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>
                <span class="sr-only">Search by ID, name or zone</span>
                <input id="eqSearch" placeholder="Search ID, name or zone">
              </label>
            </div>
            <div class="table-wrap" style="max-height:640px">
              <table class="tight">
                <thead><tr><th scope="col">ID</th><th scope="col">Name</th><th scope="col">Category</th><th scope="col">Zone</th><th scope="col">Condition</th><th scope="col" class="num">Usage</th></tr></thead>
                <tbody id="eqBody"></tbody>
              </table>
            </div>
            <div class="table-foot">
              <span id="eqShowing">Loading…</span>
              <div class="pager">
                <button class="btn ghost sm" id="eqPrev" aria-label="Previous page">‹</button>
                <span id="eqPage"></span>
                <button class="btn ghost sm" id="eqNext" aria-label="Next page">›</button>
              </div>
            </div>
          </div>
        </div>
        <aside class="card glass s4" id="eqPanel" style="position:sticky;top:20px" aria-live="polite"></aside>
      </div>`;

    $("#eqTabs").addEventListener("click", (e) => { const t = e.target.closest(".ftab"); if (t) setCategory(t.dataset.cat); });
    $("#eqSearch").addEventListener("input", () => { clearTimeout(timer); timer = setTimeout(() => { offset = 0; load(); }, 200); });
    $("#eqPrev").addEventListener("click", () => { offset = Math.max(0, offset - PAGE); load(); });
    $("#eqNext").addEventListener("click", () => { offset += PAGE; load(); });
    $("#eqBody").addEventListener("click", (e) => {
      const tr = e.target.closest("tr[data-id]");
      if (tr) select(rows.find((r) => r.id === tr.dataset.id));
    });
    $("#eqBody").addEventListener("keydown", (e) => {
      const tr = e.target.closest("tr[data-id]");
      if (tr && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); select(rows.find((r) => r.id === tr.dataset.id)); }
    });
    $("#eqPanel").addEventListener("click", onPanelClick);
    $("#eqPanel").addEventListener("submit", onSubmit);
    renderPanel();
  }

  // ---------- table ----------
  async function load() {
    const q = encodeURIComponent($("#eqSearch").value.trim());
    const r = await api(`/api/equipment?q=${q}&category=${category}&offset=${offset}&limit=${PAGE}`);
    if (!r.ok) return;
    rows = r.rows; matched = r.matched;
    if (!category) $("#eqAllCount").textContent = fmt(r.total);
    $("#eqBody").innerHTML = rows.length ? rows.map((e) => `
      <tr class="clickable ${selected && selected.id === e.id ? "selected" : ""}" data-id="${esc(e.id)}" tabindex="0">
        <td class="mono" style="color:var(--lime)">${esc(e.id)}</td>
        <td class="wrap" style="font-weight:500">${esc(e.name)}</td>
        <td class="muted wrap">${pretty(e.category)}</td>
        <td class="muted wrap">${esc(e.location)}</td>
        <td>${condPill(e.condition)}</td>
        <td class="num mono">${fmt(e.usage)}</td>
      </tr>`).join("")
      : `<tr><td colspan="6"><div class="empty-state">${ICON.box}No equipment matches your search.</div></td></tr>`;
    const from = matched ? offset + 1 : 0, to = Math.min(offset + PAGE, matched);
    $("#eqShowing").textContent = `Showing ${fmt(from)}–${fmt(to)} of ${fmt(matched)}`;
    $("#eqPage").textContent = `${Math.floor(offset / PAGE) + 1} / ${Math.max(1, Math.ceil(matched / PAGE))}`;
    $("#eqPrev").disabled = offset === 0;
    $("#eqNext").disabled = offset + PAGE >= matched;
  }

  function setCategory(cat) {
    category = cat || "";
    offset = 0;
    $$("#eqTabs .ftab").forEach((t) => t.classList.toggle("active", t.dataset.cat === category));
    if (App.current === "equipment") load();
  }

  function select(e) {
    if (!e) return;
    selected = e; mode = "detail";
    $$("#eqBody tr").forEach((tr) => tr.classList.toggle("selected", tr.dataset.id === e.id));
    renderPanel();
  }

  // Used by Ctrl+K: show exactly this ID.
  async function focusOn(id) {
    setCategory("");
    $("#eqSearch").value = id;
    offset = 0;
    await load();
    select(rows.find((r) => r.id === id));
  }

  function newItem() { selected = null; editing = false; mode = "form"; renderPanel(); $("#eqPanel [name=id]").focus(); }

  // ---------- side panel ----------
  function renderPanel() {
    const p = $("#eqPanel");
    if (mode === "form") {
      const e = editing ? selected : { id: "", name: "", category: category || "CARDIO", location: "Cardio Zone A", condition: "GOOD", usage: 0 };
      const zones = App.meta.zones.filter((z) => z !== "Entrance");
      p.innerHTML = `
        <div class="card-head" style="margin-bottom:14px">
          <div><h2 class="card-title">${editing ? "Edit equipment" : "Register equipment"}</h2>
          <div class="card-sub">${editing ? "The ID is a key, so it can't change." : "Saved to the array, hash table and BST."}</div></div>
        </div>
        <form class="form" novalidate>
          <label class="field"><span class="label">Equipment ID</span>
            <input class="input mono" name="id" value="${esc(e.id)}" placeholder="EQ-05001" ${editing ? "readonly" : "required"} autocomplete="off"></label>
          <label class="field"><span class="label">Name</span>
            <input class="input" name="name" value="${esc(e.name)}" placeholder="Treadmill 5001" required autocomplete="off"></label>
          <div class="row2">
            <label class="field"><span class="label">Category</span>
              <select class="select" name="category">${options(App.meta.categories.map((c) => [c, pretty(c)]), e.category)}</select></label>
            <label class="field"><span class="label">Condition</span>
              <select class="select" name="condition">${options(App.meta.conditions.map((c) => [c, pretty(c)]), e.condition)}</select></label>
          </div>
          <label class="field"><span class="label">Zone</span>
            <select class="select" name="location">${options(zones, e.location)}</select></label>
          <label class="field"><span class="label">Usage count</span>
            <input class="input mono" name="usage" type="number" min="0" inputmode="numeric" value="${e.usage}" required></label>
          <p class="error-text" id="eqError" role="alert"></p>
          <div style="display:flex;gap:10px">
            <button class="btn lime" type="submit" style="flex:1">${editing ? "Save changes" : `${ICON.plus} Register`}</button>
            <button class="btn ghost" type="button" data-act="cancel">Cancel</button>
          </div>
        </form>`;
      return;
    }
    if (mode === "detail" && selected) {
      const e = selected;
      p.innerHTML = `
        <div class="mono" style="font-size:14px;color:var(--lime)">${esc(e.id)}</div>
        <div class="detail-title">${esc(e.name)}</div>
        <div class="chips">${condPill(e.condition)}<span class="pill glassy">${pretty(e.category)}</span><span class="pill glassy">${esc(e.location)}</span></div>
        <div class="kv" style="margin-top:18px">
          <div><span class="label">Usage</span><b>${fmt(e.usage)}</b></div>
          <div><span class="label">Severity</span><b>${App.meta.conditions.indexOf(e.condition)}</b></div>
          <div><span class="label">Priority</span><b class="gtext">${fmt(App.meta.conditions.indexOf(e.condition) * 10000 + e.usage)}</b></div>
        </div>
        <div style="display:flex;flex-direction:column;gap:10px;margin-top:20px">
          <button class="btn lime" data-act="flag">${ICON.flag} Flag for maintenance</button>
          <div style="display:flex;gap:10px">
            <button class="btn ghost" data-act="edit" style="flex:1">${ICON.edit} Edit</button>
            <button class="btn danger" data-act="delete" style="flex:1">${ICON.trash} Delete</button>
          </div>
        </div>
        <div style="margin-top:22px">
          <div class="label" style="margin-bottom:8px">Service history</div>
          <div id="eqHistory" class="stack"><div class="skel" style="height:44px"></div></div>
        </div>`;
      loadHistory(e.id);
      return;
    }
    p.innerHTML = `
      <div class="empty-state" style="min-height:300px">
        ${ICON.box}
        <b style="color:var(--text);font-size:16px">Pick a machine</b>
        Select a row to flag, edit or delete it, or register a new one.
        <button class="btn lime" data-act="new" style="margin-top:8px">${ICON.plus} Register equipment</button>
      </div>`;
  }

  async function loadHistory(id) {
    const r = await api("/api/maintenance/history?id=" + encodeURIComponent(id));
    const box = $("#eqHistory");
    if (!box) return;
    box.innerHTML = r.ok && r.records.length ? r.records.map((x) => `
      <div class="item" style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
        <span class="mono muted" style="font-size:12px">${x.date}</span>${condPill(x.before)}<span class="muted">→</span>${condPill(x.after)}
      </div>`).join("")
      : `<div class="hint">No services yet this session. Each service is added to this machine's linked list.</div>`;
  }

  async function onPanelClick(ev) {
    const b = ev.target.closest("[data-act]");
    if (!b) return;
    const act = b.dataset.act;
    if (act === "new") return newItem();
    if (act === "cancel") { mode = selected ? "detail" : "empty"; editing = false; return renderPanel(); }
    if (act === "edit") { editing = true; mode = "form"; return renderPanel(); }
    if (act === "flag") { await action("/api/maintenance/flag", { id: selected.id }, b); return; }
    if (act === "delete") {
      const ok = await confirmDialog("Delete equipment",
        `<p>Delete <b class="mono">${esc(selected.id)}</b> · ${esc(selected.name)}?</p><p class="muted">It's removed from the array, hash table, BST and the database.</p>`, "Delete", true);
      if (!ok) return;
      const r = await action("/api/equipment/delete", { id: selected.id });
      if (r.ok) { selected = null; mode = "empty"; renderPanel(); load(); }
    }
  }

  async function onSubmit(ev) {
    ev.preventDefault();
    const data = Object.fromEntries(new FormData(ev.target));
    if (editing) data.id = selected.id;
    const err = $("#eqError");
    if (!data.id.trim() || !data.name.trim()) { err.textContent = "ID and name are required."; return; }
    if (!/^\d+$/.test(String(data.usage).trim())) { err.textContent = "Usage must be a whole number, 0 or more."; return; }
    const r = await action(editing ? "/api/equipment/update" : "/api/equipment/add", data, ev.submitter);
    if (!r.ok) { err.textContent = r.message; return; }
    const id = data.id.trim();
    editing = false;
    await focusOn(id);           // show the saved machine in the table and panel
  }

  return {
    title: "Equipment",
    eyebrow: () => "Array · hash table · BST, always in sync",
    actions: () => `<button class="btn lime" id="hdNewEq">${ICON.plus} Register equipment</button>`,
    bindActions: () => $("#hdNewEq").addEventListener("click", newItem),
    init,
    onShow: load,
    setCategory,
    focusOn,
    newItem,
  };
})();
