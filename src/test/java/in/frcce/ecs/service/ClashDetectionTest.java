package in.frcce.ecs.service;

import in.frcce.ecs.model.TimetableEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}

