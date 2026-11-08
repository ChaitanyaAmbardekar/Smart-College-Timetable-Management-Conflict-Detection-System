package in.frcce.ecs;

import in.frcce.ecs.ui.LoginFrame;
import in.frcce.ecs.util.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point.
 * Initialises Look & Feel (FlatLaf if present, otherwise built-in Nimbus),
 * verifies DB connectivity, then shows the login screen.
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Set up FlatLaf if on classpath; otherwise use Nimbus
        boolean lafSet = false;
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Method setupMethod = flatLafClass.getMethod("setup");
            setupMethod.invoke(null);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            lafSet = true;
            LOG.info("FlatLaf Look & Feel applied successfully.");
        } catch (Throwable t) {
            LOG.info("FlatLaf not on classpath, falling back to built-in Nimbus L&F.");
        }

        if (!lafSet) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Nimbus not available, using system default", ex);
            }
        }

        // Verify DB on a background thread so the splash appears first
        SwingUtilities.invokeLater(() -> {
            // Show splash / login
            if (!DBConnection.getInstance().testConnection()) {
                JOptionPane.showMessageDialog(null,
                        "<html><b>Cannot connect to the database.</b><br>" +
                        "Please check your <code>db.properties</code> file and ensure MySQL is running.<br>" +
                        "Expected URL: " + System.getProperty("db.url", "(see db.properties)") + "</html>",
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });

        // Shutdown hook to close DB pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DBConnection.getInstance().shutdown();
            LOG.info("Application shutdown complete.");
        }));
    }
}

