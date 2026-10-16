// Floor & reports page: graph routes, sorting, searching, BST and hash table tools.

App.pages.reports = (() => {
  const TOOLS = [["graph", "Floor plan"], ["sorting", "Sorting"], ["searching", "Searching"], ["bst", "BST"], ["hash", "Hash table"]];

  // Where each zone sits on the drawing, on a 10 x 6.7 grid.
  const POS = {
    "Entrance": [0.0, 3.0], "Cardio Zone A": [2.0, 0.5], "Cardio Zone B": [4.5, 0.5], "Cardio Mezzanine": [7.0, 0.5],
    "Rooftop Deck": [9.5, 0.5], "Strength Floor": [2.0, 2.6], "Machine Row 1": [4.5, 2.0], "Machine Row 2": [7.0, 2.2],
    "Free Weight Area": [4.5, 3.7], "Squat Rack Bay": [7.0, 3.8], "Olympic Lifting Platform": [8.6, 4.7],
    "Stretching Area": [2.0, 5.0], "Functional Training Zone": [4.5, 5.2], "Turf Area": [7.0, 5.8],
    "Group Class Studio": [4.5, 6.7], "Storage Room": [10.0, 6.0],
  };
  let graph = null, zoneCritical = {}, tree = null;
  let bstMark = { range: null, path: [], order: "inorder" };

  const card = (title, sub, body, extra = "") =>
    `<section class="card glass ${extra}"><div class="card-head"><h2 class="card-title">${title}</h2>${sub ? `<span class="card-sub">${sub}</span>` : ""}</div>${body}</section>`;
  const field = (label, control) => `<label class="field"><span class="label">${label}</span>${control}</label>`;
  const intOf = (sel) => { const v = $(sel).value.trim(); return /^\d+$/.test(v) ? Number(v) : -1; };

  function init() {
    const zones = App.meta.zones;
    $("#page-reports").innerHTML = `
      <div class="seg-tabs flat" role="tablist" aria-label="Tool" style="align-self:flex-start">
        ${TOOLS.map(([k, l], i) => `<button class="seg-tab ${i === 0 ? "active" : ""}" role="tab" data-tool="${k}">${l}</button>`).join("")}
      </div>

      <!-- Graph -->
      <div class="tool bento active" id="tool-graph">
        ${card("Route finder", "Zones are nodes, walkways are edges", `
          <div class="form">
            ${field("From", `<select class="select" id="gFrom">${options(zones, "Entrance")}</select>`)}
            ${field("To", `<select class="select" id="gTo">${options(zones, "Olympic Lifting Platform")}</select>`)}
            <div style="display:flex;gap:10px"><button class="btn lime" id="gBfs" style="flex:1">BFS route</button><button class="btn ghost" id="gDfs" style="flex:1">DFS route</button></div>
            <div class="kv" id="gStats"></div>
            <p class="note" style="margin:0">BFS explores level by level, so its route always has the fewest hops. DFS dives deep first, so its route is often longer. Zone rings show critical machines.</p>
          </div>`, "s4")}
        <div class="s8" style="display:flex;flex-direction:column;gap:20px;min-width:0">
          ${card("Floor plan", "Solid = BFS · dashed = DFS", `<svg class="svg-viz" id="gSvg" viewBox="0 0 1000 620" role="img" aria-label="Gym floor plan graph"></svg>`)}
          ${card("Route", "", `<div class="route" id="gRoute"><span class="muted">Pick two zones and run BFS or DFS.</span></div><pre class="output" id="gOut" data-hint="" style="margin-top:14px;min-height:0"></pre>`)}
        </div>
      </div>

      <!-- Sorting -->
      <div class="tool bento" id="tool-sorting">
        ${card("Sorting", "On a copy of the array", `
          <div class="form">
            ${field("Algorithm", `<select class="select" id="sAlgo">${options([["bubble", "Bubble sort · usage"], ["insertion", "Insertion sort · usage"], ["insertion-id", "Insertion sort · ID"]])}</select>`)}
            <div class="row2">
              ${field("Order", `<select class="select" id="sOrder">${options([["desc", "Descending"], ["asc", "Ascending"]])}</select>`)}
              ${field("Rows", `<input class="input mono" id="sLimit" type="number" min="1" max="500" value="15">`)}
            </div>
            <button class="btn lime" id="sRun">${ICON.play} Run sort</button>
            <div class="kv"><div><span class="label">Time</span><b id="sTime">–</b></div><div><span class="label">Records</span><b id="sCount">–</b></div></div>
            <p class="note" style="margin:0">Both are O(n²) on average. Insertion sort by ID is close to O(n) here because the data already arrives in ID order.</p>
          </div>`, "s4")}
        ${card("Result", "Master array is never reordered", `
          <div class="table-wrap" style="max-height:560px"><table>
            <thead><tr><th>#</th><th>ID</th><th>Name</th><th>Condition</th><th class="num">Usage</th></tr></thead>
            <tbody id="sBody"><tr><td colspan="5"><div class="empty-state">${ICON.play}Pick an algorithm and run it.</div></td></tr></tbody>
          </table></div>`, "s8")}
      </div>

      <!-- Searching -->
      <div class="tool bento" id="tool-searching">
        ${card("Searching", "Same ID, three strategies", `
          <div class="form">
            ${field("Equipment ID", `<input class="input mono" id="qId" value="EQ-04321">`)}
            <button class="btn lime" data-mode="all">Race all three</button>
            <div style="display:flex;gap:8px">
              <button class="btn ghost sm" data-mode="hash" style="flex:1">Hash</button>
              <button class="btn ghost sm" data-mode="binary" style="flex:1">Binary</button>
              <button class="btn ghost sm" data-mode="linear" style="flex:1">Linear</button>
            </div>
            ${field("Usage count (BST)", `<div style="display:flex;gap:8px"><input class="input mono" id="qUsage" type="number" min="0" placeholder="e.g. 119"><button class="btn ghost" id="qBst">Search</button></div>`)}
            <p class="note" style="margin:0">Hash O(1) average · Binary O(log n) · Linear O(n). Try a missing ID like EQ-99999.</p>
          </div>`, "s4")}
        ${card("Search race", "Nanoseconds for one lookup · shorter is faster", `
          <div class="race" id="qRace"></div>
          <pre class="output" id="qOut" data-hint="Results appear here." style="margin-top:18px"></pre>`, "s8")}
      </div>

      <!-- BST -->
      <div class="tool bento" id="tool-bst">
        ${card("Binary search tree", "Keyed by usage count", `
          <div class="form">
            <div class="row2">
              ${field("Min usage", `<input class="input mono" id="bMin" type="number" min="0" value="3000">`)}
              ${field("Max usage", `<input class="input mono" id="bMax" type="number" min="0" value="3050">`)}
            </div>
            <button class="btn lime" id="bRange">Range query</button>
            ${field("Traversal", `<div style="display:flex;gap:8px"><select class="select" id="bOrder">${options([["inorder", "Inorder"], ["preorder", "Preorder"], ["postorder", "Postorder"]])}</select><button class="btn ghost" id="bTraverse">Run</button></div>`)}
            <div class="kv" id="bStats"></div>
          </div>`, "s4")}
        <div class="s8" style="display:flex;flex-direction:column;gap:20px;min-width:0">
          ${card("Tree", "Top 5 levels · small numbers = visit order", `<svg class="svg-viz" id="bSvg" viewBox="0 0 960 350" role="img" aria-label="Top five levels of the usage BST"></svg>`)}
          ${card("Output", "", `<pre class="output" id="bOut" data-hint="Run a range query or a traversal."></pre>`)}
        </div>
      </div>

      <!-- Hash -->
      <div class="tool bento" id="tool-hash">
        ${card("Hash table", "ID index · separate chaining", `
          <div class="form">
            ${field("Equipment ID", `<input class="input mono" id="hId" value="EQ-00001">`)}
            <button class="btn lime" id="hLocate">Locate bucket</button>
            <button class="btn ghost" id="hDemo">Collision demo · capacity 7</button>
            <div class="kv" id="hStats"></div>
          </div>`, "s4")}
        <div class="s8" style="display:flex;flex-direction:column;gap:20px;min-width:0">
          ${card("Buckets", `<span id="hCaption">Keys in one bucket form a linked list</span>`, `<div class="buckets" id="hBuckets"><div class="empty-state">${ICON.box}Locate an ID to draw its bucket.</div></div>`)}
          ${card("Output", "", `<pre class="output" id="hOut" data-hint="Locate an ID or run the collision demo."></pre>`)}
        </div>
      </div>`;

    $$("#page-reports .seg-tab").forEach((t) => t.addEventListener("click", () => selectTool(t.dataset.tool)));
    $("#gBfs").addEventListener("click", () => route("bfs"));
    $("#gDfs").addEventListener("click", () => route("dfs"));
    ["#gFrom", "#gTo"].forEach((s) => $(s).addEventListener("change", () => { drawGraph(); $("#gRoute").innerHTML = `<span class="muted">Pick two zones and run BFS or DFS.</span>`; $("#gOut").textContent = ""; }));
    $("#sRun").addEventListener("click", (e) => runSort(e.currentTarget));
    $$("#tool-searching [data-mode]").forEach((b) => b.addEventListener("click", () => runSearch(b.dataset.mode)));
    $("#qBst").addEventListener("click", runBstSearch);
    $("#bRange").addEventListener("click", runRange);
    $("#bTraverse").addEventListener("click", runTraverse);
    $("#bOrder").addEventListener("change", () => { bstMark.order = $("#bOrder").value; drawTree(); });
    $("#hLocate").addEventListener("click", locate);
    $("#hDemo").addEventListener("click", collisionDemo);
    $("#qRace").innerHTML = raceHtml([]);
  }

  async function onShow() {
    const [g, o] = await Promise.all([graph ? null : api("/api/graph"), api("/api/overview")]);
    if (g && g.ok) graph = g;
    if (o && o.ok) { zoneCritical = {}; o.zones.forEach((z) => (zoneCritical[z.name] = z.critical)); }
    if (graph) {
      $("#gStats").innerHTML = `<div><span class="label">Zones</span><b>${graph.zoneCount}</b></div><div><span class="label">Walkways</span><b>${graph.edgeCount}</b></div><div><span class="label">Parts</span><b>${graph.components}</b></div>`;
      drawGraph();
    }
    loadTree();
  }

  function selectTool(key) {
    $$("#page-reports .seg-tab").forEach((t) => { const on = t.dataset.tool === key; t.classList.toggle("active", on); t.setAttribute("aria-selected", on); });
    $$("#page-reports .tool").forEach((t) => t.classList.toggle("active", t.id === "tool-" + key));
  }

  // Graph
  const gx = (z) => 70 + (POS[z][0] / 10) * 860;
  const gy = (z) => 50 + (POS[z][1] / 6.7) * 520;

  function drawGraph(path = [], dashed = false) {
    if (!graph) return;
    const from = $("#gFrom").value, to = $("#gTo").value;
    const onPath = new Set();
    for (let i = 0; i < path.length - 1; i++) onPath.add([path[i], path[i + 1]].sort().join("|"));
    const maxCrit = Math.max(1, ...Object.values(zoneCritical));
    let s = `<defs><filter id="glowF" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter></defs>`;
    graph.edges.forEach(([a, b]) => {
      if (!POS[a] || !POS[b] || onPath.has([a, b].sort().join("|"))) return;
      s += `<line x1="${gx(a)}" y1="${gy(a)}" x2="${gx(b)}" y2="${gy(b)}" stroke="rgba(255,255,255,0.12)" stroke-width="2"/>`;
    });
    for (let i = 0; i < path.length - 1; i++) {
      const a = path[i], b = path[i + 1];
      s += `<line x1="${gx(a)}" y1="${gy(a)}" x2="${gx(b)}" y2="${gy(b)}" stroke="#C8FF00" stroke-width="10" stroke-linecap="round" opacity=".25" filter="url(#glowF)"/>`;
      s += `<line class="${dashed ? "flow" : ""}" x1="${gx(a)}" y1="${gy(a)}" x2="${gx(b)}" y2="${gy(b)}" stroke="#C8FF00" stroke-width="4" stroke-linecap="round"/>`;
    }
    graph.zones.forEach((z) => {
      if (!POS[z]) return;
      const x = gx(z), y = gy(z), step = path.indexOf(z), crit = zoneCritical[z] || 0;
      const isEnd = z === from || z === to;
      const ring = 8 + (crit / maxCrit) * 18;
      if (crit) s += `<circle cx="${x}" cy="${y}" r="${ring + 6}" fill="rgba(242,92,97,${0.05 + (crit / maxCrit) * 0.12})"/>`;
      if (step >= 0) {
        s += `<circle cx="${x}" cy="${y}" r="16" fill="${dashed ? "#111111" : "#C8FF00"}" stroke="#C8FF00" stroke-width="3"/>
              <text x="${x}" y="${y + 5}" text-anchor="middle" font-size="13" font-weight="800" fill="${dashed ? "#C8FF00" : "#0A0A0A"}">${step + 1}</text>`;
      } else {
        s += `<circle cx="${x}" cy="${y}" r="9" fill="#141414" stroke="${isEnd ? "#C8FF00" : crit > maxCrit * 0.6 ? "#FF5A5A" : "#5C5C5C"}" stroke-width="${isEnd ? 3 : 2}"/>`;
      }
      const anchor = POS[z][0] >= 9.4 ? "end" : POS[z][0] <= 0.2 ? "start" : "middle";
      const ly = POS[z][1] > 6 ? y + 36 : y - 24;
      s += `<text class="zlabel" x="${x}" y="${ly}" text-anchor="${anchor}" font-size="13" font-weight="${step >= 0 || isEnd ? 700 : 500}" fill="${step >= 0 || isEnd ? "#FFFFFF" : "#8F8F8F"}">${esc(z)}${crit ? ` · ${crit}` : ""}</text>`;
    });
    $("#gSvg").innerHTML = s;
  }

  async function route(algo) {
    const from = $("#gFrom").value, to = $("#gTo").value;
    const r = await api(`/api/graph/route?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&algo=${algo}`);
    if (!r.ok) { toast(r.message, true); return; }
    drawGraph(r.path, algo === "dfs");
    $("#gRoute").innerHTML = r.path.length
      ? r.path.map((z, i) => `<span class="stop ${i === 0 || i === r.path.length - 1 ? "end" : ""}">${esc(z)}</span>`).join(`<span class="arrow">→</span>`)
      : `<span class="muted">${esc(to)} can't be reached from ${esc(from)}.</span>`;
    $("#gOut").textContent = (algo === "bfs" ? `BFS · ${r.hops} hop(s) · fewest possible` : `DFS · ${r.hops} hop(s) · BFS shortest is ${r.bfsHops}`)
      + `\nVisit order from ${from}: ${r.visitOrder.join(", ")}`;
  }

  // Sorting
  async function runSort(btn) {
    const algo = $("#sAlgo").value, order = $("#sOrder").value, limit = Math.max(1, intOf("#sLimit"));
    btn.disabled = true;
    btn.innerHTML = `${ICON.clock} Sorting…`;
    const r = await api(`/api/sort?algo=${algo}&order=${order}&limit=${limit}`);
    btn.disabled = false;
    btn.innerHTML = `${ICON.play} Run sort`;
    if (!r.ok) { toast(r.message, true); return; }
    $("#sTime").textContent = r.ms.toFixed(1) + " ms";
    $("#sCount").textContent = fmt(r.count);
    $("#sBody").innerHTML = r.rows.map((e, i) => `<tr><td class="muted">${i + 1}</td><td class="mono" style="color:var(--lime)">${esc(e.id)}</td><td>${esc(e.name)}</td><td>${condPill(e.condition)}</td><td class="num mono">${fmt(e.usage)}</td></tr>`).join("");
  }

  // Searching
  function raceHtml(results) {
    const label = { hash: "Hash", binary: "Binary", linear: "Linear" };
    const max = Math.max(1, ...results.map((r) => r.ns));
    return ["hash", "binary", "linear"].map((m) => {
      const r = results.find((x) => x.mode === m);
      const pct = r ? Math.max(2, (r.ns / max) * 100) : 0;
      const slow = r && results.length > 1 && r.ns === max;
      return `<div class="race-row"><b>${label[m]}</b><div class="bar ${slow ? "red" : ""}"><i style="width:${pct}%"></i></div><span>${r ? fmt(r.ns) + " ns" : "–"}</span></div>`;
    }).join("");
  }

  async function runSearch(mode) {
    const id = $("#qId").value.trim();
    if (!id) { toast("Type an ID first.", true); return; }
    const r = await api(`/api/search?id=${encodeURIComponent(id)}&mode=${mode}`);
    if (!r.ok) return;
    $("#qRace").innerHTML = raceHtml(r.results);
    const f = r.results[0].found;
    $("#qOut").textContent = r.results.map((x) => `${x.mode.padEnd(7)} ${fmt(x.ns).padStart(10)} ns   ${x.found ? "found" : "not found"}`).join("\n")
      + "\n\n" + (f ? `${f.id} · ${f.name}\n${pretty(f.category)} · ${f.location} · ${pretty(f.condition)} · used ${fmt(f.usage)}x` : `${id} is not in the table.`)
      + (mode === "all" || mode === "binary" ? "\n\nBinary search needs the array sorted by ID first; that sort isn't timed." : "");
  }

  async function runBstSearch() {
    const u = intOf("#qUsage");
    if (u < 0) { toast("Usage must be a whole number, 0 or more.", true); return; }
    const r = await api("/api/search/usage?usage=" + u);
    if (!r.ok) { toast(r.message, true); return; }
    $("#qOut").textContent = `${fmt(r.count)} item(s) used exactly ${fmt(u)} times (${fmt(r.ns)} ns)\nPath from the root: ${r.path.join(" → ")} (${r.path.length} nodes)\n\n`
      + r.rows.map((e) => `  ${e.id}  ${e.name}  [${pretty(e.condition)}]`).join("\n");
    bstMark.path = r.path; bstMark.range = null;
    drawTree();
  }

  // BST
  async function loadTree() {
    tree = await api("/api/bst/tree");
    if (!tree.ok) return;
    $("#bStats").innerHTML = `<div><span class="label">Nodes</span><b>${fmt(tree.nodes)}</b></div><div><span class="label">Height</span><b>${tree.height}</b></div><div><span class="label">Root</span><b>${fmt(tree.root)}</b></div>`;
    drawTree();
  }

  function visitOrder(order) {
    const out = [], L = tree.levels;
    (function walk(i) {
      if (i >= L.length || L[i] < 0) return;
      if (order === "preorder") out.push(i);
      walk(2 * i + 1);
      if (order === "inorder") out.push(i);
      walk(2 * i + 2);
      if (order === "postorder") out.push(i);
    })(0);
    return out;
  }

  function drawTree() {
    if (!tree || !tree.ok) return;
    const L = tree.levels, W = 960, top = 30, gap = 68;
    const pos = (i) => { const d = Math.floor(Math.log2(i + 1)), k = i - ((1 << d) - 1); return [((k + 0.5) / (1 << d)) * W, top + d * gap]; };
    const rank = {}; visitOrder(bstMark.order).forEach((idx, n) => (rank[idx] = n + 1));
    const hit = (k) => (bstMark.range && k >= bstMark.range[0] && k <= bstMark.range[1]) || bstMark.path.includes(k);
    let lines = "", nodes = "";
    L.forEach((k, i) => {
      if (k < 0) return;
      const [x, y] = pos(i);
      [2 * i + 1, 2 * i + 2].forEach((c) => {
        if (c < L.length && L[c] >= 0) {
          const [cx, cy] = pos(c);
          const lit = bstMark.path.includes(k) && bstMark.path.includes(L[c]);
          lines += `<line x1="${x}" y1="${y}" x2="${cx}" y2="${cy}" stroke="${lit ? "#C8FF00" : "rgba(255,255,255,0.14)"}" stroke-width="${lit ? 3 : 1.5}"/>`;
        }
      });
      const r = i < 15 ? 20 : 15, on = hit(k);
      nodes += `<circle cx="${x}" cy="${y}" r="${r}" fill="${on ? "#C8FF00" : i === 0 ? "rgba(200,255,0,0.15)" : "#161616"}" stroke="${on || i === 0 ? "#C8FF00" : "rgba(255,255,255,0.18)"}" stroke-width="1.5"/>
        <text x="${x}" y="${y + 4}" text-anchor="middle" font-size="${i < 15 ? 11.5 : 9.5}" font-weight="700" fill="${on ? "#0A0A0A" : "#FFFFFF"}">${k}</text>
        <text x="${x + r - 2}" y="${y - r + 2}" font-size="10" font-weight="700" fill="#C8FF00">${rank[i] || ""}</text>`;
    });
    const shown = L.filter((k) => k >= 0).length;
    $("#bSvg").innerHTML = lines + nodes + `<text x="${W / 2}" y="${top + 4 * gap + 40}" text-anchor="middle" font-size="12" fill="#6B6B6B">… ${fmt(tree.nodes - shown)} more nodes below · height ${tree.height}</text>`;
  }

  async function runRange() {
    const lo = intOf("#bMin"), hi = intOf("#bMax");
    if (lo < 0 || hi < 0) { toast("Min and max must be whole numbers.", true); return; }
    const r = await api(`/api/bst/range?min=${lo}&max=${hi}`);
    if (!r.ok) { toast(r.message, true); return; }
    bstMark.range = [lo, hi]; bstMark.path = [];
    drawTree();
    $("#bOut").textContent = `${fmt(r.count)} item(s) used ${fmt(lo)}–${fmt(hi)} times, ascending\n`
      + r.rows.map((e) => `  ${e.id}  ${String(fmt(e.usage)).padStart(6)}  ${e.name}`).join("\n")
      + (r.count > r.rows.length ? `\n  … ${fmt(r.count - r.rows.length)} more` : "");
  }

  async function runTraverse() {
    const order = $("#bOrder").value;
    const r = await api("/api/bst/traverse?order=" + order);
    if (!r.ok) return;
    bstMark.order = order;
    drawTree();
    let s = `First ${r.keys.length} of ${fmt(r.total)} keys (${order}):\n`;
    r.keys.forEach((k, i) => { s += String(k).padStart(6) + ((i + 1) % 10 === 0 ? "\n" : ""); });
    $("#bOut").textContent = s;
  }

  // Hash
  function drawBuckets(buckets, hitIndex, key) {
    $("#hBuckets").innerHTML = buckets.map((b) => `
      <div class="bucket ${b.index === hitIndex ? "hit" : ""}">
        <span class="bucket-idx">[${fmt(b.index)}]</span>
        <div class="chain">${b.keys.length ? b.keys.map((k) => `<span class="node ${k === key ? "key" : ""}">${esc(k)}</span>`).join(`<span class="arrow">→</span>`) + `<span class="arrow">→</span><span class="null">null</span>` : `<span class="null">empty</span>`}</div>
      </div>`).join("");
  }

  async function locate() {
    const id = $("#hId").value.trim();
    const r = await api("/api/hash/locate?id=" + encodeURIComponent(id));
    if (!r.ok) { toast(r.message, true); return; }
    $("#hStats").innerHTML = `<div><span class="label">Size</span><b>${fmt(r.size)}</b></div><div><span class="label">Capacity</span><b>${fmt(r.capacity)}</b></div>
      <div><span class="label">Load</span><b>${r.loadFactor.toFixed(2)}</b></div><div><span class="label">Collisions</span><b>${fmt(r.collisions)}</b></div>
      <div><span class="label">Longest</span><b>${r.longestChain}</b></div><div><span class="label">Resizes</span><b>${r.resizes}</b></div>`;
    drawBuckets(r.buckets, r.bucket, id);
    const chain = r.buckets.find((b) => b.index === r.bucket).keys;
    $("#hCaption").textContent = `${id} → bucket ${fmt(r.bucket)} of ${fmt(r.capacity)}`;
    $("#hOut").textContent = `${id} → bucket ${fmt(r.bucket)} (${r.present ? "present" : "not in the table"})\nChain (${chain.length}): ${chain.join(" → ") || "empty"}\n`
      + (chain.length > 1 ? "Collision: these keys share one bucket and are found by walking the chain." : "No collision in this bucket.");
  }

  async function collisionDemo() {
    const r = await api("/api/hash/demo");
    if (!r.ok) return;
    drawBuckets(r.buckets, r.target, r.collider);
    $("#hCaption").textContent = `Capacity 7 · EQ-00001 and ${r.collider} collide in bucket ${r.target}`;
    $("#hOut").textContent = `Inserted EQ-00001, ${r.collider}, EQ-00002 into 7 buckets\n`
      + r.buckets.map((b) => `  bucket ${b.index}: ${b.keys.join(" → ")}`).join("\n")
      + `\nget(${r.collider}) = ${r.getCollider}\nremove(${r.collider}) = ${r.removed}, EQ-00001 still found = ${r.stillFound}`;
  }

  return { title: "Floor & reports", eyebrow: "graph · sorting · searching · BST · hash table", init, onShow };
})();
