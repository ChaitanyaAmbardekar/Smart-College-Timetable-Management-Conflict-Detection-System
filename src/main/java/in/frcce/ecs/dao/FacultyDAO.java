package in.frcce.ecs.dao;

import in.frcce.ecs.model.Faculty;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code faculty} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class FacultyDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_ALL =
            "SELECT faculty_id, name, abbreviation, email, designation, " +
            "       max_hours_per_week, dept_id, is_active " +
            "FROM   faculty " +
            "ORDER  BY name";

    private static final String SQL_FIND_BY_ID =
            "SELECT faculty_id, name, abbreviation, email, designation, " +
            "       max_hours_per_week, dept_id, is_active " +
            "FROM   faculty " +
            "WHERE  faculty_id = ?";

    private static final String SQL_FIND_BY_DEPT =
            "SELECT faculty_id, name, abbreviation, email, designation, " +
            "       max_hours_per_week, dept_id, is_active " +
            "FROM   faculty " +
            "WHERE  dept_id = ? " +
            "ORDER  BY name";

    private static final String SQL_INSERT =
            "INSERT INTO faculty (name, abbreviation, email, designation, " +
            "                    max_hours_per_week, dept_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE faculty " +
            "SET    name = ?, abbreviation = ?, email = ?, designation = ?, " +
            "       max_hours_per_week = ?, dept_id = ?, is_active = ? " +
            "WHERE  faculty_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM faculty WHERE faculty_id = ?";

    private static final String SQL_TOGGLE_ACTIVE =
            "UPDATE faculty SET is_active = NOT is_active WHERE faculty_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns every faculty member ordered by name. */
    public List<Faculty> findAll() {
        List<Faculty> list = new ArrayList<>();
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
            throw new RuntimeException("FacultyDAO.findAll failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /** Looks up a single faculty member by primary key. */
    public Optional<Faculty> findById(int facultyId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
                ps.setInt(1, facultyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.findById failed for id=" + facultyId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /** Returns all faculty members belonging to the given department. */
    public List<Faculty> findByDept(int deptId) {
        List<Faculty> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_DEPT)) {
                ps.setInt(1, deptId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.findByDept failed for deptId=" + deptId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /**
     * Inserts a new faculty row.
     *
     * @return the generated {@code faculty_id}, or -1 if no key was returned.
     */
    public int insert(Faculty f) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, f.getName());
                ps.setString(2, f.getAbbreviation());
                ps.setString(3, f.getEmail());
                ps.setString(4, f.getDesignation());
                ps.setInt(5, f.getMaxHoursPerWeek());
                ps.setInt(6, f.getDeptId());
                ps.setBoolean(7, f.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Updates every editable column for the given faculty.
     *
     * @return {@code true} if exactly one row was affected.
     */
    public boolean update(Faculty f) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, f.getName());
                ps.setString(2, f.getAbbreviation());
                ps.setString(3, f.getEmail());
                ps.setString(4, f.getDesignation());
                ps.setInt(5, f.getMaxHoursPerWeek());
                ps.setInt(6, f.getDeptId());
                ps.setBoolean(7, f.isActive());
                ps.setInt(8, f.getFacultyId());
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.update failed for id=" + f.getFacultyId(), e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a faculty row.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int facultyId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, facultyId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.delete failed for id=" + facultyId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Flips {@code is_active} for the given faculty member.
     *
     * @return {@code true} if the row was found and toggled.
     */
    public boolean toggleActive(int facultyId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_TOGGLE_ACTIVE)) {
                ps.setInt(1, facultyId);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("FacultyDAO.toggleActive failed for id=" + facultyId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Faculty mapRow(ResultSet rs) throws SQLException {
        Faculty f = new Faculty();
        f.setFacultyId(rs.getInt("faculty_id"));
        f.setName(rs.getString("name"));
        f.setAbbreviation(rs.getString("abbreviation"));
        f.setEmail(rs.getString("email"));
        f.setDesignation(rs.getString("designation"));
        f.setMaxHoursPerWeek(rs.getInt("max_hours_per_week"));
        f.setDeptId(rs.getInt("dept_id"));
        f.setActive(rs.getBoolean("is_active"));
        return f;
    }
}
