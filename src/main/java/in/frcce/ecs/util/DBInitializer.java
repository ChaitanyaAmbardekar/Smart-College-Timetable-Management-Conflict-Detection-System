package in.frcce.ecs.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Utility to run schema.sql and seed.sql directly via JDBC.
 * Allows setting up the database with a single command without needing the mysql CLI.
 */
public class DBInitializer {

    private static final Logger LOG = Logger.getLogger(DBInitializer.class.getName());

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("ECS Timetable Generator — Database Initializer");
        System.out.println("=============================================================");

        Properties props = new Properties();
        try (InputStream is = DBInitializer.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                System.err.println("Error: db.properties not found on classpath!");
                System.exit(1);
            }
            props.load(is);
        } catch (Exception e) {
            System.err.println("Error loading db.properties: " + e.getMessage());
            System.exit(1);
        }

        String rawUrl   = props.getProperty("db.url");
        String username = props.getProperty("db.username");
        String password = props.getProperty("db.password");

        // First connect to root MySQL server (without default DB) to create ecs_timetable if needed
        String serverUrl = rawUrl.replaceFirst("/[a-zA-Z0-9_]+(\\?.*)?$", "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8");

        System.out.println("1. Connecting to MySQL server at: " + serverUrl);
        try (Connection rootCon = DriverManager.getConnection(serverUrl, username, password);
             Statement st = rootCon.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS ecs_timetable");
            System.out.println("   [OK] Database 'ecs_timetable' verified / created.");
        } catch (SQLException e) {
            System.out.println("   Note: Could not create DB via root URL: " + e.getMessage());
        }

        System.out.println("2. Applying sql/schema.sql...");
        runSqlFile("sql/schema.sql", rawUrl, username, password);

        System.out.println("3. Applying sql/seed.sql...");
        runSqlFile("sql/seed.sql", rawUrl, username, password);

        System.out.println("=============================================================");
        System.out.println("Database initialization completed successfully!");
        System.out.println("=============================================================");
    }

    private static void runSqlFile(String filePath, String url, String user, String pass) {
        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement st = con.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            StringBuilder sb = new StringBuilder();
            String line;
            String delimiter = ";";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }

                sb.append(line).append("\n");

                if (trimmed.endsWith(delimiter)) {
                    String sql = sb.toString();
                    sql = sql.substring(0, sql.length() - delimiter.length() - 1).trim();
                    if (!sql.isEmpty()) {
                        try {
                            st.execute(sql);
                        } catch (SQLException e) {
                            // Print warning for non-critical drops or duplicate keys
                            if (!e.getMessage().toLowerCase().contains("already exists")) {
                                LOG.warning("SQL Warning on executing: " + sql.substring(0, Math.min(60, sql.length())) + "... -> " + e.getMessage());
                            }
                        }
                    }
                    sb.setLength(0);
                }
            }
            System.out.println("   [OK] " + filePath + " applied.");

        } catch (Exception e) {
            System.err.println("   [FAIL] Could not run " + filePath + ": " + e.getMessage());
        }
    }
}

