package in.frcce.ecs.model;

public class Faculty {

    private int     facultyId;
    private String  name;
    private String  abbreviation;
    private String  email;
    private String  designation;
    private int     maxHoursPerWeek;
    private int     deptId;
    private boolean isActive;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Faculty() {}

    public Faculty(int facultyId, String name, String abbreviation, String email,
                   String designation, int maxHoursPerWeek, int deptId, boolean isActive) {
        this.facultyId       = facultyId;
        this.name            = name;
        this.abbreviation    = abbreviation;
        this.email           = email;
        this.designation     = designation;
        this.maxHoursPerWeek = maxHoursPerWeek;
        this.deptId          = deptId;
        this.isActive        = isActive;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public int getMaxHoursPerWeek() { return maxHoursPerWeek; }
    public void setMaxHoursPerWeek(int maxHoursPerWeek) { this.maxHoursPerWeek = maxHoursPerWeek; }

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Faculty{" +
               "facultyId=" + facultyId +
               ", name='" + name + '\'' +
               ", abbreviation='" + abbreviation + '\'' +
               ", email='" + email + '\'' +
               ", designation='" + designation + '\'' +
               ", maxHoursPerWeek=" + maxHoursPerWeek +
               ", deptId=" + deptId +
               ", isActive=" + isActive +
               '}';
    }
}
