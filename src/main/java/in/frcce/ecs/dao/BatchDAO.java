package in.frcce.ecs.dao;

import in.frcce.ecs.model.Batch;
import in.frcce.ecs.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code batch} table.
 * All operations use {@link PreparedStatement}; no string concatenation in SQL.
 */
public class BatchDAO {

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------
    private static final String SQL_FIND_BY_CLASS =
            "SELECT batch_id, class_id, batch_name, strength " +
            "FROM   batch " +
            "WHERE  class_id = ? " +
            "ORDER  BY batch_name";

    private static final String SQL_INSERT =
            "INSERT INTO batch (class_id, batch_name, strength) " +
            "VALUES (?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM batch WHERE batch_id = ?";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns all batches that belong to the given class division, ordered by name. */
    public List<Batch> findByClass(int classId) {
        List<Batch> list = new ArrayList<>();
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_CLASS)) {
                ps.setInt(1, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "BatchDAO.findByClass failed for classId=" + classId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
        return list;
    }

    /**
     * Inserts a new batch row.
     *
     * @return the generated {@code batch_id}, or -1 if no key was returned.
     */
    public int insert(Batch b) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT,
                                                              Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, b.getClassId());
                ps.setString(2, b.getBatchName());
                ps.setInt(3, b.getStrength());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("BatchDAO.insert failed", e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    /**
     * Hard-deletes a batch row.
     * Timetable entries that reference this batch are set to {@code NULL} via the FK cascade.
     *
     * @return {@code true} if a row was removed.
     */
    public boolean delete(int batchId) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, batchId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("BatchDAO.delete failed for id=" + batchId, e);
        } finally {
            DBConnection.getInstance().release(con);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Batch mapRow(ResultSet rs) throws SQLException {
        Batch b = new Batch();
        b.setBatchId(rs.getInt("batch_id"));
        b.setClassId(rs.getInt("class_id"));
        b.setBatchName(rs.getString("batch_name"));
        b.setStrength(rs.getInt("strength"));
        return b;
    }
}
