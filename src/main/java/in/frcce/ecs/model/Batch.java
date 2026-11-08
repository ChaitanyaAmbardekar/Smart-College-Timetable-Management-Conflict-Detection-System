package in.frcce.ecs.model;

/**
 * Represents a practical / lab batch within a {@link ClassDivision}.
 */
public class Batch {

    private int    batchId;
    private int    classId;
    private String batchName;
    private int    strength;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Batch() {}

    public Batch(int batchId, int classId, String batchName, int strength) {
        this.batchId   = batchId;
        this.classId   = classId;
        this.batchName = batchName;
        this.strength  = strength;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Batch{" +
               "batchId=" + batchId +
               ", classId=" + classId +
               ", batchName='" + batchName + '\'' +
               ", strength=" + strength +
               '}';
    }
}
