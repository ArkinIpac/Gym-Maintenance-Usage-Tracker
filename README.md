# Lime Gym · Gym Equipment Maintenance Prioritization System

CCDATRCL Data Structures and Algorithms · Section ITE-201 · Java (custom data structures, MySQL) + web UI (HTML/CSS/JS)

A system that lets gym staff register, search and delete equipment, flag broken machines, and always service the most urgent one first. Flagged equipment waits in a FIFO queue, then a max-heap ranks it by **priority = severity × 10,000 + usage count**. Every service is logged to a linked-list history. The floor plan is a graph, so BFS gives the technician the fewest-hop route.

## Team

Jay Mark Lampa · Krissy Gomez · Kahlil Mendoza · Rhobe Arkin Ipac · John Benedict Jesalva

## Project layout

```
src/
  model/        Equipment, MaintenanceRecord
  structures/   CustomQueue, CustomLinkedList, CustomHeap, CustomBST, CustomHashTable, CustomGraph
  service/      EquipmentStore, MaintenanceManager, GymFloorPlan
  db/           DatabaseConnector, EquipmentDAO, MaintenanceDAO, UserDAO, DatasetImporter
  benchmark/    Benchmark
  auth/         Role, User, UserStore, UserRepository, FileUserRepository, PasswordHasher, SessionManager, LoginThrottle
  web/          WebServer (sign-in gate), ApiRoutes (every /api address), AuthRoutes (sign in, users), Json
  Main.java     start here: runs the web server and opens the browser
web/            the UI: login.html, index.html, css/style.css, js/ (one file per page), img/logo.svg
data/equipment_dataset.csv   5,000 records
lib/                         MySQL Connector/J
schema.sql                   the database tables (the app also creates them)
db.properties.example        template for your MySQL password
```

## How it works

`web.WebServer` loads the data into ONE `EquipmentStore` and ONE `MaintenanceManager` (MySQL first, the CSV if MySQL isn't reachable), then starts the JDK's built-in HTTP server. The browser calls `/api/...` with `fetch()`; `ApiRoutes` runs the store / manager methods and answers with JSON. No Spring and no extra libraries.

| Page | What it shows | Structures behind it |
|---|---|---|
| Overview | heap size, next up, fleet health, category folders, hot zones, activity | heap, array, hash table, linked list |
| Equipment | category folders, search, register / edit / delete, service history | array + hash table + BST in sync, linked list |
| Maintenance | Queue → Heap → Serviced as three folders | queue, max-heap, linked lists |
| Floor & reports | BFS / DFS routes on the floor plan, sorting, searching race, BST, hash buckets | graph, sorts, searches, BST, hash table |
| Benchmarks | measured times + charts next to the Big-O table | all of the above |
| Users (admins) | add accounts, change roles, reset passwords, remove people | hash table, linked list |

Press **Ctrl + K** anywhere to search equipment.

## Accounts and roles

Everyone signs in before seeing the dashboard. The first time the app runs there are no accounts, so the sign-in page asks you to **create the admin account**. After that, the admin adds everyone else on the **Users** page.

| Role | Can do |
|---|---|
| Admin | everything, plus deleting equipment, clearing the heap / serviced list, and managing users |
| Staff | flag, promote and service equipment; register and edit machines; run benchmarks |
| Member | gym members: look at every page, change nothing |

How it's protected (all plain Java, no libraries):

- Passwords are stored only as salted **PBKDF2-SHA256** hashes (120,000 rounds): in the `app_users` table when MySQL is connected, otherwise in `users.txt` (in `.gitignore`). Accounts made offline are copied into MySQL the first time it connects.
- Admins can reset someone's password from the Users page; the app can generate a strong temporary one.
- After signing in, the browser gets a random session cookie that is `HttpOnly` and `SameSite=Strict`. Sessions end after 30 minutes idle or 8 hours total.
- 5 wrong passwords lock the account for 5 minutes, and 20 failed sign-ins from one address block it for 10 minutes. The error never says whether the username exists.
- **Every** `/api` call is checked on the server: no session → 401, role too low → 403. Hidden buttons are only a convenience.
- Changes must carry an `X-Requested-With` header that other websites can't add (CSRF protection), and every response sends security headers (CSP, no framing, no sniffing; HSTS behind HTTPS).
- Request bodies are capped at 16 KB, every form field is validated on the server, and database errors are logged on the server instead of being shown in the browser.

> The username and password your browser fills in on the sign-in page come from the **browser's own password manager** (Chrome/Edge offered to save them). Lime Gym never stores or sends back your password. To stop it, remove the saved login in the browser's password settings.

## Database

| Table | What's in it |
|---|---|
| `equipment` | the 5,000 machines (one row each) |
| `maintenance_log` | every finished service: machine, before → after, who, when. Loaded back into each machine's linked-list history on start-up |
| `app_users` | accounts: username, role, salt, PBKDF2 hash |

The app creates missing tables by itself and fills an empty `equipment` table from the CSV, so a brand-new database (like one on Azure) works on the first run. `schema.sql` documents the same tables.

Settings are read from environment variables first, then `db.properties`:

| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/gym_equipment_tracker?rewriteBatchedStatements=true&connectTimeout=3000` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | your password |
| `LIMEGYM_DATA_DIR` | folder for `users.txt` when MySQL is offline (default: the project folder) |

## How to run (VS Code)

1. Install the **Extension Pack for Java** in VS Code and open this folder. `.vscode/settings.json` already points to `src`, `bin` and `lib`.
2. **MySQL password:** copy `db.properties.example` to `db.properties` (same folder) and put in your MySQL root password. `db.properties` is in `.gitignore`, so it is never uploaded.
3. **Database (first time only):** create an empty `gym_equipment_tracker` database in MySQL Workbench (or run `schema.sql`). The app creates the tables and loads the 5,000 rows on its first run.
4. Open `src/Main.java` and click **Run**. It starts the web server and opens **http://localhost:8080** in your browser. Run it from the project folder so it finds `web/`, `data/` and `db.properties`. Stop it with the red square in VS Code.

If MySQL isn't running or `db.properties` is missing, the app still opens using the CSV and shows **Offline · CSV data** in the sidebar. Changes are not saved in that mode.

The port comes from the `PORT` environment variable when it's set (Azure App Service sets it), otherwise 8080.
