
// MainFrame.java
// Part of: Arkin Ipac  (Integration)
// Presented in: Slide 4 (Arkin Ipac) - architecture; this window runs the demo.
// What it does: Main window and starting point of the app. Loads the data
//               and connects all four pages to the same store and manager.
// Used by: run this class to start the app


package ui;

import db.DatasetImporter;
import db.EquipmentDAO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.Equipment;
import service.EquipmentStore;
import service.MaintenanceManager;

// The main window and the starting point of the app (see main() at the bottom).
// It creates ONE EquipmentStore and ONE MaintenanceManager, loads the data into them,
// and passes the same objects to all four pages, so every page sees the same data.
// Layout: sidebar on the left, current page in the middle, activity log at the bottom.
public class MainFrame extends JFrame {

    // Shared by every page. `store` is declared first because the manager needs it.
    private final EquipmentStore store = new EquipmentStore();
    private final MaintenanceManager manager = new MaintenanceManager(store);

    // Null while the app is running offline (MySQL unreachable, data loaded from the CSV):
    // pages check for null and skip saving instead of crashing.
    private EquipmentDAO dao = new EquipmentDAO();

    // Pages
    private EquipmentPanel equipmentPage;
    private MaintenancePanel maintenancePage;
    private ReportsPanel reportsPage;
    private BenchmarkPanel benchmarkPage;

    // Chrome
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHost = new JPanel(pages);
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private final JLabel statEquipment = new JLabel();
    private final JLabel statQueue = new JLabel();
    private final JLabel statHeap = new JLabel();
    private final JLabel dbStatus = new JLabel();
    private final JTextArea console = Theme.outputArea("Everything you do in the app is logged here.");
    private NavItem[] navItems;
    private Chip queueChip, heapChip;

    private static final String[][] PAGES = {
        // key, nav label, page title, page subtitle
        {"equipment",   "Equipment",   "Equipment",   "Register, search and remove gym equipment"},
        {"maintenance", "Maintenance", "Maintenance", "Flag problems, rank them by urgency, and service the most critical first"},
        {"reports",     "Reports",     "Reports",     "Sorting, searching, BST, hash table and floor-plan tools"},
        {"benchmark",   "Benchmarks",  "Benchmarks",  "Measured running times compared with Big-O predictions"},
    };

    public MainFrame() {
        super("Gym Equipment Maintenance Prioritization System");
        Theme.install();
        buildUi();
        loadData();
        refreshAll();
        showPage(0);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ================= Layout =================

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);

        equipmentPage = new EquipmentPanel(store, manager, () -> dao, this::log, this::refreshAll);
        maintenancePage = new MaintenancePanel(store, manager, () -> dao, this::log, this::refreshAll);
        reportsPage = new ReportsPanel(store, this::log);
        benchmarkPage = new BenchmarkPanel(store, this::log);

        pageHost.setOpaque(false);
        pageHost.add(Theme.page(equipmentPage), PAGES[0][0]);      // page() = scrolls if the window is small
        pageHost.add(Theme.page(maintenancePage), PAGES[1][0]);
        pageHost.add(Theme.page(reportsPage), PAGES[2][0]);
        pageHost.add(Theme.page(benchmarkPage), PAGES[3][0]);

        JPanel main = Theme.transparent(new BorderLayout(0, 16));
        main.setBorder(new EmptyBorder(20, 28, 16, 28));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(pageHost, BorderLayout.CENTER);
        main.add(buildConsole(), BorderLayout.SOUTH);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(236, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        // Brand block: painted logo mark + app name
        JPanel brand = Theme.transparent(new BorderLayout(12, 0));
        brand.setBorder(new EmptyBorder(26, 20, 26, 16));
        brand.add(new LogoMark(), BorderLayout.WEST);
        JPanel names = Theme.transparent(new GridLayout(2, 1, 0, 2));
        JLabel name = new JLabel("GymCare");
        name.setFont(Theme.FONT_BRAND);
        name.setForeground(Theme.TEXT);
        names.add(name);
        names.add(Theme.muted("Maintenance System"));
        brand.add(names, BorderLayout.CENTER);
        side.add(brand, BorderLayout.NORTH);

        // Navigation
        JPanel nav = Theme.transparent(null);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(4, 12, 4, 12));
        JLabel section = Theme.fieldLabel("Menu");
        section.setBorder(new EmptyBorder(0, 10, 10, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(section);
        navItems = new NavItem[PAGES.length];
        for (int i = 0; i < PAGES.length; i++) {
            final int index = i;
            navItems[i] = new NavItem(PAGES[i][1], i);
            navItems[i].addActionListener(e -> showPage(index));
            navItems[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            nav.add(navItems[i]);
            nav.add(Box.createVerticalStrut(4));
        }
        side.add(nav, BorderLayout.CENTER);

        // Database status at the bottom of the sidebar
        JPanel footer = Theme.transparent(new BorderLayout());
        footer.setBorder(new EmptyBorder(16, 22, 22, 16));
        dbStatus.setFont(Theme.FONT_SMALL);
        footer.add(dbStatus, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private JComponent buildHeader() {
        JPanel header = Theme.transparent(new BorderLayout(20, 0));
        JPanel titles = Theme.transparent(new GridLayout(2, 1, 0, 4));
        pageTitle.setFont(Theme.FONT_PAGE);
        pageTitle.setForeground(Theme.TEXT);
        pageSubtitle.setFont(Theme.FONT_BODY);
        pageSubtitle.setForeground(Theme.TEXT_MUTED);
        titles.add(pageTitle);
        titles.add(pageSubtitle);
        header.add(titles, BorderLayout.CENTER);

        JPanel chips = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        chips.add(new Chip(statEquipment, Theme.ACCENT));
        queueChip = new Chip(statQueue, Theme.WARNING);
        heapChip = new Chip(statHeap, Theme.DANGER);
        chips.add(queueChip);
        chips.add(heapChip);
        header.add(chips, BorderLayout.EAST);
        return header;
    }

    private JComponent buildConsole() {
        JButton clear = Theme.ghostButton("Clear");
        clear.addActionListener(e -> console.setText(""));
        Theme.Card card = new Theme.Card(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(8, 18, 12, 18));
        card.add(Theme.cardHeader("Activity", null, clear), BorderLayout.NORTH);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        console.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane sp = Theme.scroll(console);
        sp.getViewport().setBackground(Theme.SURFACE_ALT);
        sp.setPreferredSize(new Dimension(0, 62));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ================= Behaviour =================

    private void showPage(int index) {
        pages.show(pageHost, PAGES[index][0]);
        pageTitle.setText(PAGES[index][2]);
        pageSubtitle.setText(PAGES[index][3]);
        for (NavItem item : navItems) item.setActive(item.index == index);
        if (index == 1) maintenancePage.refresh();
        if (index == 2) reportsPage.refresh();
    }

    // MySQL first. If it can't be reached, fall back to the CSV so the app still works
    // (useful if the database is down during a demo) -- but changes won't be saved.
    private void loadData() {
        List<Equipment> all;
        try {
            all = dao.findAll();
            setDbStatus(true);
            log("Loaded " + Theme.number(all.size()) + " pieces of equipment from MySQL.");
        } catch (Exception dbError) {
            dao = null;
            setDbStatus(false);
            log("MySQL unavailable: " + dbError.getMessage());
            try {
                all = DatasetImporter.readCsv("data/equipment_dataset.csv");
                log("Offline mode: loaded " + Theme.number(all.size()) + " records from the CSV. Changes will NOT be saved.");
            } catch (Exception csvError) {
                log("Could not load any data: " + csvError.getMessage());
                return;
            }
        }
        for (Equipment e : all) store.addEquipment(e);   // fills array + hash table + BST
    }

    private void setDbStatus(boolean online) {
        dbStatus.setText(online ? "\u25CF  MySQL connected" : "\u25CF  Offline (CSV)");
        dbStatus.setForeground(online ? Theme.SUCCESS : Theme.WARNING);
    }

    // Called by any page after it changes data, so every view stays in sync.
    private void refreshAll() {
        statEquipment.setText(Theme.number(store.getCount()) + " equipment");
        statQueue.setText(manager.getPendingCount() + " in queue");
        statHeap.setText(manager.getHeapSize() + " in heap");
        queueChip.setActive(manager.getPendingCount() > 0);   // grey dot when empty, so 0 doesn't look like an alert
        heapChip.setActive(manager.getHeapSize() > 0);
        equipmentPage.refreshTable();
        maintenancePage.refresh();
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Every page reports what it did here, with a timestamp.
    private void log(String message) {
        console.append("[" + LocalTime.now().format(TIME) + "]  " + message + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    // ================= Small painted components =================

    // Lime rounded square with a dumbbell drawn inside it.
    private static class LogoMark extends JComponent {
        LogoMark() { setPreferredSize(new Dimension(38, 38)); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(Theme.ACCENT);
            g2.fill(new RoundRectangle2D.Float(0, 0, 38, 38, 12, 12));
            g2.setColor(Theme.ON_ACCENT);
            g2.fillRoundRect(8, 13, 5, 12, 2, 2);    // left plate
            g2.fillRoundRect(25, 13, 5, 12, 2, 2);   // right plate
            g2.fillRoundRect(12, 17, 14, 4, 2, 2);   // bar
            g2.dispose();
        }
    }

    // Sidebar entry: icon + label; highlighted with a lime bar and tint when active.
    private static class NavItem extends JButton {
        final int index;
        private boolean active;

        NavItem(String text, int index) {
            super(text);
            this.index = index;
            setFont(Theme.FONT_BODY_B);
            setHorizontalAlignment(LEFT);
            setBorder(new EmptyBorder(11, 44, 11, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            if (active || getModel().isRollover()) {
                g2.setColor(active ? Theme.ACCENT_SOFT : Theme.SURFACE_HOVER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            }
            if (active) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
            }
            Color c = active ? Theme.ACCENT : Theme.TEXT_MUTED;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            paintIcon(g2, 16, getHeight() / 2 - 8);
            g2.dispose();
            setForeground(active ? Theme.ACCENT : getModel().isRollover() ? Theme.TEXT : Theme.TEXT_MUTED);
            super.paintComponent(g);
        }

        // Simple 16x16 line icons, one per page.
        private void paintIcon(Graphics2D g, int x, int y) {
            switch (index) {
                case 0: // equipment: list
                    for (int i = 0; i < 3; i++) {
                        g.fillOval(x, y + 2 + i * 5, 2, 2);
                        g.drawLine(x + 5, y + 3 + i * 5, x + 15, y + 3 + i * 5);
                    }
                    break;
                case 1: // maintenance: wrench-like ring + handle
                    g.drawOval(x + 1, y + 1, 7, 7);
                    g.drawLine(x + 7, y + 7, x + 15, y + 15);
                    break;
                case 2: // reports: bar chart
                    g.drawLine(x + 2, y + 15, x + 2, y + 9);
                    g.drawLine(x + 8, y + 15, x + 8, y + 3);
                    g.drawLine(x + 14, y + 15, x + 14, y + 7);
                    break;
                default: // benchmarks: stopwatch
                    g.drawOval(x + 1, y + 3, 13, 13);
                    g.drawLine(x + 7, y + 9, x + 7, y + 6);
                    g.drawLine(x + 5, y + 1, x + 10, y + 1);
                    break;
            }
        }
    }

    // Rounded pill with a colored dot, used for the header statistics.
    private static class Chip extends JPanel {
        private final Color dot;
        private boolean active = true;

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        Chip(JLabel text, Color dot) {
            super(new BorderLayout());
            this.dot = dot;
            setOpaque(false);
            setBorder(new EmptyBorder(7, 28, 7, 14));
            text.setFont(Theme.FONT_BODY_B);
            text.setForeground(Theme.TEXT);
            add(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, getHeight(), getHeight()));
            g2.setColor(Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, getHeight(), getHeight()));
            g2.setColor(active ? dot : Theme.TEXT_DIM);
            g2.fillOval(13, getHeight() / 2 - 4, 8, 8);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
