-- Full ECS Timetable Database: schema followed by seed data

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
    prac_mode     ENUM('BATCH','WHOLE_CLASS') NOT NULL DEFAULT 'BATCH',
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
    prac_mode    ENUM('BATCH','WHOLE_CLASS') NOT NULL DEFAULT 'BATCH',
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

    UNIQUE KEY uq_faculty_slot     (faculty_id, slot_id, academic_year),
    UNIQUE KEY uq_class_batch_slot (class_id,   batch_id, slot_id, academic_year),
    UNIQUE KEY uq_room_slot        (room_id,    slot_id, academic_year),

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


-- SEED DATA
-- =============================================================================
-- SEED DATA — based on the actual S.E. ECS timetable (Jul-Nov 2026)
-- Fr. Conceicao Rodrigues College of Engineering, Bandra
-- =============================================================================

USE ecs_timetable;

-- ---------------------------------------------------------------------------
-- DEPARTMENT
-- ---------------------------------------------------------------------------
INSERT INTO department (dept_id, name, code) VALUES
(1, 'Electronics and Computer Science', 'ECS'),
(2, 'Computer Engineering',             'CE'),
(3, 'Information Technology',           'IT')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ---------------------------------------------------------------------------
-- FACULTY  (real data from timetable image)
-- ---------------------------------------------------------------------------
INSERT INTO faculty (faculty_id, name, abbreviation, email, designation, max_hours_per_week, dept_id) VALUES
(1,  'Dr. Sapna Prabhu',       'SUP', 'sapna.prabhu@frcoe.in',      'Associate Professor',  20, 1),
(2,  'Dr. D.V. Bhoir',         'DVB', 'dv.bhoir@frcoe.in',          'Associate Professor',  20, 1),
(3,  'Prof. Vaibhav Godbole',  'VVG', 'vaibhav.godbole@frcoe.in',   'Assistant Professor',  18, 1),
(4,  'Prof. Archana Lopes',    'AL',  'archana.lopes@frcoe.in',     'Assistant Professor',  18, 1),
(5,  'Prof. Annivilia Pereira','AP',  'annivilia.pereira@frcoe.in', 'Assistant Professor',  18, 1),
(6,  'Dr. Dipak Bauskar',      'DAB', 'dipak.bauskar@frcoe.in',     'Associate Professor',  20, 1),
(7,  'Prof. K. Rodrigues',     'KR',  'k.rodrigues@frcoe.in',       'Assistant Professor',  18, 1),
(8,  'Prof. I. Khan',          'IK',  'i.khan@frcoe.in',            'Assistant Professor',  18, 1),
(9,  'Prof. N. Nair',          'N1',  'n.nair@frcoe.in',            'Lab Instructor',       16, 1),
(10, 'Prof. Riya D Souza',     'RDS', 'riya.dsouza@frcoe.in',       'Assistant Professor',  18, 1),
(11, 'Prof. Meena Sharma',     'MS',  'meena.sharma@frcoe.in',      'Assistant Professor',  18, 1),
(12, 'Dr. Priya Nambiar',      'PN',  'priya.nambiar@frcoe.in',     'Professor',            16, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), abbreviation = VALUES(abbreviation),
    designation = VALUES(designation), max_hours_per_week = VALUES(max_hours_per_week);

-- T.E. ECS faculty (Sem V); null emails were not present in the supplied list.
-- KRT is used for Kavita Rathi because KR is already assigned to K. Rodrigues.
INSERT INTO faculty (faculty_id, name, abbreviation, email, designation, max_hours_per_week, dept_id) VALUES
(13, 'Prof. Kavita Rathi',       'KRT', NULL, 'Assistant Professor', 18, 1),
(14, 'Dr. Prajita Bhangale',     'PKB', NULL, 'Assistant Professor', 18, 1),
(15, 'Dr. Swapnali Makdey',      'SAM', NULL, 'Assistant Professor', 18, 1),
(16, 'Prof. Ayen Modi',          'JM',  NULL, 'Assistant Professor', 18, 1),
(17, 'Prof. Vinita Vartak',      'VV',  NULL, 'Assistant Professor', 18, 1),
(18, 'Dr. Saisami Parvathi',     'SP',  NULL, 'Assistant Professor', 18, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), abbreviation = VALUES(abbreviation), email = VALUES(email),
    designation = VALUES(designation), max_hours_per_week = VALUES(max_hours_per_week);

-- ---------------------------------------------------------------------------
-- SUBJECTS — Sem III
-- ---------------------------------------------------------------------------
INSERT INTO subject (subject_id, course_code, name, abbreviation, semester, type, theory_hrs, practical_hrs, tutorial_hrs, project_hrs) VALUES
(1,  '25PCC12EC01', 'Electronic Devices and Applications',              'EDA',  3, 'PCC',  2, 2, 0, 0),
(2,  '25PCC12EC02', 'Computer Organization and Architecture',           'COA',  3, 'PCC',  2, 2, 0, 0),
(3,  '25PCC12EC03', 'Database Management System',                       'DBMS', 3, 'PCC',  2, 2, 0, 0),
(4,  '25PCC12EC04', 'Object Oriented Programming with JAVA',            'OOPJ', 3, 'PCC',  0, 2, 0, 0),
(5,  '25OE312EC01', 'Law for Engineers',                                'LE',   3, 'OE',   2, 0, 0, 0),
(6,  '25OE312EC02', 'Financial Planning, Taxation & Investment',        'FPTI', 3, 'OE',   2, 0, 0, 0),
(7,  '25MDM12EC01', 'MDM Course-1 Finance Accounting',                  'MDM1', 3, 'MDM',  2, 0, 0, 0),
(8,  '25MDM12EC02', 'MDM Course-2 Economics',                           'MDM2', 3, 'MDM',  2, 0, 0, 0),
(9,  '25HSSM2EC01', 'Modern Indian Language',                           'MIL',  3, 'HSSM', 2, 0, 0, 0),
(10, '25HSSM2EC02', 'Human Values and Professional Ethics',             'HVPE', 3, 'HSSM', 1, 2, 0, 0),
(11, '25CEP12EC01', 'Community Engagement Project',                     'CEP',  3, 'CEP',  0, 0, 0, 4),
(12, '25DM12EC01',  'Double Minor Course',                              'DM',   3, 'MDM',  2, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), theory_hrs = VALUES(theory_hrs),
    practical_hrs = VALUES(practical_hrs), tutorial_hrs = VALUES(tutorial_hrs),
    project_hrs = VALUES(project_hrs);

-- Sem IV subjects
INSERT INTO subject (subject_id, course_code, name, abbreviation, semester, type, theory_hrs, practical_hrs, tutorial_hrs) VALUES
(13, '25PCC22EC01', 'Mathematics and Numerical Methods',                'MNM',  4, 'BSC',  2, 0, 1),
(14, '25PCC22EC02', 'Analog Electronics Circuits',                      'AEC',  4, 'PCC',  2, 2, 0),
(15, '25PCC22EC03', 'Discrete Structures and Automata Theory',          'DSAT', 4, 'PCC',  2, 0, 2),
(16, '25PCC22EC04', 'Web Technologies Lab',                             'WTL',  4, 'PCC',  0, 2, 0),
(17, '25OE422EC01', 'Emerging Technology and Law',                      'ETL',  4, 'OE',   2, 0, 0),
(18, '25OE422EC02', 'Principles of Management',                         'POM',  4, 'OE',   2, 0, 0),
(19, '25MDM22EC01', 'MDM Course-3',                                     'MDM3', 4, 'MDM',  2, 0, 0),
(20, '25PCC22EC05', 'Data Structures',                                  'DS',   4, 'PCC',  3, 2, 1),
(21, '25HSSM2EC03', 'Technology Entrepreneurship',                      'TE',   4, 'HSSM', 2, 0, 0),
(22, '25HSSM2EC04', 'Technology Innovation for Sustainable Development','TISD', 4, 'HSSM', 1, 2, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), theory_hrs = VALUES(theory_hrs),
    practical_hrs = VALUES(practical_hrs), tutorial_hrs = VALUES(tutorial_hrs);

-- T.E. ECS Sem V. Internal course codes are used because official codes were not supplied.
-- Each listed subject has 2 weekly theory hours and one 2-hour practical block.
INSERT INTO subject (subject_id, course_code, name, abbreviation, semester, type, theory_hrs, practical_hrs, tutorial_hrs) VALUES
(23, 'TE5ECS-CS',    'Control Systems',                         'CS',    5, 'PCC', 2, 2, 0),
(24, 'TE5ECS-ADA',   'Analysis of Algorithms',                  'ADA',   5, 'PCC', 2, 2, 0),
(25, 'TE5ECS-AUT',   'Automation',                              'AUT',   5, 'PCC', 2, 2, 0),
(26, 'TE5ECS-ESI',   'Emotional and Spiritual Intelligence',    'ESI',   5, 'HSSM',2, 2, 0),
(27, 'TE5ECS-VLSI',  'VLSI Design',                             'VLSI',  5, 'PCC', 2, 2, 0),
(28, 'TE5ECS-SE',    'Software Engineering',                    'SE',    5, 'PCC', 2, 2, 0),
(29, 'TE5ECS-CRYPT', 'Cryptography',                            'CRYPT', 5, 'PCC', 2, 2, 0),
(30, 'TE5ECS-AI',    'Artificial Intelligence',                 'AI',    5, 'PCC', 2, 2, 0),
(31, 'TE5ECS-ERTOS', 'Embedded Systems and RTOS',               'ERTOS', 5, 'PCC', 2, 2, 0),
(32, 'TE5ECS-HWP',   'Health, Wellness and Psychology',         'HWP',   5, 'HSSM',2, 2, 0)
ON DUPLICATE KEY UPDATE
    course_code = VALUES(course_code), name = VALUES(name), abbreviation = VALUES(abbreviation),
    semester = VALUES(semester), type = VALUES(type), theory_hrs = VALUES(theory_hrs),
    practical_hrs = VALUES(practical_hrs), tutorial_hrs = VALUES(tutorial_hrs);

-- ---------------------------------------------------------------------------
-- ROOMS (based on typical FrCRCE layout; Room 307 confirmed from timetable)
-- ---------------------------------------------------------------------------
INSERT INTO room (room_id, name, type, capacity, location) VALUES
(1,  '307',         'CLASSROOM',    65,  'Third Floor, Main Building'),
(2,  '308',         'CLASSROOM',    65,  'Third Floor, Main Building'),
(3,  '309',         'CLASSROOM',    65,  'Third Floor, Main Building'),
(4,  '310',         'CLASSROOM',    65,  'Third Floor, Main Building'),
(5,  'ECS Lab 1',   'LAB',          30,  'Ground Floor, ECS Wing'),
(6,  'ECS Lab 2',   'LAB',          30,  'Ground Floor, ECS Wing'),
(7,  'Computer Lab 1','LAB',        30,  'First Floor, Computer Wing'),
(8,  'Computer Lab 2','LAB',        30,  'First Floor, Computer Wing'),
(9,  'Electronics Lab 1','LAB',     30,  'Second Floor, Electronics Wing'),
(10, 'Electronics Lab 2','LAB',     30,  'Second Floor, Electronics Wing'),
(11, 'Tutorial Room 1','TUTORIAL_ROOM', 30, 'Second Floor, Main Building'),
(12, 'Seminar Hall', 'SEMINAR_HALL', 120, 'Ground Floor, Main Building')
ON DUPLICATE KEY UPDATE name = VALUES(name), capacity = VALUES(capacity);

-- ---------------------------------------------------------------------------
-- TIME SLOTS (Mon–Sat, 8:45 AM – 4:30 PM; matching the real timetable)
-- Short break: 10:45–11:00; Lunch break: 13:00–13:30
-- ---------------------------------------------------------------------------

-- MONDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(1,  'MONDAY', '08:45:00','09:45:00', 1, FALSE),
(2,  'MONDAY', '09:45:00','10:45:00', 2, FALSE),
(3,  'MONDAY', '10:45:00','11:00:00', 3, TRUE),   -- Short Break
(4,  'MONDAY', '11:00:00','12:00:00', 4, FALSE),
(5,  'MONDAY', '12:00:00','13:00:00', 5, FALSE),
(6,  'MONDAY', '13:00:00','13:30:00', 6, TRUE),   -- Lunch Break
(7,  'MONDAY', '13:30:00','14:30:00', 7, FALSE),
(8,  'MONDAY', '14:30:00','15:30:00', 8, FALSE),
(9,  'MONDAY', '15:30:00','16:30:00', 9, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- TUESDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(10, 'TUESDAY', '08:45:00','09:45:00', 1, FALSE),
(11, 'TUESDAY', '09:45:00','10:45:00', 2, FALSE),
(12, 'TUESDAY', '10:45:00','11:00:00', 3, TRUE),
(13, 'TUESDAY', '11:00:00','12:00:00', 4, FALSE),
(14, 'TUESDAY', '12:00:00','13:00:00', 5, FALSE),
(15, 'TUESDAY', '13:00:00','13:30:00', 6, TRUE),
(16, 'TUESDAY', '13:30:00','14:30:00', 7, FALSE),
(17, 'TUESDAY', '14:30:00','15:30:00', 8, FALSE),
(18, 'TUESDAY', '15:30:00','16:30:00', 9, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- WEDNESDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(19, 'WEDNESDAY', '08:45:00','09:45:00', 1, FALSE),
(20, 'WEDNESDAY', '09:45:00','10:45:00', 2, FALSE),
(21, 'WEDNESDAY', '10:45:00','11:00:00', 3, TRUE),
(22, 'WEDNESDAY', '11:00:00','12:00:00', 4, FALSE),
(23, 'WEDNESDAY', '12:00:00','13:00:00', 5, FALSE),
(24, 'WEDNESDAY', '13:00:00','13:30:00', 6, TRUE),
(25, 'WEDNESDAY', '13:30:00','14:30:00', 7, FALSE),
(26, 'WEDNESDAY', '14:30:00','15:30:00', 8, FALSE),
(27, 'WEDNESDAY', '15:30:00','16:30:00', 9, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- THURSDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(28, 'THURSDAY', '08:45:00','09:45:00', 1, FALSE),
(29, 'THURSDAY', '09:45:00','10:45:00', 2, FALSE),
(30, 'THURSDAY', '10:45:00','11:00:00', 3, TRUE),
(31, 'THURSDAY', '11:00:00','12:00:00', 4, FALSE),
(32, 'THURSDAY', '12:00:00','13:00:00', 5, FALSE),
(33, 'THURSDAY', '13:00:00','13:30:00', 6, TRUE),
(34, 'THURSDAY', '13:30:00','14:30:00', 7, FALSE),
(35, 'THURSDAY', '14:30:00','15:30:00', 8, FALSE),
(36, 'THURSDAY', '15:30:00','16:30:00', 9, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- FRIDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(37, 'FRIDAY', '08:45:00','09:45:00', 1, FALSE),
(38, 'FRIDAY', '09:45:00','10:45:00', 2, FALSE),
(39, 'FRIDAY', '10:45:00','11:00:00', 3, TRUE),
(40, 'FRIDAY', '11:00:00','12:00:00', 4, FALSE),
(41, 'FRIDAY', '12:00:00','13:00:00', 5, FALSE),
(42, 'FRIDAY', '13:00:00','13:30:00', 6, TRUE),
(43, 'FRIDAY', '13:30:00','14:30:00', 7, FALSE),
(44, 'FRIDAY', '14:30:00','15:30:00', 8, FALSE),
(45, 'FRIDAY', '15:30:00','16:30:00', 9, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- SATURDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(46, 'SATURDAY', '08:45:00','09:45:00', 1, FALSE),
(47, 'SATURDAY', '09:45:00','10:45:00', 2, FALSE),
(48, 'SATURDAY', '10:45:00','11:00:00', 3, TRUE),
(49, 'SATURDAY', '11:00:00','12:00:00', 4, FALSE),
(50, 'SATURDAY', '12:00:00','13:00:00', 5, FALSE)
ON DUPLICATE KEY UPDATE start_time = VALUES(start_time), end_time = VALUES(end_time), is_break = VALUES(is_break);

-- ---------------------------------------------------------------------------
-- CLASS DIVISIONS — SE ECS (Sem III & IV) and T.E. ECS (Sem V, 2026-27)
-- ---------------------------------------------------------------------------
INSERT INTO class_division (class_id, year, semester, division, strength, academic_year, dept_id) VALUES
(1, 2, 3, 'A', 65, '2026-27', 1),
(2, 2, 4, 'A', 65, '2026-27', 1),
(3, 3, 5, 'A', 65, '2026-27', 1)
ON DUPLICATE KEY UPDATE strength = VALUES(strength);

-- ---------------------------------------------------------------------------
-- BATCHES — 4 batches per division (A, B, C, D) ~16 students each
-- ---------------------------------------------------------------------------
INSERT INTO batch (batch_id, class_id, batch_name, strength) VALUES
-- Sem III Division A batches
(1, 1, 'A', 16),
(2, 1, 'B', 16),
(3, 1, 'C', 16),
(4, 1, 'D', 17),
-- Sem IV Division A batches
(5, 2, 'A', 16),
(6, 2, 'B', 16),
(7, 2, 'C', 16),
(8, 2, 'D', 17),
-- Sem V Division A batches
(9, 3, 'A', 16),
(10, 3, 'B', 16),
(11, 3, 'C', 16),
(12, 3, 'D', 17)
ON DUPLICATE KEY UPDATE strength = VALUES(strength);

-- ---------------------------------------------------------------------------
-- USERS
-- password_hash is SHA-256 of the plaintext password
-- admin123  -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a
-- faculty123 -> 44c8f631c3736bab19b15168bee7acada3ae68e9e7f1a76d5fc5f9e0e3a6e5fc
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password_hash, role, faculty_id) VALUES
('admin',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN',   NULL),
('vvg',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 3),
('sup',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 1),
('dvb',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 2),
('al',      '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 4),
('ap',      '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 5),
('dab',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 6)
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- ---------------------------------------------------------------------------
-- FACULTY-SUBJECT ALLOCATION — Sem III (based on actual timetable)
-- DVB=EDA, SUP=COA, VVG=DBMS, AL=OOPJ, AP=HVPE
-- ---------------------------------------------------------------------------
INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) VALUES
-- EDA: Dr. D.V. Bhoir (theory + lab)
(2, 1, 1, 'TH'),
(2, 1, 1, 'PR'),
-- COA: Dr. Sapna Prabhu (theory + lab)
(1, 2, 1, 'TH'),
(7, 2, 1, 'PR'),  -- KR handles COA lab
-- DBMS: Prof. Vaibhav Godbole (theory + lab)
(3, 3, 1, 'TH'),
(3, 3, 1, 'PR'),
-- OOPJ: Prof. Archana Lopes (lab only)
(4, 4, 1, 'PR'),
-- Open Elective (LE/FPTI): Prof. I. Khan / Prof. K. Rodrigues
(8, 5, 1, 'TH'),  -- LE by IK
(7, 6, 1, 'TH'),  -- FPTI by KR
-- MDM-1 Finance Accounting
(5, 7, 1, 'TH'),  -- AP (tentative)
-- MDM-2 Economics
(5, 8, 1, 'TH'),
-- MIL/DM: Modern Indian Language
(10, 9, 1, 'TH'),
-- HVPE: Prof. Annivilia Pereira
(5, 10, 1, 'TH'),
(5, 10, 1, 'PR'),
-- CEP
(3, 11, 1, 'PJ'),
-- Double Minor
(6, 12, 1, 'TH')
ON DUPLICATE KEY UPDATE faculty_id = VALUES(faculty_id);

-- T.E. ECS Sem V allocations; ESI/HWP are retained as subjects but left unassigned.
INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) VALUES
(13, 23, 3, 'TH'), (13, 23, 3, 'PR'), -- Control Systems: Kavita Rathi
(4,  24, 3, 'TH'), (4,  24, 3, 'PR'), -- Analysis of Algorithms: Archana Lopes
(14, 25, 3, 'TH'), (14, 25, 3, 'PR'), -- Automation: Prajita Bhangale
(15, 27, 3, 'TH'), (15, 27, 3, 'PR'), -- VLSI Design: Swapnali Makdey
(3,  28, 3, 'TH'), (3,  28, 3, 'PR'), -- Software Engineering: Vaibhav Godbole
(16, 29, 3, 'TH'), (16, 29, 3, 'PR'), -- Cryptography: Ayen Modi
(17, 30, 3, 'TH'), (17, 30, 3, 'PR'), -- Artificial Intelligence: Vinita Vartak
(18, 31, 3, 'TH'), (18, 31, 3, 'PR')  -- Embedded Systems and RTOS: Saisami Parvathi
ON DUPLICATE KEY UPDATE faculty_id = VALUES(faculty_id);
