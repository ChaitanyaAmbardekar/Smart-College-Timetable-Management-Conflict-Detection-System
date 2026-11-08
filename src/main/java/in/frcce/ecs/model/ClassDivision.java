package in.frcce.ecs.model;

/**
 * Represents a class division, e.g., "SE-ECS-A Sem 3".
 */
public class ClassDivision {

    private int    classId;
    private int    year;
    private int    semester;
    private String division;
    private int    strength;
    private String academicYear;
    private int    deptId;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ClassDivision() {}

    public ClassDivision(int classId, int year, int semester, String division,
                         int strength, String academicYear, int deptId) {
        this.classId      = classId;
        this.year         = year;
        this.semester     = semester;
        this.division     = division;
        this.strength     = strength;
        this.academicYear = academicYear;
        this.deptId       = deptId;
    }

    // -------------------------------------------------------------------------
    // Computed helper
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable label such as "SE-ECS-A Sem 3".
     * Year abbreviations: 1=FE, 2=SE, 3=TE, 4=BE (generic fallback = "Y{year}").
     */
    public String getLabel() {
        String yearAbbr;
        switch (year) {
            case 1:  yearAbbr = "FE"; break;
            case 2:  yearAbbr = "SE"; break;
            case 3:  yearAbbr = "TE"; break;
            case 4:  yearAbbr = "BE"; break;
            default: yearAbbr = "Y" + year;
        }
        return yearAbbr + "-ECS-" + division + " Sem " + semester;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getDivision() { return division; }
    public void setDivision(String division) { this.division = division; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ClassDivision{" +
               "classId=" + classId +
               ", year=" + year +
               ", semester=" + semester +
               ", division='" + division + '\'' +
               ", strength=" + strength +
               ", academicYear='" + academicYear + '\'' +
               ", deptId=" + deptId +
               ", label='" + getLabel() + '\'' +
               '}';
    }
}
