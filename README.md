# Smart College Timetable Management & Conflict Detection System

A Java-based timetable management system designed to simplify college timetable scheduling by providing a centralized view of multiple department timetables and automatically detecting scheduling conflicts.

## 📌 Problem Statement

College timetable scheduling becomes difficult when teachers have limited availability, especially when the same teacher needs to teach multiple departments. Manually comparing different department timetables to find conflict-free slots is time-consuming and error-prone.

This system provides a centralized view of multiple department timetables and automatically identifies available slots and conflicts involving **teachers, departments, and classrooms**.

---

## 🎯 Objectives

* View multiple department timetables simultaneously.
* Check teacher availability for a particular day and time.
* Detect teacher scheduling conflicts.
* Detect department timetable conflicts.
* Detect classroom/room conflicts.
* Recommend suitable time slots for a class.
* Provide a centralized timetable management interface.
* Eventually support automatic timetable generation.

---

## 🚀 Key Features

### 1. Multi-Department Timetable View

View the timetable status of multiple departments for a specific day and time.

Example:

| Department | 1:30 PM Status |
| ---------- | -------------- |
| E&CS       | Mathematics    |
| CSE        | FREE           |
| IT         | Java           |
| EXTC       | FREE           |
| AIDS       | FREE           |

This makes it easy to identify which departments can accommodate a particular teacher.

### 2. Teacher Availability

Store and manage the time slots during which each teacher is available.

Example:

```text
Teacher: Mrs. Sharma

Monday    1:30 PM  ✓
Tuesday   1:30 PM  ✗
Wednesday 10:30 AM ✓
Friday    1:30 PM  ✓
```

### 3. Conflict Detection

The system checks for:

* Teacher conflicts
* Department conflicts
* Room conflicts
* Teacher availability violations

### 4. Slot Recommendation

Instead of manually checking every timetable, the system can identify suitable slots automatically.

Example:

```text
Monday 1:30 PM       ❌ Department occupied
Wednesday 10:30 AM  ✅ Available
Friday 1:30 PM       ❌ Room occupied
```

### 5. Centralized Management

Teachers, departments, subjects, rooms, availability, and timetable entries can be managed from a single application.

---

## 🏗️ System Architecture

```text
                    ┌─────────────────────┐
                    │      JavaFX UI      │
                    │                     │
                    │ Dashboard           │
                    │ Timetable Viewer    │
                    │ Schedule Class      │
                    │ Conflict Reports    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    Service Layer    │
                    │                     │
                    │ Timetable Service   │
                    │ Conflict Service    │
                    │ Teacher Service     │
                    │ Scheduling Service  │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │       DAO Layer     │
                    │                     │
                    │ TeacherDAO          │
                    │ DepartmentDAO       │
                    │ TimetableDAO        │
                    │ RoomDAO             │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     SQLite DB       │
                    │                     │
                    │ Teachers            │
                    │ Departments         │
                    │ Subjects            │
                    │ Rooms               │
                    │ Availability        │
                    │ Timetables          │
                    └─────────────────────┘
```

---

## 🛠️ Technology Stack

* **Java** — Core programming language
* **JavaFX** — Graphical User Interface
* **SQLite** — Database
* **JDBC** — Database connectivity
* **Maven** — Project/build management
* **Git & GitHub** — Version control

---

## 🗄️ Database Structure

The system uses the following main tables:

```text
departments
    │
    ▼
timetable
   / | \
  /  |  \
 ▼   ▼   ▼
subject teacher room
         │
         ▼
teacher_availability

time_slots
    │
    ▼
timetable
```

### Main Tables

#### `departments`

Stores department information.

```text
department_id
department_name
department_code
```

#### `teachers`

Stores teacher information.

```text
teacher_id
teacher_name
department_id
email
```

#### `subjects`

Stores subjects.

```text
subject_id
subject_name
subject_code
```

#### `rooms`

Stores classroom/laboratory information.

```text
room_id
room_number
room_type
capacity
```

#### `time_slots`

Stores available timetable slots.

```text
slot_id
start_time
end_time
```

#### `teacher_availability`

Stores teacher availability.

```text
availability_id
teacher_id
day
slot_id
available
```

#### `timetable`

Stores scheduled classes.

```text
timetable_id
department_id
subject_id
teacher_id
room_id
day
slot_id
```

---

## 📂 Project Structure

```text
SmartTimetable/
│
├── src/
│   └── main/
│       ├── java/
│       │   ├── Main.java
│       │   │
│       │   ├── model/
│       │   │   ├── Teacher.java
│       │   │   ├── Department.java
│       │   │   ├── Subject.java
│       │   │   ├── Room.java
│       │   │   ├── TimeSlot.java
│       │   │   ├── TimetableEntry.java
│       │   │   └── TeacherAvailability.java
│       │   │
│       │   ├── dao/
│       │   │   ├── TeacherDAO.java
│       │   │   ├── DepartmentDAO.java
│       │   │   ├── SubjectDAO.java
│       │   │   ├── RoomDAO.java
│       │   │   └── TimetableDAO.java
│       │   │
│       │   ├── service/
│       │   │   ├── TimetableService.java
│       │   │   ├── ConflictService.java
│       │   │   ├── TeacherService.java
│       │   │   └── SchedulingService.java
│       │   │
│       │   ├── controller/
│       │   │   ├── DashboardController.java
│       │   │   ├── TimetableController.java
│       │   │   ├── ScheduleController.java
│       │   │   └── ConflictController.java
│       │   │
│       │   └── database/
│       │       └── DatabaseConnection.java
│       │
│       └── resources/
│           ├── css/
│           │   └── style.css
│           │
│           └── views/
│               ├── dashboard.fxml
│               ├── timetable.fxml
│               ├── schedule.fxml
│               └── conflicts.fxml
│
├── pom.xml
├── README.md
└── timetable.db
```

---

## ⚙️ How the Conflict Detection Works

When a user attempts to schedule a class, the system performs multiple checks.

### Step 1 — Check Teacher Availability

```text
Is the teacher available on the selected day and time?

             │
       ┌─────┴─────┐
       │           │
      YES          NO
       │           │
       ▼           ▼
   Continue     Conflict
```

### Step 2 — Check Teacher Conflict

The system checks whether the teacher is already teaching another department at the selected time.

```text
Mrs. Sharma
Monday — 1:30 PM

E&CS → Mathematics
CSE  → Finance

❌ Teacher Conflict
```

### Step 3 — Check Department Conflict

The system checks whether the selected department already has another class at that time.

```text
CSE — Monday 1:30 PM

Java

❌ Department Conflict
```

### Step 4 — Check Room Conflict

The system checks whether the selected classroom is already occupied.

```text
Room 301 — Monday 1:30 PM

E&CS → Finance

❌ Room Conflict
```

### Step 5 — Final Decision

If all checks pass:

```text
Teacher Available      ✓
Teacher Conflict       ✓ No conflict
Department Conflict    ✓ No conflict
Room Conflict          ✓ No conflict

        ↓

✅ SLOT AVAILABLE
```

---

## 🖥️ Planned UI Screens

### Dashboard

Provides an overview of:

* Number of departments
* Number of teachers
* Number of rooms
* Number of scheduled classes
* Quick navigation to other modules

### Multi-Department Timetable

Displays multiple department schedules simultaneously.

### Schedule Class

Allows the user to select:

```text
Department
Subject
Teacher
Day
Time
Room
```

and check whether the class can be scheduled.

### Conflict Report

Displays detected:

```text
Teacher Conflicts
Department Conflicts
Room Conflicts
Availability Conflicts
```

### Teacher Availability

Provides a visual representation of each teacher's available and unavailable slots.

---

## 🧠 Scheduling Algorithm

The initial version uses a **constraint-based scheduling approach**.

For every proposed class:

```text
                 New Class
                     │
                     ▼
             Teacher Available?
                /          \
              NO            YES
              │              │
              ▼              ▼
           REJECT      Teacher Free?
                            /   \
                          NO     YES
                          │       │
                          ▼       ▼
                       REJECT  Department Free?
                                  /    \
                                NO      YES
                                │        │
                                ▼        ▼
                             REJECT   Room Free?
                                         /  \
                                       NO    YES
                                       │      │
                                       ▼      ▼
                                    REJECT  ACCEPT
```

---

## 🔮 Future Improvements

The project can be extended with:

* Automatic timetable generation
* Constraint Satisfaction Problem (CSP) implementation
* Timetable optimization
* Genetic Algorithm-based scheduling
* Teacher workload analysis
* Automatic room allocation
* Import timetable from Excel
* Export generated timetable to PDF/Excel
* Admin login and role management
* Web-based version
* Cloud database
* Notifications for timetable changes

---

## 📈 Development Roadmap

### Phase 1 — Basic Timetable System

* [ ] Create database
* [ ] Add departments
* [ ] Add teachers
* [ ] Add subjects
* [ ] Add rooms
* [ ] Add time slots
* [ ] Store timetable entries

### Phase 2 — Conflict Detection

* [ ] Teacher availability check
* [ ] Teacher conflict detection
* [ ] Department conflict detection
* [ ] Room conflict detection
* [ ] Conflict report

### Phase 3 — Smart Scheduling

* [ ] Find available slots
* [ ] Recommend suitable slots
* [ ] Compare multiple department timetables
* [ ] Improve scheduling algorithm

### Phase 4 — Automatic Timetable Generation

* [ ] Constraint-based timetable generation
* [ ] Optimization
* [ ] Automatic room allocation
* [ ] Timetable export

---

## 🎓 Project Objective

The ultimate goal is to reduce the amount of **manual timetable comparison and conflict checking** required by college timetable coordinators.

Instead of manually checking multiple department timetables, the coordinator can use one centralized system to determine:

> **Who is free? Where is the room free? Which department is free? Can this class be scheduled here?**

---

## 👥 Project Status

**Current Status:** 🚧 Under Development

The project is currently being developed as a Java-based desktop application with a focus on timetable visualization and conflict detection.

---

## 📜 License

This project is intended for educational and academic purposes.
