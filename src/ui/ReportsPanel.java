
// ReportsPanel.java
// Part of: Shared page. Code owner of each tool:
//            Sorting + Searching -> Arkin Ipac
//            BST                 -> John Benedict Jesalva
//            Hash table          -> Kahlil Mendoza
//            Graph (BFS / DFS)   -> Arkin Ipac / Kahlil Mendoza
// Presented in: Slides 5-8 (John Benedict Jesalva) - BST and hash collision tools;
//               Slides 9-10 (Arkin Ipac) - graph tool;
//               Slide 11 (Krissy Gomez) - sorting and searching tools.
// What it does: The Reports page: one tool per data structure or algorithm.
// Used by: MainFrame


package ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.Equipment;
import service.EquipmentStore;
import service.GymFloorPlan;
import structures.CustomBST;
import structures.CustomGraph;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// "Reports" page: one tool per data structure / algorithm, chosen with the tabs on top.
//   Sorting    -> bubble / insertion sort from EquipmentStore
//   Searching  -> hash vs linear vs binary by ID, and BST search by usage
//   BST        -> range query and the three traversals (drawn by BstView)
//   Hash table -> which bucket an ID lands in, and the capacity-7 collision demo (drawn by HashView)
//   Graph      -> BFS route (shortest) and DFS route between two zones
// Every result is printed in the Output card on the right.
public class ReportsPanel extends JPanel {

    private final EquipmentStore store;
    private final Consumer<String> log;
    private final CustomGraph floor = GymFloorPlan.build();   // the gym floor plan used by the Graph tab

    private final CardLayout toolCards = new CardLayout();
    private final JPanel toolHost = Theme.transparent(toolCards);
    private final JTextArea output = Theme.outputArea("Pick a tool on the left and run it. Results will appear here.");
    private final FloorPlanView floorView = new FloorPlanView(floor);
    private final HashView hashView = new HashView();
    private final BstView bstView;
    private final JPanel rightHost = Theme.transparent(new BorderLayout());
    private JComponent outputCard;
    private final JLabel bstStats = Theme.muted(" ");
    private final JLabel hashStats = Theme.muted(" ");
    private final JLabel graphStats = Theme.muted(" ");

    private static final String[] TOOLS = {"Sorting", "Searching", "BST", "Hash table", "Graph"};

    public ReportsPanel(EquipmentStore store, Consumer<String> log) {
        super(new BorderLayout(0, 20));
        this.store = store;
        this.log = log;
        this.bstView = new BstView(store.getUsageIndex());
        setOpaque(false);

        toolHost.add(sortingTool(), TOOLS[0]);
        toolHost.add(searchingTool(), TOOLS[1]);
        toolHost.add(bstTool(), TOOLS[2]);
        toolHost.add(hashTool(), TOOLS[3]);
        toolHost.add(graphTool(), TOOLS[4]);

        add(new Segmented(TOOLS, i -> {
            toolCards.show(toolHost, TOOLS[i]);
            showRightSide(TOOLS[i]);
        }), BorderLayout.NORTH);

        outputCard = buildOutput();
        JPanel body = Theme.transparent(new BorderLayout(20, 0));
        body.add(Theme.fixedWidth(toolHost, 400), BorderLayout.WEST);
        body.add(rightHost, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        showRightSide(TOOLS[0]);
    }

    // Right side of the page. Sorting and Searching only need the Output card. The BST,
    // Hash table and Graph tabs also show a drawing above it, so the structure can be
    // seen, not just read.
    private void showRightSide(String tool) {
        rightHost.removeAll();
        JComponent drawing = null;
        if (tool.equals("Graph"))
            drawing = Theme.card("Floor plan", "Solid line = BFS (shortest)  \u00B7  dashed line = DFS", floorView);
        else if (tool.equals("BST"))
            drawing = Theme.card("Tree", "Smaller keys to the left, bigger keys to the right", bstView);
        else if (tool.equals("Hash table"))
            drawing = Theme.card("Buckets", "Separate chaining: keys in the same bucket form a linked list", hashView);
        if (drawing != null) {
            JPanel split = Theme.transparent(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.fill = GridBagConstraints.BOTH; c.weightx = 1;
            c.gridy = 0; c.weighty = 0.7; c.insets = new Insets(0, 0, 20, 0);
            split.add(drawing, c);
            c.gridy = 1; c.weighty = 0.3; c.insets = new Insets(0, 0, 0, 0);
            split.add(outputCard, c);
            rightHost.add(split);
        } else {
            rightHost.add(outputCard);
        }
        rightHost.revalidate();
        rightHost.repaint();
    }

    private JComponent buildOutput() {
        JButton clear = Theme.ghostButton("Clear");
        clear.addActionListener(e -> output.setText(""));
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.cardHeader("Output", "Results from the tools on the left", clear), BorderLayout.NORTH);
        JScrollPane sp = Theme.scroll(output);
        sp.getViewport().setBackground(Theme.SURFACE_ALT);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // Prints a titled block to the Output card and scrolls to it.
    private void print(String title, String body) {
        if (output.getDocument().getLength() > 0) output.append("\n");
        output.append("\u25B8 " + title + "\n" + body + (body.endsWith("\n") ? "" : "\n"));
        output.setCaretPosition(output.getDocument().getLength());
    }

    // Called when the page is shown: refresh the live statistics.
    public void refresh() {
        CustomBST bst = store.getUsageIndex();
        bstStats.setText(Theme.number(bst.getNodeCount()) + " nodes  \u00B7  height " + bst.height()
                + "  \u00B7  root key " + bst.getRootKey());
        CustomHashTable<String, Equipment> h = store.getIdIndex();
        hashStats.setText(String.format("capacity %,d  \u00B7  load %.2f  \u00B7  %,d collisions  \u00B7  longest chain %d",
                h.getCapacity(), h.getLoadFactor(), h.getCollisionCount(), h.getLongestChain()));
        graphStats.setText(floor.getZoneCount() + " zones  \u00B7  " + floor.getEdgeCount() + " edges  \u00B7  "
                + floor.countComponents() + " component(s)");
        bstView.repaint();   // usage changes (servicing) can reshape the tree
    }

    // ================= Sorting =================

    private JComponent sortingTool() {
        JComboBox<String> algorithm = Theme.comboBox(new String[]{
                "Bubble sort \u00B7 usage count", "Insertion sort \u00B7 usage count", "Insertion sort \u00B7 equipment ID"});
        JComboBox<String> order = Theme.comboBox(new String[]{"Descending", "Ascending"});
        JTextField top = Theme.textField("15", "How many rows");
        JButton run = Theme.primaryButton("Run sort");

        run.addActionListener(e -> {
            int limit = parseInt(top, 15);
            if (limit < 0) return;
            boolean asc = order.getSelectedIndex() == 1;
            int algo = algorithm.getSelectedIndex();
            long start = System.nanoTime();
            Equipment[] sorted = algo == 0 ? store.sortByUsageCountBubble(asc)
                               : algo == 1 ? store.sortByUsageCountInsertion(asc)
                               : store.sortByIdInsertion(asc);
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            StringBuilder sb = new StringBuilder(String.format("Sorted %,d records in %.2f ms%n%n", sorted.length, ms));
            sb.append(String.format("%-4s %-10s %-26s %-12s %8s%n", "#", "ID", "Name", "Condition", "Usage"));
            for (int i = 0; i < Math.min(limit, sorted.length); i++) {
                Equipment q = sorted[i];
                sb.append(String.format("%-4d %-10s %-26s %-12s %,8d%n", i + 1, q.getEquipmentId(),
                        cut(q.getName(), 26), Theme.pretty(q.getCondition()), q.getUsageCount()));
            }
            print(algorithm.getSelectedItem() + " (" + order.getSelectedItem().toString().toLowerCase() + ")", sb.toString());
        });

        return toolCard("Sorting", "Two algorithms on the equipment array",
                form(Theme.field("Algorithm", algorithm), Theme.field("Order", order),
                     Theme.field("Rows to show", top)),
                run, null, note("Bubble and insertion sort are both O(n\u00B2) on average. Insertion sort by ID "
                        + "is near O(n) here because the data is already almost in ID order."));
    }

    // ================= Searching =================

    private JComponent searchingTool() {
        JTextField id = Theme.textField("EQ-04321", "Equipment ID");
        JTextField usage = Theme.textField("", "Exact usage count, e.g. 119");
        JButton compare = Theme.primaryButton("Compare all");
        JButton hash = Theme.secondaryButton("Hash");
        JButton linear = Theme.secondaryButton("Linear");
        JButton binary = Theme.secondaryButton("Binary");
        JButton bst = Theme.secondaryButton("Search BST");

        hash.addActionListener(e -> print("Hash table lookup", timedSearch("Hash table", id.getText().trim(), 0)));
        linear.addActionListener(e -> print("Linear search", timedSearch("Linear", id.getText().trim(), 1)));
        binary.addActionListener(e -> print("Binary search", timedSearch("Binary", id.getText().trim(), 2)));
        compare.addActionListener(e -> {
            String key = id.getText().trim();
            print("Search comparison for " + key,
                    timedSearch("Hash table", key, 0) + timedSearch("Linear", key, 1) + timedSearch("Binary", key, 2)
                    + "\nBinary search needs the array sorted by ID first; that step is not timed here.");
        });
        bst.addActionListener(e -> {
            int u = parseInt(usage, -1);
            if (u < 0) return;
            long start = System.nanoTime();
            CustomLinkedList<Equipment> found = store.findByUsage(u);
            long ns = System.nanoTime() - start;
            StringBuilder sb = new StringBuilder(String.format("%,d item(s) used exactly %,d times  (%,d ns)%n", found.size(), u, ns));
            int shown = 0;
            for (Equipment q : found) {
                if (shown++ == 10) { sb.append("  ...\n"); break; }
                sb.append("  ").append(q).append("\n");
            }
            print("BST search by usage", sb.toString());
        });

        JPanel buttons = Theme.transparent(new GridLayout(1, 3, 8, 0));
        buttons.add(hash);
        buttons.add(linear);
        buttons.add(binary);

        JPanel usageRow = Theme.transparent(new BorderLayout(10, 0));
        usageRow.add(usage, BorderLayout.CENTER);
        usageRow.add(bst, BorderLayout.EAST);

        return toolCard("Searching", "Same ID, different strategies",
                form(Theme.field("Equipment ID", id), buttons, Theme.field("Usage count (BST)", usageRow)),
                compare, null, note("Hash O(1) average  \u00B7  Binary O(log n)  \u00B7  Linear O(n).  "
                        + "Try a missing ID like EQ-99999."));
    }

    // index: 0 = hash, 1 = linear, 2 = binary
    private String timedSearch(String label, String id, int index) {
        Equipment[] sorted = index == 2 ? store.sortByIdInsertion(true) : null;
        long start = System.nanoTime();
        Equipment found = index == 0 ? store.findById(id)
                        : index == 1 ? store.linearSearchById(id)
                        : store.binarySearchById(sorted, id);
        long ns = System.nanoTime() - start;
        return String.format("%-11s %,9d ns   %s%n", label, ns, found == null ? "not found" : found.toString());
    }

    // ================= BST =================

    private JComponent bstTool() {
        JTextField min = Theme.textField("3000", "Min");
        JTextField max = Theme.textField("3050", "Max");
        JComboBox<String> order = Theme.comboBox(new String[]{"Inorder", "Preorder", "Postorder"});
        JTextField limit = Theme.textField("30", "Keys to show");
        JButton range = Theme.primaryButton("Range query");
        JButton traverse = Theme.secondaryButton("Traverse");

        range.addActionListener(e -> {
            int lo = parseInt(min, -1), hi = parseInt(max, -1);
            if (lo < 0 || hi < 0) return;
            if (lo > hi) { log.accept("Min must not be greater than max."); return; }
            CustomLinkedList<Equipment> found = store.getByUsageRange(lo, hi);
            bstView.showRange(lo, hi);
            StringBuilder sb = new StringBuilder(String.format("%,d item(s) used %,d to %,d times (ascending)%n", found.size(), lo, hi));
            int shown = 0;
            for (Equipment q : found) {
                if (shown++ == 25) { sb.append("  ... ").append(found.size() - 25).append(" more\n"); break; }
                sb.append("  ").append(q).append("\n");
            }
            print("BST range query", sb.toString());
        });

        traverse.addActionListener(e -> {
            int n = parseInt(limit, 30);
            if (n < 0) return;
            CustomBST bst = store.getUsageIndex();
            int which = order.getSelectedIndex();
            bstView.showTraversal(which);
            CustomLinkedList<Integer> keys = which == 0 ? bst.inorder() : which == 1 ? bst.preorder() : bst.postorder();
            StringBuilder sb = new StringBuilder(String.format("First %d of %,d keys (usage counts):%n", Math.min(n, keys.size()), keys.size()));
            int i = 0;
            for (int k : keys) {
                if (i == n) break;
                sb.append(String.format("%6d", k)).append(++i % 10 == 0 ? "\n" : "");
            }
            print(order.getSelectedItem() + " traversal", sb.toString());
        });

        JPanel minMax = Theme.transparent(new GridLayout(1, 2, 10, 0));
        minMax.add(Theme.field("Min usage", min));
        minMax.add(Theme.field("Max usage", max));

        JPanel traversal = Theme.transparent(new GridLayout(1, 2, 10, 0));
        traversal.add(Theme.field("Traversal", order));
        traversal.add(Theme.field("Keys to show", limit));

        JPanel travRow = Theme.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        travRow.add(traverse);

        return toolCard("Binary search tree", "Indexed by usage count",
                form(minMax, traversal, travRow), range, bstStats,
                note("Inorder visits keys smallest to largest. Preorder starts at the root; postorder ends at it."));
    }

    // ================= Hash table =================

    private JComponent hashTool() {
        JTextField id = Theme.textField("EQ-00001", "Equipment ID");
        JButton locate = Theme.primaryButton("Locate bucket");
        JButton demo = Theme.secondaryButton("Collision demo (capacity 7)");

        locate.addActionListener(e -> {
            String key = id.getText().trim();
            if (key.isEmpty()) { log.accept("Type an ID first."); return; }
            CustomHashTable<String, Equipment> h = store.getIdIndex();
            int b = h.bucketIndexOf(key);
            CustomLinkedList<String> chain = h.keysInBucket(b);
            hashView.show(h, b - 3, 7, b, key, String.format("Buckets %,d\u2013%,d of %,d  \u00B7  %s \u2192 bucket %,d",
                    Math.max(0, b - 3), Math.min(h.getCapacity() - 1, b + 3), h.getCapacity(), key, b));
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s \u2192 bucket %,d of %,d  (%s)%n", key, b, h.getCapacity(),
                    h.contains(key) ? "present" : "not in the table"));
            sb.append("Chain in this bucket (").append(chain.size()).append(" key").append(chain.size() == 1 ? "" : "s")
              .append("): ").append(join(chain, " \u2192 ")).append("\n");
            sb.append(chain.size() > 1 ? "Collision: these keys share one bucket and are found by walking the chain.\n"
                                       : "No collision in this bucket.\n");
            print("Hash bucket lookup", sb.toString());
        });

        demo.addActionListener(e -> {
            CustomHashTable<String, String> tiny = new CustomHashTable<>(7);
            tiny.put("EQ-00001", "Medicine Ball 1");
            int target = tiny.bucketIndexOf("EQ-00001");
            String collider = null;
            for (int i = 2; collider == null; i++) {
                String candidate = String.format("EQ-%05d", i);
                if (tiny.bucketIndexOf(candidate) == target) collider = candidate;
            }
            tiny.put(collider, "collides with EQ-00001");
            tiny.put("EQ-00002", "no collision");

            // Drawn from a copy, because the demo below removes the colliding key again.
            CustomHashTable<String, String> picture = new CustomHashTable<>(7);
            picture.put("EQ-00001", "");
            picture.put(collider, "");
            picture.put("EQ-00002", "");
            hashView.show(picture, 0, 7, target, collider,
                    "Demo table, capacity 7  \u00B7  EQ-00001 and " + collider + " collide in bucket " + target);
            StringBuilder sb = new StringBuilder("Inserted EQ-00001, " + collider + ", EQ-00002 into 7 buckets:\n");
            for (int b = 0; b < tiny.getCapacity(); b++) {
                sb.append(String.format("  bucket %d: %s%n", b, join(tiny.keysInBucket(b), " \u2192 ")));
            }
            sb.append("get(").append(collider).append(") = ").append(tiny.get(collider)).append("\n");
            sb.append("remove(").append(collider).append(") = ").append(tiny.remove(collider))
              .append(", EQ-00001 still found = ").append(tiny.get("EQ-00001") != null).append("\n");
            print("Collision demonstration", sb.toString());
        });

        JPanel demoRow = Theme.transparent(new BorderLayout());
        demoRow.add(demo, BorderLayout.CENTER);

        return toolCard("Hash table", "Equipment ID index, separate chaining",
                form(Theme.field("Equipment ID", id), demoRow), locate, hashStats,
                note("Try EQ-00001: it shares its bucket with three other IDs in the full table."));
    }

    // ================= Graph =================

    private JComponent graphTool() {
        String[] zones = zoneArray();
        JComboBox<String> from = Theme.comboBox(zones);
        JComboBox<String> to = Theme.comboBox(zones);
        to.setSelectedItem("Olympic Lifting Platform");
        // Picking a new zone moves the outlines on the drawing and clears the old route.
        from.addActionListener(e -> floorView.setEndpoints(sel(from), sel(to)));
        to.addActionListener(e -> floorView.setEndpoints(sel(from), sel(to)));
        floorView.setEndpoints(sel(from), sel(to));
        JButton bfs = Theme.secondaryButton("BFS route");
        JButton dfs = Theme.secondaryButton("DFS route");

        // BFS: goes level by level, so the route it finds is always the SHORTEST (fewest hops).
        bfs.addActionListener(e -> {
            CustomLinkedList<String> route = floor.shortestPath(sel(from), sel(to));
            floorView.showRoute(route, false);
            print("BFS route (shortest): " + sel(from) + " to " + sel(to), route.isEmpty()
                    ? sel(to) + " cannot be reached from " + sel(from) + "."
                    : join(route, " \u2192 ") + "\n" + (route.size() - 1) + " hop(s)  \u00B7  fewest possible");
        });

        // DFS: goes as deep as it can first, so the route it finds is usually LONGER.
        // We show the hop count of both so the difference is easy to explain.
        dfs.addActionListener(e -> {
            CustomLinkedList<String> route = floor.dfsPath(sel(from), sel(to));
            floorView.showRoute(route, true);
            if (route.isEmpty()) {
                print("DFS route: " + sel(from) + " to " + sel(to), sel(to) + " cannot be reached from " + sel(from) + ".");
                return;
            }
            int bfsHops = floor.shortestPath(sel(from), sel(to)).size() - 1;
            print("DFS route (first route found, not always shortest): " + sel(from) + " to " + sel(to),
                    join(route, " \u2192 ") + "\n" + (route.size() - 1) + " hop(s)  \u00B7  BFS shortest is "
                    + bfsHops + " hop(s)\n"
                    + "Reachable: " + floor.dfs(sel(from)).size() + " of " + floor.getZoneCount()
                    + " zones  \u00B7  components: " + floor.countComponents());
        });

        JPanel buttons = Theme.transparent(new GridLayout(1, 2, 8, 0));
        buttons.add(bfs);
        buttons.add(dfs);

        return toolCard("Floor plan graph", "Zones connected by walkways",
                form(Theme.field("From", from), Theme.field("To", to), buttons),
                null, graphStats, null);
    }

    // ================= Layout helpers =================

    // A tool card: header, stacked controls, main action button, optional stats line and note.
    private JComponent toolCard(String title, String subtitle, JComponent controls, JButton action,
                                JLabel stats, JComponent note) {
        JPanel content = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.gridy = 0; c.insets = new Insets(0, 0, 16, 0);
        content.add(controls, c);
        c.gridy = 1;
        if (action != null) content.add(action, c);   // the Graph card has its buttons in the form instead
        if (stats != null) { c.gridy = 2; content.add(stats, c); }
        if (note != null) { c.gridy = 3; content.add(note, c); }
        c.gridy = 4; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), c);
        return Theme.card(title, subtitle, content);
    }

    // Stacks components vertically with even spacing.
    private JComponent form(JComponent... rows) {
        JPanel p = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        for (int i = 0; i < rows.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i == rows.length - 1 ? 0 : 12, 0);
            p.add(rows[i], c);
        }
        return p;
    }

    // Muted explanatory text that wraps inside the card.
    private JComponent note(String text) {
        JTextArea a = new JTextArea(text);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setFont(Theme.FONT_SMALL);
        a.setForeground(Theme.TEXT_MUTED);
        a.setBorder(new EmptyBorder(0, 2, 0, 0));
        return a;
    }

    private int parseInt(JTextField f, int fallback) {
        String t = f.getText().trim();
        if (t.isEmpty()) return fallback;
        try {
            int v = Integer.parseInt(t);
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException ex) {
            log.accept("\"" + t + "\" is not a valid non-negative whole number.");
            return -1;
        }
    }

    private String[] zoneArray() {
        String[] zones = new String[floor.getZoneCount()];
        int i = 0;
        for (String z : floor.getZones()) zones[i++] = z;
        return zones;
    }

    private static String sel(JComboBox<String> box) { return (String) box.getSelectedItem(); }

    private static String cut(String s, int max) { return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026"; }

    private static String join(CustomLinkedList<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(s);
        }
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }

    // ================= Segmented control =================

    // A row of pill buttons where exactly one is selected (like tabs, but modern-looking).
    private static class Segmented extends JPanel {
        private final JButton[] buttons;
        private int selected;

        Segmented(String[] labels, java.util.function.IntConsumer onSelect) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 4));
            setOpaque(false);
            buttons = new JButton[labels.length];
            for (int i = 0; i < labels.length; i++) {
                final int index = i;
                JButton b = new JButton(labels[i]) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = Theme.smooth(g);
                        boolean on = index == selected;
                        if (on || getModel().isRollover()) {
                            g2.setColor(on ? Theme.ACCENT : Theme.SURFACE_HOVER);
                            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                        }
                        g2.dispose();
                        setForeground(on ? Theme.ON_ACCENT : getModel().isRollover() ? Theme.TEXT : Theme.TEXT_MUTED);
                        super.paintComponent(g);
                    }
                };
                b.setFont(Theme.FONT_BODY_B);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setFocusPainted(false);
                b.setOpaque(false);
                b.setRolloverEnabled(true);
                b.setBorder(new EmptyBorder(9, 18, 9, 18));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addActionListener(e -> {
                    selected = index;
                    for (JButton other : buttons) other.repaint();
                    onSelect.accept(index);
                });
                buttons[i] = b;
                add(b);
            }
        }

        @Override
        public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            Dimension d = getPreferredSize();
            g2.setColor(Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, d.width - 1f, getHeight() - 1f, 14, 14));
            g2.setColor(Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, d.width - 1f, getHeight() - 1f, 14, 14));
            g2.dispose();
        }
    }
}
