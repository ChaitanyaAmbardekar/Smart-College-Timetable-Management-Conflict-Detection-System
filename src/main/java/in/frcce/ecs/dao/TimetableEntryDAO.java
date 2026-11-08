package in.frcce.ecs.dao;

import in.frcce.ecs.model.TimetableEntry;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code timetable_entry} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 *
 * <p>The read methods that return lists JOIN to subject, faculty, room, time_slot,
 * class_division, and batch so callers receive fully populated display fields
 * without issuing additional queries.</p>
 */
public class TimetableEntryDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    /**
     * Rich SELECT used by findByClass / findByFaculty / findByRoom / findBySlot.
     * Populates all display fields in a single round-trip.
     */
    private static final String SELECT_RICH =
            "SELECT te.entry_id, te.class_id, te.batch_id, te.subject_id, te.faculty_id, " +
            "       te.room_id, te.slot_id, te.session_type, te.academic_year, te.is_locked, " +
            // subject display
            "       s.name  AS subject_name,  s.abbreviation AS subject_abbr,  s.course_code, " +
            // faculty display
            "       f.name  AS faculty_name,  f.abbreviation AS faculty_abbr, " +
            // room display
            "       r.name  AS room_name,     r.type         AS room_type, " +
            // batch display (nullable)
            "       b.batch_name, " +
            // time slot display
            "       ts.day, ts.slot_no, ts.start_time, ts.end_time, " +
            // class display
            "       cd.year AS class_year, cd.semester, cd.division " +
            "FROM   timetable_entry te " +
            "JOIN   subject        s   ON te.subject_id = s.subject_id " +
            "JOIN   faculty        f   ON te.faculty_id = f.faculty_id " +
            "JOIN   room           r   ON te.room_id    = r.room_id " +
            "JOIN   time_slot      ts  ON te.slot_id    = ts.slot_id " +
            "JOIN   class_division cd  ON te.class_id   = cd.class_id " +
            "LEFT JOIN batch       b   ON te.batch_id   = b.batch_id ";

    private static final String SQL_FIND_BY_CLASS =
            SELECT_RICH +
            "WHERE  te.class_id = ? AND te.academic_year = ? " +
            "ORDER  BY CASE ts.day WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 ELSE 7 END, ts.slot_no";

    private static final String SQL_FIND_BY_FACULTY =
            SELECT_RICH +
            "WHERE  te.faculty_id = ? AND te.academic_year = ? " +
            "ORDER  BY CASE ts.day WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 ELSE 7 END, ts.slot_no";

    private static final String SQL_FIND_BY_ROOM =
            SELECT_RICH +
            "WHERE  te.room_id = ? AND te.academic_year = ? " +
            "ORDER  BY CASE ts.day WHEN 'MONDAY' THEN 1 WHEN 'TUESDAY' THEN 2 WHEN 'WEDNESDAY' THEN 3 WHEN 'THURSDAY' THEN 4 WHEN 'FRIDAY' THEN 5 WHEN 'SATURDAY' THEN 6 ELSE 7 END, ts.slot_no";

    private static final String SQL_FIND_BY_SLOT =
            SELECT_RICH +
            "WHERE  te.slot_id = ? AND te.academic_year = ? " +
            "ORDER  BY te.class_id";

    // Conflict-detection
    private static final String SQL_IS_FACULTY_BUSY =
            "SELECT COUNT(*) FROM timetable_entry " +
            "WHERE  faculty_id = ? AND slot_id = ? AND academic_year = ?";

    private static final String SQL_IS_CLASS_BUSY =
            "SELECT COUNT(*) FROM timetable_entry " +
            "WHERE  class_id = ? AND slot_id = ? AND academic_year = ?";

    private static final String SQL_IS_ROOM_BUSY =
            "SELECT COUNT(*) FROM timetable_entry " +
            "WHERE  room_id = ? AND slot_id = ? AND academic_year = ?";

    // Write operations
    private static final String SQL_INSERT =
            "INSERT INTO timetable_entry " +
            "       (class_id, batch_id, subject_id, faculty_id, room_id, " +
            "        slot_id, session_type, academic_year, is_locked) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE timetable_entry " +
            "SET    class_id = ?, batch_id = ?, subject_id = ?, faculty_id = ?, " +
            "       room_id  = ?, slot_id  = ?, session_type = ?, academic_year = ?, is_locked = ? " +
            "WHERE  entry_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM timetable_entry WHERE entry_id = ?";

    private static final String SQL_DELETE_BY_CLASS =
            "DELETE FROM timetable_entry WHERE class_id = ? AND academic_year = ?";

    private static final String SQL_COUNT_BY_FACULTY =
            "SELECT COUNT(*) FROM timetable_entry " +
            "WHERE  faculty_id = ? AND academic_year = ?";

    // -------------------------------------------------------------------------
    // Public API — reads
    // -------------------------------------------------------------------------

    /**
     * Returns all timetable entries for a class division, richly joined,
     * ordered by day and slot number.
     */
    public List<TimetableEntry> findByClass(int classId, String academicYear) {
        return fetchRichList(SQL_FIND_BY_CLASS, ps -> {
            ps.setInt(1, classId);
            ps.setString(2, academicYear);
        });
    }

    /**
     * Returns all timetable entries for a faculty member, richly joined,
     * ordered by day and slot number.
     */
    public List<TimetableEntry> findByFaculty(int facultyId, String academicYear) {
        return fetchRichList(SQL_FIND_BY_FACULTY, ps -> {
            ps.setInt(1, facultyId);
            ps.setString(2, academicYear);
        });
    }

    /**
     * Returns all timetable entries for a room, richly joined,
     * ordered by day and slot number.
     */
    public List<TimetableEntry> findByRoom(int roomId, String academicYear) {
        return fetchRichList(SQL_FIND_BY_ROOM, ps -> {
            ps.setInt(1, roomId);
            ps.setString(2, academicYear);
        });
    }

    /**
     * Returns all timetable entries for a time slot, richly joined,
     * ordered by class id.
     */
    public List<TimetableEntry> findBySlot(int slotId, String academicYear) {
        return fetchRichList(SQL_FIND_BY_SLOT, ps -> {
            ps.setInt(1, slotId);
            ps.setString(2, academicYear);
        });
    }

    // -------------------------------------------------------------------------
    // Public API — conflict detection
    // -------------------------------------------------------------------------

    /**
     * @return {@code true} when the faculty member already has an entry in this slot
     *         for the given academic year (double-booking check).
     */
    public boolean isFacultyBusy(int facultyId, int slotId, String academicYear) {
        return countExists(SQL_IS_FACULTY_BUSY, facultyId, slotId, academicYear);
    }

    /**
     * @return {@code true} when the class already has an entry in this slot
     *         for the given academic year.
     */
    public boolean isClassBusy(int classId, int slotId, String academicYear) {
        return countExists(SQL_IS_CLASS_BUSY, classId, slotId, academicYear);
    }

    /**
     * @return {@code true} when the room already has an entry in this slot
     *         for the given academic year.
     */
    public boolean isRoomBusy(int roomId, int slotId, String academicYear) {
        return countExists(SQL_IS_ROOM_BUSY, roomId, slotId, academicYear);
    }

    // -------------------------------------------------------------------------
    // Public API — writes
    // -------------------------------------------------------------------------

    /**
     * Inserts a new timetable entry.
     *
     * @return the generated {@code entry_id}, or -1 if no key was returned.
     */
    public int insert(TimetableEntry te) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            return insertWithConnection(te, con);
        } catch (SQLException e) {
            throw new RuntimeException("TimetableEntryDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    public int insertWithConnection(TimetableEntry te, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                          Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, te.getClassId());
            // batch_id is nullable
            if (te.getBatchId() != null && te.getBatchId() > 0) {
                ps.setInt(2, te.getBatchId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, te.getSubjectId());
            ps.setInt(4, te.getFacultyId());
            ps.setInt(5, te.getRoomId());
            ps.setInt(6, te.getSlotId());
            ps.setString(7, te.getSessionType());
            ps.setString(8, te.getAcademicYear());
            ps.setBoolean(9, te.isLocked());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Updates every editable column for the given entry.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(TimetableEntry te) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setInt(1, te.getClassId());
                if (te.getBatchId() != null && te.getBatchId() > 0) {
                    ps.setInt(2, te.getBatchId());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setInt(3, te.getSubjectId());
                ps.setInt(4, te.getFacultyId());
                ps.setInt(5, te.getRoomId());
                ps.setInt(6, te.getSlotId());
                ps.setString(7, te.getSessionType());
                ps.setString(8, te.getAcademicYear());
                ps.setBoolean(9, te.isLocked());
                ps.setInt(10, te.getEntryId());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "TimetableEntryDAO.update failed for id=" + te.getEntryId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a single timetable entry.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int entryId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, entryId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "TimetableEntryDAO.delete failed for id=" + entryId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Deletes all non-locked timetable entries for a class division and academic year.
     * Used to clear a class timetable before regeneration.
     *
     * @return the number of rows deleted.
     */
    public int deleteByClass(int classId, String academicYear) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            return deleteByClassWithConnection(classId, academicYear, con);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "TimetableEntryDAO.deleteByClass failed for classId=" + classId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    public int deleteByClassWithConnection(int classId, String academicYear, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_BY_CLASS)) {
            ps.setInt(1, classId);
            ps.setString(2, academicYear);
            return ps.executeUpdate();
        }
    }

    /**
     * Returns the total number of timetable entries assigned to a faculty member
     * for the given academic year (used for workload accounting).
     */
    public int countByFaculty(int facultyId, String academicYear) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_COUNT_BY_FACULTY)) {
                ps.setInt(1, facultyId);
                ps.setString(2, academicYear);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "TimetableEntryDAO.countByFaculty failed for facultyId=" + facultyId, e);
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

    private List<TimetableEntry> fetchRichList(String sql, ParamSetter setter) {
        List<TimetableEntry> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRichRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("TimetableEntryDAO query failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /**
     * Conflict-detection helper: returns {@code true} when COUNT(*) > 0 for the given
     * (entity_id, slot_id, academic_year) triple.
     */
    private boolean countExists(String sql, int entityId, int slotId, String academicYear) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, entityId);
                ps.setInt(2, slotId);
                ps.setString(3, academicYear);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("TimetableEntryDAO.countExists failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /** Maps all columns (FK ids + joined display fields) onto a {@link TimetableEntry}. */
    private TimetableEntry mapRichRow(ResultSet rs) throws SQLException {
        TimetableEntry te = new TimetableEntry();
        te.setEntryId(rs.getInt("entry_id"));
        te.setClassId(rs.getInt("class_id"));
        te.setBatchId(rs.getInt("batch_id"));           // 0 when NULL (auto-unbox)
        te.setSubjectId(rs.getInt("subject_id"));
        te.setFacultyId(rs.getInt("faculty_id"));
        te.setRoomId(rs.getInt("room_id"));
        te.setSlotId(rs.getInt("slot_id"));
        te.setSessionType(rs.getString("session_type"));
        te.setAcademicYear(rs.getString("academic_year"));
        te.setLocked(rs.getBoolean("is_locked"));
        // --- joined display fields ---
        te.setSubjectName(rs.getString("subject_name"));
        te.setSubjectAbbr(rs.getString("subject_abbr"));
        te.setCourseCode(rs.getString("course_code"));
        te.setFacultyName(rs.getString("faculty_name"));
        te.setFacultyAbbr(rs.getString("faculty_abbr"));
        te.setRoomName(rs.getString("room_name"));
        te.setRoomType(rs.getString("room_type"));
        te.setBatchName(rs.getString("batch_name"));    // nullable
        te.setDay(rs.getString("day"));
        te.setSlotNo(rs.getInt("slot_no"));
        te.setStartTime(rs.getString("start_time"));
        te.setEndTime(rs.getString("end_time"));
        te.setClassYear(rs.getInt("class_year"));
        te.setSemester(rs.getInt("semester"));
        te.setDivision(rs.getString("division"));
        return te;
    }
}
