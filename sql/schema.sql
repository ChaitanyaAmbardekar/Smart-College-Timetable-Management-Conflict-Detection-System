-- =============================================================================
-- ECS Faculty Timetable Generator
-- Fr. Conceicao Rodrigues College of Engineering (FrCRCE), Bandra, Mumbai
-- Electronics and Computer Science (ECS) Department
-- Schema: MySQL 8.0+, normalized to 3NF
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ecs_timetable
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecs_timetable;

-- ---------------------------------------------------------------------------
-- 1. DEPARTMENT
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department (
    dept_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    code      VARCHAR(20)  NOT NULL,
    UNIQUE KEY uq_dept_code (code)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 2. FACULTY
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faculty (
    faculty_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    abbreviation       VARCHAR(10)  NOT NULL,
    email              VARCHAR(150) NULL COMMENT 'Optional; not provided for every faculty member',
    designation        ENUM('Professor','Associate Professor','Assistant Professor','Lab Instructor','Visiting Faculty') NOT NULL DEFAULT 'Assistant Professor',
    max_hours_per_week TINYINT UNSIGNED NOT NULL DEFAULT 20,
    dept_id            INT UNSIGNED NOT NULL,
    is_active          BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_faculty_email (email),
    UNIQUE KEY uq_faculty_abbr (abbreviation, dept_id),
    INDEX idx_faculty_dept (dept_id),
    CONSTRAINT fk_faculty_dept FOREIGN KEY (dept_id) REFERENCES department (dept_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 3. SUBJECT
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subject (
    subject_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    course_code   VARCHAR(20)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    abbreviation  VARCHAR(10)  NOT NULL,
    semester      TINYINT UNSIGNED NOT NULL COMMENT '3 or 4 for SE, 5-6 for TE, 7-8 for BE',
    type          ENUM('PCC','OE','MDM','HSSM','EL','BSC','VAC','CEP') NOT NULL DEFAULT 'PCC',
    theory_hrs    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    practical_hrs TINYINT UNSIGNED NOT NULL DEFAULT 0,
    tutorial_hrs  TINYINT UNSIGNED NOT NULL DEFAULT 0,
    project_hrs   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_course_code (course_code),
    INDEX idx_subject_semester (semester)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 4. CLASS DIVISION
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_division (
    class_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    year          TINYINT UNSIGNED NOT NULL COMMENT '2=SE, 3=TE, 4=BE',
    semester      TINYINT UNSIGNED NOT NULL,
    division      VARCHAR(10)  NOT NULL,
    strength      SMALLINT UNSIGNED NOT NULL DEFAULT 60,
    academic_year VARCHAR(9)   NOT NULL,
    dept_id       INT UNSIGNED NOT NULL,
    INDEX idx_cd_semester (semester),
    INDEX idx_cd_dept (dept_id),
    UNIQUE KEY uq_class_div (semester, division, academic_year, dept_id),
    CONSTRAINT fk_cd_dept FOREIGN KEY (dept_id) REFERENCES department (dept_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 5. BATCH
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS batch (
    batch_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    class_id   INT UNSIGNED NOT NULL,
    batch_name VARCHAR(10)  NOT NULL,
    strength   SMALLINT UNSIGNED NOT NULL DEFAULT 20,
    UNIQUE KEY uq_batch (class_id, batch_name),
    INDEX idx_batch_class (class_id),
    CONSTRAINT fk_batch_class FOREIGN KEY (class_id) REFERENCES class_division (class_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 6. ROOM
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room (
    room_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL,
    type      ENUM('CLASSROOM','LAB','SEMINAR_HALL','TUTORIAL_ROOM') NOT NULL DEFAULT 'CLASSROOM',
    capacity  SMALLINT UNSIGNED NOT NULL DEFAULT 60,
    location  VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_room_name (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 7. TIME SLOT
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS time_slot (
    slot_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    day        ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    slot_no    TINYINT UNSIGNED NOT NULL,
    is_break   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_slot (day, slot_no),
    INDEX idx_slot_day (day),
    CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 8. FACULTY-SUBJECT ALLOCATION
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faculty_subject (
    id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    faculty_id   INT UNSIGNED NOT NULL,
    subject_id   INT UNSIGNED NOT NULL,
    class_id     INT UNSIGNED NOT NULL,
    session_type ENUM('TH','PR','TU','PJ','ALL') NOT NULL DEFAULT 'ALL',
    UNIQUE KEY uq_faculty_subject_class (faculty_id, subject_id, class_id, session_type),
    INDEX idx_fs_faculty (faculty_id),
    INDEX idx_fs_subject (subject_id),
    INDEX idx_fs_class   (class_id),
    CONSTRAINT fk_fs_faculty FOREIGN KEY (faculty_id) REFERENCES faculty       (faculty_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_fs_subject FOREIGN KEY (subject_id) REFERENCES subject       (subject_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_fs_class   FOREIGN KEY (class_id)  REFERENCES class_division (class_id)  ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 9. FACULTY UNAVAILABILITY
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faculty_unavailability (
    id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    faculty_id INT UNSIGNED NOT NULL,
    slot_id    INT UNSIGNED NOT NULL,
    reason     VARCHAR(255),
    UNIQUE KEY uq_unavail (faculty_id, slot_id),
    INDEX idx_unavail_faculty (faculty_id),
    CONSTRAINT fk_unavail_faculty FOREIGN KEY (faculty_id) REFERENCES faculty    (faculty_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_unavail_slot   FOREIGN KEY (slot_id)    REFERENCES time_slot  (slot_id)    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 10. TIMETABLE ENTRY
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS timetable_entry (
    entry_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    class_id      INT UNSIGNED NOT NULL,
    batch_id      INT UNSIGNED NULL,
    subject_id    INT UNSIGNED NOT NULL,
    faculty_id    INT UNSIGNED NOT NULL,
    room_id       INT UNSIGNED NOT NULL,
    slot_id       INT UNSIGNED NOT NULL,
    session_type  ENUM('TH','PR','TU','PJ') NOT NULL,
    academic_year VARCHAR(9)   NOT NULL DEFAULT '2026-27',
    is_locked     BOOLEAN NOT NULL DEFAULT FALSE,

    UNIQUE KEY uq_faculty_slot (faculty_id, slot_id, academic_year),
    UNIQUE KEY uq_class_slot   (class_id,   slot_id, academic_year),
    UNIQUE KEY uq_room_slot    (room_id,    slot_id, academic_year),

    INDEX idx_te_class   (class_id),
    INDEX idx_te_faculty (faculty_id),
    INDEX idx_te_room    (room_id),
    INDEX idx_te_slot    (slot_id),
    INDEX idx_te_subject (subject_id),
    INDEX idx_te_composite (academic_year, class_id, slot_id),

    CONSTRAINT fk_te_class   FOREIGN KEY (class_id)   REFERENCES class_division (class_id)  ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_te_batch   FOREIGN KEY (batch_id)   REFERENCES batch          (batch_id)   ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_te_subject FOREIGN KEY (subject_id) REFERENCES subject        (subject_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_te_faculty FOREIGN KEY (faculty_id) REFERENCES faculty        (faculty_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_te_room    FOREIGN KEY (room_id)    REFERENCES room           (room_id)    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_te_slot    FOREIGN KEY (slot_id)    REFERENCES time_slot      (slot_id)    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 11. USERS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    username      VARCHAR(50)  NOT NULL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL COMMENT 'SHA-256 hex',
    role          ENUM('ADMIN','FACULTY') NOT NULL DEFAULT 'FACULTY',
    faculty_id    INT UNSIGNED NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    last_login    DATETIME,
    INDEX idx_users_faculty (faculty_id),
    CONSTRAINT fk_users_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================================
-- VIEWS
-- =============================================================================

CREATE OR REPLACE VIEW vw_class_timetable AS
SELECT
    cd.year, cd.semester, cd.division, cd.academic_year,
    ts.day, ts.slot_no, ts.start_time, ts.end_time,
    sub.course_code, sub.name AS subject_name, sub.abbreviation AS subject_abbr,
    te.session_type,
    f.name AS faculty_name, f.abbreviation AS faculty_abbr,
    r.name AS room_name, r.type AS room_type,
    b.batch_name,
    te.entry_id, te.class_id, te.slot_id, te.faculty_id, te.room_id, te.subject_id, te.batch_id, te.is_locked
FROM timetable_entry te
JOIN class_division cd  ON te.class_id   = cd.class_id
JOIN time_slot      ts  ON te.slot_id    = ts.slot_id
JOIN subject        sub ON te.subject_id = sub.subject_id
JOIN faculty        f   ON te.faculty_id = f.faculty_id
JOIN room           r   ON te.room_id    = r.room_id
LEFT JOIN batch     b   ON te.batch_id   = b.batch_id;

CREATE OR REPLACE VIEW vw_faculty_timetable AS
SELECT
    f.faculty_id, f.name AS faculty_name, f.designation, te.academic_year,
    ts.day, ts.slot_no, ts.start_time, ts.end_time,
    sub.course_code, sub.name AS subject_name, sub.abbreviation AS subject_abbr,
    te.session_type,
    CONCAT(cd.year,'/',cd.division,' Sem',cd.semester) AS class_label,
    r.name AS room_name, b.batch_name,
    te.entry_id, te.class_id, te.slot_id, te.room_id, te.subject_id
FROM timetable_entry te
JOIN faculty        f   ON te.faculty_id = f.faculty_id
JOIN time_slot      ts  ON te.slot_id    = ts.slot_id
JOIN subject        sub ON te.subject_id = sub.subject_id
JOIN class_division cd  ON te.class_id   = cd.class_id
JOIN room           r   ON te.room_id    = r.room_id
LEFT JOIN batch     b   ON te.batch_id   = b.batch_id;

CREATE OR REPLACE VIEW vw_room_timetable AS
SELECT
    r.room_id, r.name AS room_name, r.type AS room_type, te.academic_year,
    ts.day, ts.slot_no, ts.start_time, ts.end_time,
    sub.name AS subject_name, te.session_type,
    f.name AS faculty_name,
    CONCAT(cd.year,'/',cd.division,' Sem',cd.semester) AS class_label,
    b.batch_name, te.entry_id
FROM timetable_entry te
JOIN room           r   ON te.room_id    = r.room_id
JOIN time_slot      ts  ON te.slot_id    = ts.slot_id
JOIN subject        sub ON te.subject_id = sub.subject_id
JOIN faculty        f   ON te.faculty_id = f.faculty_id
JOIN class_division cd  ON te.class_id   = cd.class_id
LEFT JOIN batch     b   ON te.batch_id   = b.batch_id;

CREATE OR REPLACE VIEW vw_faculty_workload AS
SELECT
    f.faculty_id, f.name AS faculty_name, f.designation, f.max_hours_per_week,
    COALESCE(te.academic_year, '2026-27') AS academic_year,
    COUNT(te.entry_id)                                           AS total_sessions,
    SUM(CASE WHEN te.session_type = 'TH' THEN 1 ELSE 0 END)     AS theory_sessions,
    SUM(CASE WHEN te.session_type = 'PR' THEN 1 ELSE 0 END)     AS practical_sessions,
    SUM(CASE WHEN te.session_type = 'TU' THEN 1 ELSE 0 END)     AS tutorial_sessions,
    COUNT(te.entry_id)                                           AS hours_per_week,
    f.max_hours_per_week - COUNT(te.entry_id)                    AS remaining_hours
FROM faculty f
LEFT JOIN timetable_entry te ON f.faculty_id = te.faculty_id
GROUP BY f.faculty_id, f.name, f.designation, f.max_hours_per_week, te.academic_year;

-- =============================================================================
-- STORED PROCEDURE
-- =============================================================================
DROP PROCEDURE IF EXISTS sp_faculty_workload;

DELIMITER $$
CREATE PROCEDURE sp_faculty_workload(
    IN  p_faculty_id    INT UNSIGNED,
    IN  p_academic_year VARCHAR(9)
)
BEGIN
    SELECT
        f.faculty_id, f.name AS faculty_name, f.designation, f.max_hours_per_week,
        p_academic_year AS academic_year,
        COUNT(te.entry_id)    AS total_hours_scheduled,
        f.max_hours_per_week - COUNT(te.entry_id) AS free_hours,
        SUM(CASE WHEN te.session_type='TH' THEN 1 ELSE 0 END) AS theory_hours,
        SUM(CASE WHEN te.session_type='PR' THEN 1 ELSE 0 END) AS practical_hours,
        SUM(CASE WHEN te.session_type='TU' THEN 1 ELSE 0 END) AS tutorial_hours
    FROM faculty f
    LEFT JOIN timetable_entry te ON f.faculty_id = te.faculty_id AND te.academic_year = p_academic_year
    WHERE f.faculty_id = p_faculty_id
    GROUP BY f.faculty_id, f.name, f.designation, f.max_hours_per_week;

    SELECT ts.day, ts.slot_no, ts.start_time, ts.end_time, 'FREE' AS status
    FROM time_slot ts
    WHERE ts.is_break = FALSE
      AND NOT EXISTS (
          SELECT 1 FROM timetable_entry te2
          WHERE te2.faculty_id = p_faculty_id AND te2.slot_id = ts.slot_id AND te2.academic_year = p_academic_year
      )
      AND NOT EXISTS (
          SELECT 1 FROM faculty_unavailability fu
          WHERE fu.faculty_id = p_faculty_id AND fu.slot_id = ts.slot_id
      )
    ORDER BY FIELD(ts.day,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), ts.slot_no;
END$$
DELIMITER ;

-- =============================================================================
-- TRIGGER: block faculty exceeding max_hours_per_week
-- =============================================================================
DROP TRIGGER IF EXISTS trg_check_faculty_max_hours;

DELIMITER $$
CREATE TRIGGER trg_check_faculty_max_hours
BEFORE INSERT ON timetable_entry
FOR EACH ROW
BEGIN
    DECLARE v_scheduled INT UNSIGNED DEFAULT 0;
    DECLARE v_max_hours INT UNSIGNED DEFAULT 0;

    SELECT COUNT(*) INTO v_scheduled
    FROM timetable_entry
    WHERE faculty_id = NEW.faculty_id AND academic_year = NEW.academic_year;

    SELECT max_hours_per_week INTO v_max_hours
    FROM faculty WHERE faculty_id = NEW.faculty_id;

    IF v_scheduled >= v_max_hours THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Faculty has reached the maximum allowed hours per week';
    END IF;
END$$
DELIMITER ;
