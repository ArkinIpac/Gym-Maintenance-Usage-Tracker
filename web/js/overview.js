// Overview page. Everything comes from one call to /api/overview.

App.pages.overview = (() => {
  const COND_COLORS = { EXCELLENT: "#C8FF00", GOOD: "#8CB300", FAIR: "#BDBDBD", POOR: "#FF9A3C", CRITICAL: "#FF4545" };
  const CAT_CODES = { CARDIO: "CRD", STRENGTH_MACHINE: "STR", FREE_WEIGHT: "FRW", FUNCTIONAL: "FNC", ACCESSORY: "ACC" };

  function init() {
    $("#page-overview").innerHTML = `
      <div class="bento">
        <section class="card glass glow lift hero s7" aria-labelledby="heroLabel">
          <div style="display:flex;align-items:center;gap:10px;font-size:13px;color:var(--text-2)">
            <span class="dot live" style="background:var(--red);color:var(--red)"></span>Live · priority heap
          </div>
          <div class="hero-row">
            <div class="big-num gtext" id="ovHeap">–</div>
            <div style="padding-bottom:8px">
              <div class="hero-label" id="heroLabel">machines in the heap</div>
              <div class="muted" style="margin-top:4px">ranked by severity × 10,000 + usage</div>
            </div>
          </div>
          <div style="display:flex;flex-wrap:wrap;gap:12px;align-items:center">
            <button class="btn lime" id="ovFlagAll">${ICON.flag} Flag all critical</button>
            <button class="btn ghost" id="ovPromote">Promote queue</button>
            <div class="hero-stats">
              <div><b id="ovQueue" style="color:var(--amber)">0</b>in queue</div>
              <div><b id="ovServiced" style="color:var(--lime)">0</b>serviced</div>
            </div>
          </div>
        </section>

        <section class="folder s5 stretch" aria-label="Next up">
          <div class="folder-tab flat" style="--tab:170px">
            <span style="width:22px;height:22px;border-radius:7px;background:var(--lime);color:var(--on-lime);display:grid;place-items:center;font:600 12px var(--f-mono)">1</span>
            Next up
          </div>
          <div class="folder-body flat lift" id="ovNext" style="--tab:170px;min-height:289px;height:calc(100% - 0px);display:flex;flex-direction:column;gap:18px"></div>
        </section>

        <section class="card glass lift s4" aria-labelledby="healthTitle">
          <div class="card-head"><h2 class="card-title" id="healthTitle">Fleet health</h2><span class="card-sub" id="ovTotal"></span></div>
          <div class="donut-wrap">
            <div class="donut">
              <svg width="160" height="160" viewBox="0 0 160 160" role="img" id="ovDonut"></svg>
              <div class="donut-center"><b id="ovHealthy">–</b><span class="muted" style="font-size:12px">good or better</span></div>
            </div>
            <div class="legend" id="ovLegend"></div>
          </div>
        </section>

        <section class="card glass s8" aria-labelledby="foldersTitle">
          <div class="card-head"><h2 class="card-title" id="foldersTitle">Equipment folders</h2><a href="#equipment" style="font-size:13px;font-weight:600;text-decoration:none">Browse all →</a></div>
          <div id="ovFolders" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(122px,1fr));gap:14px"></div>
        </section>

        <section class="folder s8" aria-label="Priority heap">
          <div class="folder-tab flat" style="--tab:236px">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--lime)" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M12 3l9 16H3z"/></svg>
            Priority heap <span class="num" id="ovHeapTab" style="color:var(--red-text)"></span>
          </div>
          <div class="folder-body flat" style="--tab:236px;padding-top:10px;padding-bottom:12px" id="ovTop"></div>
        </section>

        <section class="card glass s4" aria-labelledby="zonesTitle">
          <div class="card-head"><h2 class="card-title" id="zonesTitle">Hot zones</h2><span class="card-sub">critical per zone</span></div>
          <div id="ovZones" class="stack" style="gap:14px"></div>
        </section>

        <section class="card glass s12" aria-labelledby="actTitle">
          <div class="card-head">
            <h2 class="card-title" id="actTitle">Activity</h2>
            <span class="card-sub" style="display:flex;align-items:center;gap:8px"><span class="dot live" style="width:6px;height:6px;background:var(--lime);color:var(--lime)"></span>live</span>
          </div>
          <div id="ovActivity"></div>
        </section>
      </div>`;

    $("#ovFlagAll").addEventListener("click", async (e) => { await action("/api/maintenance/flag-critical", {}, e.currentTarget); load(); });
    $("#ovPromote").addEventListener("click", async (e) => { await action("/api/maintenance/promote", {}, e.currentTarget); load(); });
    $("#ovNext").addEventListener("click", async (e) => {
      const b = e.target.closest("[data-service]");
      if (b) { await action("/api/maintenance/service", {}, b); load(); }
    });
  }

  async function load() {
    const d = await api("/api/overview");
    if (!d.ok) return;

    $("#ovHeap").textContent = fmt(d.heap);
    $("#ovQueue").textContent = fmt(d.queue);
    $("#ovServiced").textContent = fmt(d.serviced);
    $("#ovPromote").disabled = d.queue === 0;
    $("#ovTotal").textContent = fmt(d.count) + " machines";
    $("#ovHeapTab").textContent = fmt(d.heap);

    // Next up (top of the heap)
    const n = d.next;
    $("#ovNext").innerHTML = n ? `
      <div>
        <div class="mono" style="font-size:14px;color:var(--lime)">${esc(n.id)}</div>
        <div class="detail-title" style="font-size:30px">${esc(n.name)}</div>
        <div class="chips">${condPill(n.condition)}<span class="pill glassy">${esc(n.location)}</span><span class="pill glassy">${fmt(n.usage)} uses</span></div>
      </div>
      <div>
        <div style="display:flex;justify-content:space-between;align-items:baseline;font-size:13px;color:var(--text-3)">
          <span>Priority</span><span class="mono">${n.severity} × ${fmt(n.severityWeight)} + ${fmt(n.usage)}</span>
        </div>
        <div class="gtext" style="font:700 34px var(--f-display)">${fmt(n.priority)}</div>
        <div style="margin-top:8px">${bar(Math.min(100, n.priority / 500))}</div>
      </div>
      <button class="btn lime" data-service style="margin-top:auto">Service now ${ICON.arrow}</button>`
      : `<div class="empty-state" style="flex:1">${ICON.check}<b style="color:var(--text)">The heap is empty</b>Flag equipment, then promote the queue to rank it.</div>`;

    // Donut: one arc per condition
    const C = 2 * Math.PI * 64;
    let start = 0, svg = `<circle cx="80" cy="80" r="64" fill="none" stroke="rgba(255,255,255,0.06)" stroke-width="14"/>`;
    const total = d.conditions.reduce((s, c) => s + c.count, 0) || 1;
    d.conditions.forEach((c, i) => {
      const len = (c.count / total) * C;
      if (c.count > 0) {
        svg += `<circle class="seg" cx="80" cy="80" r="64" fill="none" stroke="${COND_COLORS[c.name]}" stroke-width="14" stroke-linecap="round"
          stroke-dasharray="${Math.max(len - 6, 1).toFixed(1)} ${C.toFixed(1)}" stroke-dashoffset="${(-start - 3).toFixed(1)}" style="animation-delay:${0.3 + i * 0.1}s"/>`;
      }
      start += len;
    });
    $("#ovDonut").innerHTML = svg;
    $("#ovDonut").setAttribute("aria-label", "Condition breakdown: " + d.conditions.map((c) => `${fmt(c.count)} ${c.name.toLowerCase()}`).join(", "));
    const healthy = d.conditions.filter((c) => c.name === "EXCELLENT" || c.name === "GOOD").reduce((s, c) => s + c.count, 0);
    $("#ovHealthy").textContent = Math.round((healthy / total) * 100) + "%";
    $("#ovLegend").innerHTML = d.conditions.map((c) =>
      `<div class="legend-row"><i style="background:${COND_COLORS[c.name]}"></i><span>${pretty(c.name)}</span><span>${fmt(c.count)}</span></div>`).join("");

    // Category folders
    const maxCat = Math.max(1, ...d.categories.map((c) => c.count));
    $("#ovFolders").innerHTML = d.categories.map((c) => `
      <a class="folder mini" href="#equipment" data-cat="${c.name}">
        <div class="folder-tab flat" style="color:${c.critical > 50 ? "var(--red-text)" : "var(--lime)"}">${CAT_CODES[c.name] || "EQ"}</div>
        <div class="folder-body flat">
          <div style="font-weight:600;font-size:14px">${pretty(c.name)}</div>
          <div style="font:700 26px var(--f-display)">${fmt(c.count)}</div>
          <div style="font-size:12px;line-height:1.5"><div style="color:var(--red-text)">${fmt(c.critical)} critical</div><div class="muted">${fmt(c.avgUsage)} avg uses</div></div>
          <div class="bar thin"><i style="width:${(c.count / maxCat) * 100}%"></i></div>
        </div>
      </a>`).join("");
    $$("#ovFolders [data-cat]").forEach((a) => a.addEventListener("click", () => App.pages.equipment.setCategory(a.dataset.cat)));

    // Top of the heap
    $("#ovTop").innerHTML = d.top.length ? d.top.map((h, i) => `
      <div class="rank-row">
        <div class="rank ${i === 0 ? "first" : ""}">${String(i + 1).padStart(2, "0")}</div>
        <div style="min-width:0"><div style="font-weight:600">${esc(h.name)}</div><div class="sub-mono">${esc(h.id)}</div></div>
        <div class="zone" style="font-size:13px;color:var(--text-2)">${esc(h.location)}</div>
        <div><div class="mono num" style="font-size:14px">${fmt(h.priority)}</div><div style="margin-top:6px">${bar((h.priority % 10000) / 60)}</div></div>
      </div>`).join("")
      : `<div class="empty-state">${ICON.box}Nothing ranked yet. Use “Flag all critical”, then “Promote queue”.</div>`;

    // Hot zones: the 6 zones with the most critical machines
    const zones = [...d.zones].sort((a, b) => b.critical - a.critical).slice(0, 6);
    const maxZone = Math.max(1, zones[0] ? zones[0].critical : 1);
    $("#ovZones").innerHTML = zones.map((z) => `
      <div>
        <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px"><span>${esc(z.name)}</span><span class="mono" style="color:var(--red-text)">${z.critical}</span></div>
        ${bar((z.critical / maxZone) * 100, true)}
      </div>`).join("");

    $("#ovActivity").innerHTML = activityHtml(d.activity);
  }

  return {
    title: "Overview",
    eyebrow: () => today(),
    actions: () => `<button class="btn ghost" id="hdSearch">${'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>'} Search <span class="kbd">Ctrl K</span></button>
                    <a class="btn lime" href="#equipment" id="hdNew">${ICON.plus} Register equipment</a>`,
    bindActions: () => {
      $("#hdSearch").addEventListener("click", openPalette);
      $("#hdNew").addEventListener("click", () => setTimeout(() => App.pages.equipment.newItem(), 0));
    },
    init,
    onShow: load,
  };
})();

// Activity feed rows, used by Overview and Maintenance.
function activityHtml(list) {
  if (!list || !list.length) return `<div class="empty-state">${ICON.clock}No activity yet.</div>`;
  return list.map((a) => {
    const m = a.message;
    const color = /^Serviced/.test(m) ? "var(--lime)" : /^Flagged/.test(m) ? "var(--amber)" : /^Deleted/.test(m) ? "var(--red)" : /MySQL|CSV/.test(m) ? "var(--green)" : "var(--text-2)";
    return `<div class="activity-row"><time>${esc(a.time)}</time><span class="dot" style="background:${color}"></span><span>${esc(m)}</span></div>`;
  }).join("");
}
