package in.frcce.ecs.model;

public class Department {

    private int deptId;
    private String name;
    private String code;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Department() {}

    public Department(int deptId, String name, String code) {
        this.deptId = deptId;
        this.name   = name;
        this.code   = code;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Department{" +
               "deptId=" + deptId +
               ", name='" + name + '\'' +
               ", code='" + code + '\'' +
               '}';
    }
}
