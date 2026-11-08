package in.frcce.ecs.service;

import in.frcce.ecs.model.*;

import java.util.List;

/**
 * Encapsulates one scheduling "task" — one session that must be placed.
 * Holds all needed metadata so the engine doesn't have to re-query.
 */
public class ScheduleTask {

    public enum Priority { LAB_BLOCK, THEORY, TUTORIAL }

    private final int         subjectId;
    private final String      subjectName;
    private final String      subjectAbbr;
    private final int         classId;
    private final Integer     batchId;      // null for theory/tutorial
    private final int         facultyId;
    private final String      sessionType;  // TH / PR / TU
    private final int         requiredSlots; // 1 for TH/TU, 2 for PR
    private final Priority    priority;
    private final int         classStrength;
    private final String      academicYear;

    public ScheduleTask(int subjectId, String subjectName, String subjectAbbr,
                        int classId, Integer batchId,
                        int facultyId, String sessionType,
                        int requiredSlots, int classStrength,
                        String academicYear) {
        this.subjectId     = subjectId;
        this.subjectName   = subjectName;
        this.subjectAbbr   = subjectAbbr;
        this.classId       = classId;
        this.batchId       = batchId;
        this.facultyId     = facultyId;
        this.sessionType   = sessionType;
        this.requiredSlots = requiredSlots;
        this.classStrength = classStrength;
        this.academicYear  = academicYear;
        this.priority      = "PR".equals(sessionType) ? Priority.LAB_BLOCK
                           : "TU".equals(sessionType) ? Priority.TUTORIAL
                           : Priority.THEORY;
    }

    // Getters
    public int      getSubjectId()     { return subjectId; }
    public String   getSubjectName()   { return subjectName; }
    public String   getSubjectAbbr()   { return subjectAbbr; }
    public int      getClassId()       { return classId; }
    public Integer  getBatchId()       { return batchId; }
    public int      getFacultyId()     { return facultyId; }
    public String   getSessionType()   { return sessionType; }
    public int      getRequiredSlots() { return requiredSlots; }
    public Priority getPriority()      { return priority; }
    public int      getClassStrength() { return classStrength; }
    public String   getAcademicYear()  { return academicYear; }

    @Override
    public String toString() {
        return String.format("Task[%s %s cls=%d batch=%s fac=%d]",
                sessionType, subjectAbbr, classId, batchId, facultyId);
    }
}

