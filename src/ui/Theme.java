
// Theme.java
// Part of: Arkin Ipac  (Integration / UI)
// Presented in: not presented on its own (UI styling only).
// What it does: Colors, fonts and styled Swing components for the whole app.
//               No data structure logic here.
// Used by: all ui/ classes


package ui;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

// All the colors, fonts and styled Swing components used by the app.
// The panels ask Theme for buttons, tables, cards, etc. instead of styling them one by
// one, so the look is the same everywhere and can be changed from this one file.
// Colors: dark charcoal background, light text, and the gym's lime green as the accent
// for buttons and highlights. No data structure logic is in this file.
public final class Theme {

    private Theme() { }

    // ================= Colors =================
    public static final Color BG            = new Color(0x0B0D10);  // app background
    public static final Color SIDEBAR       = new Color(0x101317);  // navigation rail
    public static final Color SURFACE       = new Color(0x15191E);  // cards
    public static final Color SURFACE_ALT   = new Color(0x1B2026);  // inputs, table header, zebra rows
    public static final Color SURFACE_HOVER = new Color(0x222830);
    public static final Color BORDER        = new Color(0x252B33);
    public static final Color BORDER_STRONG = new Color(0x343C47);

    public static final Color TEXT       = new Color(0xE6E8EB);     // main reading text
    public static final Color TEXT_MUTED = new Color(0x8D96A2);     // labels, captions
    public static final Color TEXT_DIM   = new Color(0x5D6672);     // placeholders

    public static final Color ACCENT         = new Color(0xC6F432); // brand lime
    public static final Color ACCENT_HOVER   = new Color(0xD5FF5A);
    public static final Color ACCENT_PRESSED = new Color(0xA9D61B);
    public static final Color ACCENT_SOFT    = new Color(0x222B14); // selected rows / active nav
    public static final Color ON_ACCENT      = new Color(0x0B0D10); // text on lime

    public static final Color DANGER       = new Color(0xF25C61);
    public static final Color DANGER_HOVER = new Color(0xFF7479);
    public static final Color WARNING      = new Color(0xF5B83D);
    public static final Color SUCCESS      = new Color(0x3DD68C);

    // Condition colors run green -> red, so severity can be read at a glance.
    public static Color conditionColor(model.Equipment.Condition c) {
        switch (c) {
            case EXCELLENT: return new Color(0x3DD68C);
            case GOOD:      return new Color(0xA6E13B);
            case FAIR:      return new Color(0xF5D042);
            case POOR:      return new Color(0xF5933D);
            default:        return new Color(0xF25C61);   // CRITICAL
        }
    }

    // ================= Fonts =================
    // Uses the best font installed on the machine: Segoe UI on Windows, Inter/Roboto if
    // present, otherwise Java's default sans-serif.
    private static final String UI_FAMILY   = pickFamily("Segoe UI", "Inter", "Roboto", "Helvetica Neue", "SansSerif");
    private static final String MONO_FAMILY = pickFamily("Cascadia Mono", "Consolas", "JetBrains Mono", "DejaVu Sans Mono", "Monospaced");

    public static final Font FONT_BRAND    = new Font(UI_FAMILY, Font.BOLD, 17);
    public static final Font FONT_PAGE     = new Font(UI_FAMILY, Font.BOLD, 24);
    public static final Font FONT_H2       = new Font(UI_FAMILY, Font.BOLD, 15);
    public static final Font FONT_BODY     = new Font(UI_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_B   = new Font(UI_FAMILY, Font.BOLD, 14);
    public static final Font FONT_SMALL    = new Font(UI_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_LABEL    = new Font(UI_FAMILY, Font.BOLD, 12);
    public static final Font FONT_STAT     = new Font(UI_FAMILY, Font.BOLD, 24);
    public static final Font FONT_MONO     = new Font(MONO_FAMILY, Font.PLAIN, 13);

    private static String pickFamily(String... candidates) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String wanted : candidates) {
            for (String have : installed) {
                if (have.equalsIgnoreCase(wanted)) return have;
            }
        }
        return candidates[candidates.length - 1];
    }

    public static final int RADIUS = 12;

    // Call once at startup, before any component is created. Sets defaults for the few
    // things we don't build ourselves (tooltips, dialogs, text selection colors).
    public static void install() {
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ToolTip.background", SURFACE_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER_STRONG));
        UIManager.put("TextField.selectionBackground", new Color(0x3A4A1C));
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TextArea.selectionBackground", new Color(0x3A4A1C));
        UIManager.put("TextArea.selectionForeground", TEXT);
        UIManager.put("ScrollBar.width", 10);
    }

    // Smooth edges and text on everything we paint by hand.
    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    // "STRENGTH_MACHINE" -> "Strength machine": enum names made readable for the screen.
    public static String pretty(Enum<?> value) {
        String s = value.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // 5000 -> "5,000"
    public static String number(long value) {
        return String.format("%,d", value);
    }

    // ================= Labels =================

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // Small uppercase caption above an input.
    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MUTED);
        l.setBorder(new EmptyBorder(0, 2, 6, 0));
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H2);
        l.setForeground(TEXT);
        return l;
    }

    // ================= Buttons =================

    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER }

    public static JButton primaryButton(String text)   { return new Button(text, Variant.PRIMARY); }
    public static JButton secondaryButton(String text) { return new Button(text, Variant.SECONDARY); }
    public static JButton ghostButton(String text)     { return new Button(text, Variant.GHOST); }
    public static JButton dangerButton(String text)    { return new Button(text, Variant.DANGER); }

    // A rounded, flat button with hover and pressed states, painted by hand so it looks the
    // same on every operating system.
    public static class Button extends JButton {
        private final Variant variant;

        public Button(String text, Variant variant) {
            super(text);
            this.variant = variant;
            setFont(FONT_BODY_B);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setForeground(variant == Variant.PRIMARY ? ON_ACCENT
                        : variant == Variant.DANGER ? DANGER : TEXT);
        }

        // A disabled button shows the normal arrow instead of the hand, so it doesn't look clickable.
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            ButtonModel m = getModel();
            boolean hover = m.isRollover(), pressed = m.isPressed(), enabled = isEnabled();
            int w = getWidth(), h = getHeight();
            Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 10, 10);

            Color fill = null, line = null;
            switch (variant) {
                case PRIMARY:
                    fill = !enabled ? BORDER_STRONG : pressed ? ACCENT_PRESSED : hover ? ACCENT_HOVER : ACCENT;
                    break;
                case SECONDARY:
                    fill = pressed ? SURFACE_ALT : hover ? SURFACE_HOVER : SURFACE;
                    line = hover && enabled ? ACCENT : BORDER_STRONG;
                    break;
                case GHOST:
                    fill = pressed ? SURFACE_ALT : hover ? SURFACE_HOVER : null;
                    break;
                case DANGER:
                    fill = pressed ? new Color(0x3A1719) : hover ? new Color(0x2C1517) : SURFACE;
                    line = hover ? DANGER_HOVER : new Color(0x5A2A2D);
                    break;
            }
            if (fill != null) { g2.setColor(fill); g2.fill(shape); }
            if (line != null) { g2.setColor(line); g2.draw(shape); }
            g2.dispose();

            if (variant == Variant.PRIMARY) setForeground(enabled ? ON_ACCENT : TEXT_DIM);
            else if (variant != Variant.DANGER) setForeground(enabled ? (hover && variant == Variant.SECONDARY ? ACCENT : TEXT) : TEXT_DIM);
            super.paintComponent(g);
        }
    }

    // ================= Cards =================

    // A rounded surface with a thin border -- the basic building block of every page.
    public static class Card extends JPanel {
        public Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(20, 20, 20, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 16, 16);
            g2.setColor(SURFACE);
            g2.fill(shape);
            g2.setColor(BORDER);
            g2.draw(shape);
            g2.dispose();
        }
    }

    // Card with a title (and optional subtitle) on top and the given content underneath.
    public static Card card(String title, String subtitle, JComponent content) {
        return card(title, subtitle, null, content);
    }

    // Same card, with an optional component (e.g. a Clear button) on the right of the title.
    public static Card card(String title, String subtitle, JComponent right, JComponent content) {
        Card card = new Card(new BorderLayout(0, 16));
        card.add(cardHeader(title, subtitle, right), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // Title block for a card, with optional component (e.g. a button) on the right.
    public static JPanel cardHeader(String title, String subtitle, JComponent right) {
        JPanel text = transparent(new GridLayout(subtitle == null ? 1 : 2, 1, 0, 4));
        text.add(heading(title));
        if (subtitle != null) text.add(muted(subtitle));
        JPanel header = transparent(new BorderLayout(12, 0));
        header.add(text, BorderLayout.CENTER);
        if (right != null) header.add(right, BorderLayout.EAST);
        return header;
    }

    // Small metric tile: caption on top, big value below.
    public static Card statTile(String caption, JLabel value, JLabel detail) {
        Card tile = new Card(new BorderLayout(0, 2));
        tile.setBorder(new EmptyBorder(12, 18, 12, 18));
        tile.add(fieldLabel(caption), BorderLayout.NORTH);
        value.setFont(FONT_STAT);
        value.setForeground(TEXT);
        tile.add(value, BorderLayout.CENTER);
        if (detail != null) {
            detail.setFont(FONT_SMALL);
            detail.setForeground(TEXT_MUTED);
            tile.add(detail, BorderLayout.SOUTH);
        }
        return tile;
    }

    public static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    // Gives a component a fixed WIDTH while keeping its natural HEIGHT. (Setting a preferred
    // size of (width, 0) instead would tell the layout "I need no height", which is what
    // squashed the Workflow card: the page thought everything fit and never scrolled.)
    public static JComponent fixedWidth(JComponent content, int width) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(content);
        return wrapper;
    }

    // Stacks a caption above an input: the standard "form field" block.
    public static JPanel field(String caption, JComponent input) {
        JPanel p = transparent(new BorderLayout());
        p.add(fieldLabel(caption), BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    // ================= Inputs =================

    // Rounded outline that turns lime while its component has keyboard focus.
    static class RoundBorder extends AbstractBorder {
        private final Insets padding;
        RoundBorder(Insets padding) { this.padding = padding; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            g2.setColor(c.isFocusOwner() || (c instanceof JComboBox && ((JComboBox<?>) c).isPopupVisible())
                    ? ACCENT : BORDER_STRONG);
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, RADIUS, RADIUS));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return padding; }
    }

    // Text field with a rounded dark background and a placeholder hint when empty.
    public static class TextField extends JTextField {
        private final String placeholder;

        public TextField(String text, String placeholder) {
            super(text);
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(FONT_BODY);
            setForeground(TEXT);
            setCaretColor(ACCENT);
            setBorder(new RoundBorder(new Insets(9, 12, 9, 12)));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(SURFACE_ALT);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS, RADIUS));
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null && !isFocusOwner()) {
                Graphics2D p = smooth(g);
                p.setColor(TEXT_DIM);
                p.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = p.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                p.drawString(placeholder, in.left, y);
                p.dispose();
            }
        }
    }

    public static JTextField textField(String placeholder) { return new TextField("", placeholder); }
    public static JTextField textField(String text, String placeholder) { return new TextField(text, placeholder); }

    // Dropdown with the same rounded look, a drawn chevron, and a dark popup list.
    public static class ComboBox<T> extends JComboBox<T> {
        public ComboBox(T[] items) {
            super(items);
            setOpaque(false);
            setFont(FONT_BODY);
            setForeground(TEXT);
            setBackground(SURFACE_ALT);
            setBorder(new RoundBorder(new Insets(4, 8, 4, 4)));
            setMaximumRowCount(12);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton b = new JButton() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = smooth(g);
                            g2.setColor(TEXT_MUTED);
                            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            int cx = getWidth() / 2, cy = getHeight() / 2;
                            g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                            g2.dispose();
                        }
                    };
                    b.setBorder(new EmptyBorder(0, 0, 0, 0));
                    b.setContentAreaFilled(false);
                    b.setFocusPainted(false);
                    b.setOpaque(false);
                    b.setPreferredSize(new Dimension(28, 28));
                    return b;
                }

                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                    // background is painted by the combo itself (rounded), nothing to do here
                }

                // Draws the chosen value as normal white text. Swing's default version uses the
                // list's "selected" colors while the dropdown has focus, which made the text turn
                // dark/gray right after picking something.
                @Override
                public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                    ListCellRenderer<? super Object> renderer = comboBox.getRenderer();
                    Component c = renderer.getListCellRendererComponent(
                            listBox, comboBox.getSelectedItem(), -1, false, false);
                    c.setFont(comboBox.getFont());
                    c.setForeground(TEXT);
                    currentValuePane.paintComponent(g, c, comboBox,
                            bounds.x, bounds.y, bounds.width, bounds.height, c instanceof JPanel);
                }

                @Override
                protected ComboPopup createPopup() {
                    BasicComboPopup popup = new BasicComboPopup(comboBox);
                    popup.setBorder(BorderFactory.createLineBorder(BORDER_STRONG));
                    popup.getList().setBackground(SURFACE_ALT);
                    return popup;
                }
            });
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof Enum) setText(pretty((Enum<?>) value));   // CARDIO -> Cardio
                    setFont(FONT_BODY);
                    setBorder(new EmptyBorder(8, 10, 8, 10));
                    if (index == -1) {                       // the closed combo's display
                        setOpaque(false);
                        setForeground(TEXT);
                        setBorder(new EmptyBorder(4, 4, 4, 4));
                    } else {
                        setOpaque(true);
                        setBackground(selected ? ACCENT_SOFT : SURFACE_ALT);
                        setForeground(selected ? ACCENT : TEXT);
                    }
                    return this;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(SURFACE_ALT);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS, RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static <T> JComboBox<T> comboBox(T[] items) { return new ComboBox<>(items); }

    // Read-only monospaced output area (results, logs).
    public static JTextArea outputArea() {
        return outputArea(null);
    }

    // Same, but shows a dim hint while it is empty (e.g. "Results will appear here").
    public static JTextArea outputArea(String hint) {
        JTextArea area = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (hint != null && getDocument().getLength() == 0) {
                    Graphics2D g2 = smooth(g);
                    g2.setFont(FONT_BODY);
                    g2.setColor(TEXT_DIM);
                    Insets in = getInsets();
                    g2.drawString(hint, in.left, in.top + g2.getFontMetrics().getAscent());
                    g2.dispose();
                }
            }
        };
        area.setEditable(false);
        area.setFont(FONT_MONO);
        area.setForeground(TEXT);
        area.setBackground(SURFACE_ALT);
        area.setCaretColor(ACCENT);
        area.setBorder(new EmptyBorder(12, 14, 12, 14));
        area.setLineWrap(false);
        return area;
    }

    // ================= Tables =================

    // Dark zebra-striped table with roomy rows, no grid lines, and a quiet header.
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT_SOFT);
        table.setSelectionForeground(ACCENT);
        table.setFillsViewportHeight(true);
        table.setBorder(null);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.setDefaultRenderer(Integer.class, new CellRenderer());
        table.setDefaultRenderer(Long.class, new CellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, value, s, f, r, c);
                setText(value == null ? "" : value.toString().toUpperCase());
                setFont(FONT_LABEL);
                setForeground(TEXT_MUTED);
                setBackground(SURFACE_ALT);
                setOpaque(true);
                setHorizontalAlignment(LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        new EmptyBorder(0, 14, 0, 14)));
                return this;
            }
        });
    }

    // A table that shows a centered hint (e.g. "Heap is empty") while it has no rows.
    public static JTable hintTable(javax.swing.table.TableModel model, String hint) {
        return new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = smooth(g);
                    g2.setFont(FONT_BODY);
                    g2.setColor(TEXT_DIM);
                    FontMetrics fm = g2.getFontMetrics();
                    int y = Math.max(fm.getAscent() + 16, getHeight() / 2);
                    g2.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2, y);
                    g2.dispose();
                }
            }
        };
    }

    // Default cell look: padding, zebra stripes, lime text when selected.
    public static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean f, int row, int col) {
            super.getTableCellRendererComponent(t, value, selected, false, row, col);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFont(FONT_BODY);
            if (selected) {
                setBackground(ACCENT_SOFT);
                setForeground(ACCENT);
            } else {
                setBackground(row % 2 == 0 ? SURFACE : new Color(0x181C21));
                setForeground(TEXT);
            }
            return this;
        }
    }

    // Condition column: a colored dot followed by the condition name.
    public static class ConditionRenderer extends CellRenderer {
        private Color dot = TEXT_MUTED;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean s, boolean f, int row, int col) {
            super.getTableCellRendererComponent(t, value, s, f, row, col);
            if (value instanceof model.Equipment.Condition) {
                model.Equipment.Condition c = (model.Equipment.Condition) value;
                dot = conditionColor(c);
                setText(c.name().charAt(0) + c.name().substring(1).toLowerCase());
                if (!s) setForeground(dot);
            }
            setBorder(new EmptyBorder(0, 32, 0, 14));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = smooth(g);
            g2.setColor(dot);
            g2.fillOval(14, getHeight() / 2 - 4, 8, 8);
            g2.dispose();
        }
    }

    // ================= Scrolling =================

    // Scroll pane with no border and slim, rounded dark scrollbars.
    public static JScrollPane scroll(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        sp.getViewport().setBackground(content.getBackground() != null ? content.getBackground() : SURFACE);
        sp.setBackground(SURFACE);
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        JPanel corner = new JPanel();
        corner.setBackground(SURFACE_ALT);
        sp.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner);
        return sp;
    }

    // Table inside a rounded, clipped frame (so the table corners follow the card radius).
    public static JComponent tableScroll(JTable table) {
        table.setPreferredScrollableViewportSize(new Dimension(300, 72));  // grows to fill; never forces height
        JScrollPane sp = scroll(table);
        sp.getViewport().setBackground(SURFACE);
        JPanel frame = new JPanel(new BorderLayout()) {
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, RADIUS, RADIUS));
                g2.dispose();
            }
        };
        frame.setOpaque(false);
        frame.setBorder(new EmptyBorder(1, 1, 1, 1));
        frame.add(sp);
        return frame;
    }

    // Wraps a whole page so it fills the window when there's room, and scrolls instead of
    // squashing or clipping its cards when the window is small.
    public static JScrollPane page(JComponent content) {
        JScrollPane sp = scroll(new PageHolder(content));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    static class PageHolder extends JPanel implements Scrollable {
        PageHolder(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    static class SlimScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = BORDER_STRONG;
            trackColor = SURFACE;
        }

        @Override
        protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override
        protected JButton createIncreaseButton(int o) { return zeroButton(); }

        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() ? new Color(0x4A5462) : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }
}
