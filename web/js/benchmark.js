// Benchmarks page: runs the Java benchmark and shows the timings next to the Big-O table.

App.pages.benchmark = (() => {
  const SORTS = [["bubbleMs", "Bubble", "#5C5C5C"], ["insertionMs", "Insertion", "#FFFFFF"], ["insertionIdMs", "Insertion by ID", "#C8FF00"]];
  const SEARCHES = [["linearNs", "Linear", "#5C5C5C"], ["binaryNs", "Binary", "#9E9E9E"], ["bstNs", "BST", "#FFFFFF"], ["hashNs", "Hash", "#C8FF00"]];
  let loaded = false;

  const legend = (series) => `<div style="display:flex;flex-wrap:wrap;gap:16px;font-size:13px;color:var(--text-2)">${series.map(([, l, c]) =>
    `<span style="display:flex;align-items:center;gap:6px"><i style="width:10px;height:10px;border-radius:3px;background:${c}"></i>${l}</span>`).join("")}</div>`;

  function init() {
    $("#page-benchmark").innerHTML = `
      <div class="bento">
        <section class="card glass glow s12">
          <div class="card-head" style="margin-bottom:6px">
            <div><h2 class="card-title">Measured running times</h2><div class="card-sub" id="bmStatus">Not run yet. Takes a few seconds on 5,000 records.</div></div>
          </div>
          <div class="table-wrap" style="margin-top:14px"><table>
            <thead><tr><th class="num">Records</th><th class="num">Bubble ms</th><th class="num">Insertion ms</th><th class="num">Insertion ID ms</th>
              <th class="num">Linear ns</th><th class="num">Binary ns</th><th class="num">Hash ns</th><th class="num">BST ns</th></tr></thead>
            <tbody id="bmBody"><tr><td colspan="8"><div class="empty-state">${ICON.clock}Run the benchmark to measure on this machine.</div></td></tr></tbody>
          </table></div>
        </section>
        <section class="card glass s6"><div class="card-head"><h2 class="card-title">Sorting time</h2><span class="card-sub">milliseconds · grows like n²</span></div>
          ${legend(SORTS)}<svg class="svg-viz" id="bmSort" viewBox="0 0 520 260" role="img" aria-label="Sorting time chart" style="margin-top:12px"></svg></section>
        <section class="card glass s6"><div class="card-head"><h2 class="card-title">Search time</h2><span class="card-sub">nanoseconds per lookup · log scale</span></div>
          ${legend(SEARCHES)}<svg class="svg-viz" id="bmSearch" viewBox="0 0 520 260" role="img" aria-label="Search time chart" style="margin-top:12px"></svg></section>
        <section class="card glass s12"><div class="card-head"><h2 class="card-title">Theoretical complexity</h2><span class="card-sub">h = BST height: log n if balanced, n if not</span></div>
          <div class="table-wrap"><table>
            <thead><tr><th>Operation</th><th>Structure</th><th>Best</th><th>Average</th><th>Worst</th><th>Space</th></tr></thead>
            <tbody id="bmComplexity"></tbody>
          </table></div></section>
      </div>`;
  }

  async function onShow() {
    if (loaded) return;
    loaded = true;
    const r = await api("/api/complexity");
    if (r.ok) $("#bmComplexity").innerHTML = r.rows.map((row) =>
      `<tr><td style="font-weight:500">${row[0]}</td><td class="muted">${row[1]}</td>${row.slice(2).map((c) => `<td class="mono">${c}</td>`).join("")}</tr>`).join("");
  }

  async function run(btn) {
    btn.disabled = true;
    btn.innerHTML = `<span class="skel" style="width:16px;height:16px;border-radius:50%"></span> Running…`;
    $("#bmStatus").textContent = "Running… bubble sort on 5,000 records takes a moment.";
    const r = await api("/api/benchmark", {});
    btn.disabled = false;
    btn.innerHTML = `${ICON.play} Run again`;
    if (!r.ok) { $("#bmStatus").textContent = r.message; toast(r.message, true); return; }
    toast("Benchmark finished. Saved to " + r.savedTo);
    $("#bmStatus").textContent = "Saved to " + r.savedTo;
    $("#bmBody").innerHTML = r.rows.map((x) => `<tr>
      <td class="num"><b>${fmt(x.size)}</b></td>
      <td class="num mono">${x.bubbleMs.toFixed(3)}</td><td class="num mono">${x.insertionMs.toFixed(3)}</td><td class="num mono">${x.insertionIdMs.toFixed(3)}</td>
      <td class="num mono">${fmt(Math.round(x.linearNs))}</td><td class="num mono">${fmt(Math.round(x.binaryNs))}</td>
      <td class="num mono">${fmt(Math.round(x.hashNs))}</td><td class="num mono">${fmt(Math.round(x.bstNs))}</td></tr>`).join("");
    chart("#bmSort", r.rows, SORTS, false, (v) => v.toFixed(v < 10 ? 2 : 0) + " ms");
    chart("#bmSearch", r.rows, SEARCHES, true, (v) => fmt(Math.round(v)) + " ns");
  }

  // Grouped bars, one group per record count.
  function chart(sel, rows, series, logScale, label) {
    const W = 520, H = 260, left = 56, bottom = 30, top = 12, plotH = H - top - bottom, plotW = W - left - 10;
    const vals = rows.flatMap((r) => series.map(([k]) => Math.max(r[k], 0.001)));
    const max = Math.max(...vals), min = logScale ? Math.min(...vals) : 0;
    const lo = logScale ? Math.log10(min) - 0.3 : 0, hi = logScale ? Math.log10(max) : max;
    const scale = (v) => logScale ? (Math.log10(Math.max(v, 0.001)) - lo) / (hi - lo) : v / max;
    let s = "";
    for (let i = 0; i <= 4; i++) {
      const y = top + plotH - (plotH * i) / 4;
      const v = logScale ? Math.pow(10, lo + ((hi - lo) * i) / 4) : (max * i) / 4;
      s += `<line x1="${left}" x2="${W - 10}" y1="${y}" y2="${y}" stroke="rgba(255,255,255,0.06)"/><text x="${left - 8}" y="${y + 4}" text-anchor="end" font-size="10" fill="#6B6B6B">${logScale ? fmt(Math.round(v)) : v.toFixed(0)}</text>`;
    }
    const gw = plotW / rows.length, bw = Math.min(22, (gw - 16) / series.length);
    rows.forEach((r, gi) => {
      const x0 = left + gi * gw + (gw - bw * series.length) / 2;
      series.forEach(([k, name, color], si) => {
        const h = Math.max(1.5, scale(r[k]) * plotH);
        s += `<rect x="${x0 + si * bw + 1}" y="${top + plotH - h}" width="${bw - 2}" height="${h}" rx="4" fill="${color}"
          style="transform-box:fill-box;transform-origin:bottom;animation:barUp .8s var(--ease) ${(gi * series.length + si) * 0.03}s both"><title>${name} · n=${fmt(r.size)} · ${label(r[k])}</title></rect>`;
      });
      s += `<text x="${left + gi * gw + gw / 2}" y="${H - 10}" text-anchor="middle" font-size="11" fill="#8F8F8F">n = ${fmt(r.size)}</text>`;
    });
    $(sel).innerHTML = `<style>@keyframes barUp{from{transform:scaleY(0)}to{transform:scaleY(1)}}</style>` + s;
  }

  return {
    title: "Benchmarks",
    eyebrow: "measured vs Big-O",
    actions: () => `<button class="btn lime" id="hdRunBench">${ICON.play} Run benchmarks</button>`,
    bindActions: () => $("#hdRunBench").addEventListener("click", (e) => run(e.currentTarget)),
    init,
    onShow,
  };
})();
