package web;

import auth.FileUserRepository;
import auth.Role;
import auth.SessionManager;
import auth.User;
import auth.UserRepository;
import auth.UserStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import db.DatabaseConnector;
import db.DatasetImporter;
import db.EquipmentDAO;
import db.MaintenanceDAO;
import db.UserDAO;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Equipment;
import model.MaintenanceRecord;
import service.EquipmentStore;
import service.MaintenanceManager;

// Loads the data once, then serves the web/ folder and answers /api/... requests with JSON.
// Every request passes the sign-in gate in handle(). Uses the JDK's built-in HTTP server.
public class WebServer {

    private static final String CSV_PATH = "data/equipment_dataset.csv";
    private static final int MAX_BODY_BYTES = 16 * 1024;     // every form we accept is tiny

    // One store and one manager shared by every request.
    private final EquipmentStore store = new EquipmentStore();
    private final MaintenanceManager manager = new MaintenanceManager(store);
    private EquipmentDAO dao = new EquipmentDAO();            // both null when offline (CSV):
    private MaintenanceDAO logDao = new MaintenanceDAO();     // changes aren't saved then
    private String loadMessage = "";

    private final ApiRoutes api;
    private final AuthRoutes auth;
    private final UserStore users;
    private final SessionManager sessions = new SessionManager();
    private final Path webRoot = Paths.get("web").toAbsolutePath().normalize();

    // Pages and files anyone may load before signing in.
    private static final String[] PUBLIC_FILES = {"/login.html", "/js/login.js", "/css/", "/img/"};

    public WebServer() {
        loadData();
        users = new UserStore(userRepository());
        api = new ApiRoutes(store, manager, dao, logDao, loadMessage);
        auth = new AuthRoutes(users, sessions, api);
        System.out.println("Accounts: " + users.size() + " saved in " + users.storageName());
    }

    // MySQL first, the CSV if MySQL can't be reached. A brand-new, empty database
    // (e.g. a fresh Azure MySQL server) is filled from the CSV once.
    private void loadData() {
        List<Equipment> all;
        try {
            DatabaseConnector.ensureTables();
            all = dao.findAll();
            if (all.isEmpty()) {
                all = DatasetImporter.readCsv(CSV_PATH);
                int added = DatasetImporter.insertAll(all);
                loadMessage = "New database: copied " + added + " records from the CSV into MySQL.";
            } else {
                loadMessage = "Loaded " + all.size() + " records from MySQL.";
            }
        } catch (Exception dbError) {
            System.out.println("MySQL not used: " + dbError.getMessage());
            dao = null;
            logDao = null;
            try {
                all = DatasetImporter.readCsv(CSV_PATH);
                loadMessage = "MySQL unavailable, offline mode: loaded " + all.size()
                        + " records from the CSV. Changes will NOT be saved.";
            } catch (Exception csvError) {
                loadMessage = "Could not load any data: " + csvError.getMessage();
                System.out.println(loadMessage);
                return;
            }
        }
        for (Equipment e : all) store.addEquipment(e);   // array + hash table + BST

        if (logDao != null) {
            try {
                for (MaintenanceRecord r : logDao.findAll()) manager.addRecord(r);
            } catch (Exception e) {
                System.out.println("Couldn't read the service history: " + e.getMessage());
            }
        }
        System.out.println(loadMessage);
    }

    // Accounts live in MySQL when it's up. The first time MySQL is used, accounts made
    // offline in users.txt are copied over so nobody has to sign up again.
    private UserRepository userRepository() {
        String dir = System.getenv("LIMEGYM_DATA_DIR");
        FileUserRepository file = new FileUserRepository(Paths.get(dir == null || dir.isEmpty() ? "." : dir, "users.txt"));
        if (dao == null) return file;
        UserDAO db = new UserDAO();
        if (db.loadAll().isEmpty() && file.exists()) {
            db.saveAll(file.loadAll());
            System.out.println("Copied the accounts from users.txt into MySQL.");
        }
        return db;
    }

    // Requests run one at a time on purpose: our structures aren't thread-safe.
    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.start();
        System.out.println("Lime Gym web app running at http://localhost:" + port);
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            securityHeaders(ex);
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String token = readCookie(ex, SessionManager.COOKIE);
            User user = users.get(sessions.check(token));

            if (!path.startsWith("/api/")) {
                serveFile(ex, path, user);
                return;
            }

            // A page on another site can make the browser send our cookie, but it can't add
            // this header, so every change must come from our own scripts (CSRF protection).
            boolean post = "POST".equalsIgnoreCase(method);
            if (post && !"LimeGym".equals(ex.getRequestHeaders().getFirst("X-Requested-With"))) {
                sendJson(ex, 403, error("Request blocked."));
                return;
            }

            Map<String, String> params;
            try {
                params = parseQuery(ex.getRequestURI().getRawQuery());
                if (post) params.putAll(parseQuery(readBody(ex)));   // form fields sent by fetch()
            } catch (IllegalArgumentException badInput) {           // too big, or broken %-encoding
                sendJson(ex, 400, error(badInput.getMessage()));
                return;
            }

            if (path.startsWith("/api/auth/") || path.startsWith("/api/users")) {
                AuthRoutes.Result r = auth.handle(method, path, params, user, token, clientAddress(ex));
                if (r.setCookieToken != null) {
                    sessions.end(token);                         // a fresh session on every sign-in
                    setCookie(ex, r.setCookieToken, 8 * 60 * 60);
                }
                if (r.clearCookie) setCookie(ex, "", 0);
                sendJson(ex, r.response.status, r.response.body);
                return;
            }

            if (user == null) {
                sendJson(ex, 401, error("Please sign in."));
                return;
            }
            Role needed = ApiRoutes.requiredRole(method, path);
            if (!user.getRole().atLeast(needed)) {
                sendJson(ex, 403, error("Your account (" + user.getRole().name().toLowerCase()
                        + ") can't do this. Ask an admin."));
                return;
            }
            ApiRoutes.Response r = api.handle(method, path, params, user.getUsername());
            sendJson(ex, r.status, r.body);
        } catch (Exception e) {
            e.printStackTrace();                                 // details stay in the server log
            sendJson(ex, 500, error("Something went wrong on the server."));
        }
    }

    private void serveFile(HttpExchange ex, String path, User user) throws IOException {
        boolean isPublic = false;
        for (String p : PUBLIC_FILES) if (path.startsWith(p)) isPublic = true;
        if (user == null && !isPublic) {
            redirect(ex, "/login.html");
            return;
        }
        if (user != null && path.equals("/login.html")) {
            redirect(ex, "/");
            return;
        }
        if (path.equals("/")) path = "/index.html";
        Path file = webRoot.resolve(path.substring(1)).normalize();
        // Never serve files outside web/ (blocks things like /../db.properties).
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, contentType(file.toString()), Files.readAllBytes(file));
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "text/javascript; charset=utf-8";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    // Stops other sites from framing the app, blocks MIME sniffing, and limits where
    // scripts, styles and fonts may load from. Behind HTTPS, HSTS tells the browser
    // to never use plain HTTP for this site again.
    private static void securityHeaders(HttpExchange ex) {
        if (isHttps(ex)) ex.getResponseHeaders().set("Strict-Transport-Security", "max-age=31536000");
        ex.getResponseHeaders().set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("X-Frame-Options", "DENY");
        ex.getResponseHeaders().set("Referrer-Policy", "same-origin");
        ex.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self' data:; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "font-src https://fonts.gstatic.com; frame-ancestors 'none'; form-action 'self'");
    }

    // HttpOnly: page scripts can't read the token. SameSite=Strict: other sites can't send it.
    // Secure is added when the app runs behind HTTPS (e.g. on Azure).
    private static void setCookie(HttpExchange ex, String value, int maxAgeSeconds) {
        ex.getResponseHeaders().add("Set-Cookie", SessionManager.COOKIE + "=" + value
                + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSeconds + (isHttps(ex) ? "; Secure" : ""));
    }

    // Azure ends HTTPS at its front door and passes the request on as HTTP with this header.
    private static boolean isHttps(HttpExchange ex) {
        return "https".equalsIgnoreCase(ex.getRequestHeaders().getFirst("X-Forwarded-Proto"));
    }

    // The caller's IP. Behind Azure's proxy the real one is the last entry the proxy added
    // to X-Forwarded-For (earlier entries can be typed in by anyone). Without a proxy
    // (on a PC) the header is ignored, since then anyone could fake it.
    private static final boolean BEHIND_PROXY = System.getenv("WEBSITE_SITE_NAME") != null;   // set by Azure

    private static String clientAddress(HttpExchange ex) {
        String forwarded = ex.getRequestHeaders().getFirst("X-Forwarded-For");
        if (BEHIND_PROXY && forwarded != null && !forwarded.trim().isEmpty()) {
            String[] parts = forwarded.split(",");
            String last = parts[parts.length - 1].trim();
            int colon = last.indexOf(':');
            return colon > 0 && colon == last.lastIndexOf(':') ? last.substring(0, colon) : last;   // drop ":port" from IPv4
        }
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String readCookie(HttpExchange ex, String name) {
        String header = ex.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private static String error(String message) {
        return new Json().obj().put("ok", false).put("message", message).end().toString();
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    // Reads at most MAX_BODY_BYTES, so nobody can fill the server's memory with one request.
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] body = in.readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) throw new IllegalArgumentException("Request is too large.");
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    // "id=EQ-00001&note=Routine+service" -> {id=EQ-00001, note=Routine service}
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            try {
                map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Malformed request.");
            }
        }
        return map;
    }

    // 8080 on a PC; Azure sets PORT.
    public static void main(String[] args) throws IOException {
        String env = System.getenv("PORT");
        int port = env != null && !env.isEmpty() ? Integer.parseInt(env) : 8080;
        new WebServer().start(port);
    }
}
