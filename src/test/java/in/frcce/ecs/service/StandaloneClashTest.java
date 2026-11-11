package in.frcce.ecs.service;

import in.frcce.ecs.model.ConflictReport;
import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.model.Room;
import in.frcce.ecs.model.Subject;
import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.model.TimetableEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standalone test runner that validates all hard constraints and clash detection
 * without needing external JUnit dependencies.
 */
public class StandaloneClashTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("Running ECS Timetable Clash Detection Tests...");
        System.out.println("=============================================================");

        testFacultyClash();
        testClassClash();
        testRoomClash();
        testFacultyUnavailability();
        testReleaseRestoresAvailability();
        testWorkloadAndConsecutiveSessions();
        testDailyCourseLimits();
        testBatchDailyPracticalAndParallelRules();
        testPracticalDeliveryModeModel();
        testCrossYearFacultyAndRoomAvailability();
        testCrossYearLabConflictPrevention();

        System.out.println("=============================================================");
        System.out.printf("Test Results: %d passed, %d failed%n", passed, failed);
        System.out.println("=============================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFacultyClash() {
        SchedulingState state = new SchedulingState();
        int facultyId = 3; // Prof. Vaibhav Godbole
        int slotId = 1;    // Monday Slot 1

        check("Faculty initially free", state.isFacultyAvailable(facultyId, slotId));

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(facultyId);
        entry1.setClassId(1);
        entry1.setRoomId(101);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(3);
        entry1.setSessionType("TH");

        state.occupy(entry1);

        check("Faculty blocked in same slot (Double-booking prevention)", !state.isFacultyAvailable(facultyId, slotId));
        check("Faculty still available in another slot", state.isFacultyAvailable(facultyId, 2));
    }

    private static void testClassClash() {
        SchedulingState state = new SchedulingState();
        int classId = 1;
        int slotId = 4;

        check("Class initially free", state.isClassFree(classId, slotId));

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(1);
        entry1.setClassId(classId);
        entry1.setRoomId(1);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(1);
        entry1.setSessionType("TH");

        state.occupy(entry1);

        check("Class busy in same slot (No overlapping class sessions)", !state.isClassFree(classId, slotId));
    }

    private static void testRoomClash() {
        SchedulingState state = new SchedulingState();
        int roomId = 5; // ECS Lab 1
        int slotId = 7; // Monday afternoon

        check("Room initially free", state.isRoomFree(roomId, slotId));

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(4);
        entry1.setClassId(1);
        entry1.setRoomId(roomId);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(4);
        entry1.setSessionType("PR");

        state.occupy(entry1);

        check("Room busy in same slot (No double-booking rooms)", !state.isRoomFree(roomId, slotId));
    }

    private static void testFacultyUnavailability() {
        SchedulingState state = new SchedulingState();
        int facultyId = 2; // Dr. D.V. Bhoir
        int slotId = 10;   // Tuesday 8:45 AM

        state.markFacultyUnavailable(facultyId, slotId);

        check("Faculty blocked when marked unavailable", !state.isFacultyAvailable(facultyId, slotId));
        check("Faculty available in unblocked slots", state.isFacultyAvailable(facultyId, 11));
    }

    private static void testReleaseRestoresAvailability() {
        SchedulingState state = new SchedulingState();
        int facultyId = 5;
        int classId = 1;
        int roomId = 307;
        int slotId = 2;

        TimetableEntry entry = new TimetableEntry();
        entry.setFacultyId(facultyId);
        entry.setClassId(classId);
        entry.setRoomId(roomId);
        entry.setSlotId(slotId);

        state.occupy(entry);
        check("Occupied slot correctly blocked", !state.isFacultyAvailable(facultyId, slotId) && !state.isClassFree(classId, slotId));

        state.release(entry);
        check("Releasing restores faculty availability", state.isFacultyAvailable(facultyId, slotId));
        check("Releasing restores class availability", state.isClassFree(classId, slotId));
        check("Releasing restores room availability", state.isRoomFree(roomId, slotId));
    }

    private static void testWorkloadAndConsecutiveSessions() {
        SchedulingState state = new SchedulingState();
        int facultyId = 3;
        int classId = 1;

        check("Initial faculty sessions is 0", state.facultySessionCount(facultyId) == 0);

        TimetableEntry e1 = new TimetableEntry();
        e1.setFacultyId(facultyId);
        e1.setClassId(classId);
        e1.setRoomId(1);
        e1.setSlotId(1);
        e1.setDay("MONDAY");
        e1.setSlotNo(1);
        state.occupy(e1);

        TimetableEntry e2 = new TimetableEntry();
        e2.setFacultyId(facultyId);
        e2.setClassId(classId);
        e2.setRoomId(1);
        e2.setSlotId(2);
        e2.setDay("MONDAY");
        e2.setSlotNo(2);
        state.occupy(e2);

        check("Faculty session count updated to 2", state.facultySessionCount(facultyId) == 2);
        check("Consecutive session count detected as 2", state.consecutiveSessionsFor(classId, "MONDAY", 2) == 2);
    }

    private static void testDailyCourseLimits() {
        SchedulingState state = new SchedulingState();
        int classId = 1;
        int subjectId = 1; // EDA

        // Theory limit: max 1 TH per day
        check("Initially allows 1st TH on MONDAY", state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "TH", 1));
        TimetableEntry th1 = new TimetableEntry();
        th1.setClassId(classId);
        th1.setSubjectId(subjectId);
        th1.setFacultyId(2);
        th1.setRoomId(1);
        th1.setSlotId(1);
        th1.setDay("MONDAY");
        th1.setSessionType("TH");
        state.occupy(th1);

        check("Blocks 2nd TH of same course on MONDAY", !state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "TH", 1));
        check("Allows TH of same course on TUESDAY", state.canScheduleCourseOnDay(subjectId, classId, "TUESDAY", "TH", 1));

        // Practical limit: max 2 PR slots per day
        check("Initially allows 2-slot PR block on MONDAY", state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "PR", 2));
        TimetableEntry pr1 = new TimetableEntry();
        pr1.setClassId(classId);
        pr1.setSubjectId(subjectId);
        pr1.setFacultyId(2);
        pr1.setRoomId(5);
        pr1.setSlotId(4);
        pr1.setDay("MONDAY");
        pr1.setSessionType("PR");
        state.occupy(pr1);

        TimetableEntry pr2 = new TimetableEntry();
        pr2.setClassId(classId);
        pr2.setSubjectId(subjectId);
        pr2.setFacultyId(2);
        pr2.setRoomId(5);
        pr2.setSlotId(5);
        pr2.setDay("MONDAY");
        pr2.setSessionType("PR");
        state.occupy(pr2);

        check("Blocks additional PR block (>2 PR slots) of same course on MONDAY", !state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "PR", 2));
        check("Allows PR block of same course on WEDNESDAY", state.canScheduleCourseOnDay(subjectId, classId, "WEDNESDAY", "PR", 2));
    }

    private static void testBatchDailyPracticalAndParallelRules() {
        SchedulingState state = new SchedulingState();
        int classId = 1;

        TimetableEntry batchAPr = new TimetableEntry();
        batchAPr.setClassId(classId);
        batchAPr.setBatchId(1); // Batch A
        batchAPr.setSubjectId(1); // EDA
        batchAPr.setFacultyId(2);
        batchAPr.setRoomId(5);
        batchAPr.setSlotId(1);
        batchAPr.setDay("MONDAY");
        batchAPr.setSessionType("PR");
        state.occupy(batchAPr);

        check("Parallel Batch B allowed in same slot", state.isClassFree(classId, 2, 1));
        check("Same Batch A blocked in same slot", !state.isClassFree(classId, 1, 1));
        check("Whole-class TH blocked in batch PR slot", !state.isClassFree(classId, null, 1));
        check("Batch A blocked from 2nd PR course on MONDAY",
                !state.canBatchPerformPracticalOnDay(classId, 1, 2, "MONDAY"));
        check("Batch B allowed to perform PR for COA on MONDAY",
                state.canBatchPerformPracticalOnDay(classId, 2, 2, "MONDAY"));
        check("Batch A allowed to perform PR for COA on TUESDAY",
                state.canBatchPerformPracticalOnDay(classId, 1, 2, "TUESDAY"));

        TimetableEntry batchBPr = new TimetableEntry();
        batchBPr.setClassId(classId);
        batchBPr.setBatchId(2); // Batch B
        batchBPr.setSubjectId(2); // COA
        batchBPr.setFacultyId(7);
        batchBPr.setRoomId(6);
        batchBPr.setSlotId(1);
        batchBPr.setDay("MONDAY");
        batchBPr.setSessionType("PR");
        state.occupy(batchBPr);

        check("Same Batch B now blocked in same slot", !state.isClassFree(classId, 2, 1));
        state.release(batchBPr);
        check("Releasing Batch B frees Batch B slot while Batch A stays occupied",
                state.isClassFree(classId, 2, 1) && !state.isClassFree(classId, null, 1));
    }

    private static void testPracticalDeliveryModeModel() {
        Subject sub = new Subject();
        check("Subject defaults practical mode to BATCH", !sub.isWholeClassPractical());
        sub.setPracMode("WHOLE_CLASS");
        check("Subject recognizes WHOLE_CLASS practical mode", sub.isWholeClassPractical());

        FacultySubject fs = new FacultySubject();
        check("Allocation defaults practical mode to BATCH", !fs.isWholeClassPractical());
        fs.setPracMode("WHOLE_CLASS");
        check("Allocation recognizes WHOLE_CLASS practical mode", fs.isWholeClassPractical());
    }

    private static void testCrossYearFacultyAndRoomAvailability() {
        SchedulingState state = new SchedulingState();

        // Simulate existing 3rd-year Sem 5 sessions in slots 1 and 2
        TimetableEntry ty1 = new TimetableEntry();
        ty1.setClassId(3);
        ty1.setFacultyId(3);
        ty1.setRoomId(1);
        ty1.setSlotId(1);
        ty1.setDay("MONDAY");
        ty1.setSessionType("TH");
        state.preloadOtherClassEntry(ty1);

        TimetableEntry ty2 = new TimetableEntry();
        ty2.setClassId(3);
        ty2.setFacultyId(3);
        ty2.setRoomId(5);
        ty2.setSlotId(2);
        ty2.setDay("MONDAY");
        ty2.setSessionType("PR");
        state.preloadOtherClassEntry(ty2);

        check("Other-class preload blocks faculty in slot 1", !state.isFacultyAvailable(3, 1));
        check("Other-class preload blocks room 1 in slot 1", !state.isRoomFree(1, 1));
        check("Other-class preload leaves target 2nd-year class free in slot 1", state.isClassFree(1, null, 1));
        check("Other-class preload counts toward faculty weekly hours", state.facultySessionCount(3) == 2);
        check("Other-class preload does not pollute target class placed entries", state.getPlaced().isEmpty());

        // End-to-end TimetableService.scheduleTasks avoids busy 3rd-year slots 1 & 2
        List<TimeSlot> slots = List.of(
                new TimeSlot(1, "MONDAY", "09:00", "10:00", 1, false),
                new TimeSlot(2, "MONDAY", "10:00", "11:00", 2, false),
                new TimeSlot(3, "MONDAY", "11:15", "12:15", 3, false),
                new TimeSlot(4, "TUESDAY", "09:00", "10:00", 1, false)
        );
        List<Room> rooms = List.of(
                new Room(1, "302", "CLASSROOM", 70, "3rd Floor", true),
                new Room(2, "303", "CLASSROOM", 70, "3rd Floor", true)
        );
        List<ScheduleTask> tasks = new ArrayList<>();
        tasks.add(new ScheduleTask(10, "DBMS", "DBMS", 1, null, 3, "TH", 1, 60, "2026-27", List.of(1)));
        tasks.add(new ScheduleTask(10, "DBMS", "DBMS", 1, null, 3, "TH", 1, 60, "2026-27", List.of(1)));

        TimetableService service = new TimetableService();
        List<ConflictReport> conflicts = new ArrayList<>();
        service.scheduleTasks(tasks, state, slots, rooms, Map.of(3, 4), conflicts);

        check("Cross-year scheduleTasks places both TH sessions without conflicts",
                conflicts.isEmpty() && state.getPlaced().size() == 2);
        boolean avoidedBusySlots = state.getPlaced().stream()
                .noneMatch(e -> e.getSlotId() == 1 || e.getSlotId() == 2);
        check("Cross-year scheduleTasks avoids faculty 3's busy 3rd-year slots (1 & 2)", avoidedBusySlots);

        // Weekly hour limit with partial retention when external + new hours exceed maxHours
        SchedulingState tightState = new SchedulingState();
        tightState.preloadOtherClassEntry(ty1);
        tightState.preloadOtherClassEntry(ty2); // 2 external hours used
        List<ScheduleTask> tightTasks = new ArrayList<>();
        tightTasks.add(new ScheduleTask(10, "DBMS", "DBMS", 1, null, 3, "TH", 1, 60, "2026-27", List.of(1)));
        tightTasks.add(new ScheduleTask(10, "DBMS", "DBMS", 1, null, 3, "TH", 1, 60, "2026-27", List.of(1)));
        List<ConflictReport> tightConflicts = new ArrayList<>();
        service.scheduleTasks(tightTasks, tightState, slots, rooms, Map.of(3, 3), tightConflicts);

        check("Partial best placement keeps 1 placed session instead of wiping all on max-hours limit",
                tightState.getPlaced().size() == 1);
        check("Unplaced second session reports weekly hour limit reason",
                tightConflicts.size() == 1 && tightConflicts.get(0).getReason().contains("weekly hour limit"));
    }

    private static void testCrossYearLabConflictPrevention() {
        List<TimeSlot> slots = List.of(
                new TimeSlot(1, "MONDAY", "08:45:00", "09:45:00", 1, false),
                new TimeSlot(2, "MONDAY", "09:45:00", "10:45:00", 2, false),
                new TimeSlot(3, "MONDAY", "10:45:00", "11:00:00", 3, true),
                new TimeSlot(4, "MONDAY", "11:00:00", "12:00:00", 4, false),
                new TimeSlot(5, "MONDAY", "12:00:00", "13:00:00", 5, false)
        );
        List<Room> labs = List.of(
                new Room(5, "301", "LAB", 30, "3rd Floor", true),
                new Room(6, "309", "LAB", 30, "3rd Floor", true),
                new Room(7, "310", "LAB", 30, "3rd Floor", true),
                new Room(8, "311", "LAB", 30, "3rd Floor", true),
                new Room(9, "312", "LAB", 30, "3rd Floor", true)
        );
        List<Integer> prefLabs = List.of(5, 6, 7, 8, 9);

        // 1. Preload TE (classId = 3) occupying labs 301, 309, 310, 311 in slots 4 & 5
        List<TimetableEntry> teLabs = new ArrayList<>();
        int[] teFacs = {14, 18, 20, 17};
        for (int b = 0; b < 4; b++) {
            for (int slotId : new int[]{4, 5}) {
                TimetableEntry te = new TimetableEntry();
                te.setClassId(3);
                te.setBatchId(9 + b);
                te.setSubjectId(24 + b);
                te.setFacultyId(teFacs[b]);
                te.setRoomId(5 + b); // 301..311
                te.setSlotId(slotId);
                te.setDay("MONDAY");
                te.setSlotNo(slotId);
                te.setSessionType("PR");
                teLabs.add(te);
            }
        }

        SchedulingState seState = new SchedulingState();
        for (TimetableEntry te : teLabs) seState.preloadOtherClassEntry(te);

        List<ScheduleTask> seTasks = new ArrayList<>();
        int[] seFacs = {2, 7, 3, 4};
        for (int b = 0; b < 4; b++) {
            seTasks.add(new ScheduleTask(1 + b, "SE Lab " + (b + 1), "SL" + (b + 1),
                    1, 1 + b, seFacs[b], "PR", 2, 16, "2026-27", prefLabs));
        }

        TimetableService service = new TimetableService();
        List<ConflictReport> seConflicts = new ArrayList<>();
        service.scheduleTasks(seTasks, seState, slots, labs,
                Map.of(2, 20, 7, 20, 3, 20, 4, 20), seConflicts);

        Set<String> teRoomSlots = new HashSet<>();
        for (TimetableEntry te : teLabs) {
            teRoomSlots.add(te.getRoomId() + ":" + te.getSlotId());
        }
        boolean seNoLabClash = seState.getPlaced().stream()
                .noneMatch(e -> teRoomSlots.contains(e.getRoomId() + ":" + e.getSlotId()));
        check("SE lab generation places all 4 parallel batch practicals without conflicts",
                seConflicts.isEmpty() && seState.getPlaced().size() == 8);
        check("SE labs never overlap TE labs in same (room, slot)", seNoLabClash);

        // 2. Reverse direction: preload SE in slots 1 & 2, generate TE without lab clashes
        SchedulingState teState = new SchedulingState();
        Set<String> seRoomSlots = new HashSet<>();
        for (TimetableEntry se : seState.getPlaced()) {
            teState.preloadOtherClassEntry(se);
            seRoomSlots.add(se.getRoomId() + ":" + se.getSlotId());
        }

        List<ScheduleTask> teTasks = new ArrayList<>();
        for (int b = 0; b < 4; b++) {
            teTasks.add(new ScheduleTask(24 + b, "TE Lab " + (b + 1), "TL" + (b + 1),
                    3, 9 + b, teFacs[b], "PR", 2, 16, "2026-27", prefLabs));
        }
        List<ConflictReport> teConflicts = new ArrayList<>();
        service.scheduleTasks(teTasks, teState, slots, labs,
                Map.of(14, 20, 18, 20, 20, 20, 17, 20), teConflicts);

        boolean teNoLabClash = teState.getPlaced().stream()
                .noneMatch(e -> seRoomSlots.contains(e.getRoomId() + ":" + e.getSlotId()));
        check("TE lab generation places all 4 parallel batch practicals without conflicts",
                teConflicts.isEmpty() && teState.getPlaced().size() == 8);
        check("TE labs never overlap SE labs in same (room, slot)", teNoLabClash);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            passed++;
        } else {
            System.err.println("  [FAIL] " + description);
            failed++;
        }
    }
}

