
// EquipmentPanel.java
// Part of: Arkin Ipac  (Integration)
// Presented in: Slides 5-8 (John Benedict Jesalva) - add, search, delete.
// What it does: The Equipment page: add, search, filter and delete equipment.
//               Saves changes to MySQL through EquipmentDAO.
// Used by: MainFrame


package ui;

import db.EquipmentDAO;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import model.Equipment;
import service.EquipmentStore;
import service.GymFloorPlan;
import service.MaintenanceManager;

// "Equipment" page: registration form on the left, full inventory table on the right,
// with search, a category filter, and delete.
//
// Add:    store.addEquipment() (array + hash table + BST), then dao.insert() for MySQL.
//         If MySQL rejects it, we remove it from memory again so both stay the same.
// Delete: dao.delete() first, then manager.cancel() (in case it's waiting for service),
//         then store.deleteEquipment().
// If the app is offline, the DAO is null and saving is skipped with a message.
public class EquipmentPanel extends JPanel {

    private final EquipmentStore store;
    private final MaintenanceManager manager;
    private final Supplier<EquipmentDAO> dao;
    private final Consumer<String> log;
    private final Runnable onChange;

    private final InventoryModel model = new InventoryModel();
    private final JTable table = new JTable(model);
    private final JLabel showing = Theme.muted("");

    private JTextField searchField;
    private JComboBox<String> categoryFilter;

    // Form inputs
    private final JTextField idField = Theme.textField("e.g. EQ-05001");
    private final JTextField nameField = Theme.textField("e.g. Treadmill 5001");
    private final JComboBox<Equipment.Category> categoryBox = Theme.comboBox(Equipment.Category.values());
    private final JComboBox<String> locationBox = Theme.comboBox(zoneNames());
    private final JComboBox<Equipment.Condition> conditionBox = Theme.comboBox(Equipment.Condition.values());
    private final JTextField usageField = Theme.textField("0", "Times used");

    public EquipmentPanel(EquipmentStore store, MaintenanceManager manager, Supplier<EquipmentDAO> dao,
                          Consumer<String> log, Runnable onChange) {
        super(new BorderLayout(20, 0));
        this.store = store;
        this.manager = manager;
        this.dao = dao;
        this.log = log;
        this.onChange = onChange;
        setOpaque(false);

        add(buildForm(), BorderLayout.WEST);
        add(buildInventory(), BorderLayout.CENTER);
    }

    // ================= Left: registration form =================

    private JComponent buildForm() {
        JPanel form = Theme.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 14, 0);

        conditionBox.setRenderer(new ConditionListRenderer());

        c.gridy = 0; form.add(Theme.field("Equipment ID", idField), c);
        c.gridy = 1; form.add(Theme.field("Name", nameField), c);
        c.gridy = 2; form.add(Theme.field("Category", categoryBox), c);
        c.gridy = 3; form.add(Theme.field("Location", locationBox), c);
        c.gridy = 4; form.add(Theme.field("Condition", conditionBox), c);
        c.gridy = 5; form.add(Theme.field("Usage count", usageField), c);

        JButton add = Theme.primaryButton("Add equipment");
        JButton clear = Theme.ghostButton("Clear");
        add.addActionListener(e -> addEquipment());
        clear.addActionListener(e -> clearForm());
        JPanel buttons = Theme.transparent(new BorderLayout(10, 0));
        buttons.add(add, BorderLayout.CENTER);
        buttons.add(clear, BorderLayout.EAST);
        c.gridy = 6; c.insets = new Insets(6, 0, 0, 0);
        form.add(buttons, c);

        c.gridy = 7; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        form.add(Box.createGlue(), c);                  // pushes everything to the top

        Theme.Card card = Theme.card("Register equipment", "Stored in the array, hash table and BST", form);
        return Theme.fixedWidth(card, 340);
    }

    private void addEquipment() {
        int usage;
        try {
            usage = Integer.parseInt(usageField.getText().trim());
        } catch (NumberFormatException ex) {
            log.accept("Usage count must be a whole number, e.g. 250.");
            return;
        }
        Equipment e;
        try {
            e = new Equipment(idField.getText(), nameField.getText(),
                    (Equipment.Category) categoryBox.getSelectedItem(),
                    (String) locationBox.getSelectedItem(),
                    (Equipment.Condition) conditionBox.getSelectedItem(), usage);
            store.addEquipment(e);                         // throws on duplicate ID
        } catch (IllegalArgumentException ex) {
            log.accept("Could not add: " + ex.getMessage());
            return;
        }

        EquipmentDAO db = dao.get();
        if (db != null) {
            try {
                db.insert(e);
            } catch (Exception ex) {
                store.deleteEquipment(e.getEquipmentId());  // keep memory and database consistent
                log.accept("Database rejected " + e.getEquipmentId() + ": " + ex.getMessage());
                return;
            }
        }
        log.accept("Added " + e.getEquipmentId() + " - " + e.getName() + (db == null ? " (offline, not saved)" : ""));
        clearForm();
        onChange.run();
        selectById(e.getEquipmentId());
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        usageField.setText("0");
        categoryBox.setSelectedIndex(0);
        locationBox.setSelectedIndex(0);
        conditionBox.setSelectedIndex(0);
        idField.requestFocusInWindow();
    }

    // ================= Right: inventory table =================

    private JComponent buildInventory() {
        searchField = Theme.textField("Search ID, name or location");
        searchField.setPreferredSize(new Dimension(260, 40));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
        });

        String[] categories = new String[Equipment.Category.values().length + 1];
        categories[0] = "All categories";
        for (int i = 0; i < Equipment.Category.values().length; i++) {
            categories[i + 1] = Theme.pretty(Equipment.Category.values()[i]);
        }
        categoryFilter = Theme.comboBox(categories);
        categoryFilter.setPreferredSize(new Dimension(190, 40));
        categoryFilter.addActionListener(e -> refreshTable());

        JButton delete = Theme.dangerButton("Delete selected");
        delete.addActionListener(e -> deleteSelected());

        JPanel tools = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.add(searchField);
        tools.add(categoryFilter);
        tools.add(delete);

        Theme.styleTable(table);
        table.setAutoCreateRowSorter(false);
        table.getColumnModel().getColumn(4).setCellRenderer(new Theme.ConditionRenderer());
        DefaultTableCellRenderer right = new Theme.CellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        TableColumnModel cols = table.getColumnModel();
        int[] widths = {100, 200, 140, 180, 115, 80};
        int[] minimum = {96, 120, 100, 120, 108, 64};
        for (int i = 0; i < widths.length; i++) {
            cols.getColumn(i).setPreferredWidth(widths[i]);
            cols.getColumn(i).setMinWidth(minimum[i]);
        }

        JPanel body = Theme.transparent(new BorderLayout(0, 12));
        body.add(Theme.tableScroll(table), BorderLayout.CENTER);
        body.add(showing, BorderLayout.SOUTH);

        Theme.Card card = new Theme.Card(new BorderLayout(0, 16));
        card.add(Theme.cardHeader("Inventory", null, tools),
                BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            log.accept("Select a row in the inventory first.");
            return;
        }
        Equipment e = model.rows[row];
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + e.getEquipmentId() + " - " + e.getName() + "?\nThis also removes it from the database.",
                "Delete equipment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;

        EquipmentDAO db = dao.get();
        if (db != null) {
            try {
                db.delete(e.getEquipmentId());
            } catch (Exception ex) {
                log.accept("Database refused to delete " + e.getEquipmentId() + ": " + ex.getMessage());
                return;
            }
        }
        manager.cancel(e);                                 // if it was waiting for maintenance
        store.deleteEquipment(e.getEquipmentId());         // array + hash table + BST
        log.accept("Deleted " + e.getEquipmentId() + " - " + e.getName() + (db == null ? " (offline, not saved)" : ""));
        onChange.run();
    }

    // Rebuilds the table rows using the search text and category filter.
    // This is a partial-text search (ID, name or location CONTAINS what was typed),
    // so it has to check every record: O(n). The hash table only helps for an exact ID.
    // 5,000 records is still fast enough to redo on every keystroke.
    public void refreshTable() {
        if (searchField == null) return;
        String query = searchField.getText().trim().toLowerCase();
        int categoryIndex = categoryFilter.getSelectedIndex();   // 0 = all
        Equipment[] all = store.getAll();
        Equipment[] matches = new Equipment[all.length];
        int count = 0;
        for (Equipment e : all) {
            if (categoryIndex > 0 && e.getCategory().ordinal() != categoryIndex - 1) continue;
            if (!query.isEmpty()
                    && !e.getEquipmentId().toLowerCase().contains(query)
                    && !e.getName().toLowerCase().contains(query)
                    && !(e.getLocation() != null && e.getLocation().toLowerCase().contains(query))) continue;
            matches[count++] = e;
        }
        Equipment[] rows = new Equipment[count];
        System.arraycopy(matches, 0, rows, 0, count);
        model.setRows(rows);
        showing.setText("Showing " + Theme.number(count) + " of " + Theme.number(all.length) + " records");
    }

    private void selectById(String id) {
        for (int i = 0; i < model.rows.length; i++) {
            if (model.rows[i].getEquipmentId().equals(id)) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
    }

    // Every dataset zone (the graph's nodes, minus the Entrance) -- a dropdown instead of free
    // text, so locations always match the floor plan exactly.
    private static String[] zoneNames() {
        structures.CustomGraph floor = GymFloorPlan.build();
        String[] names = new String[floor.getZoneCount() - 1];
        int i = 0;
        for (String zone : floor.getZones()) {
            if (!zone.equals("Entrance")) names[i++] = zone;
        }
        return names;
    }

    // ================= Table model =================

    // Backs the JTable directly with an Equipment[] -- no copying into Vector rows.
    private static class InventoryModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"ID", "Name", "Category", "Location", "Condition", "Usage"};
        Equipment[] rows = new Equipment[0];

        void setRows(Equipment[] rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.length; }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Object getValueAt(int r, int c) {
            Equipment e = rows[r];
            switch (c) {
                case 0: return e.getEquipmentId();
                case 1: return e.getName();
                case 2: return Theme.pretty(e.getCategory());
                case 3: return e.getLocation();
                case 4: return e.getCondition();
                default: return Theme.number(e.getUsageCount());
            }
        }
    }

    // Condition dropdown items get the same colored dot as the table.
    private static class ConditionListRenderer extends DefaultListCellRenderer {
        private Color dot = Theme.TEXT_MUTED;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            Equipment.Condition c = (Equipment.Condition) value;
            dot = Theme.conditionColor(c);
            setText(Theme.pretty(c));
            setFont(Theme.FONT_BODY);
            if (index == -1) {
                setOpaque(false);
                setForeground(Theme.TEXT);
                setBorder(new EmptyBorder(4, 22, 4, 4));
            } else {
                setOpaque(true);
                setBackground(selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT);
                setForeground(selected ? Theme.ACCENT : Theme.TEXT);
                setBorder(new EmptyBorder(8, 28, 8, 10));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(dot);
            int x = getBorder().getBorderInsets(this).left - 16;
            g2.fillOval(x, getHeight() / 2 - 4, 8, 8);
            g2.dispose();
        }
    }
}
