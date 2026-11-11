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
    /** BATCH | WHOLE_CLASS (applies when practicals are included) */
    private String pracMode = "BATCH";

    // Preferred 5 choices of classroom/lab
    private Integer prefRoom1;
    private Integer prefRoom2;
    private Integer prefRoom3;
    private Integer prefRoom4;
    private Integer prefRoom5;

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

    public String getPracMode() { return pracMode != null ? pracMode : "BATCH"; }
    public void setPracMode(String pracMode) { this.pracMode = pracMode != null ? pracMode : "BATCH"; }

    public boolean isWholeClassPractical() { return "WHOLE_CLASS".equalsIgnoreCase(getPracMode()); }

    public Integer getPrefRoom1() { return prefRoom1; }
    public void setPrefRoom1(Integer prefRoom1) { this.prefRoom1 = prefRoom1; }

    public Integer getPrefRoom2() { return prefRoom2; }
    public void setPrefRoom2(Integer prefRoom2) { this.prefRoom2 = prefRoom2; }

    public Integer getPrefRoom3() { return prefRoom3; }
    public void setPrefRoom3(Integer prefRoom3) { this.prefRoom3 = prefRoom3; }

    public Integer getPrefRoom4() { return prefRoom4; }
    public void setPrefRoom4(Integer prefRoom4) { this.prefRoom4 = prefRoom4; }

    public Integer getPrefRoom5() { return prefRoom5; }
    public void setPrefRoom5(Integer prefRoom5) { this.prefRoom5 = prefRoom5; }

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
