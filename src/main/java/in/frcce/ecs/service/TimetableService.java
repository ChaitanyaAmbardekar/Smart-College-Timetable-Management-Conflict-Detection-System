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
    private static final long TIME_LIMIT_MS = 5_000;

    /** Max failed backtracking branches before restoring best partial placement and repairing. */
    private static final int MAX_BACKTRACK_FAILURES = 2_500;

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
        List<TimetableEntry>    otherClassEntries = timetableEntryDAO.findOtherClasses(classId, academicYear);
        List<TimetableEntry>    lockedSameClass = timetableEntryDAO.findByClass(classId, academicYear).stream()
                .filter(TimetableEntry::isLocked)
                .collect(Collectors.toList());

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

        // 2. Build task list (skip sessions already fulfilled by locked entries)
        notifyProgress("Building task list...");
        Map<Integer, Subject> subjectMap = new HashMap<>();
        for (FacultySubject fs : allocs) {
            subjectMap.computeIfAbsent(fs.getSubjectId(),
                    id -> subjectDAO.findById(id).orElse(null));
        }
        List<ScheduleTask> tasks = filterUnlockedTasks(
                buildTasks(allocs, cd, batches, academicYear), lockedSameClass, subjectMap);

        // 3. Init scheduling state (manual unavailability, locked same-class rows, + other classes/years)
        SchedulingState state = new SchedulingState();
        for (Map.Entry<Integer, Set<Integer>> e : unavailMap.entrySet()) {
            for (int slotId : e.getValue()) {
                state.markFacultyUnavailable(e.getKey(), slotId);
            }
        }
        if (!lockedSameClass.isEmpty()) {
            notifyProgress("Preserving " + lockedSameClass.size() + " locked entries for this class...");
            for (TimetableEntry locked : lockedSameClass) {
                state.occupy(locked);
            }
        }
        if (!otherClassEntries.isEmpty()) {
            notifyProgress("Respecting " + otherClassEntries.size()
                    + " existing slot commitments from other classes/years...");
            for (TimetableEntry existing : otherClassEntries) {
                state.preloadOtherClassEntry(existing);
            }
        }

        // 4. Schedule using availability-aware MRV + backtracking, falling back to greedy repair
        List<ConflictReport> conflicts = new ArrayList<>();
        scheduleTasks(tasks, state, slots, rooms, maxHours, conflicts);

        List<TimetableEntry> placed = new ArrayList<>(state.getPlaced());
        notifyProgress("Placement complete: " + placed.size() + " entries placed, "
                + conflicts.size() + " unresolved.");

        // 5. Persist in a transaction
        if (!placed.isEmpty()) {
            persistWithTransaction(classId, academicYear, placed);
        }

        return new GenerationResult(placed, conflicts);
    }

    /**
     * Orders tasks using priority + Minimum Remaining Values (fewest valid
     * candidate slots given current faculty/room availability) and schedules them
     * via bounded backtracking followed by greedy repair if incomplete.
     */
    void scheduleTasks(List<ScheduleTask> tasks, SchedulingState state,
                       List<TimeSlot> slots, List<Room> rooms,
                       Map<Integer, Integer> maxHours,
                       List<ConflictReport> conflicts) {
        Map<ScheduleTask, Integer> initialDomainSizes = new IdentityHashMap<>();
        Map<Integer, Integer> subjectMinDomainSizes = new HashMap<>();
        for (ScheduleTask task : tasks) {
            int size = getCandidates(task, state, slots, rooms, maxHours).size();
            initialDomainSizes.put(task, size);
            subjectMinDomainSizes.merge(task.getSubjectId(), size, Math::min);
        }

        // Order PR batch tasks batch-by-batch so Batch 1 establishes parallel windows
        // across days and subsequent batches rotate cleanly into those open windows.
        tasks.sort(Comparator.comparing(ScheduleTask::getPriority)
                .thenComparingInt(t -> isBatchPractical(t) ? t.getBatchId() : 0)
                .thenComparingInt(t -> subjectMinDomainSizes.getOrDefault(t.getSubjectId(), Integer.MAX_VALUE))
                .thenComparingInt(t -> initialDomainSizes.getOrDefault(t, Integer.MAX_VALUE))
                .thenComparingInt(ScheduleTask::getSubjectId)
                .thenComparingInt(t -> t.getBatchId() == null ? 0 : t.getBatchId()));

        int totalClassSlots = (int) slots.stream().filter(s -> !s.isBreak()).count();
        int[] remWholeClassSlotsSuffix = new int[tasks.size() + 1];
        for (int i = tasks.size() - 1; i >= 0; i--) {
            ScheduleTask t = tasks.get(i);
            int wholeSlots = isBatchPractical(t) ? 0 : t.getRequiredSlots();
            remWholeClassSlotsSuffix[i] = remWholeClassSlotsSuffix[i + 1] + wholeSlots;
        }
        int targetClassId = tasks.isEmpty() ? 0 : tasks.get(0).getClassId();

        notifyProgress("Total tasks to schedule: " + tasks.size());

        SearchContext ctx = new SearchContext(
                System.currentTimeMillis() + TIME_LIMIT_MS, MAX_BACKTRACK_FAILURES,
                targetClassId, totalClassSlots, remWholeClassSlotsSuffix, state.getPlaced());
        boolean success = backtrack(tasks, 0, state, slots, rooms, maxHours, ctx);

        if (!success) {
            notifyProgress("Backtracking could not place all sessions (best partial: "
                    + ctx.bestPlaced.size() + " entries) — switching to greedy repair...");
            restoreBestPartial(state, ctx.bestPlaced);
            greedyRepair(tasks, state, slots, rooms, maxHours, conflicts);
        }
    }

    private static List<ScheduleTask> filterUnlockedTasks(List<ScheduleTask> tasks,
                                                            List<TimetableEntry> lockedEntries,
                                                            Map<Integer, Subject> subjectMap) {
        if (lockedEntries.isEmpty()) return tasks;
        Map<String, Integer> lockedSlotsByKey = new HashMap<>();
        Map<String, Integer> totalLockedByKey = new HashMap<>();
        Map<Integer, Set<Integer>> wholeClassSubjectsBySlot = new HashMap<>();
        for (TimetableEntry te : lockedEntries) {
            String k = lockedKey(te.getSubjectId(), te.getSessionType(), te.getBatchId());
            lockedSlotsByKey.merge(k, 1, Integer::sum);
            totalLockedByKey.merge(k, 1, Integer::sum);
            if (te.getBatchId() == null) {
                wholeClassSubjectsBySlot
                        .computeIfAbsent(te.getSlotId(), ignored -> new HashSet<>())
                        .add(te.getSubjectId());
            }
        }
        Map<Integer, Set<Integer>> electiveGroups = buildElectiveGroups(wholeClassSubjectsBySlot);
        List<ScheduleTask> remaining = new ArrayList<>();
        for (ScheduleTask task : tasks) {
            String k = lockedKey(task.getSubjectId(), task.getSessionType(), task.getBatchId());
            Subject sub = subjectMap.get(task.getSubjectId());
            int targetSlots = weeklyTargetSlots(sub, task.getSessionType());
            if (targetSlots > 0 && totalLockedByKey.getOrDefault(k, 0) >= targetSlots) {
                continue; // Already fulfilled by locked entries (even with co-taught allocations)
            }
            if (targetSlots > 0 && task.getBatchId() != null) {
                Set<Integer> group = electiveGroups.get(task.getSubjectId());
                if (group != null && group.size() > 1) {
                    int groupLockedForBatch = 0;
                    for (int altSubjectId : group) {
                        groupLockedForBatch += totalLockedByKey.getOrDefault(
                                lockedKey(altSubjectId, task.getSessionType(), task.getBatchId()), 0);
                    }
                    if (groupLockedForBatch >= targetSlots) {
                        continue; // Batch already fulfilled by a peer subject in the same elective group
                    }
                }
            }
            int lockedAvail = lockedSlotsByKey.getOrDefault(k, 0);
            if (lockedAvail >= task.getRequiredSlots()) {
                lockedSlotsByKey.put(k, lockedAvail - task.getRequiredSlots());
            } else {
                remaining.add(task);
            }
        }
        return remaining;
    }

    /**
     * Infers mutually-exclusive elective subject groups from whole-class locked entries
     * that share the exact same timetable slot (e.g., PEC choices scheduled in parallel).
     */
    private static Map<Integer, Set<Integer>> buildElectiveGroups(Map<Integer, Set<Integer>> wholeClassSubjectsBySlot) {
        Map<Integer, Set<Integer>> groups = new HashMap<>();
        for (Set<Integer> coScheduled : wholeClassSubjectsBySlot.values()) {
            if (coScheduled.size() < 2) continue;
            Set<Integer> merged = new HashSet<>(coScheduled);
            for (int sid : coScheduled) {
                Set<Integer> existing = groups.get(sid);
                if (existing != null) merged.addAll(existing);
            }
            for (int sid : merged) {
                groups.put(sid, merged);
            }
        }
        return groups;
    }

    private static int weeklyTargetSlots(Subject sub, String sessionType) {
        if (sub == null || sessionType == null) return 0;
        switch (sessionType.toUpperCase()) {
            case "TH": return sub.getTheoryHrs();
            case "PR": return sub.getPracticalHrs();
            case "TU": return sub.getTutorialHrs();
            default:   return 0;
        }
    }

    private static String lockedKey(int subjectId, String sessionType, Integer batchId) {
        return subjectId + ":" + sessionType + ":" + (batchId == null ? 0 : batchId);
    }

    private static boolean isBatchPractical(ScheduleTask t) {
        return "PR".equals(t.getSessionType()) && t.getBatchId() != null && t.getBatchId() > 0;
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

            // Extract preferred rooms from allocation
            List<Integer> prefRooms = new ArrayList<>();
            if (fs.getPrefRoom1() != null) prefRooms.add(fs.getPrefRoom1());
            if (fs.getPrefRoom2() != null) prefRooms.add(fs.getPrefRoom2());
            if (fs.getPrefRoom3() != null) prefRooms.add(fs.getPrefRoom3());
            if (fs.getPrefRoom4() != null) prefRooms.add(fs.getPrefRoom4());
            if (fs.getPrefRoom5() != null) prefRooms.add(fs.getPrefRoom5());

            // Theory sessions
            if (("TH".equals(type) || "ALL".equals(type)) && sub.getTheoryHrs() > 0) {
                for (int i = 0; i < sub.getTheoryHrs(); i++) {
                    tasks.add(new ScheduleTask(
                            sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                            cd.getClassId(), null, fs.getFacultyId(),
                            "TH", 1, cd.getStrength(), academicYear, prefRooms));
                }
            }

            // Tutorial sessions
            if (("TU".equals(type) || "ALL".equals(type)) && sub.getTutorialHrs() > 0) {
                for (int i = 0; i < sub.getTutorialHrs(); i++) {
                    tasks.add(new ScheduleTask(
                            sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                            cd.getClassId(), null, fs.getFacultyId(),
                            "TU", 1, cd.getStrength(), academicYear, prefRooms));
                }
            }

            // Practical sessions — either batch-wise or as a whole class, 2 consecutive slots each
            if (("PR".equals(type) || "ALL".equals(type)) && sub.getPracticalHrs() > 0) {
                int blocks = sub.getPracticalHrs() / 2;
                if (blocks == 0) blocks = 1;
                boolean wholeClassPrac = fs.isWholeClassPractical()
                        || (fs.getPracMode() == null && sub.isWholeClassPractical());
                List<Batch> useBatches = (wholeClassPrac || batches.isEmpty())
                        ? List.of(new Batch(0, cd.getClassId(), "ALL", cd.getStrength()))
                        : batches;
                for (Batch batch : useBatches) {
                    for (int i = 0; i < blocks; i++) {
                        tasks.add(new ScheduleTask(
                                sub.getSubjectId(), sub.getName(), sub.getAbbreviation(),
                                cd.getClassId(), batch.getBatchId() == 0 ? null : batch.getBatchId(),
                                fs.getFacultyId(), "PR", 2,
                                batch.getStrength(), academicYear, prefRooms));
                    }
                }
            }
        }
        return tasks;
    }

    // =========================================================================
    // BACKTRACKING ENGINE
    // =========================================================================

    private static final class SearchContext {
        final long deadline;
        final int maxFailures;
        final int targetClassId;
        final int totalClassSlots;
        final int[] remWholeClassSlotsSuffix;
        int failures = 0;
        List<TimetableEntry> bestPlaced = new ArrayList<>();

        SearchContext(long deadline, int maxFailures, int targetClassId,
                      int totalClassSlots, int[] remWholeClassSlotsSuffix,
                      List<TimetableEntry> initialPlaced) {
            this.deadline = deadline;
            this.maxFailures = maxFailures;
            this.targetClassId = targetClassId;
            this.totalClassSlots = totalClassSlots;
            this.remWholeClassSlotsSuffix = remWholeClassSlotsSuffix;
            this.bestPlaced = new ArrayList<>(initialPlaced);
        }
    }

    private void restoreBestPartial(SchedulingState state, List<TimetableEntry> bestPlaced) {
        for (TimetableEntry existing : new ArrayList<>(state.getPlaced())) {
            state.release(existing);
        }
        for (TimetableEntry entry : bestPlaced) {
            state.occupy(entry);
        }
    }

    private boolean backtrack(List<ScheduleTask> tasks, int index,
                               SchedulingState state,
                               List<TimeSlot> slots, List<Room> rooms,
                               Map<Integer, Integer> maxHours,
                               SearchContext ctx) {
        if (state.getPlaced().size() > ctx.bestPlaced.size()) {
            ctx.bestPlaced = new ArrayList<>(state.getPlaced());
        }
        if (index == tasks.size()) return true;
        if (ctx.targetClassId > 0) {
            int freeClassSlots = ctx.totalClassSlots - state.distinctClassSlotsTotal(ctx.targetClassId);
            if (freeClassSlots < ctx.remWholeClassSlotsSuffix[index]) {
                ctx.failures++;
                return false;
            }
        }
        if (System.currentTimeMillis() > ctx.deadline || ctx.failures >= ctx.maxFailures) {
            return false;
        }

        ScheduleTask task = tasks.get(index);

        // Get candidate (slot, room) pairs, ordered by soft-constraint score
        List<SlotRoomPair> candidates = getCandidates(task, state, slots, rooms, maxHours);

        for (SlotRoomPair candidate : candidates) {
            List<TimetableEntry> placed = placeTask(task, candidate, state);
            if (placed == null) continue; // constraint violated

            boolean result = backtrack(tasks, index + 1, state, slots, rooms, maxHours, ctx);
            if (result) return true;

            // Undo placement
            placed.forEach(state::release);

            if (System.currentTimeMillis() > ctx.deadline || ctx.failures >= ctx.maxFailures) {
                return false;
            }
        }

        ctx.failures++;
        return false;
    }

    // =========================================================================
    // GREEDY REPAIR (fallback)
    // =========================================================================

    private void greedyRepair(List<ScheduleTask> tasks, SchedulingState state,
                               List<TimeSlot> slots, List<Room> rooms,
                               Map<Integer, Integer> maxHours,
                               List<ConflictReport> conflicts) {
        for (ScheduleTask task : tasks) {
            int alreadyPlacedSlots = (int) state.getPlaced().stream().filter(e ->
                    e.getSubjectId() == task.getSubjectId()
                 && e.getClassId()   == task.getClassId()
                 && e.getFacultyId() == task.getFacultyId()
                 && e.getSessionType().equals(task.getSessionType())
                 && Objects.equals(e.getBatchId(), task.getBatchId())).count();

            int requiredSlots = tasks.stream().filter(t ->
                    t.getSubjectId() == task.getSubjectId()
                 && t.getClassId()   == task.getClassId()
                 && t.getFacultyId() == task.getFacultyId()
                 && t.getSessionType().equals(task.getSessionType())
                 && Objects.equals(t.getBatchId(), task.getBatchId()))
                    .mapToInt(ScheduleTask::getRequiredSlots)
                    .sum();

            if (alreadyPlacedSlots >= requiredSlots) continue;

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
                     && cf.getFacultyId() == task.getFacultyId()
                     && cf.getSessionType().equals(task.getSessionType()));
                if (!alreadyConflicted) {
                    conflicts.add(new ConflictReport(
                            task.getSessionType(), task.getSubjectId(), task.getSubjectName(),
                            task.getFacultyId(), "", task.getClassId(), "",
                            buildConflictReason(task, state, slots, rooms, maxHours), false));
                }
            }
        }
    }

    private String buildConflictReason(ScheduleTask task, SchedulingState state,
                                       List<TimeSlot> slots, List<Room> rooms,
                                       Map<Integer, Integer> maxHours) {
        int assigned = state.facultySessionCount(task.getFacultyId());
        int max = maxHours.getOrDefault(task.getFacultyId(), 20);
        if (assigned + task.getRequiredSlots() > max) {
            return "Faculty weekly hour limit reached (" + assigned + "/" + max
                    + " hrs used across classes; needs " + task.getRequiredSlots() + " more)";
        }

        boolean facultyHasSlot = false;
        boolean facultyAndClassHaveSlot = false;
        boolean dailyRuleAllowsSlot = false;

        if ("PR".equals(task.getSessionType())) {
            for (int i = 0; i + 1 < slots.size(); i++) {
                TimeSlot s1 = slots.get(i);
                TimeSlot s2 = slots.get(i + 1);
                if (s1.isBreak() || s2.isBreak()) continue;
                if (!s1.getDay().equals(s2.getDay()) || s2.getSlotNo() != s1.getSlotNo() + 1) continue;
                boolean breakBetween = slots.stream().anyMatch(s ->
                        s.getDay().equals(s1.getDay())
                     && s.getSlotNo() > s1.getSlotNo()
                     && s.getSlotNo() < s2.getSlotNo()
                     && s.isBreak());
                if (breakBetween) continue;
                if (!state.isFacultyAvailable(task.getFacultyId(), s1.getSlotId())
                        || !state.isFacultyAvailable(task.getFacultyId(), s2.getSlotId())) continue;
                facultyHasSlot = true;
                if (!state.isClassFree(task.getClassId(), task.getBatchId(), s1.getSlotId())
                        || !state.isClassFree(task.getClassId(), task.getBatchId(), s2.getSlotId())) continue;
                facultyAndClassHaveSlot = true;
                if (state.canScheduleCourseOnDay(task.getSubjectId(), task.getClassId(), s1.getDay(), "PR", 2)
                        && state.canBatchPerformPracticalOnDay(
                                task.getClassId(), task.getBatchId(), task.getSubjectId(), s1.getDay())) {
                    dailyRuleAllowsSlot = true;
                }
            }
        } else {
            for (TimeSlot s1 : slots) {
                if (s1.isBreak()) continue;
                if (!state.isFacultyAvailable(task.getFacultyId(), s1.getSlotId())) continue;
                facultyHasSlot = true;
                if (!state.isClassFree(task.getClassId(), task.getBatchId(), s1.getSlotId())) continue;
                facultyAndClassHaveSlot = true;
                if (state.canScheduleCourseOnDay(
                        task.getSubjectId(), task.getClassId(), s1.getDay(), task.getSessionType(), 1)) {
                    dailyRuleAllowsSlot = true;
                }
            }
        }

        if (!facultyHasSlot) {
            return "Faculty unavailable or busy in another class/year across all valid slots";
        }
        if (!facultyAndClassHaveSlot) {
            return "No common free slot where both faculty (including other-class duties) and class/batch are free";
        }
        if (!dailyRuleAllowsSlot) {
            return "Daily course or batch practical limit prevents placement in remaining free slots";
        }
        return "No active room/lab of sufficient capacity is free in remaining faculty/class slots";
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

                // Enforce daily course limit: max 2 practical lec slots of same course per day
                if (!state.canScheduleCourseOnDay(task.getSubjectId(), task.getClassId(), slot1.getDay(), "PR", 2)) {
                    continue;
                }
                // Enforce per-batch daily practical limit: each batch can perform practical for only 1 course per day
                if (!state.canBatchPerformPracticalOnDay(task.getClassId(), task.getBatchId(), task.getSubjectId(), slot1.getDay())) {
                    continue;
                }

                // Check faculty + class/batch availability for BOTH slots
                if (!state.isFacultyAvailable(task.getFacultyId(), slot1.getSlotId())) continue;
                if (!state.isFacultyAvailable(task.getFacultyId(), slot2.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), task.getBatchId(), slot1.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), task.getBatchId(), slot2.getSlotId())) continue;

                // Faculty max hours check
                int assigned = state.facultySessionCount(task.getFacultyId());
                int max = maxHours.getOrDefault(task.getFacultyId(), 20);
                if (assigned + 2 > max) continue;

                // Find suitable room: labs for batch practicals; any sufficiently large room/classroom for whole-class practicals
                boolean batchPractical = task.getBatchId() != null && task.getBatchId() > 0;
                int freeLabsInWindow = batchPractical
                        ? countFreeLabs(slot1, slot2, state, rooms, task.getClassStrength())
                        : 0;
                for (Room room : rooms) {
                    if (batchPractical && !"LAB".equals(room.getType())) continue;
                    if (!room.isActive()) continue;
                    if (room.getCapacity() < task.getClassStrength()) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot1.getSlotId())) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot2.getSlotId())) continue;

                    int score = softScore(task, slot1, room, state, freeLabsInWindow);
                    candidates.add(new SlotRoomPair(slot1, slot2, room, score));
                }
            } else {
                // Enforce daily course limit: max 1 theory lecture of same course per day
                if (!state.canScheduleCourseOnDay(task.getSubjectId(), task.getClassId(), slot1.getDay(), task.getSessionType(), 1)) {
                    continue;
                }

                // Single slot (TH or TU)
                if (!state.isFacultyAvailable(task.getFacultyId(), slot1.getSlotId())) continue;
                if (!state.isClassFree(task.getClassId(), task.getBatchId(), slot1.getSlotId())) continue;

                int assigned = state.facultySessionCount(task.getFacultyId());
                int max = maxHours.getOrDefault(task.getFacultyId(), 20);
                if (assigned + 1 > max) continue;

                for (Room room : rooms) {
                    if ("LAB".equals(room.getType())) continue; // TH/TU don't go in labs
                    if (!room.isActive()) continue;
                    if (room.getCapacity() < task.getClassStrength()) continue;
                    if (!state.isRoomFree(room.getRoomId(), slot1.getSlotId())) continue;

                    int score = softScore(task, slot1, room, state, 0);
                    candidates.add(new SlotRoomPair(slot1, null, room, score));
                }
            }
        }

        // Higher score = better candidate
        candidates.sort(Comparator.comparingInt(SlotRoomPair::getScore).reversed());
        return candidates;
    }

    private static int countFreeLabs(TimeSlot slot1, TimeSlot slot2, SchedulingState state,
                                     List<Room> rooms, int minCapacity) {
        int count = 0;
        for (Room r : rooms) {
            if ("LAB".equals(r.getType()) && r.isActive() && r.getCapacity() >= minCapacity
                    && state.isRoomFree(r.getRoomId(), slot1.getSlotId())
                    && state.isRoomFree(r.getRoomId(), slot2.getSlotId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Soft-constraint score. Higher = better placement.
     * <ul>
     *   <li>Preferred room match: Choice 1 = +50, Choice 2 = +40, Choice 3 = +30, Choice 4 = +20, Choice 5 = +10.</li>
     *   <li>+10 if the subject hasn't appeared on this day yet (spread across days).</li>
     *   <li>+15 if afternoon slot for practicals.</li>
     *   <li>Parallel batch packing and multi-lab window availability for batch PRs.</li>
     *   <li>-10 if more than 2 consecutive sessions would result for the class.</li>
     * </ul>
     */
    private int softScore(ScheduleTask task, TimeSlot slot, Room room,
                          SchedulingState state, int freeLabsInWindow) {
        int score = 50; // baseline

        // Preferred room priority scoring
        List<Integer> prefs = task.getPreferredRoomIds();
        if (prefs != null && !prefs.isEmpty()) {
            int prefIndex = prefs.indexOf(room.getRoomId());
            if (prefIndex >= 0) {
                // Choice 0 -> +50, Choice 1 -> +40, Choice 2 -> +30, Choice 3 -> +20, Choice 4 -> +10
                score += (50 - (prefIndex * 10));
            }
        }

        // Spread subject across days
        if (state.subjectSessionsOnDay(task.getSubjectId(), task.getClassId(), slot.getDay()) == 0) {
            score += 10;
        }

        // Prefer afternoon for practicals (slot_no >= 7 i.e. after lunch)
        if ("PR".equals(task.getSessionType()) && slot.getSlotNo() >= 7) {
            score += 15;
        }

        // For batch practicals, strongly prefer filling existing parallel batch windows first
        // and opening new batch windows only where multiple labs are free concurrently.
        boolean batchPr = isBatchPractical(task);
        int activeBatchesHere = batchPr
                ? state.batchPracticalCountAtSlot(task.getClassId(), slot.getSlotId())
                : 0;
        boolean joiningParallelBatchWindow = activeBatchesHere > 0;

        if ("PR".equals(task.getSessionType())) {
            if (joiningParallelBatchWindow) {
                score += 200 - (activeBatchesHere * 5);
            } else {
                if (batchPr) {
                    if (freeLabsInWindow >= 4) {
                        score += 60;
                    } else {
                        score -= (4 - freeLabsInWindow) * 50;
                    }
                }
                int prWindowsOnDay = state.practicalWindowsOnDay(task.getClassId(), slot.getDay());
                score -= prWindowsOnDay * 60;
            }
        }

        // Balance total class sessions across days so remaining free slots stay spread out
        if (!joiningParallelBatchWindow) {
            score -= state.distinctClassSlotsOnDay(task.getClassId(), slot.getDay()) * 4;
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
        if (!state.isClassFree(task.getClassId(), task.getBatchId(), e1.getSlotId())) return null;
        if (!state.isRoomFree(e1.getRoomId(), e1.getSlotId())) return null;
        if (!state.canScheduleCourseOnDay(task.getSubjectId(), task.getClassId(),
                e1.getDay(), task.getSessionType(), task.getRequiredSlots())) return null;
        if ("PR".equals(task.getSessionType())
                && !state.canBatchPerformPracticalOnDay(task.getClassId(), task.getBatchId(),
                task.getSubjectId(), e1.getDay())) return null;

        entries.add(e1);
        state.occupy(e1);

        if ("PR".equals(task.getSessionType()) && candidate.getSlot2() != null) {
            TimetableEntry e2 = buildEntry(task, candidate.getSlot2(), candidate.getRoom());
            if (!state.isFacultyAvailable(task.getFacultyId(), e2.getSlotId())
             || !state.isClassFree(task.getClassId(), task.getBatchId(), e2.getSlotId())
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
            timetableEntryDAO.ensureBatchSlotSchema(con);
            con.setAutoCommit(false);

            // Clear unlocked entries for this class
            timetableEntryDAO.deleteByClassWithConnection(classId, academicYear, con);

            // Insert newly generated (non-locked) entries
            int inserted = 0;
            for (TimetableEntry te : entries) {
                if (!te.isLocked()) {
                    timetableEntryDAO.insertWithConnection(te, con);
                    inserted++;
                }
            }

            con.commit();
            notifyProgress("Timetable saved to database (" + inserted + " generated, "
                    + (entries.size() - inserted) + " locked preserved).");
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
        int excludeId = proposed.getEntryId();

        if (timetableEntryDAO.isFacultyBusy(proposed.getFacultyId(), proposed.getSlotId(), academicYear, excludeId)) {
            clashes.add("Faculty is already assigned to another session in this slot.");
        }
        if (timetableEntryDAO.isClassOrBatchBusy(proposed.getClassId(), proposed.getBatchId(),
                proposed.getSlotId(), academicYear, excludeId)) {
            clashes.add("This class or batch already has a session in the selected slot.");
        }
        if (timetableEntryDAO.isRoomBusy(proposed.getRoomId(), proposed.getSlotId(), academicYear, excludeId)) {
            clashes.add("Room is already in use in the selected slot.");
        }
        // Check faculty unavailability
        List<FacultyUnavailability> unavails = unavailDAO.findByFaculty(proposed.getFacultyId());
        boolean isUnavailable = unavails.stream()
                .anyMatch(u -> u.getSlotId() == proposed.getSlotId());
        if (isUnavailable) {
            clashes.add("Faculty has marked this slot as unavailable.");
        }

        // Enforce daily course limits (max 1 TH, max 2 PR of the same course per day)
        // and per-batch daily practical limit (each batch can perform PR for only 1 course per day)
        String targetDay = proposed.getDay();
        if (targetDay == null) {
            targetDay = timeSlotDAO.findAllIncBreaks().stream()
                    .filter(ts -> ts.getSlotId() == proposed.getSlotId())
                    .map(TimeSlot::getDay)
                    .findFirst()
                    .orElse(null);
        }
        if (targetDay != null && proposed.getSubjectId() > 0) {
            final String dayToCheck = targetDay;
            List<TimetableEntry> classEntries = timetableEntryDAO.findByClass(proposed.getClassId(), academicYear);
            long sameCourseOnDay = classEntries.stream()
                    .filter(e -> e.getEntryId() != proposed.getEntryId())
                    .filter(e -> e.getSubjectId() == proposed.getSubjectId())
                    .filter(e -> dayToCheck.equalsIgnoreCase(e.getDay()))
                    .filter(e -> proposed.getSessionType().equalsIgnoreCase(e.getSessionType()))
                    .count();
            if ("TH".equalsIgnoreCase(proposed.getSessionType()) && sameCourseOnDay >= 1) {
                clashes.add("Cannot schedule more than 1 theory lecture of the same course on " + dayToCheck + ".");
            } else if ("PR".equalsIgnoreCase(proposed.getSessionType()) && sameCourseOnDay >= 2) {
                clashes.add("Cannot schedule more than 2 practical lectures of the same course on " + dayToCheck + ".");
            }

            if ("PR".equalsIgnoreCase(proposed.getSessionType())
                    && proposed.getBatchId() != null && proposed.getBatchId() > 0) {
                boolean batchHasOtherCoursePR = classEntries.stream()
                        .filter(e -> e.getEntryId() != proposed.getEntryId())
                        .filter(e -> Objects.equals(e.getBatchId(), proposed.getBatchId()))
                        .filter(e -> dayToCheck.equalsIgnoreCase(e.getDay()))
                        .filter(e -> "PR".equalsIgnoreCase(e.getSessionType()))
                        .anyMatch(e -> e.getSubjectId() != proposed.getSubjectId());
                if (batchHasOtherCoursePR) {
                    String batchLabel = proposed.getBatchName() != null && !proposed.getBatchName().isEmpty()
                            ? "Batch " + proposed.getBatchName() : "This batch";
                    clashes.add(batchLabel + " can perform practical for only one course per day ("
                            + dayToCheck + " already has another practical course).");
                }
            }
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

