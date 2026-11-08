package in.frcce.ecs.model;

/**
 * Represents an application user (admin or faculty member).
 * <p>
 * Valid {@code role} values: ADMIN, FACULTY
 */
public class User {

    private String  username;
    private String  passwordHash;
    /** ADMIN | FACULTY */
    private String  role;
    /** Nullable — only set when {@code role} is FACULTY. */
    private Integer facultyId;
    private boolean isActive;
    private java.time.LocalDateTime lastLogin;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public User() {}

    public User(String username, String passwordHash, String role,
                Integer facultyId, boolean isActive) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.facultyId    = facultyId;
        this.isActive     = isActive;
    }

    // -------------------------------------------------------------------------
    // Computed helper
    // -------------------------------------------------------------------------

    /** Returns {@code true} when this user has the ADMIN role. */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getFacultyId() { return facultyId; }
    public void setFacultyId(Integer facultyId) { this.facultyId = facultyId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public java.time.LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(java.time.LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    // -------------------------------------------------------------------------
    // toString  (passwordHash intentionally omitted for security)
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "User{" +
               "username='" + username + '\'' +
               ", role='" + role + '\'' +
               ", facultyId=" + facultyId +
               ", isActive=" + isActive +
               '}';
    }
}
