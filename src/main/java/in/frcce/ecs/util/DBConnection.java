package in.frcce.ecs.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight JDBC connection pool.
 * Reads credentials from db.properties on the classpath.
 * Thread-safe singleton.
 */
public final class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());
    private static volatile DBConnection instance;

    private final String url;
    private final String username;
    private final String password;
    private final int    poolMax;

    private final BlockingQueue<Connection> pool;

    // -------------------------------------------------------------------------
    private DBConnection() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is == null) throw new RuntimeException("db.properties not found on classpath");
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load db.properties", e);
        }

        this.url      = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.poolMax  = Integer.parseInt(props.getProperty("db.pool.max", "10"));
        this.pool     = new ArrayBlockingQueue<>(poolMax);

        // Auto-detect and load JDBC driver based on URL
        try {
            if (url != null && url.startsWith("jdbc:postgresql:")) {
                Class.forName("org.postgresql.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            LOG.log(Level.INFO, "Driver class note: " + e.getMessage());
        }

        // Pre-create minimum connections
        int minCon = Integer.parseInt(props.getProperty("db.pool.min", "2"));
        for (int i = 0; i < minCon; i++) {
            try {
                pool.offer(createConnection());
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Could not pre-create connection #" + i, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) instance = new DBConnection();
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // -------------------------------------------------------------------------
    /**
     * Borrow a connection from the pool (or create a new one if pool is empty).
     * Caller MUST call {@link #release(Connection)} when finished.
     */
    public Connection getConnection() throws SQLException {
        Connection con = pool.poll();
        if (con == null || con.isClosed() || !con.isValid(2)) {
            con = createConnection();
        }
        return con;
    }

    // -------------------------------------------------------------------------
    /**
     * Return a connection to the pool.
     * If the pool is full or the connection is broken, it is closed instead.
     */
    public void release(Connection con) {
        if (con == null) return;
        try {
            if (con.isClosed() || !con.isValid(1)) {
                // discard broken connection
                return;
            }
            if (!con.getAutoCommit()) {
                con.rollback();
                con.setAutoCommit(true);
            }
            if (!pool.offer(con)) {
                con.close(); // pool is full
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Error returning connection to pool", e);
        }
    }

    // -------------------------------------------------------------------------
    /** Test that we can reach the database. Used on startup. */
    public boolean testConnection() {
        try (Connection con = getConnection()) {
            return con.isValid(3);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    /** Drain and close all pooled connections (call on application shutdown). */
    public void shutdown() {
        Connection con;
        while ((con = pool.poll()) != null) {
            try { con.close(); } catch (SQLException ignored) {}
        }
    }
}

