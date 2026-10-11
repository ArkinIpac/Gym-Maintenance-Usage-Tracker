
// HashView.java
// Part of: Kahlil Mendoza  (Hash Table)
// Presented in: Slides 5-8 (Arkin Ipac) - live demo of the bucket lookup and the collision demo.
// What it does: Draws hash table buckets as boxes, each with its chain of keys
//               (separate chaining), and highlights the bucket / key being looked up.
// Used by: ReportsPanel (Hash table tab)


package ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// One row per bucket:   [ 1,234 ] -> EQ-00001 -> EQ-01834 -> ...
// The left box is the bucket (array slot); the boxes after it are the linked-list chain
// of keys that hashed to that slot. More than one key in a row = a collision.
//
// The full table has thousands of buckets, so for a lookup we draw only the target bucket
// and a few neighbors around it. The collision demo table has 7 buckets, so all are drawn.
// Only bucketIndexOf() / keysInBucket() / getCapacity() are used: read-only.
public class HashView extends JComponent {

    private static final int ROW_H = 44, BOX_H = 30, BUCKET_W = 92, KEY_W = 104, GAP = 26;

    private CustomHashTable<String, ?> table;
    private int first, count, target = -1;
    private String key, caption;

    public HashView() {
        setPreferredSize(new Dimension(600, 380));
        setOpaque(false);
    }

    // Draws buckets [first, first + count) and highlights `target` bucket and `key`.
    public void show(CustomHashTable<String, ?> table, int first, int count, int target, String key, String caption) {
        this.table = table;
        this.first = Math.max(0, first);
        this.count = Math.min(count, table.getCapacity() - this.first);
        this.target = target;
        this.key = key;
        this.caption = caption;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (table == null) {
            g2.setFont(Theme.FONT_BODY);
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString("Click Locate bucket or the collision demo to draw the buckets here.", 4, 24);
            g2.dispose();
            return;
        }

        int top = Math.max(10, (getHeight() - 30 - count * ROW_H) / 2);
        for (int r = 0; r < count; r++) {
            int b = first + r;
            int y = top + r * ROW_H;
            boolean hit = b == target;

            // bucket box
            box(g2, 4, y, BUCKET_W, String.format("%,d", b), hit ? Theme.ACCENT : Theme.SURFACE_ALT,
                    hit ? Theme.ACCENT : Theme.BORDER_STRONG, hit ? Theme.ON_ACCENT : Theme.TEXT_MUTED);

            // chain: bucket -> key -> key -> ...
            CustomLinkedList<String> chain = table.keysInBucket(b);
            int x = 4 + BUCKET_W;
            if (chain.isEmpty()) {
                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.TEXT_DIM);
                g2.drawString("empty", x + 14, y + BOX_H / 2 + 5);
                continue;
            }
            int shown = 0;
            boolean walking = hit;          // lookup walks the chain until it reaches the key
            for (String k : chain) {
                if (x + GAP + KEY_W > getWidth() - 4) {                 // no room left: "+3 more"
                    g2.setFont(Theme.FONT_SMALL);
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.drawString("+" + (chain.size() - shown) + " more", x + 10, y + BOX_H / 2 + 5);
                    break;
                }
                arrow(g2, x, x + GAP, y + BOX_H / 2, walking);
                x += GAP;
                boolean found = hit && k.equals(key);
                box(g2, x, y, KEY_W, k, found ? Theme.ACCENT_SOFT : Theme.SURFACE,
                        found ? Theme.ACCENT : hit ? Theme.TEXT_MUTED : Theme.BORDER_STRONG,
                        found ? Theme.ACCENT : Theme.TEXT);
                x += KEY_W;
                shown++;
                if (found) walking = false;
            }
        }

        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        if (caption != null) g2.drawString(caption, 4, getHeight() - 8);
        g2.dispose();
    }

    private static void box(Graphics2D g2, int x, int y, int w, String text, Color fill, Color line, Color ink) {
        Shape s = new RoundRectangle2D.Float(x, y, w, BOX_H, 8, 8);
        g2.setColor(fill);
        g2.fill(s);
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(line);
        g2.draw(s);
        g2.setFont(Theme.FONT_LABEL);
        g2.setColor(ink);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + BOX_H / 2 + fm.getAscent() / 2 - 1);
    }

    private static void arrow(Graphics2D g2, int x1, int x2, int y, boolean lit) {
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(lit ? Theme.ACCENT : Theme.BORDER_STRONG);
        g2.drawLine(x1 + 4, y, x2 - 5, y);
        g2.fillPolygon(new int[]{x2 - 4, x2 - 10, x2 - 10}, new int[]{y, y - 4, y + 4}, 3);
    }
}
