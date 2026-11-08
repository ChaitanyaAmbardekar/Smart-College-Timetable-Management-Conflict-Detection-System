package in.frcce.ecs.model;

/**
 * Represents a single timetable time-slot on a given day.
 * <p>
 * Valid {@code day} values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
 */
public class TimeSlot {

    private int     slotId;
    /** MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY */
    private String  day;
    private String  startTime;
    private String  endTime;
    private int     slotNo;
    private boolean isBreak;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TimeSlot() {}

    public TimeSlot(int slotId, String day, String startTime, String endTime,
                    int slotNo, boolean isBreak) {
        this.slotId    = slotId;
        this.day       = day;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.slotNo    = slotNo;
        this.isBreak   = isBreak;
    }

    // -------------------------------------------------------------------------
    // Computed helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the numeric order of the day (Monday = 1 … Saturday = 6).
     * Returns -1 for unrecognised day strings.
     */
    public int getDayOrder() {
        if (day == null) return -1;
        switch (day.toUpperCase()) {
            case "MONDAY":    return 1;
            case "TUESDAY":   return 2;
            case "WEDNESDAY": return 3;
            case "THURSDAY":  return 4;
            case "FRIDAY":    return 5;
            case "SATURDAY":  return 6;
            default:          return -1;
        }
    }

    /** Returns a formatted display string such as {@code "09:00 - 10:00"}. */
    public String getDisplayTime() {
        return startTime + " - " + endTime;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getSlotNo() { return slotNo; }
    public void setSlotNo(int slotNo) { this.slotNo = slotNo; }

    public boolean isBreak() { return isBreak; }
    public void setBreak(boolean aBreak) { isBreak = aBreak; }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "TimeSlot{" +
               "slotId=" + slotId +
               ", day='" + day + '\'' +
               ", displayTime='" + getDisplayTime() + '\'' +
               ", slotNo=" + slotNo +
               ", isBreak=" + isBreak +
               '}';
    }
}
