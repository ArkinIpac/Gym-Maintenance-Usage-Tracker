# GymCare · Gym Equipment Maintenance Prioritization System

CCDATRCL Data Structures and Algorithms · Section ITE-201 · Java (custom data structures, MySQL) + web UI (HTML/CSS/JS)

A system that lets gym staff register, search and delete equipment, flag broken machines, and always service the most urgent one first. Flagged equipment waits in a FIFO queue, then a max-heap ranks it by **priority = severity × 10,000 + usage count**. Every service is logged to a linked-list history. The floor plan is a graph, so BFS gives the technician the fewest-hop route.

## Group and parts

| Member | Part | Main classes | Presents |
|---|---|---|---|
| Jay Mark Lampa | Queue + Linked List | `CustomQueue`, `CustomLinkedList`, `MaintenanceRecord` | Slide 1 |
| Krissy Gomez | Heap + Priority | `CustomHeap`, `MaintenanceManager` | Slides 2, 14 |
| Kahlil Mendoza | Hash Table + Graph representation + dataset | `CustomHashTable`, `CustomGraph` (adjacency list), `GymFloorPlan` | Slides 3, 13, 15 |
| Rhobe Arkin Ipac | BFS/DFS, Sorting, Searching, Benchmark, Integration | `EquipmentStore`, `CustomGraph` (BFS/DFS), `Benchmark`, `db/*`, `web/*`, `Main`, `ConsoleDemo` | Slides 4, 5–8 (live demo), 9–10, 12 |
| John Benedict Jesalva | BST | `CustomBST`, usage index in `EquipmentStore` | Slide 11 |

The Swing UI used at the defense (`ui.MainFrame` and its panels) was replaced by the web UI below. It is still in the git history: commit "Final defense version".

## Project layout

```
src/
  model/        Equipment, MaintenanceRecord
  structures/   CustomQueue, CustomLinkedList, CustomHeap, CustomBST, CustomHashTable, CustomGraph
  service/      EquipmentStore, MaintenanceManager, GymFloorPlan
  db/           DatabaseConnector, EquipmentDAO, DatasetImporter
  benchmark/    Benchmark
  web/          WebServer, ApiRoutes (every /api address), Json (tiny JSON writer)
  Main.java     start here: runs the web server and opens the browser
  ConsoleDemo.java  text-only demo of every feature (the old Main)
web/            the UI: index.html, css/style.css, js/ (one file per page), img/logo.svg
data/equipment_dataset.csv   5,000 records
lib/                         MySQL Connector/J
schema.sql                   creates the database and table
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

Press **Ctrl + K** anywhere to search equipment.

## How to run (VS Code)

1. Install the **Extension Pack for Java** in VS Code and open this folder. `.vscode/settings.json` already points to `src`, `bin` and `lib`.
2. **MySQL password:** copy `db.properties.example` to `db.properties` (same folder) and put in your MySQL root password. `db.properties` is in `.gitignore`, so it is never uploaded.
3. **Database (first time only):** run `schema.sql` in MySQL Workbench, then run `db.DatasetImporter` once. It loads the 5,000 rows.
4. Open `src/Main.java` and click **Run**. It starts the web server and opens **http://localhost:8080** in your browser. Run it from the project folder so it finds `web/`, `data/` and `db.properties`. Stop it with the red square in VS Code.

If MySQL isn't running or `db.properties` is missing, the app still opens using the CSV and shows **Offline · CSV data** in the sidebar. Changes are not saved in that mode.

The port comes from the `PORT` environment variable when it's set (Azure App Service sets it), otherwise 8080. The console demo used for the test cases is now `ConsoleDemo`.
