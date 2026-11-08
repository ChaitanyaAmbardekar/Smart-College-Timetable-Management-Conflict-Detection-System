package in.frcce.ecs.model;

/**
 * Records a time slot during which a faculty member is unavailable.
 */
public class FacultyUnavailability {

    // -------------------------------------------------------------------------
    // Core fields (mapped to DB columns)
    // -------------------------------------------------------------------------

    private int    id;
    private int    facultyId;
    private int    slotId;
    private String reason;

    // -------------------------------------------------------------------------
    // Display fields (populated via JOINs / view queries)
    // -------------------------------------------------------------------------

    private String facultyName;
    private String slotDisplay;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FacultyUnavailability() {}

    public FacultyUnavailability(int id, int facultyId, int slotId, String reason,
                                 String facultyName, String slotDisplay) {
        this.id          = id;
        this.facultyId   = facultyId;
        this.slotId      = slotId;
        this.reason      = reason;
        this.facultyName = facultyName;
        this.slotDisplay = slotDisplay;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters — core fields
    // -------------------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    // -------------------------------------------------------------------------
    // Getters & Setters — display fields
    // -------------------------------------------------------------------------

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getSlotDisplay() { return slotDisplay; }
    public void setSlotDisplay(String slotDisplay) { this.slotDisplay = slotDisplay; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "FacultyUnavailability{" +
               "id=" + id +
               ", facultyId=" + facultyId +
               ", slotId=" + slotId +
               ", reason='" + reason + '\'' +
               ", facultyName='" + facultyName + '\'' +
               ", slotDisplay='" + slotDisplay + '\'' +
               '}';
    }
}
