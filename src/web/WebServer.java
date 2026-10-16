package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import db.DatasetImporter;
import db.EquipmentDAO;
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
import service.EquipmentStore;
import service.MaintenanceManager;

// Loads the data once, then serves the web/ folder and answers /api/... requests with JSON.
// Uses the JDK's built-in HTTP server, no frameworks.
public class WebServer {

    // One store and one manager shared by every request.
    private final EquipmentStore store = new EquipmentStore();
    private final MaintenanceManager manager = new MaintenanceManager(store);
    private EquipmentDAO dao = new EquipmentDAO();   // null when offline (CSV), changes aren't saved
    private String loadMessage = "";

    private final ApiRoutes api;
    private final Path webRoot = Paths.get("web").toAbsolutePath().normalize();

    public WebServer() {
        loadData();
        api = new ApiRoutes(store, manager, () -> dao, loadMessage);
    }

    // MySQL first, the CSV if MySQL can't be reached.
    private void loadData() {
        List<Equipment> all;
        try {
            all = dao.findAll();
            loadMessage = "Loaded " + all.size() + " records from MySQL.";
        } catch (Exception dbError) {
            dao = null;
            try {
                all = DatasetImporter.readCsv("data/equipment_dataset.csv");
                loadMessage = "MySQL unavailable, offline mode: loaded " + all.size()
                        + " records from the CSV. Changes will NOT be saved.";
            } catch (Exception csvError) {
                loadMessage = "Could not load any data: " + csvError.getMessage();
                System.out.println(loadMessage);
                return;
            }
        }
        for (Equipment e : all) store.addEquipment(e);   // array + hash table + BST
        System.out.println(loadMessage);
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
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
                if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                    params.putAll(parseQuery(readBody(ex)));   // form fields sent by fetch()
                }
                ApiRoutes.Response r = api.handle(ex.getRequestMethod(), path, params);
                send(ex, r.status, "application/json; charset=utf-8", r.body.getBytes(StandardCharsets.UTF_8));
            } else {
                serveFile(ex, path);
            }
        } catch (Exception e) {
            String msg = new Json().obj().put("ok", false).put("message", "Server error: " + e).end().toString();
            send(ex, 500, "application/json; charset=utf-8", msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void serveFile(HttpExchange ex, String path) throws IOException {
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

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
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
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
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
