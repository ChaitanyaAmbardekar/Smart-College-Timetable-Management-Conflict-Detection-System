package in.frcce.ecs.model;

/**
 * Represents a single scheduled entry in the timetable grid.
 * <p>
 * Valid {@code sessionType} values: TH (Theory), PR (Practical), TU (Tutorial)
 */
public class TimetableEntry {

    // -------------------------------------------------------------------------
    // Core fields (mapped to DB columns)
    // -------------------------------------------------------------------------

    private int     entryId;
    private int     classId;
    /** Nullable — only set for practical (batch) sessions. */
    private Integer batchId;
    private int     subjectId;
    private int     facultyId;
    private int     roomId;
    private int     slotId;
    /** TH | PR | TU */
    private String  sessionType;
    private String  academicYear;
    private boolean isLocked;

    // -------------------------------------------------------------------------
    // Display fields (populated via JOINs / view queries)
    // -------------------------------------------------------------------------

    private String subjectName;
    private String subjectAbbr;
    private String courseCode;
    private String facultyName;
    private String facultyAbbr;
    private String roomName;
    private String roomType;
    private String batchName;
    private String day;
    private int    slotNo;
    private String startTime;
    private String endTime;
    private String divisionLabel;
    private int    classYear;
    private int    semester;
    private String division;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TimetableEntry() {}

    public TimetableEntry(int entryId, int classId, Integer batchId, int subjectId,
                          int facultyId, int roomId, int slotId, String sessionType,
                          String academicYear, boolean isLocked,
                          String subjectName, String subjectAbbr,
                          String facultyName, String facultyAbbr,
                          String roomName, String batchName,
                          String day, int slotNo, String startTime, String endTime,
                          String divisionLabel) {
        this.entryId      = entryId;
        this.classId      = classId;
        this.batchId      = batchId;
        this.subjectId    = subjectId;
        this.facultyId    = facultyId;
        this.roomId       = roomId;
        this.slotId       = slotId;
        this.sessionType  = sessionType;
        this.academicYear = academicYear;
        this.isLocked     = isLocked;
        this.subjectName  = subjectName;
        this.subjectAbbr  = subjectAbbr;
        this.facultyName  = facultyName;
        this.facultyAbbr  = facultyAbbr;
        this.roomName     = roomName;
        this.batchName    = batchName;
        this.day          = day;
        this.slotNo       = slotNo;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.divisionLabel = divisionLabel;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters — core fields
    // -------------------------------------------------------------------------

    public int getEntryId() { return entryId; }
    public void setEntryId(int entryId) { this.entryId = entryId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    // -------------------------------------------------------------------------
    // Getters & Setters — display fields
    // -------------------------------------------------------------------------

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectAbbr() { return subjectAbbr; }
    public void setSubjectAbbr(String subjectAbbr) { this.subjectAbbr = subjectAbbr; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getFacultyAbbr() { return facultyAbbr; }
    public void setFacultyAbbr(String facultyAbbr) { this.facultyAbbr = facultyAbbr; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public int getSlotNo() { return slotNo; }
    public void setSlotNo(int slotNo) { this.slotNo = slotNo; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDivisionLabel() { return divisionLabel; }
    public void setDivisionLabel(String divisionLabel) { this.divisionLabel = divisionLabel; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public int getClassYear() { return classYear; }
    public void setClassYear(int classYear) { this.classYear = classYear; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "TimetableEntry{" +
               "entryId=" + entryId +
               ", classId=" + classId +
               ", batchId=" + batchId +
               ", subjectId=" + subjectId +
               ", facultyId=" + facultyId +
               ", roomId=" + roomId +
               ", slotId=" + slotId +
               ", sessionType='" + sessionType + '\'' +
               ", academicYear='" + academicYear + '\'' +
               ", isLocked=" + isLocked +
               ", subjectName='" + subjectName + '\'' +
               ", subjectAbbr='" + subjectAbbr + '\'' +
               ", facultyName='" + facultyName + '\'' +
               ", facultyAbbr='" + facultyAbbr + '\'' +
               ", roomName='" + roomName + '\'' +
               ", batchName='" + batchName + '\'' +
               ", day='" + day + '\'' +
               ", slotNo=" + slotNo +
               ", startTime='" + startTime + '\'' +
               ", endTime='" + endTime + '\'' +
               ", divisionLabel='" + divisionLabel + '\'' +
               '}';
    }
}
