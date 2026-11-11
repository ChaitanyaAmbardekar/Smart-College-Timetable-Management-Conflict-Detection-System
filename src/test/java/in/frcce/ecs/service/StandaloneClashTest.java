package in.frcce.ecs.service;

import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.model.Subject;
import in.frcce.ecs.model.TimetableEntry;

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

