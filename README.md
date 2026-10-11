# Gym Equipment Maintenance Prioritization System

CCDATRCL Data Structures and Algorithms · Section ITE-201 · Java (Swing UI, MySQL, custom data structures)

A desktop system that lets gym staff register, search, and delete equipment, flag broken machines, and always service the most urgent one first. Flagged equipment waits in a FIFO queue, then a max-heap ranks it by **priority = severity × 10,000 + usage count**. Every service is logged to a linked-list history. The floor plan is a graph, so BFS gives the technician the fewest-hop route.

## Group and parts

| Member | Part | Main classes | Presents |
|---|---|---|---|
| Jay Mark Lampa | Queue + Linked List | `CustomQueue`, `CustomLinkedList`, `MaintenanceRecord` | Slide 1 |
| Krissy Gomez | Heap + Priority | `CustomHeap`, `MaintenanceManager`, `MaintenancePanel` | Slides 2, 14 |
| Kahlil Mendoza | Hash Table + Graph representation + dataset | `CustomHashTable`, `CustomGraph` (adjacency list), `GymFloorPlan`, `HashView` | Slides 3, 13, 15 |
| Rhobe Arkin Ipac | BFS/DFS, Sorting, Searching, Benchmark, Integration | `EquipmentStore`, `CustomGraph` (BFS/DFS), `Benchmark`, `db/*`, `MainFrame`, `FloorPlanView`, `Main` | Slides 4, 5–8 (live demo), 9–10, 12 |
| John Benedict Jesalva | BST | `CustomBST`, `BstView`, usage index in `EquipmentStore` | Slide 11 |

## Project layout

```
src/
  model/        Equipment, MaintenanceRecord
  structures/   CustomQueue, CustomLinkedList, CustomHeap, CustomBST, CustomHashTable, CustomGraph
  service/      EquipmentStore, MaintenanceManager, GymFloorPlan
  db/           DatabaseConnector, EquipmentDAO, DatasetImporter
  benchmark/    Benchmark
  ui/           MainFrame (start here), pages, Theme, FloorPlanView, BstView, HashView
  Main.java     console demo (backup if the Swing app fails)
data/equipment_dataset.csv   5,000 records
lib/                         MySQL Connector/J
schema.sql                   creates the database and table
db.properties.example        template for your MySQL password
```

## How to run (VS Code)

1. Install the **Extension Pack for Java** in VS Code and open this folder. `.vscode/settings.json` already points to `src`, `bin`, and `lib`.
2. **MySQL password:** copy `db.properties.example` to `db.properties` (same folder) and replace `YOUR_MYSQL_PASSWORD` with your own MySQL root password. `db.properties` is in `.gitignore`, so it is never uploaded.
3. **Database (first time only):** run `schema.sql` in MySQL Workbench, then run `db.DatasetImporter` once. It loads the 5,000 rows.
4. Run **`ui.MainFrame`** for the app, or **`Main`** for the console demo.

If MySQL isn't running or `db.properties` is missing, the app still opens using the CSV and shows **Offline (CSV)** in the sidebar. Changes are not saved in that mode.

**Before the defense:** practice runs save to MySQL (for example, EQ-00202 becomes Good after it's serviced). To restore the original data, run `TRUNCATE TABLE equipment;` and then `db.DatasetImporter` again.
