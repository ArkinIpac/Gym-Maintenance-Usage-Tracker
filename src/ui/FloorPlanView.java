
// FloorPlanView.java
// Part of: Arkin Ipac  (BFS / DFS)
// Presented in: Slides 9-10 (Arkin Ipac) - shows the BFS / DFS route on the floor plan.
// What it does: Draws the 16-zone floor plan graph (zones = circles, walkways = lines)
//               and highlights the route found by BFS or DFS.
// Used by: ReportsPanel (Graph tab)


package ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.*;
import structures.CustomGraph;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// A picture of the graph. Nothing here changes the graph: it only READS the zones and
// their neighbors from CustomGraph, and draws them.
//
// Where each zone goes on screen is fixed by hand (like a real floor plan) in POSITIONS:
// cardio areas on top, strength in the middle, functional / studio areas at the bottom.
// The numbers are on a 10 x 7 grid and get scaled to the size of the panel.
//
// The route is a linked list of zone names, e.g. Entrance -> Strength Floor -> ...
// Each step gets a numbered circle so you can follow it.
//   BFS route -> solid lime line, filled circles
//   DFS route -> dashed lime line, outlined circles   (so the two are easy to tell apart)
public class FloorPlanView extends JComponent {

    private static final Object[][] POSITIONS = {
        // zone,                       x,    y
        {"Entrance",                   0.0,  3.0},
        {"Cardio Zone A",              2.0,  0.5},
        {"Cardio Zone B",              4.5,  0.5},
        {"Cardio Mezzanine",           7.0,  0.5},
        {"Rooftop Deck",               9.5,  0.5},
        {"Strength Floor",             2.0,  2.6},
        {"Machine Row 1",              4.5,  2.0},
        {"Machine Row 2",              7.0,  2.2},
        {"Free Weight Area",           4.5,  3.7},
        {"Squat Rack Bay",             7.0,  3.8},
        {"Olympic Lifting Platform",   8.6,  4.7},
        {"Stretching Area",            2.0,  5.0},
        {"Functional Training Zone",   4.5,  5.2},
        {"Turf Area",                  7.0,  5.8},
        {"Group Class Studio",         4.5,  6.7},
        {"Storage Room",              10.0,  6.0},
    };
    private static final double GRID_W = 10.0, GRID_H = 6.7;
    private static final int NODE = 14;            // circle diameter
    private static final int ROUTE_NODE = 22;      // bigger circle for zones on the route

    private final CustomGraph graph;
    private final CustomHashTable<String, double[]> spots = new CustomHashTable<>();

    private CustomLinkedList<String> route = new CustomLinkedList<>();
    private boolean dashed;
    private String from, to;

    public FloorPlanView(CustomGraph graph) {
        this.graph = graph;
        for (Object[] p : POSITIONS)
            spots.put((String) p[0], new double[]{(Double) p[1], (Double) p[2]});
        setPreferredSize(new Dimension(600, 380));
        setOpaque(false);
    }

    // The two zones picked in the dropdowns (drawn with an outline before any route is found).
    public void setEndpoints(String from, String to) {
        this.from = from;
        this.to = to;
        route = new CustomLinkedList<>();   // a new pair means the old route no longer applies
        repaint();
    }

    // dashed = false for BFS, true for DFS
    public void showRoute(CustomLinkedList<String> route, boolean dashed) {
        this.route = route;
        this.dashed = dashed;
        repaint();
    }

    // Grid position -> pixel position, keeping a margin so the labels fit.
    private Point toScreen(String zone) {
        double[] p = spots.get(zone);
        if (p == null) p = new double[]{GRID_W / 2, GRID_H / 2};   // a zone added later without a position
        int padX = 90, padTop = 22, padBottom = 34;
        int w = getWidth() - padX * 2, h = getHeight() - padTop - padBottom;
        return new Point(padX + (int) (p[0] / GRID_W * w), padTop + (int) (p[1] / GRID_H * h));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);

        // 1. All walkways, thin and grey. Each edge is stored twice (A->B and B->A),
        //    so we only draw it when A comes before B alphabetically.
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(Theme.BORDER_STRONG);
        for (String a : graph.getZones()) {
            for (String b : graph.getNeighbors(a)) {
                if (a.compareTo(b) < 0) g2.draw(line(a, b));
            }
        }

        // 2. The route on top: thick colored lines between consecutive zones.
        g2.setStroke(dashed
                ? new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{9f, 7f}, 0f)
                : new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.ACCENT);
        String prev = null;
        for (String z : route) {
            if (prev != null) g2.draw(line(prev, z));
            prev = z;
        }

        // 3. The zones. Zones on the route get a filled circle with their step number.
        for (String z : graph.getZones()) {
            int step = route.indexOf(z);          // -1 if not on the route
            Point c = toScreen(z);
            if (step >= 0) {
                g2.setColor(dashed ? Theme.ACCENT_SOFT : Theme.ACCENT);
                g2.fill(circle(c, ROUTE_NODE));
                if (dashed) {
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.setColor(Theme.ACCENT);
                    g2.draw(circle(c, ROUTE_NODE));
                }
                g2.setColor(dashed ? Theme.ACCENT : Theme.ON_ACCENT);
                g2.setFont(Theme.FONT_LABEL);
                String n = String.valueOf(step);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(n, c.x - fm.stringWidth(n) / 2, c.y + fm.getAscent() / 2 - 1);
            } else {
                boolean picked = z.equals(from) || z.equals(to);
                g2.setColor(Theme.SURFACE_ALT);
                g2.fill(circle(c, picked ? ROUTE_NODE : NODE));
                g2.setStroke(new BasicStroke(picked ? 2.2f : 1.6f));
                g2.setColor(picked ? Theme.ACCENT : Theme.TEXT_MUTED);
                g2.draw(circle(c, picked ? ROUTE_NODE : NODE));
            }
            label(g2, z, c, step >= 0 || z.equals(from) || z.equals(to));
        }
        g2.dispose();
    }

    private void label(Graphics2D g2, String zone, Point c, boolean strong) {
        g2.setFont(strong ? Theme.FONT_LABEL : Theme.FONT_SMALL);
        g2.setColor(strong ? Theme.TEXT : Theme.TEXT_MUTED);
        FontMetrics fm = g2.getFontMetrics();
        int x = c.x - fm.stringWidth(zone) / 2, y = c.y + ROUTE_NODE / 2 + fm.getAscent() + 2;
        // small dark box behind the text so walkway lines don't run through the label
        Color text = g2.getColor();
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(x - 4, y - fm.getAscent(), fm.stringWidth(zone) + 8, fm.getHeight(), 6, 6);
        g2.setColor(text);
        g2.drawString(zone, x, y);
    }

    private Line2D line(String a, String b) {
        Point p = toScreen(a), q = toScreen(b);
        return new Line2D.Float(p.x, p.y, q.x, q.y);
    }

    private static Ellipse2D circle(Point c, int d) {
        return new Ellipse2D.Float(c.x - d / 2f, c.y - d / 2f, d, d);
    }
}
