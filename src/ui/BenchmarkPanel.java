
// BenchmarkPanel.java
// Part of: Arkin Ipac  (Sorting, Searching, Benchmark, Integration)
// Presented in: Slide 13 (Kahlil Mendoza) - benchmark results.
// What it does: The Benchmarks page: runs Benchmark.measure() and shows the
//               measured times next to the theoretical Big-O table.
// Used by: MainFrame


package ui;

import benchmark.Benchmark;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import service.EquipmentStore;

// "Benchmarks" page: runs Benchmark.measure() in the background (so the window never
// freezes), shows the timings in a table, and shows the theoretical Big-O table below it.
public class BenchmarkPanel extends JPanel {

    private final EquipmentStore store;
    private final Consumer<String> log;

    private final ResultsModel results = new ResultsModel();
    private final JLabel status = Theme.muted("Not run yet. Takes a few seconds.");
    private final JButton run = Theme.primaryButton("Run benchmarks");

    public BenchmarkPanel(EquipmentStore store, Consumer<String> log) {
        super(new BorderLayout(0, 20));
        this.store = store;
        this.log = log;
        setOpaque(false);

        run.addActionListener(e -> runBenchmarks());

        // Top: measured results
        JTable table = new JTable(results);
        Theme.styleTable(table);
        DefaultTableCellRenderer right = new Theme.CellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < results.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(right);

        JPanel headerRight = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.add(status);
        headerRight.add(run);

        JComponent tableFrame = Theme.tableScroll(table);
        tableFrame.setPreferredSize(new Dimension(0, 38 + 36 * 4 + 4));
        Theme.Card measured = new Theme.Card(new BorderLayout(0, 16));
        measured.add(Theme.cardHeader("Measured running times",
                "System.nanoTime() \u00B7 sorts in milliseconds, searches in nanoseconds per lookup", headerRight),
                BorderLayout.NORTH);
        measured.add(tableFrame, BorderLayout.CENTER);
        add(measured, BorderLayout.NORTH);

        // Bottom: the theoretical Big-O table
        add(buildComplexity(), BorderLayout.CENTER);
    }

    private JComponent buildComplexity() {
        String[] cols = {"Operation", "Structure", "Best", "Average", "Worst", "Space"};
        JTable table = new JTable(Benchmark.COMPLEXITY, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        Theme.styleTable(table);
        table.setRowHeight(32);
        int[] widths = {130, 105, 70, 85, 85, 60};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        return Theme.card("Theoretical complexity", "h = BST height: log n if balanced, n if not", Theme.tableScroll(table));
    }

    // SwingWorker runs the benchmark on a background thread, then updates the table on the
    // Swing thread when it's done, so the window doesn't freeze while it runs.
    private void runBenchmarks() {
        run.setEnabled(false);
        status.setText("Running...");
        log.accept("Benchmark started on " + Theme.number(store.getCount()) + " records.");
        new SwingWorker<Benchmark.Row[], Void>() {
            @Override
            protected Benchmark.Row[] doInBackground() {
                return Benchmark.measure(store.getAll());
            }

            @Override
            protected void done() {
                try {
                    Benchmark.Row[] rows = get();
                    results.setRows(rows);
                    status.setText("Saved to " + Benchmark.getResultsFile());
                    log.accept("Benchmark finished. Results saved to " + Benchmark.getResultsFile() + ".");
                } catch (Exception ex) {
                    status.setText("Failed");
                    log.accept("Benchmark failed: " + ex.getMessage());
                }
                run.setEnabled(true);
            }
        }.execute();
    }

    private static class ResultsModel extends AbstractTableModel {
        private final String[] cols = {"Records", "Bubble (ms)", "Insertion (ms)", "Insertion by ID (ms)",
                                       "Linear (ns)", "Binary (ns)", "Hash (ns)", "BST (ns)"};
        private Benchmark.Row[] rows = new Benchmark.Row[0];

        void setRows(Benchmark.Row[] rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.length; }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Benchmark.Row row = rows[r];
            switch (c) {
                case 0: return Theme.number(row.size);
                case 1: return String.format("%.3f", row.bubbleMs);
                case 2: return String.format("%.3f", row.insertionMs);
                case 3: return String.format("%.3f", row.insertionIdMs);
                case 4: return String.format("%,.0f", row.linearNs);
                case 5: return String.format("%,.0f", row.binaryNs);
                case 6: return String.format("%,.0f", row.hashNs);
                default: return String.format("%,.0f", row.bstNs);
            }
        }
    }
}
