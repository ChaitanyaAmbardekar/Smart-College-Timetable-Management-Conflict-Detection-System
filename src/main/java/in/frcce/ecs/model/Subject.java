package in.frcce.ecs.model;

/**
 * Represents an academic subject / course.
 * <p>
 * Valid {@code type} values: PCC, OE, MDM, HSSM, EL, BSC, VAC, CEP
 */
public class Subject {

    private int     subjectId;
    private String  courseCode;
    private String  name;
    private String  abbreviation;
    private int     semester;
    /** PCC | OE | MDM | HSSM | EL | BSC | VAC | CEP */
    private String  type;
    private int     theoryHrs;
    private int     practicalHrs;
    private int     tutorialHrs;
    private boolean isActive;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Subject() {}

    public Subject(int subjectId, String courseCode, String name, String abbreviation,
                   int semester, String type, int theoryHrs, int practicalHrs,
                   int tutorialHrs, boolean isActive) {
        this.subjectId    = subjectId;
        this.courseCode   = courseCode;
        this.name         = name;
        this.abbreviation = abbreviation;
        this.semester     = semester;
        this.type         = type;
        this.theoryHrs    = theoryHrs;
        this.practicalHrs = practicalHrs;
        this.tutorialHrs  = tutorialHrs;
        this.isActive     = isActive;
    }

    // -------------------------------------------------------------------------
    // Computed helper
    // -------------------------------------------------------------------------

    /** Returns the sum of theory, practical, and tutorial hours per week. */
    public int getTotalHours() {
        return theoryHrs + practicalHrs + tutorialHrs;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getTheoryHrs() { return theoryHrs; }
    public void setTheoryHrs(int theoryHrs) { this.theoryHrs = theoryHrs; }

    public int getPracticalHrs() { return practicalHrs; }
    public void setPracticalHrs(int practicalHrs) { this.practicalHrs = practicalHrs; }

    public int getTutorialHrs() { return tutorialHrs; }
    public void setTutorialHrs(int tutorialHrs) { this.tutorialHrs = tutorialHrs; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Subject{" +
               "subjectId=" + subjectId +
               ", courseCode='" + courseCode + '\'' +
               ", name='" + name + '\'' +
               ", abbreviation='" + abbreviation + '\'' +
               ", semester=" + semester +
               ", type='" + type + '\'' +
               ", theoryHrs=" + theoryHrs +
               ", practicalHrs=" + practicalHrs +
               ", tutorialHrs=" + tutorialHrs +
               ", totalHrs=" + getTotalHours() +
               ", isActive=" + isActive +
               '}';
    }
}
