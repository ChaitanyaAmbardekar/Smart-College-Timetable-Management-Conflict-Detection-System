package in.frcce.ecs.dao;

import in.frcce.ecs.model.Room;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code room} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class RoomDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_ALL =
            "SELECT room_id, name, type, capacity, location, is_active " +
            "FROM   room " +
            "ORDER  BY name";

    private static final String SQL_FIND_BY_TYPE =
            "SELECT room_id, name, type, capacity, location, is_active " +
            "FROM   room " +
            "WHERE  type = ? " +
            "ORDER  BY name";

    private static final String SQL_FIND_BY_ID =
            "SELECT room_id, name, type, capacity, location, is_active " +
            "FROM   room " +
            "WHERE  room_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO room (name, type, capacity, location, is_active) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE room " +
            "SET    name = ?, type = ?, capacity = ?, location = ?, is_active = ? " +
            "WHERE  room_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM room WHERE room_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns all rooms ordered by name. */
    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
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
            throw new RuntimeException("RoomDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /**
     * Returns all rooms of the given type (e.g., {@code "CLASSROOM"}, {@code "LAB"}).
     */
    public List<Room> findByType(String type) {
        List<Room> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_TYPE)) {
                ps.setString(1, type);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("RoomDAO.findByType failed for type=" + type, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Looks up a single room by primary key. */
    public Optional<Room> findById(int roomId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("RoomDAO.findById failed for id=" + roomId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Inserts a new room row.
     *
     * @return the generated {@code room_id}, or -1 if no key was returned.
     */
    public int insert(Room r) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, r.getName());
                ps.setString(2, r.getType());
                ps.setInt(3, r.getCapacity());
                ps.setString(4, r.getLocation());   // may be null
                ps.setBoolean(5, r.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("RoomDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Updates every editable column for the given room.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(Room r) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, r.getName());
                ps.setString(2, r.getType());
                ps.setInt(3, r.getCapacity());
                ps.setString(4, r.getLocation());
                ps.setBoolean(5, r.isActive());
                ps.setInt(6, r.getRoomId());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("RoomDAO.update failed for id=" + r.getRoomId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a room row.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int roomId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, roomId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("RoomDAO.delete failed for id=" + roomId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("room_id"));
        r.setName(rs.getString("name"));
        r.setType(rs.getString("type"));
        r.setCapacity(rs.getInt("capacity"));
        r.setLocation(rs.getString("location"));
        r.setActive(rs.getBoolean("is_active"));
        return r;
    }
}
