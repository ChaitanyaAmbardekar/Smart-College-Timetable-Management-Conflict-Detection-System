package in.frcce.ecs.model;

/**
 * Represents a physical room / venue.
 * <p>
 * Valid {@code type} values: CLASSROOM, LAB, SEMINAR_HALL, TUTORIAL_ROOM
 */
public class Room {

    private int     roomId;
    private String  name;
    /** CLASSROOM | LAB | SEMINAR_HALL | TUTORIAL_ROOM */
    private String  type;
    private int     capacity;
    private String  location;
    private boolean isActive;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Room() {}

    public Room(int roomId, String name, String type, int capacity,
                String location, boolean isActive) {
        this.roomId   = roomId;
        this.name     = name;
        this.type     = type;
        this.capacity = capacity;
        this.location = location;
        this.isActive = isActive;
    }

    // -------------------------------------------------------------------------
    // Computed helper
    // -------------------------------------------------------------------------

    /** Returns {@code true} when the room type is {@code LAB}. */
    public boolean isLab() {
        return "LAB".equals(type);
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Room{" +
               "roomId=" + roomId +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", capacity=" + capacity +
               ", location='" + location + '\'' +
               ", isActive=" + isActive +
               '}';
    }
}
