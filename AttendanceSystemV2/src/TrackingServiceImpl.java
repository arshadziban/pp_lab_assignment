import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side implementation of TrackingService.
 *
 * Thread safety strategy:
 *   - ConcurrentHashMap for the top-level map (atomic putIfAbsent).
 *   - synchronized on each StudentRecord instance for mutation and snapshot reads.
 *   - UnicastRemoteObject (superclass) assigns each incoming RMI call to its own thread.
 */
public class TrackingServiceImpl extends UnicastRemoteObject implements TrackingService {

    private static final long serialVersionUID = 2L;

    private final ConcurrentHashMap<String, StudentRecord> store = new ConcurrentHashMap<>();

    public TrackingServiceImpl() throws RemoteException {
        super();
        loadDemoData();
    }

    private void loadDemoData() {
        String[][] students = {
            {"T001", "Farhan Ahmed"},
            {"T002", "Sadia Islam"},
            {"T003", "Nabil Hossain"},
            {"T004", "Mitu Akter"}
        };
        String[] schedule = {"Present", "Present", "Present", "Absent", "Present"};
        for (String[] s : students) {
            StudentRecord rec = new StudentRecord(s[0], s[1]);
            for (String st : schedule) rec.addEntry(st);
            store.put(s[0], rec);
        }
    }

    @Override
    public String recordAttendance(String studentId, String name, String status) throws RemoteException {
        if (studentId == null || studentId.isBlank())
            return "ERROR: Student ID must not be empty.";
        if (!status.equalsIgnoreCase("Present") && !status.equalsIgnoreCase("Absent"))
            return "ERROR: Status must be 'Present' or 'Absent'.";

        String id = studentId.trim().toUpperCase();
        StudentRecord rec = store.computeIfAbsent(id, k -> new StudentRecord(k,
                (name != null && !name.isBlank()) ? name.trim() : k));

        synchronized (rec) {
            rec.addEntry(status);
        }
        log("recordAttendance", id, rec.getStudentName() + " -> " + status);
        return String.format("[OK] %s (%s) marked %s  |  Total classes now: %d",
                id, rec.getStudentName(), status, rec.totalClasses());
    }

    @Override
    public StudentRecord fetchRecord(String studentId) throws RemoteException {
        if (studentId == null) return null;
        String id  = studentId.trim().toUpperCase();
        StudentRecord rec = store.get(id);
        if (rec == null) { log("fetchRecord", id, "NOT FOUND"); return null; }
        synchronized (rec) {
            StudentRecord snap = new StudentRecord(rec.getStudentId(), rec.getStudentName());
            rec.getEntries().forEach(e -> snap.addEntry(e.status));
            log("fetchRecord", id, snap.totalClasses() + " entries");
            return snap;
        }
    }

    @Override
    public String fetchPercentage(String studentId) throws RemoteException {
        if (studentId == null) return "ERROR: null ID.";
        String id  = studentId.trim().toUpperCase();
        StudentRecord rec = store.get(id);
        if (rec == null) return "ERROR: No record for student ID " + id;
        synchronized (rec) {
            return String.format("Student %s (%s)  —  %.1f%%  (%d present / %d total)",
                    id, rec.getStudentName(), rec.percentage(), rec.presentCount(), rec.totalClasses());
        }
    }

    @Override
    public List<StudentRecord> fetchAll() throws RemoteException {
        List<StudentRecord> out = new ArrayList<>();
        for (StudentRecord rec : store.values()) {
            synchronized (rec) {
                StudentRecord snap = new StudentRecord(rec.getStudentId(), rec.getStudentName());
                rec.getEntries().forEach(e -> snap.addEntry(e.status));
                out.add(snap);
            }
        }
        out.sort((a, b) -> a.getStudentId().compareTo(b.getStudentId()));
        log("fetchAll", "-", out.size() + " records");
        return out;
    }

    @Override
    public String resetAll() throws RemoteException {
        int n = store.size();
        store.clear();
        log("resetAll", "-", n + " records removed");
        return "RESET: " + n + " record(s) removed from the server.";
    }

    private void log(String op, String id, String detail) {
        System.out.printf("[%s] %-20s  id=%-8s  %s%n",
                java.time.LocalTime.now().toString().substring(0, 8), op, id, detail);
    }
}
