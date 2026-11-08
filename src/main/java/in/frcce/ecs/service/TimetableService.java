package in.frcce.ecs.service;

import in.frcce.ecs.dao.*;
import in.frcce.ecs.model.*;
import in.frcce.ecs.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Core scheduling engine.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Build a list of {@link ScheduleTask}s — one per required session.</li>
 *   <li>Sort tasks by priority: lab blocks first, then tutorials, then theory
 *       (most-constrained-first heuristic).</li>
 *   <li>Backtracking with constraint propagation over the sorted task list.
 *       At each step, iterate candidate (slot, room) pairs and prune any that
 *       violate a hard constraint instantly.</li>
 *   <li>Soft-constraint scoring guides candidate ordering (fewer backtracks).</li>
 *   <li>If backtracking exceeds the time limit, switch to greedy placement and
 *       record unplaced sessions in the conflict report.</li>
 *   <li>Persist results in a single JDBC transaction (commit/rollback).</li>
 * </ol>
 */
public class TimetableService {

    private static final Logger LOG = Logger.getLogger(TimetableService.class.getName());

    /** Max milliseconds before giving up on backtracking and switching to greedy. */
    private static final long TIME_LIMIT_MS = 15_000;

    private final ClassDivisionDAO        classDivisionDAO;
    private final BatchDAO                batchDAO;
    private final SubjectDAO              subjectDAO;
    private final FacultyDAO              facultyDAO;
    private final FacultySubjectDAO       facultySubjectDAO;
    private final FacultyUnavailabilityDAO unavailDAO;
    private final TimeSlotDAO             timeSlotDAO;
    private final RoomDAO                 roomDAO;
    private final TimetableEntryDAO       timetableEntryDAO;

    private Consumer<String> progressListener; // notified during generation

    public TimetableService() {
        classDivisionDAO  = new ClassDivisionDAO();
        batchDAO          = new BatchDAO();
        subjectDAO        = new SubjectDAO();
        facultyDAO        = new FacultyDAO();
        facultySubjectDAO = new FacultySubjectDAO();
        unavailDAO        = new FacultyUnavailabilityDAO();
        timeSlotDAO       = new TimeSlotDAO();
        roomDAO           = new RoomDAO();
        timetableEntryDAO = new TimetableEntryDAO();
    }

    public void setProgressListener(Consumer<String> listener) {
        this.progressListener = listener;
    }

    private void notifyProgress(String msg) {
        LOG.info(msg);
        if (progressListener != null) progressListener.accept(msg);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Generate a timetable for the given class division.
     * Clears any existing (non-locked) entries first.
     *
     * @return GenerationResult with placed entries and unresolved conflicts.
     */
    public GenerationResult generate(int classId, String academicYear) {
        notifyProgress("Starting generation for class " + classId + " (" + academicYear + ")");

        ClassDivision cd = classDivisionDAO.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        // 1. Load all required data
        List<TimeSlot>          slots     = timeSlotDAO.findAll();           // excludes breaks
        List<Room>              rooms     = roomDAO.findAll();
        List<FacultySubject>    allocs    = facultySubjectDAO.findByClass(classId);
        List<Batch>             batches   = batchDAO.findByClass(classId);
        List<FacultyUnavailability> unavails = unavailDAO.findAll();

        // Build unavailability lookup: facultyId -> Set<slotId>
        Map<Integer, Set<Integer>> unavailMap = new HashMap<>();
        for (FacultyUnavailability fu : unavails) {
            unavailMap.computeIfAbsent(fu.getFacultyId(), k -> new HashSet<>()).add(fu.getSlotId());
        }

        // Faculty max-hours lookup
        Map<Integer, Integer> maxHours = new HashMap<>();
        for (Faculty f : facultyDAO.findAll()) {
            maxHours.put(f.getFacultyId(), f.getMaxHoursPerWeek());
        }

        // 2. Build task list
        notifyProgress("Building task list...");
        List<ScheduleTask> tasks = buildTasks(allocs, cd, batches, academicYear);

        // Sort: LAB_BLOCK first, then TUTORIAL, then THEORY (most-constrained first)
        tasks.sort(Comparator.comparing(ScheduleTask::getPriority));

        notifyProgress("Total tasks to schedule: " + tasks.size());

        // 3. Init scheduling state
        SchedulingState state = new SchedulingState();
        for (Map.Entry<Integer, Set<Integer>> e : unavailMap.entrySet()) {
            for (int slotId : e.getValue()) {
                state.markFacultyUnavailable(e.getKey(), slotId);
            }
        }

        // 4. Backtracking with time limit
        List<ConflictReport> conflicts = new ArrayList<>();
        long deadline = System.currentTimeMillis() + TIME_LIMIT_MS;

        boolean success = backtrack(tasks, 0, state, slots, rooms, maxHours, deadline, conflicts);

        if (!success) {
            notifyProgress("Backtracking timed out or failed — switching to greedy repair...");
            greedyRepair(tasks, state, slots, rooms, maxHours, conflicts);
        }

        List<TimetableEntry> placed = new ArrayList<>(state.getPlaced());
        notifyProgress("Placement complete: " + placed.size() + " entries placed, "
                + conflicts.size() + " unresolved.");

        // 5. Persist in a transaction
        if (!placed.isEmpty()) {
            persistWithTransaction(classId, academicYear, placed);
        }

        return new GenerationResult(placed, conflicts);
    }

    // =========================================================================
    // TASK BUILDER
    // =========================================================================

    private List<ScheduleTask> buildTasks(List<FacultySubject> allocs,
                                           ClassDivision cd,
                                           List<Batch> batches,
                                           String academicYear) {
        List<ScheduleTask> tasks = new ArrayList<>();

        for (FacultySubject fs : allocs) {
            Subject sub = subjectDAO.findById(fs.getSubjectId())
                    .orElse(null);
            if (sub == null || !sub.isActive()) continue;

            String type = fs.getSessionType(); // TH, PR, TU, ALL

            // Theory sessions
            if (("TH".equals(type) || "ALL".equals(type)) && sub.getTheoryHrs() > 0) {
                for (int i = 0; i < sub.getTheoryHrs(); i++) {
                    tasks.add(new ScheduleTask(
                            sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                            cd.getClassId(), null, fs.getFacultyId(),
                            "TH", 1, cd.getStrength(), academicYear));
                }
            }

            // Tutorial sessions
            if (("TU".equals(type) || "ALL".equals(type)) && sub.getTutorialHrs() > 0) {
                for (int i = 0; i < sub.getTutorialHrs(); i++) {
                    tasks.add(new ScheduleTask(
                            sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                            cd.getClassId(), null, fs.getFacultyId(),
                            "TU", 1, cd.getStrength(), academicYear));
                }
            }

            // Practical sessions — batch-wise, 2 consecutive slots each
            if (("PR".equals(type) || "ALL".equals(type)) && sub.getPracticalHrs() > 0) {
                // practicalHrs is the TOTAL hours; each batch gets (practicalHrs) lab slots (1 hour each)
                // We schedule "practicalHrs / 2" lab blocks per batch (each block = 2 consecutive slots)
                int blocks = sub.getPracticalHrs() / 2;
                if (blocks == 0) blocks = 1;
                List<Batch> useBatches = batches.isEmpty()
                        ? List.of(new Batch(0, cd.getClassId(), "ALL", cd.getStrength()))
                        : batches;
                for (Batch batch : useBatches) {
                    for (int i = 0; i < blocks; i++) {
                        tasks.add(new ScheduleTask(
                                sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                                cd.getClassId(), batch.getBatchId() == 0 ? null : batch.getBatchId(),
                                fs.getFacultyId(), "PR", 2,
                                batch.getStrength(), academicYear));
                    }
                }
            }
        }
        return tasks;
    }

    // =========================================================================
    // BACKTRACKING ENGINE
    // =========================================================================

    private boolean backtrack(List<ScheduleTask> tasks, int index,
                               SchedulingState state,
                               List<TimeSlot> slots, List<Room> rooms,
                               Map<Integer, Integer> maxHours,
                               long deadline,
                               List<ConflictReport> conflicts) {
        if (index == tasks.size()) return true;
        if (System.currentTimeMillis() > deadline) return false;

        ScheduleTask task = tasks.get(index);

        // Get candidate (slot, room) pairs, ordered by soft-constraint score
        List<SlotRoomPair> candidates = getCandidates(task, state, slots, rooms, maxHours);

        for (SlotRoomPair candidate : candidates) {
            List<TimetableEntry> placed = placeTask(task, candidate, state);
            if (placed == null) continue; // constraint violated

            boolean result = backtrack(tasks, index + 1, state, slots, rooms, maxHours, deadline, conflicts);
            if (result) return true;

            // Undo placement
            placed.forEach(state::release);
        }

        // Could not place this task
        conflicts.add(new ConflictReport(
                task.getSessionType(), task.getSubjectId(), task.getSubjectName(),
                task.getFacultyId(), "", task.getClassId(), "",
                "No valid slot/room found", false));
        return false;
    }

    // =========================================================================
    // GREEDY REPAIR (fallback)
    // =========================================================================

    private void greedyRepair(List<ScheduleTask> tasks, SchedulingState state,
                               List<TimeSlot> slots, List<Room> rooms,
                               Map<Integer, Integer> maxHours,
                               List<ConflictReport> conflicts) {
        // Remove already-placed tasks (tracked by subject+class+type combos)
        Set<String> placedKeys = new HashSet<>();
        for (TimetableEntry e : state.getPlaced()) {
            placedKeys.add(e.getSubjectId() + ":" + e.getClassId() + ":"
                    + e.getSessionType() + ":" + Objects.toString(e.getBatchId(), "null"));
        }

        for (ScheduleTask task : tasks) {
            String key = task.getSubjectId() + ":" + task.getClassId() + ":"
                    + task.getSessionType() + ":" + task.getBatchId();
            // Skip duplicates that were already placed by backtracking
            // (count how many times this exact task type appears in placed)
            long alreadyPlaced = state.getPlaced().stream().filter(e ->
                    e.getSubjectId() == task.getSubjectId()
                 && e.getClassId()   == task.getClassId()
                 && e.getSessionType().equals(task.getSessionType())
                 && Objects.equals(e.getBatchId(), task.getBatchId())).count();

            long requiredCount = tasks.stream().filter(t ->
                    t.getSubjectId() == task.getSubjectId()
                 && t.getClassId()   == task.getClassId()
                 && t.getSessionType().equals(task.getSessionType())
                 && Objects.equals(t.getBatchId(), task.getBatchId())).count();

            if (alreadyPlaced >= requiredCount) continue;

            List<SlotRoomPair> candidates = getCandidates(task, state, slots, rooms, maxHours);
            boolean placed = false;
            for (SlotRoomPair c : candidates) {
                List<TimetableEntry> entries = placeTask(task, c, state);
                if (entries != null) { placed = true; break; }
            }
            if (!placed) {
                boolean alreadyConflicted = conflicts.stream().anyMatch(cf ->
                        cf.getSubjectId() == task.getSubjectId()
                     && cf.getClassId()   == task.getClassId()
                     && cf.getSessionType().equals(task.getSessionType()));
                if (!alreadyConflicted) {
                    conflicts.add(new ConflictReport(
                            task.getSessionType(), task.getSubjectId(), task.getSubjectName(),
                            task.getFacultyId(), "", task.getClassId(), "",
                            "Greedy: no valid slot/room found", false));
                }
            }
        }
    }

    // =========================================================================
    // CANDIDATE GENERATION + SCORING
    // =========================================================================

    private List<SlotRoomPair> getCandidates(ScheduleTask task, SchedulingState state,
                                              List<TimeSlot> slots, List<Room> rooms,
                                              Map<Integer, Integer> maxHours) {
        List<SlotRoomPair> candidates = new ArrayList<>();
        String roomTypeNeeded = "PR".equals(task.getSessionType()) ? "LAB" : "CLASSROOM";

        for (int i = 0; i < slots.size(); i++) {
            TimeSlot slot1 = slots.get(i);
            if (slot1.isBreak()) continue;

            if ("PR".equals(task.getSessionType())) {
                // Need 2 consecutive slots, same day, not straddling lunch
                if (i + 1 >= slots.size()) continue;
                TimeSlot slot2 = slots.get(i + 1);
                if (slot2.isBreak()) continue;
                if (!slot1.getDay().equals(slot2.getDay())) continue;
                if (slot2.getSlotNo() != slot1.getSlotNo() + 1) continue;
                // Don't straddle short or lunch break — if there's a break between them, skip
                boolean breakBetween = slots.stream().anyMatch(s ->
                        s.getDay().equals(slot1.getDay())
                     && s.getSlotNo() > slot1.getSlotNo()
                     && s.getSlotNo() < slot2.getSlotNo()
                     && s.isBreak());
                if (breakBetween) continue;

                // Check faculty + class availability for BOTH slots
                if (!state.isFacultyAvailable(task.getFacultyId(), slot1.getSlotId())) continue;
                if (!state.isFacultyAvailable(task.getFacultyId(), slot2.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), slot1.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), slot2.getSlotId())) continue;

                // Faculty max hours check
                int assigned = state.facultySessionCount(task.getFacultyId());
                int max = maxHours.getOrDefault(task.getFacultyId(), 20);
                if (assigned + 2 > max) continue;

                // Find suitable lab
                for (Room room : rooms) {
                    if (!"LAB".equals(room.getType())) continue;
                    if (!room.isActive()) continue;
                    if (room.getCapacity() < task.getClassStrength()) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot1.getSlotId())) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot2.getSlotId())) continue;

                    int score = softScore(task, slot1, state);
                    candidates.add(new SlotRoomPair(slot1, slot2, room, score));
                }
            } else {
                // Single slot (TH or TU)
                if (!state.isFacultyAvailable(task.getFacultyId(), slot1.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), slot1.getSlotId())) continue;

                int assigned = state.facultySessionCount(task.getFacultyId());
                int max = maxHours.getOrDefault(task.getFacultyId(), 20);
                if (assigned + 1 > max) continue;

                for (Room room : rooms) {
                    if ("LAB".equals(room.getType())) continue; // TH/TU don't go in labs
                    if (!room.isActive()) continue;
                    if (room.getCapacity() < task.getClassStrength()) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot1.getSlotId())) continue;

                    int score = softScore(task, slot1, state);
                    candidates.add(new SlotRoomPair(slot1, null, room, score));
                }
            }
        }

        // Higher score = better candidate
        candidates.sort(Comparator.comparingInt(SlotRoomPair::getScore).reversed());
        return candidates;
    }

    /**
     * Soft-constraint score. Higher = better placement.
     * <ul>
     *   <li>+10 if the subject hasn't appeared on this day yet (spread across days).</li>
     *   <li>+5 if afternoon slot for practicals.</li>
     *   <li>-5 if more than 2 consecutive sessions would result for the class.</li>
     *   <li>+2 for earlier slots (minimize gaps — implicit in ordering).</li>
     * </ul>
     */
    private int softScore(ScheduleTask task, TimeSlot slot, SchedulingState state) {
        int score = 50; // baseline

        // Spread subject across days
        if (state.subjectSessionsOnDay(task.getSubjectId(), task.getClassId(), slot.getDay()) == 0) {
            score += 10;
        }

        // Prefer afternoon for practicals (slot_no >= 7 i.e. after lunch)
        if ("PR".equals(task.getSessionType()) && slot.getSlotNo() >= 7) {
            score += 5;
        }

        // Penalise if this would create > 3 consecutive sessions
        int consecutive = state.consecutiveSessionsFor(task.getClassId(), slot.getDay(), slot.getSlotNo() - 1);
        if (consecutive >= 3) score -= 10;

        return score;
    }

    // =========================================================================
    // PLACEMENT
    // =========================================================================

    /**
     * Attempt to place a task on the given candidate.
     * Returns the list of TimetableEntry objects placed (1 for TH/TU, 2 for PR).
     * Returns null if any constraint is violated at place-time.
     */
    private List<TimetableEntry> placeTask(ScheduleTask task, SlotRoomPair candidate,
                                            SchedulingState state) {
        List<TimetableEntry> entries = new ArrayList<>();

        TimetableEntry e1 = buildEntry(task, candidate.getSlot1(), candidate.getRoom());

        // Final double-check constraints (state may have changed since candidates were scored)
        if (!state.isFacultyAvailable(task.getFacultyId(), e1.getSlotId())) return null;
        if (!state.isClassFree(task.getClassId(), e1.getSlotId())) return null;
        if (!state.isRoomFree(e1.getRoomId(), e1.getSlotId())) return null;

        entries.add(e1);
        state.occupy(e1);

        if ("PR".equals(task.getSessionType()) && candidate.getSlot2() != null) {
            TimetableEntry e2 = buildEntry(task, candidate.getSlot2(), candidate.getRoom());
            if (!state.isFacultyAvailable(task.getFacultyId(), e2.getSlotId())
             || !state.isClassFree(task.getClassId(), e2.getSlotId())
             || !state.isRoomFree(e2.getRoomId(), e2.getSlotId())) {
                // Undo e1
                state.release(e1);
                return null;
            }
            entries.add(e2);
            state.occupy(e2);
        }
        return entries;
    }

    private TimetableEntry buildEntry(ScheduleTask task, TimeSlot slot, Room room) {
        TimetableEntry te = new TimetableEntry();
        te.setClassId(task.getClassId());
        te.setBatchId(task.getBatchId());
        te.setSubjectId(task.getSubjectId());
        te.setFacultyId(task.getFacultyId());
        te.setRoomId(room.getRoomId());
        te.setSlotId(slot.getSlotId());
        te.setSessionType(task.getSessionType());
        te.setAcademicYear(task.getAcademicYear());
        te.setLocked(false);
        // display fields
        te.setSubjectAbbr(task.getSubjectAbbr());
        te.setDay(slot.getDay());
        te.setSlotNo(slot.getSlotNo());
        te.setStartTime(slot.getStartTime());
        te.setEndTime(slot.getEndTime());
        te.setRoomName(room.getName());
        return te;
    }

    // =========================================================================
    // PERSISTENCE (transaction)
    // =========================================================================

    private void persistWithTransaction(int classId, String academicYear,
                                         List<TimetableEntry> entries) {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            con.setAutoCommit(false);

            // Clear unlocked entries for this class
            timetableEntryDAO.deleteByClassWithConnection(classId, academicYear, con);

            // Insert all placed entries
            for (TimetableEntry te : entries) {
                timetableEntryDAO.insertWithConnection(te, con);
            }

            con.commit();
            notifyProgress("Timetable saved to database (" + entries.size() + " entries).");
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ignored) {}
            }
            throw new RuntimeException("Failed to persist timetable", e);
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); } catch (SQLException ignored) {}
                DBConnection.getInstance().release(con);
            }
        }
    }

    // =========================================================================
    // CONFLICT CHECKER (for manual edit validation)
    // =========================================================================

    /**
     * Check if a proposed move would create any clashes.
     * Returns a list of conflict descriptions (empty = no clashes).
     */
    public List<String> checkConflicts(TimetableEntry proposed, String academicYear) {
        List<String> clashes = new ArrayList<>();

        if (timetableEntryDAO.isFacultyBusy(proposed.getFacultyId(), proposed.getSlotId(), academicYear)) {
            clashes.add("Faculty is already assigned to another session in this slot.");
        }
        if (timetableEntryDAO.isClassBusy(proposed.getClassId(), proposed.getSlotId(), academicYear)) {
            clashes.add("This class already has a session in the selected slot.");
        }
        if (timetableEntryDAO.isRoomBusy(proposed.getRoomId(), proposed.getSlotId(), academicYear)) {
            clashes.add("Room is already in use in the selected slot.");
        }
        // Check faculty unavailability
        List<FacultyUnavailability> unavails = unavailDAO.findByFaculty(proposed.getFacultyId());
        boolean isUnavailable = unavails.stream()
                .anyMatch(u -> u.getSlotId() == proposed.getSlotId());
        if (isUnavailable) {
            clashes.add("Faculty has marked this slot as unavailable.");
        }
        return clashes;
    }

    // =========================================================================
    // INNER TYPES
    // =========================================================================

    /** Result of a generation run. */
    public static class GenerationResult {
        private final List<TimetableEntry>  placed;
        private final List<ConflictReport>  conflicts;

        public GenerationResult(List<TimetableEntry> placed, List<ConflictReport> conflicts) {
            this.placed    = Collections.unmodifiableList(placed);
            this.conflicts = Collections.unmodifiableList(conflicts);
        }

        public List<TimetableEntry> getPlaced()    { return placed; }
        public List<ConflictReport> getConflicts() { return conflicts; }
        public boolean isFullyPlaced()             { return conflicts.isEmpty(); }
    }

    /** Candidate (slot(s), room) pair with soft-constraint score. */
    private static class SlotRoomPair {
        private final TimeSlot slot1;
        private final TimeSlot slot2; // null for single-slot sessions
        private final Room     room;
        private final int      score;

        SlotRoomPair(TimeSlot slot1, TimeSlot slot2, Room room, int score) {
            this.slot1 = slot1; this.slot2 = slot2; this.room = room; this.score = score;
        }

        TimeSlot getSlot1() { return slot1; }
        TimeSlot getSlot2() { return slot2; }
        Room     getRoom()  { return room; }
        int      getScore() { return score; }

        int getRoomId() { return room.getRoomId(); }
    }
}

