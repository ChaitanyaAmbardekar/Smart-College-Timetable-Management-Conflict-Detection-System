package in.frcce.ecs.dao;

import in.frcce.ecs.model.ClassDivision;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code class_division} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class ClassDivisionDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_ALL =
            "SELECT class_id, year, semester, division, strength, academic_year, dept_id " +
            "FROM   class_division " +
            "ORDER  BY academic_year DESC, semester, division";

    private static final String SQL_FIND_BY_SEMESTER =
            "SELECT class_id, year, semester, division, strength, academic_year, dept_id " +
            "FROM   class_division " +
            "WHERE  semester = ? " +
            "ORDER  BY academic_year DESC, division";

    private static final String SQL_FIND_BY_ID =
            "SELECT class_id, year, semester, division, strength, academic_year, dept_id " +
            "FROM   class_division " +
            "WHERE  class_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO class_division (year, semester, division, strength, academic_year, dept_id) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE class_division " +
            "SET    year = ?, semester = ?, division = ?, strength = ?, " +
            "       academic_year = ?, dept_id = ? " +
            "WHERE  class_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM class_division WHERE class_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns every class division, newest academic year first. */
    public List<ClassDivision> findAll() {
        List<ClassDivision> list = new ArrayList<>();
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
            throw new RuntimeException("ClassDivisionDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Returns all class divisions for the given semester number. */
    public List<ClassDivision> findBySemester(int semester) {
        List<ClassDivision> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_SEMESTER)) {
                ps.setInt(1, semester);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "ClassDivisionDAO.findBySemester failed for sem=" + semester, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Looks up a single class division by primary key. */
    public Optional<ClassDivision> findById(int classId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "ClassDivisionDAO.findById failed for id=" + classId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Inserts a new class-division row.
     *
     * @return the generated {@code class_id}, or -1 if no key was returned.
     */
    public int insert(ClassDivision cd) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, cd.getYear());
                ps.setInt(2, cd.getSemester());
                ps.setString(3, cd.getDivision());
                ps.setInt(4, cd.getStrength());
                ps.setString(5, cd.getAcademicYear());
                ps.setInt(6, cd.getDeptId());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("ClassDivisionDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Updates every editable column for the given class division.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(ClassDivision cd) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setInt(1, cd.getYear());
                ps.setInt(2, cd.getSemester());
                ps.setString(3, cd.getDivision());
                ps.setInt(4, cd.getStrength());
                ps.setString(5, cd.getAcademicYear());
                ps.setInt(6, cd.getDeptId());
                ps.setInt(7, cd.getClassId());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "ClassDivisionDAO.update failed for id=" + cd.getClassId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a class-division row (cascades to batch and timetable_entry via FK).
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int classId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, classId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "ClassDivisionDAO.delete failed for id=" + classId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ClassDivision mapRow(ResultSet rs) throws SQLException {
        ClassDivision cd = new ClassDivision();
        cd.setClassId(rs.getInt("class_id"));
        cd.setYear(rs.getInt("year"));
        cd.setSemester(rs.getInt("semester"));
        cd.setDivision(rs.getString("division"));
        cd.setStrength(rs.getInt("strength"));
        cd.setAcademicYear(rs.getString("academic_year"));
        cd.setDeptId(rs.getInt("dept_id"));
        return cd;
    }
}
