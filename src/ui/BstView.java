
// BstView.java
// Part of: John Benedict Jesalva  (Binary Search Tree)
// Presented in: Slides 5-8 (Arkin Ipac) - live demo of the range query and traversals.
// What it does: Draws the top levels of the usage-count BST and highlights the search
//               path of a range query, or the visit order of a traversal.
// Used by: ReportsPanel (BST tab)


package ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.*;
import structures.CustomBST;
import structures.CustomLinkedList;

// The real tree has 2,868 nodes and is 26 levels deep, which can't fit on screen.
// So we draw only the top LEVELS levels (up to 31 nodes) -- the part every search
// starts with. A short line under a bottom node means "the tree continues below".
//
// The keys come from CustomBST.topLevels(), which stores them like a heap array:
// root at 0, children of i at 2i+1 and 2i+2, and -1 where there is no node.
//
// Two things can be highlighted:
//   Range query -> the search path from the root toward the min key (lime line), and
//                  any drawn node whose key is inside the range (filled lime)
//   Traversal   -> a small number on each drawn node: the order the traversal visits it.
//                  Removing the lower levels doesn't change the order of the nodes that
//                  are left, so these numbers match the real traversal's order.
public class BstView extends JComponent {

    private static final int LEVELS = 5;
    private static final int NODE = 38;

    private final CustomBST tree;

    private CustomLinkedList<Integer> path = new CustomLinkedList<>();   // range query path
    private int lo = -1, hi = -1;                                        // range, -1 = none
    private int traversal = -1;                                          // 0 in, 1 pre, 2 post, -1 none

    public BstView(CustomBST tree) {
        this.tree = tree;
        setPreferredSize(new Dimension(600, 380));
        setOpaque(false);
    }

    public void showRange(int lo, int hi) {
        this.path = tree.searchPath(lo);
        this.lo = lo;
        this.hi = hi;
        this.traversal = -1;
        repaint();
    }

    public void showTraversal(int which) {
        this.traversal = which;
        this.path = new CustomLinkedList<>();
        this.lo = this.hi = -1;
        repaint();
    }

    // Center of slot i (heap-array index) in pixels.
    private Point slot(int i) {
        int level = 31 - Integer.numberOfLeadingZeros(i + 1);    // 0 for the root, 1 for its children, ...
        int indexInLevel = i + 1 - (1 << level);
        int perLevel = 1 << level;
        int top = 30, bottom = 60;
        double rowH = (getHeight() - top - bottom) / (double) (LEVELS - 1);
        int x = (int) ((indexInLevel + 0.5) / perLevel * getWidth());
        return new Point(x, top + (int) (level * rowH));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        if (tree.isEmpty()) {
            g2.setFont(Theme.FONT_BODY);
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString("The tree is empty.", 20, 30);
            g2.dispose();
            return;
        }

        // One extra level so we know which bottom nodes have children further down.
        int[] keys = tree.topLevels(LEVELS + 1);
        int drawn = (1 << LEVELS) - 1;

        // Which drawn slots are on the search path? Walk down the same way the search does.
        boolean[] onPath = new boolean[drawn];
        int i = 0, steps = 0;
        for (int key : path) {
            if (i >= drawn || keys[i] != key) break;
            onPath[i] = true;
            steps++;
            i = (lo < key) ? 2 * i + 1 : 2 * i + 2;
        }
        int hiddenSteps = path.size() - steps;      // part of the path below the drawing

        // Visit order numbers for a traversal of the drawn part.
        int[] order = new int[drawn];
        if (traversal >= 0) number(keys, 0, drawn, order, new int[]{0});

        // 1. Lines from each node to its children (lime if both ends are on the path).
        for (int s = 0; s < drawn; s++) {
            if (keys[s] < 0) continue;
            for (int c = 2 * s + 1; c <= 2 * s + 2; c++) {
                if (c >= keys.length || keys[c] < 0) continue;
                Point p = slot(s);
                if (c < drawn) {
                    boolean lit = onPath[s] && onPath[c];
                    g2.setStroke(new BasicStroke(lit ? 4f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(lit ? Theme.ACCENT : Theme.BORDER_STRONG);
                    Point q = slot(c);
                    g2.draw(new Line2D.Float(p.x, p.y, q.x, q.y));
                } else {
                    // child exists but isn't drawn: a short stub pointing down-left or down-right
                    int dx = (c == 2 * s + 1) ? -8 : 8;
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.setColor(Theme.BORDER_STRONG);
                    g2.draw(new Line2D.Float(p.x, p.y + NODE / 2f, p.x + dx, p.y + NODE / 2f + 12));
                }
            }
        }

        // 2. The nodes.
        for (int s = 0; s < drawn; s++) {
            if (keys[s] < 0) continue;
            Point c = slot(s);
            boolean inRange = lo >= 0 && keys[s] >= lo && keys[s] <= hi;
            Ellipse2D circle = new Ellipse2D.Float(c.x - NODE / 2f, c.y - NODE / 2f, NODE, NODE);
            g2.setColor(inRange ? Theme.ACCENT : onPath[s] ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT);
            g2.fill(circle);
            g2.setStroke(new BasicStroke(onPath[s] || inRange ? 2.2f : 1.4f));
            g2.setColor(onPath[s] || inRange ? Theme.ACCENT : Theme.BORDER_STRONG);
            g2.draw(circle);

            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(inRange ? Theme.ON_ACCENT : onPath[s] ? Theme.ACCENT : Theme.TEXT);
            String k = String.valueOf(keys[s]);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(k, c.x - fm.stringWidth(k) / 2, c.y + fm.getAscent() / 2 - 1);

            if (order[s] > 0) badge(g2, String.valueOf(order[s]), c.x + NODE / 2 - 4, c.y - NODE / 2 - 2);
        }

        // 3. "The path keeps going" note under the last drawn path node.
        if (hiddenSteps > 0 && steps > 0) {
            int last = 0;
            for (int s = 0; s < drawn; s++) if (onPath[s]) last = s;
            Point c = slot(last);
            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(Theme.ACCENT);
            String t = "↓ " + hiddenSteps + " more level" + (hiddenSteps == 1 ? "" : "s");
            FontMetrics fm = g2.getFontMetrics();
            int x = Math.max(4, Math.min(c.x - fm.stringWidth(t) / 2, getWidth() - fm.stringWidth(t) - 4));
            g2.drawString(t, x, c.y + NODE / 2 + fm.getAscent() + 14);
        }

        // 4. Caption at the bottom.
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        String caption = "Top " + LEVELS + " of " + tree.height() + " levels  ·  key = usage count";
        if (traversal >= 0) caption += "  ·  numbers = " + new String[]{"inorder", "preorder", "postorder"}[traversal] + " visit order";
        if (lo >= 0) caption += "  ·  lime path = search for " + lo + "  ·  filled = inside " + lo + "–" + hi;
        g2.drawString(caption, 4, getHeight() - 8);
        g2.dispose();
    }

    // Gives each drawn slot its visit number for the chosen traversal (recursion on the
    // heap-array layout, same shape as CustomBST's own inorder / preorder / postorder).
    private void number(int[] keys, int i, int drawn, int[] order, int[] counter) {
        if (i >= drawn || keys[i] < 0) return;
        if (traversal == 1) order[i] = ++counter[0];             // preorder: node first
        number(keys, 2 * i + 1, drawn, order, counter);
        if (traversal == 0) order[i] = ++counter[0];             // inorder: node in the middle
        number(keys, 2 * i + 2, drawn, order, counter);
        if (traversal == 2) order[i] = ++counter[0];             // postorder: node last
    }

    private static void badge(Graphics2D g2, String text, int cx, int cy) {
        g2.setFont(Theme.FONT_SMALL);
        FontMetrics fm = g2.getFontMetrics();
        int w = Math.max(18, fm.stringWidth(text) + 8), h = 16;
        g2.setColor(Theme.ACCENT);
        g2.fillRoundRect(cx - w / 2, cy - h / 2, w, h, h, h);
        g2.setColor(Theme.ON_ACCENT);
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2 - 2);
    }
}
