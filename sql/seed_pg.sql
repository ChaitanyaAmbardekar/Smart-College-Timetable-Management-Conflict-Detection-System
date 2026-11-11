-- =============================================================================
-- SEED DATA — PostgreSQL compatible
-- =============================================================================

-- DEPARTMENT
INSERT INTO department (dept_id, name, code) VALUES
(1, 'Electronics and Computer Science', 'ECS'),
(2, 'Computer Engineering',             'CE'),
(3, 'Information Technology',           'IT')
ON CONFLICT (dept_id) DO UPDATE SET name = EXCLUDED.name;

-- Reset sequence
SELECT setval('department_dept_id_seq', (SELECT MAX(dept_id) FROM department));

-- FACULTY (from actual FrCRCE timetable)
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
ON CONFLICT (faculty_id) DO UPDATE SET
    name = EXCLUDED.name, abbreviation = EXCLUDED.abbreviation,
    designation = EXCLUDED.designation, max_hours_per_week = EXCLUDED.max_hours_per_week;

SELECT setval('faculty_faculty_id_seq', (SELECT MAX(faculty_id) FROM faculty));

-- T.E. ECS faculty (Sem V); null emails were not present in the supplied list.
-- KRT is used for Kavita Rathi because KR is already assigned to K. Rodrigues.
INSERT INTO faculty (faculty_id, name, abbreviation, email, designation, max_hours_per_week, dept_id) VALUES
(13, 'Prof. Kavita Rathi',       'KRT', NULL, 'Assistant Professor', 18, 1),
(14, 'Dr. Prajita Bhangale',     'PKB', NULL, 'Assistant Professor', 18, 1),
(15, 'Dr. Swapnali Makdey',      'SAM', NULL, 'Assistant Professor', 18, 1),
(16, 'Prof. Ayen Modi',          'JM',  NULL, 'Assistant Professor', 18, 1),
(17, 'Prof. Vinita Vartak',      'VV',  NULL, 'Assistant Professor', 18, 1),
(18, 'Dr. Saisami Parvathi',     'SP',  NULL, 'Assistant Professor', 18, 1)
ON CONFLICT (faculty_id) DO UPDATE SET
    name = EXCLUDED.name, abbreviation = EXCLUDED.abbreviation, email = EXCLUDED.email,
    designation = EXCLUDED.designation, max_hours_per_week = EXCLUDED.max_hours_per_week,
    dept_id = EXCLUDED.dept_id;

SELECT setval('faculty_faculty_id_seq', (SELECT MAX(faculty_id) FROM faculty));

-- SUBJECTS (Sem III)
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
ON CONFLICT (subject_id) DO UPDATE SET
    name = EXCLUDED.name, theory_hrs = EXCLUDED.theory_hrs,
    practical_hrs = EXCLUDED.practical_hrs, tutorial_hrs = EXCLUDED.tutorial_hrs,
    project_hrs = EXCLUDED.project_hrs;

-- SUBJECTS (Sem IV)
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
ON CONFLICT (subject_id) DO UPDATE SET
    name = EXCLUDED.name, theory_hrs = EXCLUDED.theory_hrs,
    practical_hrs = EXCLUDED.practical_hrs, tutorial_hrs = EXCLUDED.tutorial_hrs;

SELECT setval('subject_subject_id_seq', (SELECT MAX(subject_id) FROM subject));

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
ON CONFLICT (subject_id) DO UPDATE SET
    course_code = EXCLUDED.course_code, name = EXCLUDED.name, abbreviation = EXCLUDED.abbreviation,
    semester = EXCLUDED.semester, type = EXCLUDED.type, theory_hrs = EXCLUDED.theory_hrs,
    practical_hrs = EXCLUDED.practical_hrs, tutorial_hrs = EXCLUDED.tutorial_hrs;

SELECT setval('subject_subject_id_seq', (SELECT MAX(subject_id) FROM subject));

-- ROOMS
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
ON CONFLICT (room_id) DO UPDATE SET name = EXCLUDED.name, capacity = EXCLUDED.capacity;

SELECT setval('room_room_id_seq', (SELECT MAX(room_id) FROM room));

-- TIME SLOTS (Mon–Sat)
INSERT INTO time_slot (slot_id, day, start_time, end_time, slot_no, is_break) VALUES
-- MONDAY
(1,  'MONDAY', '08:45:00','09:45:00', 1, FALSE),
(2,  'MONDAY', '09:45:00','10:45:00', 2, FALSE),
(3,  'MONDAY', '10:45:00','11:00:00', 3, TRUE),
(4,  'MONDAY', '11:00:00','12:00:00', 4, FALSE),
(5,  'MONDAY', '12:00:00','13:00:00', 5, FALSE),
(6,  'MONDAY', '13:00:00','13:30:00', 6, TRUE),
(7,  'MONDAY', '13:30:00','14:30:00', 7, FALSE),
(8,  'MONDAY', '14:30:00','15:30:00', 8, FALSE),
(9,  'MONDAY', '15:30:00','16:30:00', 9, FALSE),
-- TUESDAY
(10, 'TUESDAY', '08:45:00','09:45:00', 1, FALSE),
(11, 'TUESDAY', '09:45:00','10:45:00', 2, FALSE),
(12, 'TUESDAY', '10:45:00','11:00:00', 3, TRUE),
(13, 'TUESDAY', '11:00:00','12:00:00', 4, FALSE),
(14, 'TUESDAY', '12:00:00','13:00:00', 5, FALSE),
(15, 'TUESDAY', '13:00:00','13:30:00', 6, TRUE),
(16, 'TUESDAY', '13:30:00','14:30:00', 7, FALSE),
(17, 'TUESDAY', '14:30:00','15:30:00', 8, FALSE),
(18, 'TUESDAY', '15:30:00','16:30:00', 9, FALSE),
-- WEDNESDAY
(19, 'WEDNESDAY', '08:45:00','09:45:00', 1, FALSE),
(20, 'WEDNESDAY', '09:45:00','10:45:00', 2, FALSE),
(21, 'WEDNESDAY', '10:45:00','11:00:00', 3, TRUE),
(22, 'WEDNESDAY', '11:00:00','12:00:00', 4, FALSE),
(23, 'WEDNESDAY', '12:00:00','13:00:00', 5, FALSE),
(24, 'WEDNESDAY', '13:00:00','13:30:00', 6, TRUE),
(25, 'WEDNESDAY', '13:30:00','14:30:00', 7, FALSE),
(26, 'WEDNESDAY', '14:30:00','15:30:00', 8, FALSE),
(27, 'WEDNESDAY', '15:30:00','16:30:00', 9, FALSE),
-- THURSDAY
(28, 'THURSDAY', '08:45:00','09:45:00', 1, FALSE),
(29, 'THURSDAY', '09:45:00','10:45:00', 2, FALSE),
(30, 'THURSDAY', '10:45:00','11:00:00', 3, TRUE),
(31, 'THURSDAY', '11:00:00','12:00:00', 4, FALSE),
(32, 'THURSDAY', '12:00:00','13:00:00', 5, FALSE),
(33, 'THURSDAY', '13:00:00','13:30:00', 6, TRUE),
(34, 'THURSDAY', '13:30:00','14:30:00', 7, FALSE),
(35, 'THURSDAY', '14:30:00','15:30:00', 8, FALSE),
(36, 'THURSDAY', '15:30:00','16:30:00', 9, FALSE),
-- FRIDAY
(37, 'FRIDAY', '08:45:00','09:45:00', 1, FALSE),
(38, 'FRIDAY', '09:45:00','10:45:00', 2, FALSE),
(39, 'FRIDAY', '10:45:00','11:00:00', 3, TRUE),
(40, 'FRIDAY', '11:00:00','12:00:00', 4, FALSE),
(41, 'FRIDAY', '12:00:00','13:00:00', 5, FALSE),
(42, 'FRIDAY', '13:00:00','13:30:00', 6, TRUE),
(43, 'FRIDAY', '13:30:00','14:30:00', 7, FALSE),
(44, 'FRIDAY', '14:30:00','15:30:00', 8, FALSE),
(45, 'FRIDAY', '15:30:00','16:30:00', 9, FALSE),
-- SATURDAY
(46, 'SATURDAY', '08:45:00','09:45:00', 1, FALSE),
(47, 'SATURDAY', '09:45:00','10:45:00', 2, FALSE),
(48, 'SATURDAY', '10:45:00','11:00:00', 3, TRUE),
(49, 'SATURDAY', '11:00:00','12:00:00', 4, FALSE),
(50, 'SATURDAY', '12:00:00','13:00:00', 5, FALSE)
ON CONFLICT (slot_id) DO UPDATE SET start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time;

SELECT setval('time_slot_slot_id_seq', (SELECT MAX(slot_id) FROM time_slot));

-- CLASS DIVISION
INSERT INTO class_division (class_id, year, semester, division, strength, academic_year, dept_id) VALUES
(1, 2, 3, 'A', 65, '2026-27', 1),
(2, 2, 4, 'A', 65, '2026-27', 1),
(3, 3, 5, 'A', 65, '2026-27', 1)
ON CONFLICT (class_id) DO UPDATE SET strength = EXCLUDED.strength;

SELECT setval('class_division_class_id_seq', (SELECT MAX(class_id) FROM class_division));

-- BATCHES
INSERT INTO batch (batch_id, class_id, batch_name, strength) VALUES
(1, 1, 'A', 16),
(2, 1, 'B', 16),
(3, 1, 'C', 16),
(4, 1, 'D', 17),
(5, 2, 'A', 16),
(6, 2, 'B', 16),
(7, 2, 'C', 16),
(8, 2, 'D', 17),
(9, 3, 'A', 16),
(10, 3, 'B', 16),
(11, 3, 'C', 16),
(12, 3, 'D', 17)
ON CONFLICT (batch_id) DO UPDATE SET strength = EXCLUDED.strength;

SELECT setval('batch_batch_id_seq', (SELECT MAX(batch_id) FROM batch));

-- USERS
INSERT INTO users (username, password_hash, role, faculty_id) VALUES
('admin',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN',   NULL),
('vvg',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 3),
('sup',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 1),
('dvb',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 2),
('al',      '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 4),
('ap',      '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 5),
('dab',     '27041f5856c7387a997252694afb048d1aa939228ffcdbd6285b979b8da20e7a', 'FACULTY', 6)
ON CONFLICT (username) DO UPDATE SET role = EXCLUDED.role;

-- ALLOCATIONS
INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) VALUES
(2, 1, 1, 'TH'),
(2, 1, 1, 'PR'),
(1, 2, 1, 'TH'),
(7, 2, 1, 'PR'),
(3, 3, 1, 'TH'),
(3, 3, 1, 'PR'),
(4, 4, 1, 'PR'),
(8, 5, 1, 'TH'),
(7, 6, 1, 'TH'),
(5, 7, 1, 'TH'),
(5, 8, 1, 'TH'),
(10, 9, 1, 'TH'),
(5, 10, 1, 'TH'),
(5, 10, 1, 'PR'),
(3, 11, 1, 'PJ'),
(6, 12, 1, 'TH')
ON CONFLICT DO NOTHING;

-- T.E. ECS Sem V allocations; ESI/HWP remain unassigned.
INSERT INTO faculty_subject (faculty_id, subject_id, class_id, session_type) VALUES
(13, 23, 3, 'TH'), (13, 23, 3, 'PR'),
(4,  24, 3, 'TH'), (4,  24, 3, 'PR'),
(14, 25, 3, 'TH'), (14, 25, 3, 'PR'),
(15, 27, 3, 'TH'), (15, 27, 3, 'PR'),
(3,  28, 3, 'TH'), (3,  28, 3, 'PR'),
(16, 29, 3, 'TH'), (16, 29, 3, 'PR'),
(17, 30, 3, 'TH'), (17, 30, 3, 'PR'),
(18, 31, 3, 'TH'), (18, 31, 3, 'PR')
ON CONFLICT DO NOTHING;

SELECT setval('faculty_subject_id_seq', (SELECT MAX(id) FROM faculty_subject));

