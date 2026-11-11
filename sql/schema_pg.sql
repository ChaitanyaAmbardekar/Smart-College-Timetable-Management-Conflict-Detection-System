-- =============================================================================
-- ECS Faculty Timetable Generator
-- Fr. Conceicao Rodrigues College of Engineering (FrCRCE), Bandra, Mumbai
-- Electronics and Computer Science (ECS) Department
-- Schema: PostgreSQL 14+
-- =============================================================================

-- Drop tables if re-initializing
DROP TABLE IF EXISTS timetable_entry CASCADE;
DROP TABLE IF EXISTS faculty_unavailability CASCADE;
DROP TABLE IF EXISTS faculty_subject CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS time_slot CASCADE;
DROP TABLE IF EXISTS room CASCADE;
DROP TABLE IF EXISTS batch CASCADE;
DROP TABLE IF EXISTS class_division CASCADE;
DROP TABLE IF EXISTS subject CASCADE;
DROP TABLE IF EXISTS faculty CASCADE;
DROP TABLE IF EXISTS department CASCADE;

-- ---------------------------------------------------------------------------
-- 1. DEPARTMENT
-- ---------------------------------------------------------------------------
CREATE TABLE department (
    dept_id SERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    code    VARCHAR(20)  NOT NULL UNIQUE
);

-- ---------------------------------------------------------------------------
-- 2. FACULTY
-- ---------------------------------------------------------------------------
CREATE TABLE faculty (
    faculty_id         SERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    abbreviation       VARCHAR(10)  NOT NULL,
    email              VARCHAR(150) UNIQUE,
    designation        VARCHAR(50)  NOT NULL DEFAULT 'Assistant Professor',
    max_hours_per_week SMALLINT     NOT NULL DEFAULT 20,
    dept_id            INT          NOT NULL REFERENCES department(dept_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_faculty_abbr UNIQUE (abbreviation, dept_id)
);

CREATE INDEX idx_faculty_dept ON faculty(dept_id);

-- ---------------------------------------------------------------------------
-- 3. SUBJECT
-- ---------------------------------------------------------------------------
CREATE TABLE subject (
    subject_id    SERIAL PRIMARY KEY,
    course_code   VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    abbreviation  VARCHAR(10)  NOT NULL,
    semester      SMALLINT     NOT NULL,
    type          VARCHAR(20)  NOT NULL DEFAULT 'PCC',
    theory_hrs    SMALLINT     NOT NULL DEFAULT 0,
    practical_hrs SMALLINT     NOT NULL DEFAULT 0,
    tutorial_hrs  SMALLINT     NOT NULL DEFAULT 0,
    project_hrs   SMALLINT     NOT NULL DEFAULT 0 CHECK (project_hrs >= 0),
    prac_mode     VARCHAR(15)  NOT NULL DEFAULT 'BATCH' CHECK (prac_mode IN ('BATCH','WHOLE_CLASS')),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_subject_semester ON subject(semester);

-- ---------------------------------------------------------------------------
-- 4. CLASS DIVISION
-- ---------------------------------------------------------------------------
CREATE TABLE class_division (
    class_id      SERIAL PRIMARY KEY,
    year          SMALLINT     NOT NULL,
    semester      SMALLINT     NOT NULL,
    division      VARCHAR(10)  NOT NULL,
    strength      INTEGER      NOT NULL DEFAULT 60,
    academic_year VARCHAR(9)   NOT NULL,
    dept_id       INT          NOT NULL REFERENCES department(dept_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_class_div UNIQUE (semester, division, academic_year, dept_id)
);

CREATE INDEX idx_cd_semester ON class_division(semester);
CREATE INDEX idx_cd_dept ON class_division(dept_id);

-- ---------------------------------------------------------------------------
-- 5. BATCH
-- ---------------------------------------------------------------------------
CREATE TABLE batch (
    batch_id   SERIAL PRIMARY KEY,
    class_id   INT         NOT NULL REFERENCES class_division(class_id) ON UPDATE CASCADE ON DELETE CASCADE,
    batch_name VARCHAR(10) NOT NULL,
    strength   INTEGER     NOT NULL DEFAULT 20,
    CONSTRAINT uq_batch UNIQUE (class_id, batch_name)
);

CREATE INDEX idx_batch_class ON batch(class_id);

-- ---------------------------------------------------------------------------
-- 6. ROOM
-- ---------------------------------------------------------------------------
CREATE TABLE room (
    room_id   SERIAL PRIMARY KEY,
    name      VARCHAR(50)  NOT NULL UNIQUE,
    type      VARCHAR(30)  NOT NULL DEFAULT 'CLASSROOM',
    capacity  INTEGER      NOT NULL DEFAULT 60,
    location  VARCHAR(100),
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------------
-- 7. TIME SLOT
-- ---------------------------------------------------------------------------
CREATE TABLE time_slot (
    slot_id    SERIAL PRIMARY KEY,
    day        VARCHAR(20) NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    slot_no    SMALLINT    NOT NULL,
    is_break   BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_slot UNIQUE (day, slot_no),
    CONSTRAINT chk_time CHECK (end_time > start_time)
);

CREATE INDEX idx_slot_day ON time_slot(day);

-- ---------------------------------------------------------------------------
-- 8. FACULTY-SUBJECT ALLOCATION
-- ---------------------------------------------------------------------------
CREATE TABLE faculty_subject (
    id           SERIAL PRIMARY KEY,
    faculty_id   INT         NOT NULL REFERENCES faculty(faculty_id) ON UPDATE CASCADE ON DELETE CASCADE,
    subject_id   INT         NOT NULL REFERENCES subject(subject_id) ON UPDATE CASCADE ON DELETE CASCADE,
    class_id     INT         NOT NULL REFERENCES class_division(class_id) ON UPDATE CASCADE ON DELETE CASCADE,
    session_type VARCHAR(10) NOT NULL DEFAULT 'ALL' CHECK (session_type IN ('TH','PR','TU','PJ','ALL')),
    prac_mode    VARCHAR(15) NOT NULL DEFAULT 'BATCH' CHECK (prac_mode IN ('BATCH','WHOLE_CLASS')),
    CONSTRAINT uq_faculty_subject_class UNIQUE (faculty_id, subject_id, class_id, session_type)
);

CREATE INDEX idx_fs_faculty ON faculty_subject(faculty_id);
CREATE INDEX idx_fs_subject ON faculty_subject(subject_id);
CREATE INDEX idx_fs_class   ON faculty_subject(class_id);

-- ---------------------------------------------------------------------------
-- 9. FACULTY UNAVAILABILITY
-- ---------------------------------------------------------------------------
CREATE TABLE faculty_unavailability (
    id         SERIAL PRIMARY KEY,
    faculty_id INT          NOT NULL REFERENCES faculty(faculty_id) ON UPDATE CASCADE ON DELETE CASCADE,
    slot_id    INT          NOT NULL REFERENCES time_slot(slot_id) ON UPDATE CASCADE ON DELETE CASCADE,
    reason     VARCHAR(255),
    CONSTRAINT uq_unavail UNIQUE (faculty_id, slot_id)
);

CREATE INDEX idx_unavail_faculty ON faculty_unavailability(faculty_id);

-- ---------------------------------------------------------------------------
-- 10. TIMETABLE ENTRY (central fact table)
-- ---------------------------------------------------------------------------
CREATE TABLE timetable_entry (
    entry_id      SERIAL PRIMARY KEY,
    class_id      INT         NOT NULL REFERENCES class_division(class_id) ON UPDATE CASCADE ON DELETE CASCADE,
    batch_id      INT         NULL REFERENCES batch(batch_id) ON UPDATE CASCADE ON DELETE SET NULL,
    subject_id    INT         NOT NULL REFERENCES subject(subject_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    faculty_id    INT         NOT NULL REFERENCES faculty(faculty_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    room_id       INT         NOT NULL REFERENCES room(room_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    slot_id       INT         NOT NULL REFERENCES time_slot(slot_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    session_type  VARCHAR(10) NOT NULL CHECK (session_type IN ('TH','PR','TU','PJ')),
    academic_year VARCHAR(9)  NOT NULL DEFAULT '2026-27',
    is_locked     BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_faculty_slot     UNIQUE (faculty_id, slot_id, academic_year),
    CONSTRAINT uq_class_batch_slot UNIQUE (class_id,   batch_id, slot_id, academic_year),
    CONSTRAINT uq_room_slot        UNIQUE (room_id,    slot_id, academic_year)
);

CREATE INDEX idx_te_class     ON timetable_entry(class_id);
CREATE INDEX idx_te_faculty   ON timetable_entry(faculty_id);
CREATE INDEX idx_te_room      ON timetable_entry(room_id);
CREATE INDEX idx_te_slot      ON timetable_entry(slot_id);
CREATE INDEX idx_te_composite ON timetable_entry(academic_year, class_id, slot_id);

-- ---------------------------------------------------------------------------
-- 11. USERS
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    username      VARCHAR(50)  PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'FACULTY',
    faculty_id    INT          NULL REFERENCES faculty(faculty_id) ON UPDATE CASCADE ON DELETE SET NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP
);

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
    CONCAT(cd.year, '/', cd.division, ' Sem', cd.semester) AS class_label,
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
    CONCAT(cd.year, '/', cd.division, ' Sem', cd.semester) AS class_label,
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
-- TRIGGER FUNCTION: check faculty max hours
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_check_faculty_max_hours()
RETURNS TRIGGER AS $$
DECLARE
    v_scheduled INT := 0;
    v_max_hours INT := 0;
BEGIN
    SELECT COUNT(*) INTO v_scheduled
    FROM timetable_entry
    WHERE faculty_id = NEW.faculty_id AND academic_year = NEW.academic_year;

    SELECT max_hours_per_week INTO v_max_hours
    FROM faculty WHERE faculty_id = NEW.faculty_id;

    IF v_scheduled >= v_max_hours THEN
        RAISE EXCEPTION 'Faculty has reached the maximum allowed hours per week';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_faculty_max_hours ON timetable_entry;
CREATE TRIGGER trg_check_faculty_max_hours
BEFORE INSERT ON timetable_entry
FOR EACH ROW EXECUTE FUNCTION fn_check_faculty_max_hours();

