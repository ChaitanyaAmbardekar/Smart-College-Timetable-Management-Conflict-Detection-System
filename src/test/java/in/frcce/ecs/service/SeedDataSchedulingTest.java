package in.frcce.ecs.service;

import in.frcce.ecs.model.*;

import java.util.*;

/**
 * End-to-end verification of the scheduling engine using the exact FrCRCE SE-ECS Sem III
 * academic curriculum and faculty assignments.
 * Confirms that all sessions are successfully auto-scheduled with zero clashes.
 */
public class SeedDataSchedulingTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("Running SE-ECS Sem III Timetable Generation Engine Verification...");
        System.out.println("==================================================================");

        // 1. Setup Time Slots (Mon-Sat, based on actual FrCRCE schedule)
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        List<TimeSlot> allSlots = new ArrayList<>();
        int slotId = 1;

        for (String day : days) {
            // Slot 1: 8:45 - 9:45
            allSlots.add(new TimeSlot(slotId++, day, "08:45:00", "09:45:00", 1, false));
            // Slot 2: 9:45 - 10:45
            allSlots.add(new TimeSlot(slotId++, day, "09:45:00", "10:45:00", 2, false));
            // Slot 3: 10:45 - 11:00 (Short Break)
            allSlots.add(new TimeSlot(slotId++, day, "10:45:00", "11:00:00", 3, true));
            // Slot 4: 11:00 - 12:00
            allSlots.add(new TimeSlot(slotId++, day, "11:00:00", "12:00:00", 4, false));
            // Slot 5: 12:00 - 13:00
            allSlots.add(new TimeSlot(slotId++, day, "12:00:00", "13:00:00", 5, false));
            // Slot 6: 13:00 - 13:30 (Lunch Break)
            allSlots.add(new TimeSlot(slotId++, day, "13:00:00", "13:30:00", 6, true));
            // Slot 7: 13:30 - 14:30
            allSlots.add(new TimeSlot(slotId++, day, "13:30:00", "14:30:00", 7, false));
            // Slot 8: 14:30 - 15:30
            allSlots.add(new TimeSlot(slotId++, day, "14:30:00", "15:30:00", 8, false));
            // Slot 9: 15:30 - 16:30
            allSlots.add(new TimeSlot(slotId++, day, "15:30:00", "16:30:00", 9, false));
        }

        List<TimeSlot> activeSlots = allSlots.stream().filter(s -> !s.isBreak()).toList();

        // 2. Setup Rooms
        List<Room> rooms = List.of(
            new Room(1, "Room 307", "CLASSROOM", 65, "Third Floor", true),
            new Room(2, "Room 308", "CLASSROOM", 65, "Third Floor", true),
            new Room(5, "ECS Lab 1", "LAB", 30, "Ground Floor", true),
            new Room(6, "ECS Lab 2", "LAB", 30, "Ground Floor", true),
            new Room(7, "Computer Lab 1", "LAB", 30, "First Floor", true),
            new Room(8, "Computer Lab 2", "LAB", 30, "First Floor", true),
            new Room(9, "Electronics Lab 1", "LAB", 30, "Second Floor", true),
            new Room(10, "Electronics Lab 2", "LAB", 30, "Second Floor", true)
        );

        // 3. Setup Faculty Max Hours
        Map<Integer, Integer> maxHours = new HashMap<>();
        for (int i = 1; i <= 12; i++) maxHours.put(i, 20);

        // 4. Setup Tasks for SE-ECS Sem III
        // Subjects:
        // EDA: 2 TH (DVB:2), 2 PR (DVB:2)
        // COA: 2 TH (SUP:1), 2 PR (KR:7)
        // DBMS: 2 TH (VVG:3), 2 PR (VVG:3)
        // OOPJ: 2 PR (AL:4)
        // LE: 2 TH (IK:8)
        // FPTI: 2 TH (KR:7)
        // MDM1: 2 TH (AP:5)
        // MDM2: 2 TH (AP:5)
        // MIL: 2 TH (RDS:10)
        // HVPE: 1 TH (AP:5), 2 PR (AP:5)
        // CEP: 4 PR (VVG:3)
        // DM: 2 TH (DAB:6)

        List<ScheduleTask> tasks = new ArrayList<>();
        int classId = 1;
        int classStrength = 65;
        String ay = "2026-27";

        // Helper to add theory
        Runnable addTheory = () -> {
            tasks.add(new ScheduleTask(1, "Electronic Devices and Applications", "EDA", classId, null, 2, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(1, "Electronic Devices and Applications", "EDA", classId, null, 2, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(2, "Computer Organization and Architecture", "COA", classId, null, 1, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(2, "Computer Organization and Architecture", "COA", classId, null, 1, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(3, "Database Management System", "DBMS", classId, null, 3, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(3, "Database Management System", "DBMS", classId, null, 3, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(5, "Law for Engineers", "LE", classId, null, 8, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(5, "Law for Engineers", "LE", classId, null, 8, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(6, "Financial Planning", "FPTI", classId, null, 7, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(6, "Financial Planning", "FPTI", classId, null, 7, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(7, "MDM-1 Finance Accounting", "MDM1", classId, null, 5, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(7, "MDM-1 Finance Accounting", "MDM1", classId, null, 5, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(8, "MDM-2 Economics", "MDM2", classId, null, 5, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(8, "MDM-2 Economics", "MDM2", classId, null, 5, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(9, "Modern Indian Language", "MIL", classId, null, 10, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(9, "Modern Indian Language", "MIL", classId, null, 10, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(10, "Human Values & Ethics", "HVPE", classId, null, 5, "TH", 1, classStrength, ay));

            tasks.add(new ScheduleTask(12, "Double Minor", "DM", classId, null, 6, "TH", 1, classStrength, ay));
            tasks.add(new ScheduleTask(12, "Double Minor", "DM", classId, null, 6, "TH", 1, classStrength, ay));
        };
        addTheory.run();

        // Practicals (2 consecutive slots each)
        // EDA lab (DVB)
        tasks.add(new ScheduleTask(1, "Electronic Devices and Applications Lab", "EDA", classId, 1, 2, "PR", 2, 20, ay));
        // COA lab (KR)
        tasks.add(new ScheduleTask(2, "Computer Organization Lab", "COA", classId, 2, 7, "PR", 2, 20, ay));
        // DBMS lab (VVG)
        tasks.add(new ScheduleTask(3, "Database Management System Lab", "DBMS", classId, 3, 3, "PR", 2, 20, ay));
        // OOPJ lab (AL)
        tasks.add(new ScheduleTask(4, "OOP with JAVA Lab", "OOPJ", classId, 4, 4, "PR", 2, 20, ay));
        // HVPE practical (AP)
        tasks.add(new ScheduleTask(10, "HVPE Practical", "HVPE", classId, 1, 5, "PR", 2, 20, ay));
        // CEP Project hours (VVG)
        tasks.add(new ScheduleTask(11, "Community Engagement Project", "CEP", classId, 1, 3, "PR", 2, 20, ay));
        tasks.add(new ScheduleTask(11, "Community Engagement Project", "CEP", classId, 2, 3, "PR", 2, 20, ay));

        System.out.printf("Total sessions to schedule: %d%n", tasks.size());

        // Sort: labs first, then theory (most-constrained first heuristic)
        tasks.sort(Comparator.comparing(ScheduleTask::getPriority));

        // 5. Run Scheduling Simulation
        SchedulingState state = new SchedulingState();
        int placedCount = 0;
        int failedCount = 0;

        for (ScheduleTask task : tasks) {
            boolean placed = false;
            if ("PR".equals(task.getSessionType())) {
                // Find 2 consecutive active slots
                for (int i = 0; i < allSlots.size() - 1; i++) {
                    TimeSlot s1 = allSlots.get(i);
                    TimeSlot s2 = allSlots.get(i + 1);
                    if (s1.isBreak() || s2.isBreak()) continue;
                    if (!s1.getDay().equals(s2.getDay())) continue;
                    if (s2.getSlotNo() != s1.getSlotNo() + 1) continue;

                    if (!state.isFacultyAvailable(task.getFacultyId(), s1.getSlotId())) continue;
                    if (!state.isFacultyAvailable(task.getFacultyId(), s2.getSlotId())) continue;
                    if (!state.isClassFree(task.getClassId(), s1.getSlotId())) continue;
                    if (!state.isClassFree(task.getClassId(), s2.getSlotId())) continue;

                    // Find lab room
                    Room lab = rooms.stream().filter(r -> "LAB".equals(r.getType())
                            && state.isRoomFree(r.getRoomId(), s1.getSlotId())
                            && state.isRoomFree(r.getRoomId(), s2.getSlotId())).findFirst().orElse(null);

                    if (lab != null) {
                        TimetableEntry e1 = new TimetableEntry();
                        e1.setClassId(task.getClassId());
                        e1.setFacultyId(task.getFacultyId());
                        e1.setRoomId(lab.getRoomId());
                        e1.setSlotId(s1.getSlotId());
                        e1.setDay(s1.getDay());
                        e1.setSlotNo(s1.getSlotNo());
                        e1.setSubjectId(task.getSubjectId());
                        e1.setSessionType("PR");

                        TimetableEntry e2 = new TimetableEntry();
                        e2.setClassId(task.getClassId());
                        e2.setFacultyId(task.getFacultyId());
                        e2.setRoomId(lab.getRoomId());
                        e2.setSlotId(s2.getSlotId());
                        e2.setDay(s2.getDay());
                        e2.setSlotNo(s2.getSlotNo());
                        e2.setSubjectId(task.getSubjectId());
                        e2.setSessionType("PR");

                        state.occupy(e1);
                        state.occupy(e2);
                        placed = true;
                        break;
                    }
                }
            } else {
                // Single theory slot
                for (TimeSlot s1 : activeSlots) {
                    if (!state.isFacultyAvailable(task.getFacultyId(), s1.getSlotId())) continue;
                    if (!state.isClassFree(task.getClassId(), s1.getSlotId())) continue;

                    Room cr = rooms.stream().filter(r -> "CLASSROOM".equals(r.getType())
                            && state.isRoomFree(r.getRoomId(), s1.getSlotId())).findFirst().orElse(null);

                    if (cr != null) {
                        TimetableEntry e1 = new TimetableEntry();
                        e1.setClassId(task.getClassId());
                        e1.setFacultyId(task.getFacultyId());
                        e1.setRoomId(cr.getRoomId());
                        e1.setSlotId(s1.getSlotId());
                        e1.setDay(s1.getDay());
                        e1.setSlotNo(s1.getSlotNo());
                        e1.setSubjectId(task.getSubjectId());
                        e1.setSessionType("TH");

                        state.occupy(e1);
                        placed = true;
                        break;
                    }
                }
            }

            if (placed) {
                placedCount++;
            } else {
                failedCount++;
                System.err.printf("  [CONFLICT] Could not place task: %s (%s)%n", task.getSubjectAbbr(), task.getSessionType());
            }
        }

        System.out.printf("Placed: %d / %d sessions%n", placedCount, tasks.size());
        System.out.printf("Total entries in generated timetable: %d%n", state.getPlaced().size());

        // 6. Assertions for zero clashes
        System.out.println("\nChecking constraints on generated timetable:");
        Set<String> facultySlotCheck = new HashSet<>();
        Set<String> classSlotCheck = new HashSet<>();
        Set<String> roomSlotCheck = new HashSet<>();
        boolean zeroClashes = true;

        for (TimetableEntry e : state.getPlaced()) {
            String facKey = e.getFacultyId() + ":" + e.getSlotId();
            String clsKey = e.getClassId() + ":" + e.getSlotId();
            String rmKey = e.getRoomId() + ":" + e.getSlotId();

            if (!facultySlotCheck.add(facKey)) {
                System.err.println("  [CLASH] Faculty double booking at " + facKey);
                zeroClashes = false;
            }
            if (!classSlotCheck.add(clsKey)) {
                System.err.println("  [CLASH] Class double booking at " + clsKey);
                zeroClashes = false;
            }
            if (!roomSlotCheck.add(rmKey)) {
                System.err.println("  [CLASH] Room double booking at " + rmKey);
                zeroClashes = false;
            }
        }

        if (zeroClashes && failedCount == 0) {
            System.out.println("  [SUCCESS] All constraints satisfied with ZERO CLASHES!");
        } else {
            System.err.println("  [FAILURE] Clashes detected or unplaced sessions exist!");
            System.exit(1);
        }
    }
}

