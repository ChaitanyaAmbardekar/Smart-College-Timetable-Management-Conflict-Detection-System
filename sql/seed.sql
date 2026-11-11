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
(14, 'Dr. Prajita Bhangale',     'PVB', NULL, 'Assistant Professor', 18, 1),
(15, 'Dr. Swapnali Makdey',      'SAM', NULL, 'Assistant Professor', 18, 1),
(16, 'Prof. Jayan Modi',         'JM',  NULL, 'Assistant Professor', 18, 1),
(17, 'Prof. Vinita Vartak',      'VV',  NULL, 'Assistant Professor', 18, 1),
(18, 'Dr. Sailaxmi Parvathi',    'SP',  NULL, 'Assistant Professor', 18, 1),
(19, 'Dr. Joseph Rodrigues',     'JR',  NULL, 'Assistant Professor', 18, 1),
(20, 'Prof. B. J.',              'BJ',  NULL, 'Assistant Professor', 18, 1),
(21, 'Prof. K. K.',              'KK',  NULL, 'Assistant Professor', 18, 1),
(22, 'Department Minor Faculty', 'DMF', NULL, 'Visiting Faculty',    20, 1)
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

-- T.E. ECS Sem V subjects (aligned with official Jul–Nov 2026 T.E. ECS timetable).
INSERT INTO subject (subject_id, course_code, name, abbreviation, semester, type, theory_hrs, practical_hrs, tutorial_hrs) VALUES
(23, 'TE5ECS-CS',     'Control Systems',                         'CS',     5, 'PCC', 2, 2, 0),
(24, 'TE5ECS-AOA',    'Analysis of Algorithms',                  'AOA',    5, 'PCC', 2, 2, 0),
(25, 'TE5ECS-AUT',    'Automation',                              'AUT',    5, 'PCC', 2, 2, 0),
(26, 'TE5ECS-ESI',    'Emotional and Spiritual Intelligence',    'ESI',    5, 'HSSM',1, 0, 0),
(27, 'TE5ECS-VLSI',   'VLSI Design',                             'VLSI',   5, 'PCC', 2, 2, 0),
(28, 'TE5ECS-SE',     'Software Engineering',                    'SE',     5, 'PCC', 2, 0, 0),
(29, 'TE5ECS-CRYPT',  'Cryptography',                            'CRYPT',  5, 'PCC', 2, 2, 0),
(30, 'TE5ECS-AI',     'Artificial Intelligence',                 'AI',     5, 'PCC', 2, 2, 0),
(31, 'TE5ECS-ESRTOS', 'Embedded Systems and RTOS',               'ESRTOS', 5, 'PCC', 1, 2, 0),
(32, 'TE5ECS-HWP',    'Health, Wellness and Psychology',         'HWP',    5, 'HSSM',1, 0, 0),
(33, 'TE5ECS-PEL',    'Program Elective Lab',                    'PEL',    5, 'EL',  0, 2, 0),
(34, 'TE5ECS-DM',     'Double Minor Course',                     'DM',     5, 'MDM', 1, 0, 1),
(35, 'TE5ECS-MDM',    'Multidisciplinary Minor',                 'MDM',    5, 'MDM', 4, 0, 0)
ON DUPLICATE KEY UPDATE
    course_code = VALUES(course_code), name = VALUES(name), abbreviation = VALUES(abbreviation),
    semester = VALUES(semester), type = VALUES(type), theory_hrs = VALUES(theory_hrs),
    practical_hrs = VALUES(practical_hrs), tutorial_hrs = VALUES(tutorial_hrs);

-- ---------------------------------------------------------------------------
-- ROOMS (Classrooms: 302, 303, 306, 307; Labs: 301, 309, 310, 311, 312;
--        308 inactive so auto-generator uses 302/303/306/307 while Sem V view shows 308)
-- ---------------------------------------------------------------------------
INSERT INTO room (room_id, name, type, capacity, location, is_active) VALUES
(1,  '302', 'CLASSROOM', 65, 'Third Floor', TRUE),
(2,  '303', 'CLASSROOM', 65, 'Third Floor', TRUE),
(3,  '306', 'CLASSROOM', 65, 'Third Floor', TRUE),
(4,  '307', 'CLASSROOM', 65, 'Third Floor', TRUE),
(5,  '301', 'LAB',       30, 'Third Floor', TRUE),
(6,  '309', 'LAB',       30, 'Third Floor', TRUE),
(7,  '310', 'LAB',       30, 'Third Floor', TRUE),
(8,  '311', 'LAB',       30, 'Third Floor', TRUE),
(9,  '312', 'LAB',       30, 'Third Floor', TRUE),
(10, '308', 'CLASSROOM', 65, 'Third Floor', FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), capacity = VALUES(capacity), is_active = VALUES(is_active);

-- ---------------------------------------------------------------------------
-- TIME SLOTS (Mon–Fri: 8:45 AM – 3:30 PM; Sat: 8:45 AM – 1:00 PM)
-- Short break: 10:45–11:00; Lunch break: 13:00–13:30
-- ---------------------------------------------------------------------------
DELETE FROM timetable_entry WHERE slot_id IN (SELECT slot_id FROM (SELECT slot_id FROM time_slot WHERE slot_no = 9) AS tmp);
DELETE FROM time_slot WHERE slot_no = 9;

-- MONDAY
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
(1,  'MONDAY', '08:45:00','09:45:00', 1, FALSE),
(2,  'MONDAY', '09:45:00','10:45:00', 2, FALSE),
(3,  'MONDAY', '10:45:00','11:00:00', 3, TRUE),   -- Short Break
(4,  'MONDAY', '11:00:00','12:00:00', 4, FALSE),
(5,  'MONDAY', '12:00:00','13:00:00', 5, FALSE),
(6,  'MONDAY', '13:00:00','13:30:00', 6, TRUE),   -- Lunch Break
(7,  'MONDAY', '13:30:00','14:30:00', 7, FALSE),
(8,  'MONDAY', '14:30:00','15:30:00', 8, FALSE)
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
(17, 'TUESDAY', '14:30:00','15:30:00', 8, FALSE)
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
(26, 'WEDNESDAY', '14:30:00','15:30:00', 8, FALSE)
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
(35, 'THURSDAY', '14:30:00','15:30:00', 8, FALSE)
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
(44, 'FRIDAY', '14:30:00','15:30:00', 8, FALSE)
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

-- T.E. ECS Sem V allocations (aligned with present Jul–Nov 2026 timetable)
DELETE FROM faculty_subject WHERE class_id = 3;

INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) VALUES
(13, 23, 3, 'TH'), (13, 23, 3, 'PR'), -- Control Systems: Prof. Kavita Rathi (KRT)
(16, 23, 3, 'PR'),                     -- CS PR batches: Prof. Jayan Modi (JM)
(20, 23, 3, 'PR'),                     -- CS PR Batch A: Prof. B. J. (BJ)
(4,  24, 3, 'PR'),                     -- Analysis of Algorithms PR: Prof. Archana Lopes (AL)
(16, 25, 3, 'TH'), (16, 25, 3, 'PR'), -- Automation (PEC): Prof. Jayan Modi (JM)
(21, 26, 3, 'TH'),                     -- ESI: Prof. K. K. (KK)
(15, 27, 3, 'TH'), (15, 27, 3, 'PR'), -- VLSI Design: Dr. Swapnali Makdey (SAM)
(20, 27, 3, 'PR'),                     -- VLSI PR batches: Prof. B. J. (BJ)
(3,  28, 3, 'TH'),                     -- Software Engineering: Prof. Vaibhav Godbole (VVG)
(14, 29, 3, 'TH'), (14, 29, 3, 'PR'), -- Cryptography (PEC): Dr. Prajita Bhangale (PVB)
(17, 30, 3, 'TH'), (17, 30, 3, 'PR'), -- Artificial Intelligence: Prof. Vinita Vartak (VV)
(13, 30, 3, 'PR'),                     -- AI PR batches: Prof. Kavita Rathi (KRT)
(18, 31, 3, 'TH'), (18, 31, 3, 'PR'), -- Embedded Systems & RTOS: Dr. Sailaxmi Parvathi (SP)
(19, 32, 3, 'TH'),                     -- HWP: Dr. Joseph Rodrigues (JR)
(14, 33, 3, 'PR'),                     -- PEL: Dr. Prajita Bhangale (PVB)
(2,  33, 3, 'PR'),                     -- PEL (BIO): Dr. D.V. Bhoir (DVB)
(22, 34, 3, 'TH'), (22, 34, 3, 'TU'), -- Double Minor (DM TH + DM TUT)
(22, 35, 3, 'TH')                      -- Multidisciplinary Minor (MDM)
ON DUPLICATE KEY UPDATE faculty_id = VALUES(faculty_id);

-- ---------------------------------------------------------------------------
-- PRESENT T.E. ECS (Sem V) TIMETABLE (w.e.f. 20 Jul 2026 – 21 Nov 2026)
-- Locked entries so Sem III generation automatically respects all Sem V faculty & room commitments.
-- ---------------------------------------------------------------------------
DELETE FROM timetable_entry WHERE academic_year = '2026-27' AND (class_id = 3 OR is_locked = FALSE);

INSERT INTO timetable_entry (class_id, batch_id, subject_id, faculty_id, room_id, slot_id, session_type, academic_year, is_locked) VALUES
-- MONDAY
(3, NULL, 25, 16, 4,  2,  'TH', '2026-27', TRUE), -- 09:45-10:45 PEC AUT (JM, 307)
(3, NULL, 29, 14, 10, 2,  'TH', '2026-27', TRUE), -- 09:45-10:45 PEC CRYPT (PKB, 308)
(3, 9,    33, 14, 5,  4,  'PR', '2026-27', TRUE), -- 11:00-12:00 PEL Batch A (PKB)
(3, 9,    33, 14, 5,  5,  'PR', '2026-27', TRUE), -- 12:00-13:00 PEL Batch A (PKB)
(3, 10,   24, 4,  6,  4,  'PR', '2026-27', TRUE), -- 11:00-12:00 AOA Batch B (AL)
(3, 10,   24, 4,  6,  5,  'PR', '2026-27', TRUE), -- 12:00-13:00 AOA Batch B (AL)
(3, 11,   27, 20, 7,  4,  'PR', '2026-27', TRUE), -- 11:00-12:00 VLSI Batch C (BJ)
(3, 11,   27, 20, 7,  5,  'PR', '2026-27', TRUE), -- 12:00-13:00 VLSI Batch C (BJ)
(3, 12,   30, 17, 8,  4,  'PR', '2026-27', TRUE), -- 11:00-12:00 AI Batch D (VV)
(3, 12,   30, 17, 8,  5,  'PR', '2026-27', TRUE), -- 12:00-13:00 AI Batch D (VV)
(3, 9,    27, 20, 5,  7,  'PR', '2026-27', TRUE), -- 13:30-14:30 VLSI Batch A (BJ)
(3, 9,    27, 20, 5,  8,  'PR', '2026-27', TRUE), -- 14:30-15:30 VLSI Batch A (BJ)
(3, 10,   33, 14, 6,  7,  'PR', '2026-27', TRUE), -- 13:30-14:30 PEL Batch B (PKB)
(3, 10,   33, 14, 6,  8,  'PR', '2026-27', TRUE), -- 14:30-15:30 PEL Batch B (PKB)
(3, 11,   24, 4,  7,  7,  'PR', '2026-27', TRUE), -- 13:30-14:30 AOA Batch C (AL)
(3, 11,   24, 4,  7,  8,  'PR', '2026-27', TRUE), -- 14:30-15:30 AOA Batch C (AL)
(3, 12,   31, 18, 8,  7,  'PR', '2026-27', TRUE), -- 13:30-14:30 ESRTOS Batch D (SP)
(3, 12,   31, 18, 8,  8,  'PR', '2026-27', TRUE), -- 14:30-15:30 ESRTOS Batch D (SP)
-- TUESDAY
(3, NULL, 28, 3,  3,  10, 'TH', '2026-27', TRUE), -- 08:45-09:45 SE (VVG, 306)
(3, NULL, 23, 13, 3,  11, 'TH', '2026-27', TRUE), -- 09:45-10:45 CS (KRT, 306)
(3, 9,    24, 4,  5,  13, 'PR', '2026-27', TRUE), -- 11:00-12:00 AOA Batch A (AL)
(3, 9,    24, 4,  5,  14, 'PR', '2026-27', TRUE), -- 12:00-13:00 AOA Batch A (AL)
(3, 10,   31, 18, 6,  13, 'PR', '2026-27', TRUE), -- 11:00-12:00 ESRTOS Batch B (SP)
(3, 10,   31, 18, 6,  14, 'PR', '2026-27', TRUE), -- 12:00-13:00 ESRTOS Batch B (SP)
(3, 11,   25, 16, 7,  13, 'PR', '2026-27', TRUE), -- 11:00-12:00 PEC AUT Batch C (JM)
(3, 11,   25, 16, 7,  14, 'PR', '2026-27', TRUE), -- 12:00-13:00 PEC AUT Batch C (JM)
(3, 12,   29, 14, 8,  13, 'PR', '2026-27', TRUE), -- 11:00-12:00 PEC CRYPT Batch D (PKB)
(3, 12,   29, 14, 8,  14, 'PR', '2026-27', TRUE), -- 12:00-13:00 PEC CRYPT Batch D (PKB)
(3, 9,    23, 20, 5,  16, 'PR', '2026-27', TRUE), -- 13:30-14:30 CS Batch A (BJ)
(3, 9,    23, 20, 5,  17, 'PR', '2026-27', TRUE), -- 14:30-15:30 CS Batch A (BJ)
(3, 10,   29, 14, 6,  16, 'PR', '2026-27', TRUE), -- 13:30-14:30 PEC CRYPT Batch B (PKB)
(3, 10,   29, 14, 6,  17, 'PR', '2026-27', TRUE), -- 14:30-15:30 PEC CRYPT Batch B (PKB)
(3, 11,   30, 17, 7,  16, 'PR', '2026-27', TRUE), -- 13:30-14:30 AI Batch C (VV)
(3, 11,   30, 17, 7,  17, 'PR', '2026-27', TRUE), -- 14:30-15:30 AI Batch C (VV)
(3, 12,   23, 16, 8,  16, 'PR', '2026-27', TRUE), -- 13:30-14:30 CS Batch D (JM)
(3, 12,   23, 16, 8,  17, 'PR', '2026-27', TRUE), -- 14:30-15:30 CS Batch D (JM)
-- WEDNESDAY
(3, NULL, 32, 19, 3,  20, 'TH', '2026-27', TRUE), -- 09:45-10:45 HWP (JR, 306)
(3, NULL, 26, 21, 1,  20, 'TH', '2026-27', TRUE), -- 09:45-10:45 ESI (KK, 302)
(3, NULL, 31, 18, 3,  22, 'TH', '2026-27', TRUE), -- 11:00-12:00 ESRTOS (SP, 306)
(3, NULL, 25, 16, 4,  23, 'TH', '2026-27', TRUE), -- 12:00-13:00 PEC AUT (JM, 307)
(3, NULL, 29, 14, 10, 23, 'TH', '2026-27', TRUE), -- 12:00-13:00 PEC CRYPT (PKB, 308)
(3, 9,    29, 14, 5,  25, 'PR', '2026-27', TRUE), -- 13:30-14:30 PEC CRYPT Batch A (PKB)
(3, 9,    29, 14, 5,  26, 'PR', '2026-27', TRUE), -- 14:30-15:30 PEC CRYPT Batch A (PKB)
(3, 10,   30, 13, 6,  25, 'PR', '2026-27', TRUE), -- 13:30-14:30 AI Batch B (KRT)
(3, 10,   30, 13, 6,  26, 'PR', '2026-27', TRUE), -- 14:30-15:30 AI Batch B (KRT)
(3, 11,   23, 16, 7,  25, 'PR', '2026-27', TRUE), -- 13:30-14:30 CS Batch C (JM)
(3, 11,   23, 16, 7,  26, 'PR', '2026-27', TRUE), -- 14:30-15:30 CS Batch C (JM)
(3, 12,   27, 15, 8,  25, 'PR', '2026-27', TRUE), -- 13:30-14:30 VLSI Batch D (SAM)
(3, 12,   27, 15, 8,  26, 'PR', '2026-27', TRUE), -- 14:30-15:30 VLSI Batch D (SAM)
-- THURSDAY
(3, NULL, 23, 13, 3,  28, 'TH', '2026-27', TRUE), -- 08:45-09:45 CS (KRT, 306)
(3, NULL, 28, 3,  3,  29, 'TH', '2026-27', TRUE), -- 09:45-10:45 SE (VVG, 306)
(3, NULL, 30, 17, 3,  31, 'TH', '2026-27', TRUE), -- 11:00-12:00 AI (VV, 306)
(3, NULL, 27, 15, 3,  32, 'TH', '2026-27', TRUE), -- 12:00-13:00 VLSI (SAM, 306)
(3, 9,    30, 13, 5,  34, 'PR', '2026-27', TRUE), -- 13:30-14:30 AI Batch A (KRT)
(3, 9,    30, 13, 5,  35, 'PR', '2026-27', TRUE), -- 14:30-15:30 AI Batch A (KRT)
(3, 10,   23, 16, 6,  34, 'PR', '2026-27', TRUE), -- 13:30-14:30 CS Batch B (JM)
(3, 10,   23, 16, 6,  35, 'PR', '2026-27', TRUE), -- 14:30-15:30 CS Batch B (JM)
(3, 11,   31, 18, 7,  34, 'PR', '2026-27', TRUE), -- 13:30-14:30 ESRTOS Batch C (SP)
(3, 11,   31, 18, 7,  35, 'PR', '2026-27', TRUE), -- 14:30-15:30 ESRTOS Batch C (SP)
(3, 12,   33, 14, 8,  34, 'PR', '2026-27', TRUE), -- 13:30-14:30 PEL Batch D (PKB)
(3, 12,   33, 14, 8,  35, 'PR', '2026-27', TRUE), -- 14:30-15:30 PEL Batch D (PKB)
-- FRIDAY
(3, NULL, 27, 15, 3,  37, 'TH', '2026-27', TRUE), -- 08:45-09:45 VLSI (SAM, 306)
(3, NULL, 30, 17, 3,  38, 'TH', '2026-27', TRUE), -- 09:45-10:45 AI (VV, 306)
(3, 9,    31, 18, 5,  40, 'PR', '2026-27', TRUE), -- 11:00-12:00 ESRTOS Batch A (SP)
(3, 9,    31, 18, 5,  41, 'PR', '2026-27', TRUE), -- 12:00-13:00 ESRTOS Batch A (SP)
(3, 10,   27, 20, 6,  40, 'PR', '2026-27', TRUE), -- 11:00-12:00 VLSI Batch B (BJ)
(3, 10,   27, 20, 6,  41, 'PR', '2026-27', TRUE), -- 12:00-13:00 VLSI Batch B (BJ)
(3, 11,   33, 2,  7,  40, 'PR', '2026-27', TRUE), -- 11:00-12:00 PEL BIO Batch C (DVB)
(3, 11,   33, 2,  7,  41, 'PR', '2026-27', TRUE), -- 12:00-13:00 PEL BIO Batch C (DVB)
(3, 12,   24, 4,  8,  40, 'PR', '2026-27', TRUE), -- 11:00-12:00 AOA Batch D (AL)
(3, 12,   24, 4,  8,  41, 'PR', '2026-27', TRUE), -- 12:00-13:00 AOA Batch D (AL)
(3, NULL, 34, 22, 3,  43, 'TH', '2026-27', TRUE), -- 13:30-14:30 DM(TH) (306)
(3, NULL, 34, 22, 3,  44, 'TU', '2026-27', TRUE), -- 14:30-15:30 DM(TUT) (306)
-- SATURDAY
(3, NULL, 35, 22, 3,  46, 'TH', '2026-27', TRUE), -- 08:45-09:45 MDM (306)
(3, NULL, 35, 22, 3,  47, 'TH', '2026-27', TRUE), -- 09:45-10:45 MDM (306)
(3, NULL, 35, 22, 3,  49, 'TH', '2026-27', TRUE), -- 11:00-12:00 MDM (306)
(3, NULL, 35, 22, 3,  50, 'TH', '2026-27', TRUE)  -- 12:00-13:00 MDM (306)
ON DUPLICATE KEY UPDATE
    subject_id = VALUES(subject_id),
    faculty_id = VALUES(faculty_id),
    room_id    = VALUES(room_id),
    session_type = VALUES(session_type),
    is_locked  = VALUES(is_locked);

