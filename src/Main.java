// Main.java
// Part of: Arkin Ipac  (Integration)
// What it does: The start button of the whole system. Run this class and it:
//                 1. loads the 5,000 machines (MySQL first, the CSV if MySQL is off)
//                 2. starts the web server (web.WebServer)
//                 3. opens http://localhost:8080 in your browser
// The old console demo of every feature is now ConsoleDemo.java.

import java.awt.Desktop;
import java.net.BindException;
import java.net.URI;
import web.WebServer;

public class Main {

    public static void main(String[] args) throws Exception {
        // On Azure the PORT environment variable is set for us; on your PC it's 8080.
        String env = System.getenv("PORT");
        boolean onServer = env != null && !env.isEmpty();
        int port = onServer ? Integer.parseInt(env) : 8080;

        try {
            new WebServer().start(port);
        } catch (BindException e) {
            System.out.println("Port " + port + " is already in use. The app is probably still running:");
            System.out.println("stop the old run in VS Code (red square), then press Run again.");
            return;
        }

        String url = "http://localhost:" + port;
        System.out.println("Open " + url + "   (press the red square in VS Code to stop)");

        // Open the browser automatically, but only on a desktop (not on a server).
        if (!onServer && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                System.out.println("Couldn't open the browser by itself. Copy the link above into it.");
            }
        }
    }
}
