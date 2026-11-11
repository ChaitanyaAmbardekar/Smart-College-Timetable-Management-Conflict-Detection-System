-- Upgrade an existing ecs_timetable created from the earlier schema.sql.
-- Run this once before the updated seed.sql. Safe to run repeatedly.

USE ecs_timetable;

SET @project_hours_ddl = IF(
    (SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'subject'
       AND COLUMN_NAME = 'project_hrs') = 0,
    'ALTER TABLE subject ADD COLUMN project_hrs TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER tutorial_hrs',
    'SELECT 1'
);
PREPARE project_hours_stmt FROM @project_hours_ddl;
EXECUTE project_hours_stmt;
DEALLOCATE PREPARE project_hours_stmt;

ALTER TABLE faculty
    MODIFY COLUMN email VARCHAR(150) NULL COMMENT 'Optional; not provided for every faculty member';

ALTER TABLE faculty_subject
    MODIFY COLUMN session_type ENUM('TH','PR','TU','PJ','ALL') NOT NULL DEFAULT 'ALL';

ALTER TABLE timetable_entry
    MODIFY COLUMN session_type ENUM('TH','PR','TU','PJ') NOT NULL;

-- Move CEP's stored workload from practical hours to project hours.
UPDATE subject
SET practical_hrs = 0, project_hrs = 4
WHERE course_code = '25CEP12EC01';

UPDATE faculty_subject fs
JOIN subject s ON s.subject_id = fs.subject_id
SET fs.session_type = 'PJ'
WHERE s.course_code = '25CEP12EC01'
  AND fs.class_id = 1
  AND fs.faculty_id = 3
  AND fs.session_type = 'PR';

-- Allow parallel batch practical sessions in the same (class_id, slot_id).
SET @uq_class_slot_ddl = IF(
    (SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'timetable_entry'
       AND INDEX_NAME = 'uq_class_slot') > 0,
    'ALTER TABLE timetable_entry ADD UNIQUE KEY uq_class_batch_slot (class_id, batch_id, slot_id, academic_year), DROP INDEX uq_class_slot',
    'SELECT 1'
);
PREPARE uq_class_slot_stmt FROM @uq_class_slot_ddl;
EXECUTE uq_class_slot_stmt;
DEALLOCATE PREPARE uq_class_slot_stmt;

-- Add prac_mode ('BATCH' vs 'WHOLE_CLASS') to subject and faculty_subject.
SET @subj_prac_mode_ddl = IF(
    (SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'subject'
       AND COLUMN_NAME = 'prac_mode') = 0,
    'ALTER TABLE subject ADD COLUMN prac_mode ENUM(\'BATCH\',\'WHOLE_CLASS\') NOT NULL DEFAULT \'BATCH\' AFTER tutorial_hrs',
    'SELECT 1'
);
PREPARE subj_prac_mode_stmt FROM @subj_prac_mode_ddl;
EXECUTE subj_prac_mode_stmt;
DEALLOCATE PREPARE subj_prac_mode_stmt;

SET @fs_prac_mode_ddl = IF(
    (SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'faculty_subject'
       AND COLUMN_NAME = 'prac_mode') = 0,
    'ALTER TABLE faculty_subject ADD COLUMN prac_mode ENUM(\'BATCH\',\'WHOLE_CLASS\') NOT NULL DEFAULT \'BATCH\' AFTER session_type',
    'SELECT 1'
);
PREPARE fs_prac_mode_stmt FROM @fs_prac_mode_ddl;
EXECUTE fs_prac_mode_stmt;
DEALLOCATE PREPARE fs_prac_mode_stmt;
