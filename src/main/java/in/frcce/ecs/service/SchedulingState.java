package in.frcce.ecs.service;

import in.frcce.ecs.model.*;

import java.util.*;

/**
 * Encapsulates the scheduling state used during a single generation run.
 * Tracks which (faculty, slot), (class, slot), (room, slot) combinations
 * are already occupied so the backtracking engine can check constraints in O(1).
 */
public class SchedulingState {

    // Key format: "entityId:slotId" or "classId:batchId:slotId"
    private final Set<String>          occupiedFacultySlots    = new HashSet<>();
    private final Map<String, Integer> classSlotCount          = new HashMap<>();
    private final Set<String>          occupiedWholeClassSlots = new HashSet<>();
    private final Set<String>          occupiedBatchSlots      = new HashSet<>();
    private final Set<String>          occupiedRoomSlots       = new HashSet<>();
    private final Set<String>          facultyUnavailable      = new HashSet<>();
    private final Set<String>          roomUnavailable         = new HashSet<>();
    private final Map<Integer, Integer> externalFacultyHours   = new HashMap<>();

    // Placed entries (committed result)
    private final List<TimetableEntry> placed = new ArrayList<>();

    // -------------------------------------------------------------------------
    public void markFacultyUnavailable(int facultyId, int slotId) {
        facultyUnavailable.add(key(facultyId, slotId));
    }

    public void markRoomUnavailable(int roomId, int slotId) {
        roomUnavailable.add(key(roomId, slotId));
    }

    /**
     * Pre-loads an already scheduled entry from another class/year (e.g. 3rd year Sem 5)
     * so faculty availability, room availability, and faculty weekly workload are
     * respected without mixing external rows into {@link #getPlaced()}.
     */
    public void preloadOtherClassEntry(TimetableEntry existing) {
        if (existing == null) return;
        markFacultyUnavailable(existing.getFacultyId(), existing.getSlotId());
        markRoomUnavailable(existing.getRoomId(), existing.getSlotId());
        externalFacultyHours.merge(existing.getFacultyId(), 1, Integer::sum);
    }

    public boolean isFacultyAvailable(int facultyId, int slotId) {
        return !occupiedFacultySlots.contains(key(facultyId, slotId))
            && !facultyUnavailable.contains(key(facultyId, slotId));
    }

    public boolean isClassFree(int classId, int slotId) {
        return classSlotCount.getOrDefault(key(classId, slotId), 0) == 0;
    }

    /**
     * Checks whether a slot is free for either the whole class ({@code batchId == null})
     * or a specific practical batch ({@code batchId > 0}). Different batches of the same
     * class may run parallel practicals in the same slot, but a batch cannot overlap a
     * whole-class lecture or another session of the same batch.
     */
    public boolean isClassFree(int classId, Integer batchId, int slotId) {
        if (batchId == null || batchId <= 0) {
            return isClassFree(classId, slotId);
        }
        return !occupiedWholeClassSlots.contains(key(classId, slotId))
            && !occupiedBatchSlots.contains(batchSlotKey(classId, batchId, slotId));
    }

    public boolean isRoomFree(int roomId, int slotId) {
        return !occupiedRoomSlots.contains(key(roomId, slotId))
            && !roomUnavailable.contains(key(roomId, slotId));
    }

    // -------------------------------------------------------------------------
    public void occupy(TimetableEntry te) {
        occupiedFacultySlots.add(key(te.getFacultyId(), te.getSlotId()));
        classSlotCount.merge(key(te.getClassId(), te.getSlotId()), 1, Integer::sum);
        if (hasBatch(te)) {
            occupiedBatchSlots.add(batchSlotKey(te.getClassId(), te.getBatchId(), te.getSlotId()));
        } else {
            occupiedWholeClassSlots.add(key(te.getClassId(), te.getSlotId()));
        }
        occupiedRoomSlots.add(key(te.getRoomId(), te.getSlotId()));
        placed.add(te);
    }

    public void release(TimetableEntry te) {
        occupiedFacultySlots.remove(key(te.getFacultyId(), te.getSlotId()));
        classSlotCount.computeIfPresent(key(te.getClassId(), te.getSlotId()), (k, v) -> v > 1 ? v - 1 : null);
        if (hasBatch(te)) {
            occupiedBatchSlots.remove(batchSlotKey(te.getClassId(), te.getBatchId(), te.getSlotId()));
        } else {
            occupiedWholeClassSlots.remove(key(te.getClassId(), te.getSlotId()));
        }
        occupiedRoomSlots.remove(key(te.getRoomId(), te.getSlotId()));
        placed.remove(te);
    }

    public List<TimetableEntry> getPlaced() { return Collections.unmodifiableList(placed); }

    // -------------------------------------------------------------------------
    /** Count how many sessions a faculty has been assigned so far (including other classes/years). */
    public int facultySessionCount(int facultyId) {
        int current = (int) placed.stream().filter(e -> e.getFacultyId() == facultyId).count();
        return current + externalFacultyHours.getOrDefault(facultyId, 0);
    }

    /** Count sessions for a subject within a class on a specific day. */
    public int subjectSessionsOnDay(int subjectId, int classId, String day) {
        return (int) placed.stream().filter(e ->
                e.getSubjectId() == subjectId
             && e.getClassId()   == classId
             && day != null && day.equalsIgnoreCase(e.getDay())
        ).count();
    }

    /** Count sessions of a specific type (TH/PR/TU) for a subject within a class on a specific day. */
    public int subjectSessionsOnDayByType(int subjectId, int classId, String day, String sessionType) {
        return (int) placed.stream().filter(e ->
                e.getSubjectId() == subjectId
             && e.getClassId()   == classId
             && day != null && day.equalsIgnoreCase(e.getDay())
             && sessionType != null && sessionType.equalsIgnoreCase(e.getSessionType())
        ).count();
    }

    /**
     * Enforces daily course limits:
     * - Max 1 theory lecture (TH) of the same course per day.
     * - Max 2 practical lecture slots (PR) of the same course per day.
     */
    public boolean canScheduleCourseOnDay(int subjectId, int classId, String day,
                                          String sessionType, int slotsToAdd) {
        if (day == null || sessionType == null) return true;
        int existing = subjectSessionsOnDayByType(subjectId, classId, day, sessionType);
        if ("TH".equalsIgnoreCase(sessionType)) {
            return existing + slotsToAdd <= 1;
        }
        if ("PR".equalsIgnoreCase(sessionType)) {
            return existing + slotsToAdd <= 2;
        }
        return true;
    }

    /**
     * Enforces per-batch daily practical limit:
     * each batch can perform practicals (PR) for at most one course on a given day.
     */
    public boolean canBatchPerformPracticalOnDay(int classId, Integer batchId, int subjectId, String day) {
        if (batchId == null || batchId <= 0 || day == null) return true;
        return placed.stream().noneMatch(e ->
                e.getClassId() == classId
             && Objects.equals(e.getBatchId(), batchId)
             && day.equalsIgnoreCase(e.getDay())
             && "PR".equalsIgnoreCase(e.getSessionType())
             && e.getSubjectId() != subjectId
        );
    }

    /** Returns true if another batch of this class already has a PR session at the given slot. */
    public boolean hasBatchPracticalAtSlot(int classId, int slotId) {
        return batchPracticalCountAtSlot(classId, slotId) > 0;
    }

    /** Count how many batches of this class already have a PR session at the given slot. */
    public int batchPracticalCountAtSlot(int classId, int slotId) {
        return (int) placed.stream().filter(e ->
                e.getClassId() == classId
             && e.getSlotId() == slotId
             && "PR".equalsIgnoreCase(e.getSessionType())
             && hasBatch(e)
        ).count();
    }

    /** Count total distinct occupied time slots for a class across the whole week. */
    public int distinctClassSlotsTotal(int classId) {
        return (int) placed.stream()
                .filter(e -> e.getClassId() == classId)
                .mapToInt(TimetableEntry::getSlotId)
                .distinct()
                .count();
    }

    /** Count distinct occupied time slots for a class on a given day (for daily load balancing). */
    public int distinctClassSlotsOnDay(int classId, String day) {
        if (day == null) return 0;
        return (int) placed.stream()
                .filter(e -> e.getClassId() == classId && day.equalsIgnoreCase(e.getDay()))
                .mapToInt(TimetableEntry::getSlotId)
                .distinct()
                .count();
    }

    /** Count distinct practical 2-hour time windows used by a class on a given day. */
    public int practicalWindowsOnDay(int classId, String day) {
        if (day == null) return 0;
        return (int) placed.stream()
                .filter(e -> e.getClassId() == classId
                          && day.equalsIgnoreCase(e.getDay())
                          && "PR".equalsIgnoreCase(e.getSessionType()))
                .mapToInt(TimetableEntry::getSlotId)
                .distinct()
                .count() / 2;
    }

    /** Count distinct practical slot-pairs/sessions on a given day for day-load balancing. */
    public int practicalEntriesOnDay(int classId, String day) {
        if (day == null) return 0;
        return (int) placed.stream().filter(e ->
                e.getClassId() == classId
             && day.equalsIgnoreCase(e.getDay())
             && "PR".equalsIgnoreCase(e.getSessionType())
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
    private static boolean hasBatch(TimetableEntry te) {
        return te.getBatchId() != null && te.getBatchId() > 0;
    }

    private static String key(int a, int b) { return a + ":" + b; }

    private static String batchSlotKey(int classId, int batchId, int slotId) {
        return classId + ":" + batchId + ":" + slotId;
    }
}

