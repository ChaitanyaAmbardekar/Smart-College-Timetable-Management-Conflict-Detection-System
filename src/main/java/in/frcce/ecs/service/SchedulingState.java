package in.frcce.ecs.service;

import in.frcce.ecs.model.*;

import java.util.*;

/**
 * Encapsulates the scheduling state used during a single generation run.
 * Tracks which (faculty, slot), (class, slot), (room, slot) combinations
 * are already occupied so the backtracking engine can check constraints in O(1).
 */
public class SchedulingState {

    // Key format: "entityId:slotId"
    private final Set<String> occupiedFacultySlots = new HashSet<>();
    private final Set<String> occupiedClassSlots   = new HashSet<>();
    private final Set<String> occupiedRoomSlots    = new HashSet<>();
    private final Set<String> facultyUnavailable   = new HashSet<>();

    // Placed entries (committed result)
    private final List<TimetableEntry> placed = new ArrayList<>();

    // -------------------------------------------------------------------------
    public void markFacultyUnavailable(int facultyId, int slotId) {
        facultyUnavailable.add(key(facultyId, slotId));
    }

    public boolean isFacultyAvailable(int facultyId, int slotId) {
        return !occupiedFacultySlots.contains(key(facultyId, slotId))
            && !facultyUnavailable.contains(key(facultyId, slotId));
    }

    public boolean isClassFree(int classId, int slotId) {
        return !occupiedClassSlots.contains(key(classId, slotId));
    }

    public boolean isRoomFree(int roomId, int slotId) {
        return !occupiedRoomSlots.contains(key(roomId, slotId));
    }

    // -------------------------------------------------------------------------
    public void occupy(TimetableEntry te) {
        occupiedFacultySlots.add(key(te.getFacultyId(), te.getSlotId()));
        occupiedClassSlots.add(key(te.getClassId(), te.getSlotId()));
        occupiedRoomSlots.add(key(te.getRoomId(), te.getSlotId()));
        placed.add(te);
    }

    public void release(TimetableEntry te) {
        occupiedFacultySlots.remove(key(te.getFacultyId(), te.getSlotId()));
        occupiedClassSlots.remove(key(te.getClassId(), te.getSlotId()));
        occupiedRoomSlots.remove(key(te.getRoomId(), te.getSlotId()));
        placed.remove(te);
    }

    public List<TimetableEntry> getPlaced() { return Collections.unmodifiableList(placed); }

    // -------------------------------------------------------------------------
    /** Count how many sessions a faculty has been assigned so far. */
    public int facultySessionCount(int facultyId) {
        return (int) placed.stream().filter(e -> e.getFacultyId() == facultyId).count();
    }

    /** Count sessions for a subject within a class on a specific day. */
    public int subjectSessionsOnDay(int subjectId, int classId, String day) {
        return (int) placed.stream().filter(e ->
                e.getSubjectId() == subjectId
             && e.getClassId()   == classId
             && day.equalsIgnoreCase(e.getDay())
        ).count();
    }

    /** Count consecutive sessions for a class ending at or before slotNo. */
    public int consecutiveSessionsFor(int classId, String day, int upToSlotNo) {
        int count = 0;
        int slot = upToSlotNo;
        while (slot > 0) {
            final int s = slot;
            boolean found = placed.stream().anyMatch(e ->
                    e.getClassId() == classId
                 && day.equalsIgnoreCase(e.getDay())
                 && e.getSlotNo() == s);
            if (!found) break;
            count++;
            slot--;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    private static String key(int a, int b) { return a + ":" + b; }
}

