package in.frcce.ecs.dao;

import in.frcce.ecs.model.FacultyUnavailability;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code faculty_unavailability} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class FacultyUnavailabilityDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_BY_FACULTY =
            "SELECT id, faculty_id, slot_id, reason " +
            "FROM   faculty_unavailability " +
            "WHERE  faculty_id = ? " +
            "ORDER  BY slot_id";

    private static final String SQL_FIND_ALL =
            "SELECT id, faculty_id, slot_id, reason " +
            "FROM   faculty_unavailability " +
            "ORDER  BY faculty_id, slot_id";

    private static final String SQL_INSERT =
            "INSERT INTO faculty_unavailability (faculty_id, slot_id, reason) " +
            "VALUES (?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM faculty_unavailability WHERE id = ?";

    /**
     * Returns {@code 0} rows when the faculty member is marked unavailable for
     * the given slot, so we COUNT and compare to verify availability.
     */
    private static final String SQL_IS_AVAILABLE =
            "SELECT COUNT(*) " +
            "FROM   faculty_unavailability " +
            "WHERE  faculty_id = ? " +
            "AND    slot_id    = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns all unavailability records for the given faculty, ordered by slot id. */
    public List<FacultyUnavailability> findByFaculty(int facultyId) {
        List<FacultyUnavailability> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_FACULTY)) {
                ps.setInt(1, facultyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "FacultyUnavailabilityDAO.findByFaculty failed for facultyId=" + facultyId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Returns every unavailability record in the table, ordered by faculty then slot. */
    public List<FacultyUnavailability> findAll() {
        List<FacultyUnavailability> list = new ArrayList<>();
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
            throw new RuntimeException("FacultyUnavailabilityDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /**
     * Inserts a new unavailability record.
     *
     * @return the generated {@code id}, or -1 if no key was returned.
     */
    public int insert(FacultyUnavailability fu) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, fu.getFacultyId());
                ps.setInt(2, fu.getSlotId());
                ps.setString(3, fu.getReason());     // nullable
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("FacultyUnavailabilityDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes an unavailability record by its surrogate key.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int id) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "FacultyUnavailabilityDAO.delete failed for id=" + id, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Checks whether a faculty member has NOT been marked unavailable for the given slot.
     *
     * @return {@code true} when no unavailability record exists for this (faculty, slot) pair,
     *         meaning the faculty member <em>is</em> available.
     */
    public boolean isFacultyAvailable(int facultyId, int slotId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_IS_AVAILABLE)) {
                ps.setInt(1, facultyId);
                ps.setInt(2, slotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) == 0;   // 0 records → available
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "FacultyUnavailabilityDAO.isFacultyAvailable failed " +
                    "for facultyId=" + facultyId + ", slotId=" + slotId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private FacultyUnavailability mapRow(ResultSet rs) throws SQLException {
        FacultyUnavailability fu = new FacultyUnavailability();
        fu.setId(rs.getInt("id"));
        fu.setFacultyId(rs.getInt("faculty_id"));
        fu.setSlotId(rs.getInt("slot_id"));
        fu.setReason(rs.getString("reason"));
        return fu;
    }
}
