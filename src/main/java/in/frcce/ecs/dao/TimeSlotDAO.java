package in.frcce.ecs.dao;

import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code time_slot} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class TimeSlotDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_ALL =
            "SELECT slot_id, day, start_time, end_time, slot_no, is_break " +
            "FROM   time_slot " +
            "WHERE  is_break = FALSE " +
            "ORDER  BY CASE day WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 ELSE 7 END, slot_no";

    private static final String SQL_FIND_ALL_INC_BREAKS =
            "SELECT slot_id, day, start_time, end_time, slot_no, is_break " +
            "FROM   time_slot " +
            "ORDER  BY CASE day WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 ELSE 7 END, slot_no";

    private static final String SQL_FIND_BY_DAY =
            "SELECT slot_id, day, start_time, end_time, slot_no, is_break " +
            "FROM   time_slot " +
            "WHERE  day = ? " +
            "ORDER  BY slot_no";

    private static final String SQL_INSERT =
            "INSERT INTO time_slot (day, start_time, end_time, slot_no, is_break) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE time_slot " +
            "SET    day = ?, start_time = ?, end_time = ?, slot_no = ?, is_break = ? " +
            "WHERE  slot_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM time_slot WHERE slot_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all non-break time slots ordered by day then slot number.
     * Break rows are excluded; use {@link #findAllIncBreaks()} to include them.
     */
    public List<TimeSlot> findAll() {
        return fetchList(SQL_FIND_ALL, ps -> { /* no params */ });
    }

    /** Returns all time slots including break periods. */
    public List<TimeSlot> findAllIncBreaks() {
        return fetchList(SQL_FIND_ALL_INC_BREAKS, ps -> { /* no params */ });
    }

    /** Returns all time slots (including breaks) for the given day, ordered by slot number. */
    public List<TimeSlot> findByDay(String day) {
        return fetchList(SQL_FIND_BY_DAY, ps -> ps.setString(1, day));
    }

    /**
     * Inserts a new time-slot row.
     *
     * @return the generated {@code slot_id}, or -1 if no key was returned.
     */
    public int insert(TimeSlot ts) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, ts.getDay());
                ps.setString(2, ts.getStartTime());
                ps.setString(3, ts.getEndTime());
                ps.setInt(4, ts.getSlotNo());
                ps.setBoolean(5, ts.isBreak());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("TimeSlotDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Updates every editable column for the given time slot.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(TimeSlot ts) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, ts.getDay());
                ps.setString(2, ts.getStartTime());
                ps.setString(3, ts.getEndTime());
                ps.setInt(4, ts.getSlotNo());
                ps.setBoolean(5, ts.isBreak());
                ps.setInt(6, ts.getSlotId());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "TimeSlotDAO.update failed for id=" + ts.getSlotId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a time-slot row.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int slotId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, slotId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("TimeSlotDAO.delete failed for id=" + slotId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<TimeSlot> fetchList(String sql, ParamSetter setter) {
        List<TimeSlot> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("TimeSlotDAO query failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    private TimeSlot mapRow(ResultSet rs) throws SQLException {
        TimeSlot ts = new TimeSlot();
        ts.setSlotId(rs.getInt("slot_id"));
        ts.setDay(rs.getString("day"));
        ts.setStartTime(rs.getString("start_time"));
        ts.setEndTime(rs.getString("end_time"));
        ts.setSlotNo(rs.getInt("slot_no"));
        ts.setBreak(rs.getBoolean("is_break"));
        return ts;
    }
}
