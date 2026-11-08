package in.frcce.ecs.dao;

import in.frcce.ecs.model.User;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code users} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class UserDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_BY_USERNAME =
            "SELECT username, password_hash, role, faculty_id, is_active, last_login " +
            "FROM   users " +
            "WHERE  username = ?";

    private static final String SQL_INSERT =
            "INSERT INTO users (username, password_hash, role, faculty_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_LAST_LOGIN =
            "UPDATE users SET last_login = NOW() WHERE username = ?";

    private static final String SQL_FIND_ALL =
            "SELECT username, password_hash, role, faculty_id, is_active, last_login " +
            "FROM   users " +
            "ORDER  BY username";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Looks up a user by their username (case-sensitive per MySQL utf8mb4 collation).
     *
     * @return an {@link Optional} containing the user if found, otherwise empty.
     */
    public Optional<User> findByUsername(String username) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_USERNAME)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "UserDAO.findByUsername failed for username=" + username, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Inserts a new user record.
     *
     * @return {@code true} if exactly one row was inserted.
     */
    public boolean insert(User u) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getPasswordHash());
                ps.setString(3, u.getRole());
                // faculty_id is nullable (admin accounts may have no linked faculty)
                if (u.getFacultyId() > 0) {
                    ps.setInt(4, u.getFacultyId());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setBoolean(5, u.isActive());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Stamps the current date-time into {@code last_login} for the given user.
     *
     * @return {@code true} if the row was found and updated.
     */
    public boolean updateLastLogin(String username) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {
                ps.setString(1, username);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "UserDAO.updateLastLogin failed for username=" + username, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /** Returns all users ordered by username. */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setFacultyId(rs.getInt("faculty_id"));        // 0 when NULL
        u.setActive(rs.getBoolean("is_active"));
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            u.setLastLogin(lastLogin.toLocalDateTime());
        }
        return u;
    }
}
