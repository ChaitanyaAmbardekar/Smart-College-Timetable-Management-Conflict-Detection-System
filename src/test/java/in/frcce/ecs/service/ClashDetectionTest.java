package in.frcce.ecs.service;

import in.frcce.ecs.model.ConflictReport;
import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.model.Room;
import in.frcce.ecs.model.Subject;
import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.model.TimetableEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for clash detection and hard constraint validation.
 */
public class ClashDetectionTest {

    private SchedulingState state;

    @BeforeEach
    void setUp() {
        state = new SchedulingState();
    }

    @Test
    @DisplayName("Hard constraint: A faculty cannot teach two classes in the same slot")
    void testFacultyClash() {
        int facultyId = 3; // Prof. Vaibhav Godbole
        int slotId = 1;    // Monday Slot 1

        assertTrue(state.isFacultyAvailable(facultyId, slotId), "Faculty should initially be free");

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(facultyId);
        entry1.setClassId(1);
        entry1.setRoomId(101);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(3);
        entry1.setSessionType("TH");

        state.occupy(entry1);

        assertFalse(state.isFacultyAvailable(facultyId, slotId),
                "Faculty must NOT be available in the same slot once occupied");
        // But should be available in other slots
        assertTrue(state.isFacultyAvailable(facultyId, 2),
                "Faculty should remain available in other slots");
    }

    @Test
    @DisplayName("Hard constraint: A class cannot have two sessions in the same slot")
    void testClassClash() {
        int classId = 1;
        int slotId = 4;

        assertTrue(state.isClassFree(classId, slotId), "Class should initially be free");

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(1);
        entry1.setClassId(classId);
        entry1.setRoomId(1);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(1);
        entry1.setSessionType("TH");

        state.occupy(entry1);

        assertFalse(state.isClassFree(classId, slotId),
                "Class must NOT be free in slot where it already has a lecture");
    }

    @Test
    @DisplayName("Hard constraint: A room cannot be used twice in the same slot")
    void testRoomClash() {
        int roomId = 5; // ECS Lab 1
        int slotId = 7; // Monday afternoon

        assertTrue(state.isRoomFree(roomId, slotId), "Room should initially be free");

        TimetableEntry entry1 = new TimetableEntry();
        entry1.setFacultyId(4);
        entry1.setClassId(1);
        entry1.setRoomId(roomId);
        entry1.setSlotId(slotId);
        entry1.setSubjectId(4);
        entry1.setSessionType("PR");

        state.occupy(entry1);

        assertFalse(state.isRoomFree(roomId, slotId),
                "Room must NOT be free if another session is already using it in the slot");
    }

    @Test
    @DisplayName("Hard constraint: Faculty marked unavailable must be blocked from scheduling")
    void testFacultyUnavailability() {
        int facultyId = 2; // Dr. D.V. Bhoir
        int slotId = 10;   // Tuesday 8:45 AM

        state.markFacultyUnavailable(facultyId, slotId);

        assertFalse(state.isFacultyAvailable(facultyId, slotId),
                "Faculty must be blocked in unavailable slot even if no session is scheduled");
        assertTrue(state.isFacultyAvailable(facultyId, 11),
                "Faculty should be available in other slots");
    }

    @Test
    @DisplayName("Backtracking support: Releasing an entry restores resource availability")
    void testReleaseRestoresAvailability() {
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
        assertFalse(state.isFacultyAvailable(facultyId, slotId));
        assertFalse(state.isClassFree(classId, slotId));
        assertFalse(state.isRoomFree(roomId, slotId));

        // Backtrack
        state.release(entry);
        assertTrue(state.isFacultyAvailable(facultyId, slotId), "Faculty available after release");
        assertTrue(state.isClassFree(classId, slotId), "Class available after release");
        assertTrue(state.isRoomFree(roomId, slotId), "Room available after release");
    }

    @Test
    @DisplayName("Workload count and consecutive session detection")
    void testWorkloadAndConsecutiveSessions() {
        int facultyId = 3;
        int classId = 1;

        assertEquals(0, state.facultySessionCount(facultyId));

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

        assertEquals(2, state.facultySessionCount(facultyId));
        assertEquals(2, state.consecutiveSessionsFor(classId, "MONDAY", 2));
    }

    @Test
    @DisplayName("Hard constraint: Max 1 theory lecture and max 2 practical lectures of the same course per day")
    void testDailyCourseLimits() {
        int classId = 1;
        int subjectId = 1;

        assertTrue(state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "TH", 1));
        TimetableEntry th1 = new TimetableEntry();
        th1.setClassId(classId);
        th1.setSubjectId(subjectId);
        th1.setFacultyId(2);
        th1.setRoomId(1);
        th1.setSlotId(1);
        th1.setDay("MONDAY");
        th1.setSessionType("TH");
        state.occupy(th1);

        assertFalse(state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "TH", 1),
                "Cannot schedule more than 1 theory lecture of the same course on the same day");
        assertTrue(state.canScheduleCourseOnDay(subjectId, classId, "TUESDAY", "TH", 1));

        assertTrue(state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "PR", 2));
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

        assertFalse(state.canScheduleCourseOnDay(subjectId, classId, "MONDAY", "PR", 2),
                "Cannot schedule more than 2 practical lecture slots of the same course on the same day");
        assertTrue(state.canScheduleCourseOnDay(subjectId, classId, "WEDNESDAY", "PR", 2));
    }

    @Test
    @DisplayName("Hard constraint: Each batch can perform practicals for only one course per day and batches may run in parallel slots")
    void testBatchDailyPracticalAndParallelRules() {
        int classId = 1;

        TimetableEntry batchAPr = new TimetableEntry();
        batchAPr.setClassId(classId);
        batchAPr.setBatchId(1);
        batchAPr.setSubjectId(1);
        batchAPr.setFacultyId(2);
        batchAPr.setRoomId(5);
        batchAPr.setSlotId(1);
        batchAPr.setDay("MONDAY");
        batchAPr.setSessionType("PR");
        state.occupy(batchAPr);

        assertTrue(state.isClassFree(classId, 2, 1), "Different batch may run in parallel in same slot");
        assertFalse(state.isClassFree(classId, 1, 1), "Same batch cannot run twice in same slot");
        assertFalse(state.isClassFree(classId, null, 1), "Whole-class session cannot overlap batch session");

        assertFalse(state.canBatchPerformPracticalOnDay(classId, 1, 2, "MONDAY"),
                "Batch A cannot perform practical for a second course on MONDAY");
        assertTrue(state.canBatchPerformPracticalOnDay(classId, 2, 2, "MONDAY"),
                "Batch B can perform practical for another course on MONDAY");
        assertTrue(state.canBatchPerformPracticalOnDay(classId, 1, 2, "TUESDAY"),
                "Batch A can perform practical for another course on TUESDAY");
    }

    @Test
    @DisplayName("Model: Subject and allocation support batch vs whole-class practical mode")
    void testPracticalDeliveryModeModel() {
        Subject sub = new Subject();
        assertFalse(sub.isWholeClassPractical());
        sub.setPracMode("WHOLE_CLASS");
        assertTrue(sub.isWholeClassPractical());

        FacultySubject fs = new FacultySubject();
        assertFalse(fs.isWholeClassPractical());
        fs.setPracMode("WHOLE_CLASS");
        assertTrue(fs.isWholeClassPractical());
    }

    @Test
    @DisplayName("Cross-year constraint: Timetable generation respects other-class faculty, room, and weekly hour commitments")
    void testCrossYearFacultyAndRoomAvailability() {
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

        assertFalse(state.isFacultyAvailable(3, 1), "Faculty busy in 3rd year cannot be assigned in slot 1");
        assertFalse(state.isRoomFree(1, 1), "Room busy in 3rd year cannot be assigned in slot 1");
        assertTrue(state.isClassFree(1, null, 1), "Target 2nd-year class itself remains free in slot 1");
        assertEquals(2, state.facultySessionCount(3), "Other-class commitments count toward faculty hours");
        assertTrue(state.getPlaced().isEmpty(), "Preloaded other-class entries must not be placed into target class");

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

        assertTrue(conflicts.isEmpty(), "Should place both TH sessions in remaining free slots");
        assertEquals(2, state.getPlaced().size());
        assertTrue(state.getPlaced().stream().noneMatch(e -> e.getSlotId() == 1 || e.getSlotId() == 2),
                "Generated entries must avoid faculty 3's busy 3rd-year slots");
    }
}

