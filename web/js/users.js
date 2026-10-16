// Users page (admins only): add accounts, change roles, remove people.
// The server checks the role on every call; hiding this page is only for convenience.

App.pages.users = (() => {
  const ROLES = [["VIEWER", "Viewer"], ["STAFF", "Staff"], ["ADMIN", "Admin"]];
  const ROLE_INFO = {
    ADMIN: "Everything, plus deleting equipment, clearing lists and managing users.",
    STAFF: "Flag, promote and service equipment; register and edit machines.",
    VIEWER: "Can look at every page but can't change anything.",
  };

  function init() {
    $("#page-users").innerHTML = `
      <div class="bento">
        <section class="folder s8" style="--tab: 170px">
          <div class="folder-tab glass">Accounts <span class="num" id="usCount">0</span></div>
          <div class="folder-body glass">
            <div class="table-wrap"><table class="tight">
              <thead><tr><th>User</th><th>Role</th><th class="num">Actions</th></tr></thead>
              <tbody id="usBody"><tr><td colspan="3"><div class="skel" style="height:44px"></div></td></tr></tbody>
            </table></div>
          </div>
        </section>

        <section class="card glass glow s4">
          <div class="card-head"><div><h2 class="card-title">Add a user</h2>
            <div class="card-sub">They can change their password after signing in.</div></div></div>
          <form class="form" id="usForm" novalidate>
            <label class="field"><span class="label">Username</span>
              <input class="input" name="username" autocomplete="off" autocapitalize="off" spellcheck="false" maxlength="32" placeholder="juan.dc" required></label>
            <label class="field"><span class="label">Temporary password</span>
              <input class="input" name="password" type="password" autocomplete="new-password" maxlength="128" placeholder="At least 8 characters" required></label>
            <label class="field"><span class="label">Role</span>
              <select class="select" name="role" id="usRole">${options(ROLES, "STAFF")}</select></label>
            <p class="hint" id="usRoleInfo">${ROLE_INFO.STAFF}</p>
            <p class="error-text" id="usError" role="alert"></p>
            <button class="btn lime" type="submit">${ICON.plus} Add user</button>
          </form>
        </section>

        <section class="card glass s12">
          <div class="card-head"><h2 class="card-title">What each role can do</h2><span class="card-sub">checked on the server for every request</span></div>
          <div class="role-grid">
            ${ROLES.slice().reverse().map(([r, label]) => `
              <div class="role-box flat"><span class="role-pill role-${r}">${label}</span><p>${ROLE_INFO[r]}</p></div>`).join("")}
          </div>
        </section>
      </div>`;

    $("#usRole").addEventListener("change", (e) => { $("#usRoleInfo").textContent = ROLE_INFO[e.target.value]; });

    $("#usForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      const form = e.currentTarget;
      const data = Object.fromEntries(new FormData(form));
      const r = await action("/api/users/add", data, e.submitter);
      $("#usError").textContent = r.ok ? "" : r.message;
      if (r.ok) { form.reset(); $("#usRoleInfo").textContent = ROLE_INFO.STAFF; load(); }
    });

    $("#usBody").addEventListener("change", async (e) => {
      const sel = e.target.closest("select[data-user]");
      if (!sel) return;
      const r = await action("/api/users/role", { username: sel.dataset.user, role: sel.value });
      if (!r.ok) load();
    });

    $("#usBody").addEventListener("click", async (e) => {
      const b = e.target.closest("[data-delete]");
      if (!b) return;
      const name = b.dataset.delete;
      const yes = await confirmDialog("Delete user?",
        `<p class="muted"><b style="color:var(--text)">${esc(name)}</b> will be signed out and won't be able to sign in again.</p>`, "Delete", true);
      if (!yes) return;
      await action("/api/users/delete", { username: name }, b);
      load();
    });
  }

  async function load() {
    const r = await api("/api/users");
    if (!r.ok) { $("#usBody").innerHTML = `<tr><td colspan="3" class="empty">${esc(r.message)}</td></tr>`; return; }
    $("#usCount").textContent = r.users.length;
    $("#usBody").innerHTML = r.users.map((u) => {
      const me = u.username === App.user.username;
      return `<tr>
        <td><span class="user-cell"><span class="avatar">${esc(u.username.charAt(0).toUpperCase())}</span>
          <b>${esc(u.username)}</b>${me ? `<span class="pill glassy">you</span>` : ""}</span></td>
        <td>${me ? `<span class="role-pill role-${u.role}">${pretty(u.role)}</span>`
          : `<select class="select sm-select" data-user="${esc(u.username)}" aria-label="Role for ${esc(u.username)}">${options(ROLES, u.role)}</select>`}</td>
        <td class="num">${me ? `<span class="hint">—</span>`
          : `<button class="btn danger sm" data-delete="${esc(u.username)}" aria-label="Delete ${esc(u.username)}">${ICON.trash}</button>`}</td>
      </tr>`;
    }).join("");
  }

  return {
    title: "Users",
    eyebrow: "accounts & roles",
    need: "ADMIN",
    init,
    onShow: load,
  };
})();
