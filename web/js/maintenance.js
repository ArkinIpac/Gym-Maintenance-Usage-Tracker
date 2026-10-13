// maintenance.js
// Maintenance page: the 3-step workflow as three folders.
//   01 QUEUE (FIFO)  --promote-->  02 PRIORITY HEAP  --extractMax-->  03 SERVICED (linked list)

App.pages.maintenance = (() => {
  const COND_TEXT = { EXCELLENT: "#C8FF00", GOOD: "#A8D100", FAIR: "#BDBDBD", POOR: "#FFAA5C", CRITICAL: "#FF7A7A" };
  let state = null;

  function init() {
    $("#page-maintenance").innerHTML = `
      <section class="pipeline glass" aria-label="Pipeline">
        <div class="pipe-stat"><b id="mQueue" style="color:var(--amber)">0</b>in queue</div>
        <div class="pipe-link"><svg viewBox="0 0 200 10" preserveAspectRatio="none" aria-hidden="true"><line class="flow" x1="0" y1="5" x2="200" y2="5" stroke="#C8FF00" stroke-width="3" stroke-linecap="round"/></svg>promote · O(k log n)</div>
        <div class="pipe-stat"><b id="mHeap" class="gtext" style="font-size:44px;font-weight:800;line-height:1">0</b>in max-heap</div>
        <div class="pipe-link"><svg viewBox="0 0 200 10" preserveAspectRatio="none" aria-hidden="true"><line class="flow" x1="0" y1="5" x2="200" y2="5" stroke="#C8FF00" stroke-width="3" stroke-linecap="round"/></svg>extractMax · O(log n)</div>
        <div class="pipe-stat"><b id="mServiced" style="color:var(--lime)">0</b>serviced</div>
      </section>

      <div class="bento">
        <section class="folder s4 stretch" aria-label="Queue">
          <div class="folder-tab flat" style="--tab:200px"><span class="num" style="color:var(--amber)">01</span>Queue · FIFO</div>
          <div class="folder-body flat" style="--tab:200px;display:flex;flex-direction:column;gap:10px;min-height:580px;height:100%">
            <div class="hint">In the order reported. #1 is the front of the line.</div>
            <div class="stack" id="mQueueList"></div>
            <button class="btn lime" id="mPromote" style="margin-top:auto">Promote to heap ${ICON.arrow}</button>
          </div>
        </section>

        <section class="folder s5 stretch" aria-label="Priority heap">
          <div class="folder-tab flat" style="--tab:214px"><span class="num" style="color:var(--red-text)">02</span>Priority heap</div>
          <div class="folder-body flat" style="--tab:214px;display:flex;flex-direction:column;gap:10px;min-height:580px;height:100%">
            <div class="stack" id="mHeapList"></div>
            <div style="margin-top:auto;display:flex;gap:10px">
              <button class="btn lime" id="mService" style="flex:1">${ICON.wrench} <span id="mServiceLabel">Service next</span></button>
              <button class="btn ghost" id="mClearHeap" aria-label="Clear the heap without servicing">Clear</button>
            </div>
          </div>
        </section>

        <section class="folder s3 stretch" aria-label="Serviced">
          <div class="folder-tab flat" style="--tab:170px"><span class="num" style="color:var(--lime)">03</span>Serviced</div>
          <div class="folder-body flat" style="--tab:170px;display:flex;flex-direction:column;gap:10px;min-height:580px;height:100%">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span class="hint">Last 10, newest first</span>
              <button class="btn ghost sm" id="mClearRecent">Clear</button>
            </div>
            <div class="stack" id="mRecent" style="flex:1"></div>
          </div>
        </section>

        <section class="card glass s7" aria-labelledby="mActTitle">
          <div class="card-head"><h2 class="card-title" id="mActTitle">Activity</h2>
            <span class="card-sub" style="display:flex;align-items:center;gap:8px"><span class="dot live" style="width:6px;height:6px;background:var(--lime);color:var(--lime)"></span>live</span></div>
          <div id="mActivity"></div>
        </section>

        <section class="card glass s5" aria-labelledby="mHowTitle">
          <h2 class="card-title" id="mHowTitle">How priority works</h2>
          <div style="display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin-top:16px;font:14px var(--f-mono)">
            <span class="pill glassy" style="padding:8px 12px">severity</span><span class="muted">×</span>
            <span class="pill glassy" style="padding:8px 12px" id="mWeight">10,000</span><span class="muted">+</span>
            <span class="pill glassy" style="padding:8px 12px">usage</span>
          </div>
          <div style="display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:6px;margin-top:18px;text-align:center;font-size:12px">
            ${["EXCELLENT", "GOOD", "FAIR", "POOR", "CRITICAL"].map((c, i) => `
              <div><div style="height:8px;border-radius:4px;background:var(--c-${c})"></div>
              <div style="margin-top:6px;color:var(--text-2)">${pretty(c)}</div><div class="mono">${i}</div></div>`).join("")}
          </div>
          <p class="note" style="margin:16px 0 0">A worse condition always wins over usage, so a Critical machine with 0 uses still outranks a Poor one with 9,999.</p>
        </section>
      </div>`;

    $("#mPromote").addEventListener("click", (e) => run("/api/maintenance/promote", {}, e.currentTarget));
    $("#mService").addEventListener("click", (e) => run("/api/maintenance/service", {}, e.currentTarget));
    $("#mClearRecent").addEventListener("click", (e) => run("/api/maintenance/clear-recent", {}, e.currentTarget));
    $("#mClearHeap").addEventListener("click", async () => {
      const ok = await confirmDialog("Clear the heap",
        `<p>Remove all ${fmt(state ? state.heap : 0)} machines from the priority heap?</p><p class="muted">Nothing is serviced. They can be flagged again later.</p>`, "Clear heap", true);
      if (ok) run("/api/maintenance/clear-heap", {});
    });
  }

  async function run(path, data, button) {
    await action(path, data, button);
    load();
  }

  async function load() {
    const s = await api("/api/maintenance");
    if (!s.ok) return;
    state = s;
    $("#mQueue").textContent = fmt(s.queue);
    $("#mHeap").textContent = fmt(s.heap);
    $("#mServiced").textContent = fmt(s.serviced);
    $("#mWeight").textContent = fmt(s.severityWeight);

    // 01 queue (show the first 6)
    const q = s.queueItems || [];
    $("#mQueueList").innerHTML = q.length ? q.slice(0, 6).map((e, i) => `
      <div class="item item-enter" style="display:flex;gap:12px;align-items:center;animation-delay:${i * 40}ms">
        <div class="qpos">#${i + 1}</div>
        <div style="min-width:0"><div style="font-weight:600;font-size:14px">${esc(e.name)}</div>
        <div class="sub-mono">${esc(e.id)} · <span style="color:${COND_TEXT[e.condition]}">${pretty(e.condition)}</span></div></div>
      </div>`).join("") + (q.length > 6 ? `<div class="hint" style="text-align:center">+ ${fmt(q.length - 6)} more waiting</div>` : "")
      : `<div class="dropzone">${ICON.flag}Queue is empty. Flag a machine by ID above.</div>`;
    $("#mPromote").disabled = s.queue === 0;
    $("#mPromote").innerHTML = (s.queue ? `Promote ${fmt(s.queue)} to heap ` : "Promote to heap ") + ICON.arrow;

    // 02 heap (root highlighted, then the next 5)
    const top = s.top || [];
    $("#mHeapList").innerHTML = top.length ? top.slice(0, 6).map((e, i) => i === 0 ? `
      <div class="item root item-enter">
        <div style="display:flex;justify-content:space-between;font-size:12px;font-weight:600;letter-spacing:1.2px;color:var(--lime)"><span>ROOT · NEXT UP</span><span class="mono" style="letter-spacing:0">heap[0]</span></div>
        <div style="font:700 22px var(--f-display);margin-top:8px">${esc(e.name)}</div>
        <div style="display:flex;flex-wrap:wrap;justify-content:space-between;align-items:baseline;gap:8px;margin-top:6px">
          <span class="sub-mono" style="font-size:13px">${esc(e.id)} · ${esc(e.location)}</span>
          <span class="gtext" style="font:700 22px var(--f-display)">${fmt(e.priority)}</span>
        </div>
      </div>` : `
      <div class="item item-enter" style="animation-delay:${i * 40}ms">
        <div style="display:flex;justify-content:space-between;gap:10px"><span style="font-weight:600;font-size:14px">${esc(e.name)}</span><span class="mono" style="font-size:13px">${fmt(e.priority)}</span></div>
        <div class="sub-mono" style="margin:2px 0 9px">${esc(e.id)} · <span style="color:${COND_TEXT[e.condition]}">${pretty(e.condition)}</span> · ${fmt(e.usage)} uses</div>
        ${bar((e.priority / top[0].priority) * 100, e.condition === "CRITICAL")}
      </div>`).join("") + (s.heap > 6 ? `<div class="hint" style="text-align:center">+ ${fmt(s.heap - 6)} more, ordered by the heap</div>` : "")
      : `<div class="dropzone">${ICON.box}The heap is empty. Promote the queue to rank machines.</div>`;
    $("#mService").disabled = s.heap === 0;
    $("#mClearHeap").disabled = s.heap === 0;
    $("#mServiceLabel").textContent = top.length ? `Service ${top[0].name}` : "Service next";

    // 03 serviced
    $("#mRecent").innerHTML = s.recent.length ? s.recent.map((r, i) => `
      <div class="item item-enter" style="animation-delay:${i * 40}ms">
        <div class="mono" style="font-size:13px;color:var(--lime)">${esc(r.id)}</div>
        <div class="sub-mono">${esc(r.date)} · ${esc(r.note)}</div>
        <div style="display:flex;align-items:center;gap:6px;margin-top:8px">${condPill(r.before)}<span class="muted">→</span>${condPill(r.after)}</div>
      </div>`).join("")
      : `<div class="dropzone" style="flex:1">${ICON.check}Serviced machines land here. Usage resets to 0.</div>`;
    $("#mClearRecent").disabled = s.recent.length === 0;

    $("#mActivity").innerHTML = activityHtml(s.activity);
  }

  async function flagFromHeader(btn) {
    const input = $("#hdFlagId");
    const id = input.value.trim();
    if (!id) { toast("Type an equipment ID first, e.g. EQ-00042.", true); input.focus(); return; }
    const r = await action("/api/maintenance/flag", { id }, btn);
    if (r.ok) input.value = "";
    load();
  }

  return {
    title: "Maintenance",
    eyebrow: "queue → heap → service",
    actions: () => `
      <label class="inline-input flat">
        <span class="label">ID</span>
        <input id="hdFlagId" class="mono" placeholder="EQ-00042" style="width:130px" autocomplete="off" aria-label="Equipment ID to flag">
        <button class="btn lime sm" id="hdFlag">Flag</button>
      </label>
      <button class="btn ghost" id="hdFlagAll">${ICON.flag} Flag all critical</button>`,
    bindActions: () => {
      $("#hdFlag").addEventListener("click", (e) => flagFromHeader(e.currentTarget));
      $("#hdFlagId").addEventListener("keydown", (e) => { if (e.key === "Enter") flagFromHeader($("#hdFlag")); });
      $("#hdFlagAll").addEventListener("click", (e) => run("/api/maintenance/flag-critical", {}, e.currentTarget));
    },
    init,
    onShow: load,
  };
})();
