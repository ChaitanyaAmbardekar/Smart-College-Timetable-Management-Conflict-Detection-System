package in.frcce.ecs.model;

/**
 * Represents a conflict or placement-failure report generated during
 * timetable scheduling.
 */
public class ConflictReport {

    private String  sessionType;
    private int     subjectId;
    private String  subjectName;
    private int     facultyId;
    private String  facultyName;
    private int     classId;
    private String  className;
    private String  reason;
    /** {@code true} if the session was successfully placed despite analysis. */
    private boolean placed;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ConflictReport() {}

    public ConflictReport(String sessionType, int subjectId, String subjectName,
                          int facultyId, String facultyName,
                          int classId, String className,
                          String reason, boolean placed) {
        this.sessionType = sessionType;
        this.subjectId   = subjectId;
        this.subjectName = subjectName;
        this.facultyId   = facultyId;
        this.facultyName = facultyName;
        this.classId     = classId;
        this.className   = className;
        this.reason      = reason;
        this.placed      = placed;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isPlaced() { return placed; }
    public void setPlaced(boolean placed) { this.placed = placed; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ConflictReport{" +
               "sessionType='" + sessionType + '\'' +
               ", subjectId=" + subjectId +
               ", subjectName='" + subjectName + '\'' +
               ", facultyId=" + facultyId +
               ", facultyName='" + facultyName + '\'' +
               ", classId=" + classId +
               ", className='" + className + '\'' +
               ", reason='" + reason + '\'' +
               ", placed=" + placed +
               '}';
    }
}
