package in.frcce.ecs.model;

/**
 * Represents the allocation of a faculty member to a subject / class.
 * <p>
 * Valid {@code sessionType} values: TH (Theory), PR (Practical), TU (Tutorial), ALL
 */
public class FacultySubject {

    // -------------------------------------------------------------------------
    // Core fields (mapped to DB columns)
    // -------------------------------------------------------------------------

    private int    id;
    private int    facultyId;
    private int    subjectId;
    private int    classId;
    /** TH | PR | TU | ALL */
    private String sessionType;

    // -------------------------------------------------------------------------
    // Display fields (populated via JOINs / view queries)
    // -------------------------------------------------------------------------

    private String facultyName;
    private String facultyAbbr;
    private String subjectName;
    private String subjectAbbr;
    private String courseCode;
    private int    semester;
    private String className;
    private String division;
    private int    classYear;
    private String academicYear;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FacultySubject() {}

    public FacultySubject(int id, int facultyId, int subjectId, int classId,
                          String sessionType,
                          String facultyName, String subjectName, String className) {
        this.id          = id;
        this.facultyId   = facultyId;
        this.subjectId   = subjectId;
        this.classId     = classId;
        this.sessionType = sessionType;
        this.facultyName = facultyName;
        this.subjectName = subjectName;
        this.className   = className;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters — core fields
    // -------------------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    // -------------------------------------------------------------------------
    // Getters & Setters — display fields
    // -------------------------------------------------------------------------

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getFacultyAbbr() { return facultyAbbr; }
    public void setFacultyAbbr(String facultyAbbr) { this.facultyAbbr = facultyAbbr; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectAbbr() { return subjectAbbr; }
    public void setSubjectAbbr(String subjectAbbr) { this.subjectAbbr = subjectAbbr; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public int getClassYear() { return classYear; }
    public void setClassYear(int classYear) { this.classYear = classYear; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "FacultySubject{" +
               "id=" + id +
               ", facultyId=" + facultyId +
               ", subjectId=" + subjectId +
               ", classId=" + classId +
               ", sessionType='" + sessionType + '\'' +
               ", facultyName='" + facultyName + '\'' +
               ", subjectName='" + subjectName + '\'' +
               ", className='" + className + '\'' +
               '}';
    }
}
