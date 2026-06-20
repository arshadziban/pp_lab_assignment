# Attendance Tracker v2.0

A distributed attendance management system built with **Java RMI** (Remote Method Invocation). Multiple GUI clients can connect simultaneously to a shared server and record, query, or manage student attendance in real time.

---

## Project Structure

```
AttendanceSystemV2/
├── src/
│   ├── StudentRecord.java        # Serializable data model (stores entries with timestamps)
│   ├── TrackingService.java      # RMI remote interface
│   ├── TrackingServiceImpl.java  # Server-side implementation (thread-safe)
│   ├── TrackingServer.java       # Server entry point
│   └── TrackerClient.java        # Swing GUI client
├── bin/                          # Compiled .class files (auto-generated)
└── README.md
```

---

## Requirements

- Java JDK 8 or higher
- No external libraries — uses only the Java standard library

---

## How to Build

From the `AttendanceSystemV2/` directory:

```bash
javac -d bin src/StudentRecord.java src/TrackingService.java src/TrackingServiceImpl.java src/TrackingServer.java src/TrackerClient.java
```

---

## How to Run

### 1. Start the Server

```bash
java -cp bin TrackingServer
```

The server starts on **port 1099** and pre-loads four demo students:

| ID   | Name           |
|------|----------------|
| T001 | Farhan Ahmed   |
| T002 | Sadia Islam    |
| T003 | Nabil Hossain  |
| T004 | Mitu Akter     |

### 2. Start the Client

```bash
java -cp bin TrackerClient
```

To connect to a remote server:

```bash
java -cp bin TrackerClient 192.168.1.100
```

> You can launch multiple client instances simultaneously — the server handles concurrent access safely.

---

## Features

| Feature | Description |
|---|---|
| **Mark Attendance** | Record Present / Absent for any student. Auto-creates the student if not found. |
| **View History** | See every attendance entry for a student with date-time timestamps. |
| **Get Percentage** | Returns attendance percentage (present / total classes). |
| **All Records Table** | Browse all students in a sortable table; attendance % shown in green (≥75%) or red (<75%). |
| **Reset All** | Clears every record from the server (with confirmation prompt). |
| **Multi-client** | Multiple clients connect to the same server concurrently with no data corruption. |



- **Sidebar** — navigate between Mark Attendance, Query Student, and All Records pages.
- **Cards** — clean white form cards on a light grey background.
- **Activity Log** — dark terminal-style log showing server responses in real time.
- **Top bar** — connection controls with a live indicator dot (red = disconnected, green = connected).

---

## Architecture

```
┌──────────────────┐          RMI / TCP          ┌────────────────────────┐
│   TrackerClient  │  ──────────────────────────► │   TrackingServer       │
│   (Swing GUI)    │ ◄──────────────────────────  │   (UnicastRemoteObject)│
└──────────────────┘       port 1099              └────────────┬───────────┘
                                                               │
                                                  ConcurrentHashMap<String, StudentRecord>
```

### Thread Safety

- `ConcurrentHashMap` for atomic student registration (`computeIfAbsent`).
- `synchronized` blocks on individual `StudentRecord` instances for mutation and snapshot reads.
- `UnicastRemoteObject` automatically assigns each incoming RMI call to its own thread.
- All client-side RMI calls run on `SwingWorker` background threads — the UI never freezes.

---

## Data Model

`StudentRecord` — transferred over RMI (implements `Serializable`):

```
StudentRecord
├── studentId   : String
├── studentName : String
└── entries     : List<Entry>
                   ├── status    : "Present" | "Absent"
                   └── timestamp : "yyyy-MM-dd HH:mm"
```

---

## RMI Interface

```java
String          recordAttendance(String studentId, String name, String status)
StudentRecord   fetchRecord(String studentId)
String          fetchPercentage(String studentId)
List<StudentRecord> fetchAll()
String          resetAll()
```



