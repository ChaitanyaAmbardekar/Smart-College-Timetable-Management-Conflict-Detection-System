package in.frcce.ecs.dao;

import in.frcce.ecs.model.Subject;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code subject} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class SubjectDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static volatile boolean schemaChecked = false;

    private static final String SQL_FIND_ALL =
            "SELECT subject_id, course_code, name, abbreviation, semester, type, " +
            "       theory_hrs, practical_hrs, tutorial_hrs, prac_mode, is_active " +
            "FROM   subject " +
            "ORDER  BY semester, name";

    private static final String SQL_FIND_BY_SEMESTER =
            "SELECT subject_id, course_code, name, abbreviation, semester, type, " +
            "       theory_hrs, practical_hrs, tutorial_hrs, prac_mode, is_active " +
            "FROM   subject " +
            "WHERE  semester = ? " +
            "ORDER  BY name";

    private static final String SQL_FIND_BY_ID =
            "SELECT subject_id, course_code, name, abbreviation, semester, type, " +
            "       theory_hrs, practical_hrs, tutorial_hrs, prac_mode, is_active " +
            "FROM   subject " +
            "WHERE  subject_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO subject (course_code, name, abbreviation, semester, type, " +
            "                    theory_hrs, practical_hrs, tutorial_hrs, prac_mode, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE subject " +
            "SET    course_code = ?, name = ?, abbreviation = ?, semester = ?, type = ?, " +
            "       theory_hrs = ?, practical_hrs = ?, tutorial_hrs = ?, prac_mode = ?, is_active = ? " +
            "WHERE  subject_id = ?";

    private static final String SQL_SYNC_ALLOCATION_PRAC_MODE =
            "UPDATE faculty_subject SET prac_mode = ? WHERE subject_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM subject WHERE subject_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns all subjects ordered by semester then name. */
    public List<Subject> findAll() {
        List<Subject> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            ensureSchema(con);
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Returns all subjects for the given semester number. */
    public List<Subject> findBySemester(int semester) {
        List<Subject> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            ensureSchema(con);
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_SEMESTER)) {
                ps.setInt(1, semester);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.findBySemester failed for sem=" + semester, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Looks up a single subject by primary key. */
    public Optional<Subject> findById(int subjectId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            ensureSchema(con);
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, subjectId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.findById failed for id=" + subjectId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Inserts a new subject row.
     *
     * @return the generated {@code subject_id}, or -1 if no key was returned.
     */
    public int insert(Subject s) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            ensureSchema(con);
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getCourseCode());
                ps.setString(2, s.getName());
                ps.setString(3, s.getAbbreviation());
                ps.setInt(4, s.getSemester());
                ps.setString(5, s.getType());
                ps.setInt(6, s.getTheoryHrs());
                ps.setInt(7, s.getPracticalHrs());
                ps.setInt(8, s.getTutorialHrs());
                ps.setString(9, s.getPracMode());
                ps.setBoolean(10, s.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Updates every editable column for the given subject and keeps existing
     * allocations synced with its practical mode.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(Subject s) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            ensureSchema(con);
            boolean updated;
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, s.getCourseCode());
                ps.setString(2, s.getName());
                ps.setString(3, s.getAbbreviation());
                ps.setInt(4, s.getSemester());
                ps.setString(5, s.getType());
                ps.setInt(6, s.getTheoryHrs());
                ps.setInt(7, s.getPracticalHrs());
                ps.setInt(8, s.getTutorialHrs());
                ps.setString(9, s.getPracMode());
                ps.setBoolean(10, s.isActive());
                ps.setInt(11, s.getSubjectId());
                updated = ps.executeUpdate() == 1;
            }
            if (updated) {
                try (PreparedStatement sync = con.prepareStatement(SQL_SYNC_ALLOCATION_PRAC_MODE)) {
                    sync.setString(1, s.getPracMode());
                    sync.setInt(2, s.getSubjectId());
                    sync.executeUpdate();
                } catch (SQLException ignored) {
                    // If faculty_subject.prac_mode isn't migrated yet, FacultySubjectDAO will migrate it
                }
            }
            return updated;
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.update failed for id=" + s.getSubjectId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a subject row.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int subjectId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, subjectId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("SubjectDAO.delete failed for id=" + subjectId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static void ensureSchema(Connection con) {
        if (schemaChecked) return;
        String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subject' AND COLUMN_NAME = 'prac_mode'";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                st.execute("ALTER TABLE subject ADD COLUMN prac_mode ENUM('BATCH','WHOLE_CLASS') NOT NULL DEFAULT 'BATCH' AFTER tutorial_hrs");
            }
            schemaChecked = true;
        } catch (SQLException ignored) {
            schemaChecked = true;
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setSubjectId(rs.getInt("subject_id"));
        s.setCourseCode(rs.getString("course_code"));
        s.setName(rs.getString("name"));
        s.setAbbreviation(rs.getString("abbreviation"));
        s.setSemester(rs.getInt("semester"));
        s.setType(rs.getString("type"));
        s.setTheoryHrs(rs.getInt("theory_hrs"));
        s.setPracticalHrs(rs.getInt("practical_hrs"));
        s.setTutorialHrs(rs.getInt("tutorial_hrs"));
        s.setPracMode(rs.getString("prac_mode"));
        s.setActive(rs.getBoolean("is_active"));
        return s;
    }
}
