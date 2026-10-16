// Sign-in page. On a fresh install (no accounts yet) the same form creates the first admin.
// The server sets an HttpOnly session cookie, so this script never sees or stores a token.

const el = (id) => document.getElementById(id);
let setupMode = false;

async function post(path, data) {
  try {
    const res = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "X-Requested-With": "LimeGym" },
      body: new URLSearchParams(data).toString(),
    });
    return await res.json();
  } catch (e) {
    return { ok: false, message: "Can't reach the server. Is Lime Gym running?" };
  }
}

function showError(message) {
  el("loginError").textContent = message || "";
  const card = document.querySelector(".login-card");
  card.classList.remove("shake");
  if (message) { void card.offsetWidth; card.classList.add("shake"); }
}

function useSetupMode() {
  setupMode = true;
  el("formTab").textContent = "First run";
  el("formTitle").textContent = "Create the admin account";
  el("formSub").textContent = "No accounts exist yet. This first one can add everyone else.";
  el("confirmField").hidden = false;
  el("pwHint").hidden = false;
  el("password").autocomplete = "new-password";
  el("submitLabel").textContent = "Create admin & sign in";
  el("loginFoot").hidden = true;
}

el("pwToggle").addEventListener("click", () => {
  const show = el("password").type === "password";
  el("password").type = show ? "text" : "password";
  el("pwToggle").setAttribute("aria-pressed", String(show));
  el("pwToggle").setAttribute("aria-label", show ? "Hide password" : "Show password");
});

// Warn about Caps Lock, the usual reason a correct password "doesn't work".
for (const id of ["password", "confirm"]) {
  el(id).addEventListener("keyup", (e) => {
    if (e.getModifierState) el("capsHint").hidden = !e.getModifierState("CapsLock");
  });
}

el("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = el("username").value.trim();
  const password = el("password").value;
  if (!username || !password) { showError("Enter your username and password."); return; }
  if (setupMode) {
    if (password.length < 8) { showError("Password must be at least 8 characters."); return; }
    if (password !== el("confirm").value) { showError("The passwords don't match."); return; }
  }

  el("submitBtn").disabled = true;
  showError("");
  const r = await post(setupMode ? "/api/auth/setup" : "/api/auth/login", { username, password });
  el("submitBtn").disabled = false;

  if (r.ok) {
    location.replace("/");
    return;
  }
  el("password").value = "";
  el("confirm").value = "";
  el("password").focus();
  showError(r.message);
});

// Already signed in, or nobody has set up the system yet?
(async () => {
  try {
    const s = await (await fetch("/api/auth/state")).json();
    if (s.user) { location.replace("/"); return; }
    if (s.needsSetup) useSetupMode();
  } catch (e) {
    showError("Can't reach the server. Is Lime Gym running?");
  }
  el("username").focus();
})();
