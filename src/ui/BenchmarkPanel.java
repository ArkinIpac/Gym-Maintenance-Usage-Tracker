
// BenchmarkPanel.java
// Part of: Arkin Ipac  (Sorting, Searching, Benchmark, Integration)
// Presented in: Slide 13 (Kahlil Mendoza) - benchmark results.
// What it does: The Benchmarks page: runs Benchmark.measure() and explains
//               how much each time grew compared with its Big-O.
// Used by: MainFrame


package ui;

import benchmark.Benchmark;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import service.EquipmentStore;

// "Benchmarks" page: runs Benchmark.measure() in the background (so the window never
// freezes), shows the timings in a table, and explains what the numbers mean by comparing
// how much each time grew against what its Big-O predicts.
public class BenchmarkPanel extends JPanel {

    private final EquipmentStore store;
    private final Consumer<String> log;

    private final ResultsModel results = new ResultsModel();
    private final JTextArea insights = new JTextArea();
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

        // Bottom: theory (left) and interpretation (right)
        JPanel bottom = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.weighty = 1; c.gridy = 0;
        // Both cards get the same small preferred size, so the weights (58% / 42%) alone decide
        // the split -- otherwise the long wrapped text would claim most of the width.
        JComponent complexity = buildComplexity(), explanation = buildInsights();
        complexity.setPreferredSize(new Dimension(300, 260));
        explanation.setPreferredSize(new Dimension(300, 260));
        c.gridx = 0; c.weightx = 0.58; c.insets = new Insets(0, 0, 0, 20);
        bottom.add(complexity, c);
        c.gridx = 1; c.weightx = 0.42; c.insets = new Insets(0, 0, 0, 0);
        bottom.add(explanation, c);
        add(bottom, BorderLayout.CENTER);
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

    private JComponent buildInsights() {
        insights.setEditable(false);
        insights.setLineWrap(true);
        insights.setWrapStyleWord(true);
        insights.setOpaque(false);
        insights.setFont(Theme.FONT_BODY);
        insights.setForeground(Theme.TEXT);
        insights.setBorder(new EmptyBorder(0, 0, 0, 0));
        insights.setText("Run the benchmarks to see how each running time grew between 1,000 and 5,000 "
                + "records, compared with what its Big-O predicts.");
        JScrollPane sp = Theme.scroll(insights);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return Theme.card("What the numbers say", "Growth from 1,000 to 5,000 records (\u00D75 data)", sp);
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
                    insights.setText(explain(rows));
                    insights.setCaretPosition(0);          // show from the top, not the end
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

    // Compares the 1,000-record row with the 5,000-record row: data grew x5, so
    // O(n) predicts about x5, O(n^2) about x25, O(log n) and O(1) barely any growth.
    private String explain(Benchmark.Row[] rows) {
        Benchmark.Row small = null, big = null;
        for (Benchmark.Row r : rows) {
            if (r.size == 1000) small = r;
            if (r.size == 5000) big = r;
        }
        if (small == null || big == null) {
            return "Need at least 5,000 records loaded to compare growth (currently "
                    + Theme.number(store.getCount()) + ").";
        }
        return String.join("\n\n",
            line("Linear search", small.linearNs, big.linearNs, "O(n) \u2192 expect about \u00D75"),
            line("Binary search", small.binaryNs, big.binaryNs, "O(log n) \u2192 expect barely any growth"),
            line("Hash lookup", small.hashNs, big.hashNs, "O(1) \u2192 expect no real growth"),
            line("Bubble sort", small.bubbleMs, big.bubbleMs, "O(n\u00B2) \u2192 expect about \u00D725"),
            line("Insertion sort", small.insertionMs, big.insertionMs, "O(n\u00B2) \u2192 expect about \u00D725"),
            String.format("Same Big-O, different speed: at 5,000 records insertion sort took %.1f ms vs bubble "
                    + "sort's %.1f ms. Insertion sort shifts items instead of swapping them repeatedly.",
                    big.insertionMs, big.bubbleMs),
            String.format("Best case in real data: insertion sort by ID finished 5,000 records in %.2f ms, "
                    + "because the data already arrives almost in ID order (near O(n)).", big.insertionIdMs),
            "Small sizes (100 records) are mostly timer and JVM overhead, so trends are clearest at 1,000+.");
    }

    private String line(String name, double small, double big, String theory) {
        return String.format("%s grew \u00D7%.1f. %s.", name, big / small, theory);
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
