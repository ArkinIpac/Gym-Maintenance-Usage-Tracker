import java.awt.Desktop;
import java.net.BindException;
import java.net.URI;
import web.WebServer;

// Run this to start Lime Gym: loads the data, starts the web server and opens the browser.
public class Main {

    public static void main(String[] args) throws Exception {
        // Azure sets PORT for us; on a PC we use 8080.
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

        // Only open a browser on a desktop, never on a server.
        if (!onServer && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                System.out.println("Couldn't open the browser by itself. Copy the link above into it.");
            }
        }
    }
}
