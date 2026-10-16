package benchmark;

import db.DatasetImporter;
import db.EquipmentDAO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import model.Equipment;
import service.EquipmentStore;

// Times the sorts and searches on 100, 500, 1,000 and 5,000 records to check the Big-O
// values with real numbers. To keep it fair: every algorithm gets the same records, there's
// an untimed warm-up first (Java's JIT makes the first run slow), sorts are averaged over
// 3 runs, searches take the median of 5 passes, and a fixed seed picks the search targets.
// Look at how the times grow, not the exact numbers, which depend on the computer.
public class Benchmark {

    public static final int[] SIZES = {100, 500, 1000, 5000};
    private static final int SORT_REPEATS = 3;
    private static final int SEARCHES = 1000;           // lookups per strategy per size
    private static final int SEARCH_PASSES = 5;         // the median pass is kept
    private static final long SEED = 20260927L;         // same seed as the dataset generator
    private static final String RESULTS_FILE = "benchmarks/benchmark_results.csv";

    // Results go here so the JIT can't treat the work as unused and skip it.
    private static long sink;

    // Timings for one dataset size.
    public static class Row {
        public final int size;
        public final double bubbleMs, insertionMs, insertionIdMs;          // sorts, milliseconds
        public final double linearNs, binaryNs, hashNs, bstNs;             // searches, ns per lookup

        Row(int size, double bubbleMs, double insertionMs, double insertionIdMs,
            double linearNs, double binaryNs, double hashNs, double bstNs) {
            this.size = size;
            this.bubbleMs = bubbleMs;
            this.insertionMs = insertionMs;
            this.insertionIdMs = insertionIdMs;
            this.linearNs = linearNs;
            this.binaryNs = binaryNs;
            this.hashNs = hashNs;
            this.bstNs = bstNs;
        }
    }

    // One Row per size (sizes bigger than the data are skipped). Also saves the CSV.
    public static Row[] measure(Equipment[] data) {
        warmUp(data);

        int usable = 0;
        for (int n : SIZES) if (n <= data.length) usable++;
        Row[] rows = new Row[usable];
        int r = 0;

        for (int n : SIZES) {
            if (n > data.length) continue;

            // A separate store with the first n records, so the app's data isn't touched.
            EquipmentStore store = new EquipmentStore();
            for (int i = 0; i < n; i++) store.addEquipment(data[i]);

            // Bubble vs insertion on the same key (usage).
            double bubbleMs = timeSortMs(() -> store.sortByUsageCountBubble(true));
            double insertMs = timeSortMs(() -> store.sortByUsageCountInsertion(true));
            // The data is already in ID order, so this shows insertion sort's O(n) best case.
            double insertIdMs = timeSortMs(() -> store.sortByIdInsertion(true));

            // Same targets for every search strategy.
            Random random = new Random(SEED);
            String[] ids = new String[SEARCHES];
            int[] usages = new int[SEARCHES];
            for (int i = 0; i < SEARCHES; i++) {
                Equipment target = data[random.nextInt(n)];
                ids[i] = target.getEquipmentId();
                usages[i] = target.getUsageCount();
            }
            Equipment[] sortedById = store.sortByIdInsertion(true);   // not timed

            double linearNs = timeSearchNs(() -> { for (String id : ids) count(store.linearSearchById(id)); });
            double binaryNs = timeSearchNs(() -> { for (String id : ids) count(store.binarySearchById(sortedById, id)); });
            double hashNs   = timeSearchNs(() -> { for (String id : ids) count(store.findById(id)); });
            double bstNs    = timeSearchNs(() -> { for (int u : usages) sink += store.findByUsage(u).size(); });

            rows[r++] = new Row(n, bubbleMs, insertMs, insertIdMs, linearNs, binaryNs, hashNs, bstNs);
        }
        saveCsv(rows);
        return rows;
    }

    // Same measurements as a text table.
    public static String run(Equipment[] data) {
        Row[] rows = measure(data);
        StringBuilder report = new StringBuilder();
        report.append(String.format("%-6s | %12s %12s %12s | %10s %10s %10s %10s%n",
                "n", "Bubble(ms)", "Insert(ms)", "InsID(ms)", "Linear(ns)", "Binary(ns)", "Hash(ns)", "BST(ns)"));
        report.append("-".repeat(98)).append("\n");
        for (Row row : rows) {
            report.append(String.format("%-6d | %12.3f %12.3f %12.3f | %10.0f %10.0f %10.0f %10.0f%n",
                    row.size, row.bubbleMs, row.insertionMs, row.insertionIdMs,
                    row.linearNs, row.binaryNs, row.hashNs, row.bstNs));
        }
        report.append("\nSort times = average of ").append(SORT_REPEATS)
              .append(" runs. Search times = nanoseconds per lookup (median of ").append(SEARCH_PASSES)
              .append(" passes of ").append(SEARCHES).append(" lookups).\n");
        report.append("Results saved to ").append(RESULTS_FILE).append("\n");
        return report.toString();
    }

    // Average of SORT_REPEATS runs after one warm-up, in ms.
    private static double timeSortMs(SortTask task) {
        sink += task.run().length;                       // warm-up
        long total = 0;
        for (int r = 0; r < SORT_REPEATS; r++) {
            long start = System.nanoTime();
            Equipment[] result = task.run();
            total += System.nanoTime() - start;
            sink += result.length;
        }
        return total / (double) SORT_REPEATS / 1_000_000.0;
    }

    // ns per lookup from the median pass. A pass slowed down by garbage collection or
    // another app would drag an average up; the median ignores it.
    private static double timeSearchNs(Runnable searches) {
        searches.run();                                   // warm-up
        long[] passes = new long[SEARCH_PASSES];
        for (int p = 0; p < SEARCH_PASSES; p++) {
            long start = System.nanoTime();
            searches.run();
            passes[p] = System.nanoTime() - start;
        }
        for (int i = 1; i < passes.length; i++) {        // sort the 5 passes to find the median
            long key = passes[i];
            int j = i - 1;
            while (j >= 0 && passes[j] > key) { passes[j + 1] = passes[j]; j--; }
            passes[j + 1] = key;
        }
        return passes[SEARCH_PASSES / 2] / (double) SEARCHES;
    }

    // Untimed runs so n = 100, measured first, doesn't include Java's warm-up time.
    private static void warmUp(Equipment[] data) {
        EquipmentStore store = new EquipmentStore();
        for (Equipment e : data) store.addEquipment(e);
        Equipment[] sortedById = store.sortByIdInsertion(true);
        for (int round = 0; round < 3; round++) {
            sink += store.sortByUsageCountBubble(true).length;
            sink += store.sortByUsageCountInsertion(true).length;
            for (int i = 0; i < data.length; i++) {
                String id = data[i].getEquipmentId();
                count(store.linearSearchById(id));
                count(store.binarySearchById(sortedById, id));
                count(store.findById(id));
                sink += store.findByUsage(data[i].getUsageCount()).size();
            }
        }
    }

    private static void count(Equipment found) {
        if (found != null) sink++;
    }

    // So a sort can be passed as a lambda, e.g. () -> store.sortByIdInsertion(true).
    private interface SortTask {
        Equipment[] run();
    }

    private static void saveCsv(Row[] rows) {
        StringBuilder csv = new StringBuilder("size,bubble_usage_ms,insertion_usage_ms,insertion_id_ms,"
                + "linear_ns,binary_ns,hash_ns,bst_ns\n");
        for (Row r : rows) {
            csv.append(String.format("%d,%.4f,%.4f,%.4f,%.1f,%.1f,%.1f,%.1f%n",
                    r.size, r.bubbleMs, r.insertionMs, r.insertionIdMs, r.linearNs, r.binaryNs, r.hashNs, r.bstNs));
        }
        try {
            Path path = Paths.get(RESULTS_FILE);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, csv.toString());
        } catch (IOException e) {
            System.out.println("Could not save results file: " + e.getMessage());
        }
    }

    public static String getResultsFile() { return RESULTS_FILE; }

    // Theoretical complexity: {operation, structure, best, average, worst, space}
    public static final String[][] COMPLEXITY = {
        {"Search by ID",      "Hash table",     "O(1)",     "O(1)",     "O(n)",     "O(n)"},
        {"Search by ID",      "Array, linear",  "O(1)",     "O(n)",     "O(n)",     "O(1)"},
        {"Search by ID",      "Array, binary",  "O(1)",     "O(log n)", "O(log n)", "O(1)"},
        {"Search by usage",   "BST",            "O(1)",     "O(log n)", "O(n)",     "O(h)"},
        {"Search record",     "Linked list",    "O(1)",     "O(n)",     "O(n)",     "O(1)"},
        {"Insert / delete",   "BST",            "O(1)",     "O(log n)", "O(n)",     "O(h)"},
        {"Insert",            "Heap",           "O(1)",     "O(log n)", "O(log n)", "O(1)"},
        {"Remove highest",    "Heap",           "O(log n)", "O(log n)", "O(log n)", "O(1)"},
        {"Enqueue / dequeue", "Queue",          "O(1)",     "O(1)",     "O(1)",     "O(1)"},
        {"Bubble sort",       "Array",          "O(n)",     "O(n^2)",   "O(n^2)",   "O(1)"},
        {"Insertion sort",    "Array",          "O(n)",     "O(n^2)",   "O(n^2)",   "O(1)"},
        {"BFS / DFS",         "Graph",          "O(V+E)",   "O(V+E)",   "O(V+E)",   "O(V)"},
    };

    public static String complexityTable() {
        StringBuilder sb = new StringBuilder();
        String fmt = "%-20s %-16s %-10s %-10s %-10s %-8s%n";
        sb.append(String.format(fmt, "Operation", "Structure", "Best", "Average", "Worst", "Space"));
        sb.append("-".repeat(78)).append("\n");
        for (String[] row : COMPLEXITY) sb.append(String.format(fmt, (Object[]) row));
        sb.append("\nSorts use O(1) extra space for the sorting itself; getAll() makes one O(n) copy\n");
        sb.append("so the master array is never reordered. h = BST height (log n if balanced, n if not).");
        return sb.toString();
    }

    // Run on its own: MySQL first, the CSV if the database isn't reachable.
    public static void main(String[] args) {
        List<Equipment> list;
        try {
            list = new EquipmentDAO().findAll();
            System.out.println("Loaded " + list.size() + " records from MySQL.");
        } catch (Exception dbError) {
            try {
                list = DatasetImporter.readCsv("data/equipment_dataset.csv");
                System.out.println("MySQL unavailable, loaded " + list.size() + " records from CSV.");
            } catch (IOException csvError) {
                System.out.println("Could not load data: " + csvError.getMessage());
                return;
            }
        }
        Equipment[] data = list.toArray(new Equipment[0]);
        System.out.println();
        System.out.println(run(data));
        System.out.println(complexityTable());
    }
}
