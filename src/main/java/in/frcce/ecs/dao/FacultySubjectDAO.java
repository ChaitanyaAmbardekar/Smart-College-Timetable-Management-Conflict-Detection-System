package in.frcce.ecs.dao;

import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code faculty_subject} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 *
 * <p>Read queries JOIN to {@code faculty}, {@code subject}, and {@code class_division}
 * so callers receive denormalized display fields alongside the FK ids.</p>
 */
public class FacultySubjectDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    /**
     * SELECT base shared by the three "find" queries.
     * Returns all columns needed to populate a fully-hydrated {@link FacultySubject}.
     */
    private static final String SELECT_BASE =
            "SELECT fs.id, " +
            "       fs.faculty_id,  f.name AS faculty_name,  f.abbreviation AS faculty_abbr, " +
            "       fs.subject_id,  s.name AS subject_name,  s.abbreviation AS subject_abbr, " +
            "       s.course_code,  s.semester, " +
            "       fs.class_id,    cd.division, cd.year AS class_year, cd.academic_year, " +
            "       fs.session_type " +
            "FROM   faculty_subject fs " +
            "JOIN   faculty        f   ON fs.faculty_id  = f.faculty_id " +
            "JOIN   subject        s   ON fs.subject_id  = s.subject_id " +
            "JOIN   class_division cd  ON fs.class_id    = cd.class_id ";

    private static final String SQL_FIND_ALL =
            SELECT_BASE +
            "ORDER  BY cd.semester, cd.division, f.name, s.name";

    private static final String SQL_FIND_BY_CLASS =
            SELECT_BASE +
            "WHERE  fs.class_id = ? " +
            "ORDER  BY f.name, s.name";

    private static final String SQL_FIND_BY_FACULTY =
            SELECT_BASE +
            "WHERE  fs.faculty_id = ? " +
            "ORDER  BY cd.semester, cd.division, s.name";

    private static final String SQL_INSERT =
            "INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM faculty_subject WHERE id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns every faculty-subject allocation with display fields from joined tables,
     * ordered by semester, division, faculty name, and subject name.
     */
    public List<FacultySubject> findAll() {
        return fetchList(SQL_FIND_ALL, ps -> { /* no params */ });
    }

    /**
     * Returns all allocations for a specific class division with joined display fields.
     */
    public List<FacultySubject> findByClass(int classId) {
        return fetchList(SQL_FIND_BY_CLASS, ps -> ps.setInt(1, classId));
    }

    /**
     * Returns all allocations for a specific faculty member with joined display fields.
     */
    public List<FacultySubject> findByFaculty(int facultyId) {
        return fetchList(SQL_FIND_BY_FACULTY, ps -> ps.setInt(1, facultyId));
    }

    /**
     * Inserts a new faculty-subject allocation.
     *
     * @return the generated {@code id}, or -1 if no key was returned.
     */
    public int insert(FacultySubject fs) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, fs.getFacultyId());
                ps.setInt(2, fs.getSubjectId());
                ps.setInt(3, fs.getClassId());
                ps.setString(4, fs.getSessionType());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("FacultySubjectDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a faculty-subject allocation by its surrogate key.
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
            throw new RuntimeException("FacultySubjectDAO.delete failed for id=" + id, e);
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

    private List<FacultySubject> fetchList(String sql, ParamSetter setter) {
        List<FacultySubject> list = new ArrayList<>();
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
            throw new RuntimeException("FacultySubjectDAO query failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    private FacultySubject mapRow(ResultSet rs) throws SQLException {
        FacultySubject fs = new FacultySubject();
        fs.setId(rs.getInt("id"));
        fs.setFacultyId(rs.getInt("faculty_id"));
        fs.setFacultyName(rs.getString("faculty_name"));
        fs.setFacultyAbbr(rs.getString("faculty_abbr"));
        fs.setSubjectId(rs.getInt("subject_id"));
        fs.setSubjectName(rs.getString("subject_name"));
        fs.setSubjectAbbr(rs.getString("subject_abbr"));
        fs.setCourseCode(rs.getString("course_code"));
        fs.setSemester(rs.getInt("semester"));
        fs.setClassId(rs.getInt("class_id"));
        fs.setDivision(rs.getString("division"));
        fs.setClassYear(rs.getInt("class_year"));
        fs.setAcademicYear(rs.getString("academic_year"));
        fs.setSessionType(rs.getString("session_type"));
        return fs;
    }
}
