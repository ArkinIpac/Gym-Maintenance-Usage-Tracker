
// MaintenancePanel.java
// Part of: Krissy Mae Gomez  (Heap + Priority Processing)
// Presented in: Slides 5-8 (John Benedict Jesalva) - maintenance workflow.
// What it does: The Maintenance page: buttons for Flag, Promote, Service next,
//               and History.
// Used by: MainFrame


package ui;

import db.EquipmentDAO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import model.Equipment;
import model.MaintenanceRecord;
import service.EquipmentStore;
import service.MaintenanceManager;

// "Maintenance" page: the queue -> heap -> service workflow on screen.
// Each button calls one MaintenanceManager method:
//   Flag             -> flagForMaintenance()  (hash lookup by ID, then queue enqueue)
//   Flag all critical-> same, for every CRITICAL item in the array
//   Promote          -> promoteQueueToHeap()  (dequeue everything, insert into heap)
//   Service next     -> serviceNext()         (heap extractMax, record added to linked lists)
//   Show history     -> getHistory()          (traverses that machine's linked list)
//   Clear heap       -> clearHeap()           (extractMax until empty, nothing serviced)
//   Clear recent     -> clearRecentlyServiced() (removeLast until empty)
// Layout: metrics on top; workflow steps and the recent list on the left; the heap gets
// the whole right side so more of its ranking is visible at once.
public class MaintenancePanel extends JPanel {

    private static final int HEAP_ROWS = 25; // how many heap entries to show

    private final EquipmentStore store;
    private final MaintenanceManager manager;
    private final Supplier<EquipmentDAO> dao;
    private final Consumer<String> log;
    private final Runnable onChange;

    private final JLabel queueValue = new JLabel(), heapValue = new JLabel(),
            servicedValue = new JLabel(), nextValue = new JLabel();
    private final JLabel nextDetail = new JLabel();
    private final JLabel heapDetail = new JLabel();
    private int servicedThisSession;

    private final JTextField idField = Theme.textField("e.g. EQ-00042");

    // Buttons that are greyed out when there is nothing for them to do (see refresh()).
    private final JButton promoteButton = Theme.secondaryButton("Promote");
    private final JButton serviceButton = Theme.primaryButton("Service next");
    private final JButton clearHeapButton = Theme.ghostButton("Clear");
    private final JButton clearRecentButton = Theme.ghostButton("Clear");

    private final HeapModel heapModel = new HeapModel();
    private final RecentModel recentModel = new RecentModel();

    public MaintenancePanel(EquipmentStore store, MaintenanceManager manager, Supplier<EquipmentDAO> dao,
            Consumer<String> log, Runnable onChange) {
        super(new BorderLayout(0, 16));
        this.store = store;
        this.manager = manager;
        this.dao = dao;
        this.log = log;
        this.onChange = onChange;
        setOpaque(false);

        add(buildStats(), BorderLayout.NORTH);

        // Left column: the workflow card at its natural height, the recent list fills the rest.
        JPanel left = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.BOTH; c.weightx = 1;
        c.gridy = 0; c.weighty = 0;
        left.add(buildWorkflow(), c);
        c.gridy = 1; c.weighty = 1; c.insets = new Insets(20, 0, 0, 0);
        left.add(buildRecentTable(), c);

        JPanel body = Theme.transparent(new BorderLayout(20, 0));
        body.add(Theme.fixedWidth(left, 400), BorderLayout.WEST);
        body.add(buildHeapTable(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    // ================= Metrics row =================

    private JComponent buildStats() {
        JPanel row = Theme.transparent(new GridLayout(1, 4, 16, 0));
        row.add(Theme.statTile("Waiting in queue", queueValue, new JLabel("FIFO, in the order reported")));
        row.add(Theme.statTile("In priority heap", heapValue, heapDetail));
        row.add(Theme.statTile("Serviced this session", servicedValue, new JLabel("Logged to linked-list history")));
        row.add(Theme.statTile("Next up", nextValue, nextDetail));
        return row;
    }

    // Workflow steps

    private JComponent buildWorkflow() {
        JButton flag = Theme.primaryButton("Flag");
        flag.addActionListener(e -> flag());
        idField.addActionListener(e -> flag()); // Enter key also flags

        JButton flagCritical = Theme.secondaryButton("All critical");
        flagCritical.addActionListener(e -> flagAllCritical());
        promoteButton.addActionListener(e -> promote());
        serviceButton.addActionListener(e -> serviceNext());
        JButton history = Theme.secondaryButton("History");
        history.addActionListener(e -> showHistory());

        JPanel idButtons = Theme.transparent(new GridLayout(1, 2, 8, 0));
        idButtons.add(flag);
        idButtons.add(history);
        JPanel idRow = Theme.transparent(new BorderLayout(10, 0));
        idRow.add(idField, BorderLayout.CENTER);
        idRow.add(idButtons, BorderLayout.EAST);

        JPanel steps = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 0);
        c.gridy = 0;
        steps.add(Theme.field("Equipment ID", idRow), c);
        c.insets = new Insets(0, 0, 10, 0);
        c.gridy = 1;
        steps.add(step(1, "Flag equipment", "Adds to the FIFO queue", flagCritical), c);
        c.gridy = 2;
        steps.add(step(2, "Promote to heap", "Re-ranks everyone by priority", promoteButton), c);
        c.gridy = 3;
        c.insets = new Insets(0, 0, 0, 0);
        steps.add(step(3, "Service next", "Fixes the top-priority item", serviceButton), c);
        c.gridy = 4;
        c.weighty = 1;
        steps.add(Box.createGlue(), c);

        return Theme.card("Workflow", "Queue \u2192 heap \u2192 service", steps);
    }

    // One numbered step: lime number badge, title + description, action button on
    // the right.
    private JComponent step(int number, String title, String description, JButton action) {
        JPanel row = Theme.transparent(new BorderLayout(14, 0));
        row.add(new NumberBadge(number), BorderLayout.WEST);
        JPanel text = Theme.transparent(new GridLayout(2, 1, 0, 2));
        text.add(Theme.label(title));
        JLabel d = Theme.muted(description);
        text.add(d);
        row.add(text, BorderLayout.CENTER);
        row.add(action, BorderLayout.EAST);
        return row;
    }

    private void flag() {
        String id = idField.getText().trim();
        Equipment e = store.findById(id); // hash table lookup, O(1)
        if (e == null) {
            log.accept("No equipment with ID \"" + id + "\".");
            return;
        }
        if (!manager.flagForMaintenance(e)) {
            log.accept(e.getEquipmentId() + " is already waiting for maintenance.");
            return;
        }
        log.accept("Flagged " + e.getEquipmentId() + " (" + Theme.pretty(e.getCondition()) + ") \u2192 queue position "
                + manager.getPendingCount());
        idField.setText("");
        onChange.run();
    }

    private void flagAllCritical() {
        int added = 0;
        for (Equipment e : store.getAll()) {
            if (e.getCondition() == Equipment.Condition.CRITICAL && manager.flagForMaintenance(e))
                added++;
        }
        log.accept("Flagged " + added + " critical items. Queue now holds " + manager.getPendingCount() + ".");
        onChange.run();
    }

    private void promote() {
        int moved = manager.promoteQueueToHeap();
        log.accept(moved == 0 ? "Queue is empty, nothing to promote."
                : "Promoted " + moved + " items into the priority heap.");
        onChange.run();
    }

    private void serviceNext() {
        if (!manager.hasEquipmentReadyForService()) {
            log.accept(manager.getPendingCount() > 0
                    ? "The heap is empty. Promote the queue first."
                    : "Nothing to service. Flag some equipment first.");
            return;
        }
        MaintenanceRecord record = manager.serviceNext("Routine service");
        servicedThisSession++;
        Equipment e = store.findById(record.getEquipmentId());
        EquipmentDAO db = dao.get();
        String saved = "";
        if (db != null && e != null) {
            try {
                db.update(e);
            } catch (Exception ex) {
                saved = " (database update failed: " + ex.getMessage() + ")";
            }
        } else if (db == null) {
            saved = " (offline, not saved)";
        }
        log.accept("Serviced " + record.getEquipmentId() + ": " + Theme.pretty(record.getConditionBefore())
                + " \u2192 " + Theme.pretty(record.getConditionAfter()) + saved);
        onChange.run();
    }

    private void showHistory() {
        String id = idField.getText().trim();
        if (store.findById(id) == null) {
            log.accept("Type a valid equipment ID above to see its history.");
            return;
        }
        int count = 0;
        for (MaintenanceRecord r : manager.getHistory(id)) { // linked list traversal
            log.accept("History " + id + ": " + r);
            count++;
        }
        if (count == 0)
            log.accept(id + " has no service history yet this session.");
    }

    // Tables 

    private JComponent buildHeapTable() {
        JTable table = Theme.hintTable(heapModel, "Heap is empty. Flag equipment, then click Promote.");
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new Theme.ConditionRenderer());
        DefaultTableCellRenderer right = new Theme.CellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(right);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        int[] widths = { 50, 110, 220, 120, 90, 100 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        clearHeapButton.addActionListener(e -> clearHeap());
        return Theme.card("Priority heap", "Top " + HEAP_ROWS + " by priority, the order they will be serviced",
                clearHeapButton, Theme.tableScroll(table));
    }

    private JComponent buildRecentTable() {
        JTable table = Theme.hintTable(recentModel, "Nothing serviced yet.");
        Theme.styleTable(table);
        int[] widths = { 100, 180, 75 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        clearRecentButton.addActionListener(e -> {
            manager.clearRecentlyServiced();
            log.accept("Recently serviced list cleared. Machine histories are kept.");
            onChange.run();
        });
        return Theme.card("Recently serviced", "Last 10, newest first (addFirst + removeLast)",
                clearRecentButton, Theme.tableScroll(table));
    }

    // Clearing throws away the whole ranking, so we ask first.
    private void clearHeap() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove all " + manager.getHeapSize() + " items from the priority heap?\n"
                        + "Nothing is serviced. They can be flagged again later.",
                "Clear priority heap", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        int cleared = manager.clearHeap();
        log.accept("Cleared " + cleared + " items from the priority heap (not serviced).");
        onChange.run();
    }

    // Re-reads everything from the manager. Called by MainFrame after any change.
    public void refresh() {
        queueValue.setText(Theme.number(manager.getPendingCount()));
        heapValue.setText(Theme.number(manager.getHeapSize()));
        servicedValue.setText(Theme.number(servicedThisSession));
        Equipment next = manager.peekNext();
        nextValue.setText(next == null ? "\u2014" : next.getEquipmentId());
        nextValue.setForeground(next == null ? Theme.TEXT_DIM : Theme.conditionColor(next.getCondition()));
        nextDetail.setText(next == null ? "Heap is empty"
                : Theme.pretty(next.getCondition()) + " \u00B7 " + Theme.number(next.getUsageCount()) + " uses");

        Equipment[] heap = manager.getHeapSnapshot();
        heapDetail.setText(heap.length == 0 ? "Ranked by severity, then usage" : conditionBreakdown(heap));

        heapModel.setRows(topByPriority(heap, HEAP_ROWS));
        recentModel.setRows(manager.getRecentlyServiced());

        // Grey out buttons that have nothing to do right now.
        promoteButton.setEnabled(manager.getPendingCount() > 0);
        serviceButton.setEnabled(manager.hasEquipmentReadyForService());
        clearHeapButton.setEnabled(manager.getHeapSize() > 0);
        clearRecentButton.setEnabled(!manager.getRecentlyServiced().isEmpty());
    }

    // e.g. "45 critical · 60 poor · 18 fair" -- one pass over the heap array, O(n).
    private static String conditionBreakdown(Equipment[] heap) {
        Equipment.Condition[] all = Equipment.Condition.values();
        int[] counts = new int[all.length];
        for (Equipment e : heap) counts[e.getCondition().ordinal()]++;
        StringBuilder sb = new StringBuilder();
        for (int i = all.length - 1; i >= 0; i--) {          // worst condition first
            if (counts[i] == 0) continue;
            if (sb.length() > 0) sb.append("  \u00B7  ");
            sb.append(counts[i]).append(' ').append(all[i].name().toLowerCase());
        }
        return sb.toString();
    }

    // The heap array is only partly ordered (each parent >= its children), so it can't be
    // shown as-is in ranked order. For the table we copy it and pick the top 25 with a
    // selection-sort style pass: O(n * k), k = 25. The real heap is not changed.
    private Equipment[] topByPriority(Equipment[] heap, int k) {
        int n = Math.min(k, heap.length);
        Equipment[] copy = heap.clone();
        for (int i = 0; i < n; i++) {
            int best = i;
            for (int j = i + 1; j < copy.length; j++) {
                if (manager.priorityOf(copy[j]) > manager.priorityOf(copy[best]))
                    best = j;
            }
            Equipment t = copy[i];
            copy[i] = copy[best];
            copy[best] = t;
        }
        Equipment[] top = new Equipment[n];
        System.arraycopy(copy, 0, top, 0, n);
        return top;
    }

    private class HeapModel extends AbstractTableModel {
        private final String[] cols = { "#", "ID", "Name", "Condition", "Usage", "Priority" };
        private Equipment[] rows = new Equipment[0];

        void setRows(Equipment[] rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.length;
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
            Equipment e = rows[r];
            switch (c) {
                case 0:
                    return r + 1;
                case 1:
                    return e.getEquipmentId();
                case 2:
                    return e.getName();
                case 3:
                    return e.getCondition();
                case 4:
                    return Theme.number(e.getUsageCount());
                default:
                    return Theme.number(manager.priorityOf(e));
            }
        }
    }

    private static class RecentModel extends AbstractTableModel {
        private final String[] cols = { "ID", "Condition change", "Date" };
        private MaintenanceRecord[] rows = new MaintenanceRecord[0];

        void setRows(structures.CustomLinkedList<MaintenanceRecord> list) {
            rows = new MaintenanceRecord[list.size()];
            int i = 0;
            for (MaintenanceRecord r : list)
                rows[i++] = r; // linked list traversal
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.length;
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
            MaintenanceRecord m = rows[r];
            switch (c) {
                case 0:
                    return m.getEquipmentId();
                case 1:
                    return "<html><nobr>" + dot(m.getConditionBefore()) + " &nbsp;\u2192&nbsp; " + dot(m.getConditionAfter())
                            + "</nobr></html>";
                default:   // short date ("Oct 10") so the column fits in the narrow left card
                    return m.getDateServiced().format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
            }
        }
    }

    // Condition name with a colored dot in front, same colors as the Condition column.
    // (JLabel understands simple HTML, so the cell renderer can show two colors.)
    private static String dot(Equipment.Condition c) {
        Color k = Theme.conditionColor(c);
        return String.format("<font color='#%02x%02x%02x'>\u25CF</font> %s",
                k.getRed(), k.getGreen(), k.getBlue(), Theme.pretty(c));
    }

    // Lime circle with a step number.
    private static class NumberBadge extends JComponent {
        private final int number;

        NumberBadge(int number) {
            this.number = number;
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int y = (getHeight() - 30) / 2;
            g2.setColor(Theme.ACCENT_SOFT);
            g2.fill(new Ellipse2D.Float(0, y, 30, 30));
            g2.setColor(Theme.ACCENT);
            g2.setFont(Theme.FONT_BODY_B);
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(number);
            g2.drawString(s, 15 - fm.stringWidth(s) / 2, y + 15 + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }
}
