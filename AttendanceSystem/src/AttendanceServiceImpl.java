import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RMI Remote Object Implementation.
 *
 * Extends UnicastRemoteObject so the object is automatically exported on
 * construction. ConcurrentHashMap provides thread-safe access to the shared
 * in-memory store; individual record mutations are guarded with synchronized
 * blocks to prevent race conditions under concurrent clients.
 */
public class AttendanceServiceImpl extends UnicastRemoteObject implements AttendanceService {

    private static final long serialVersionUID = 1L;

    // Shared in-memory store: studentId -> AttendanceRecord
    private final ConcurrentHashMap<String, AttendanceRecord> attendanceMap;

    public AttendanceServiceImpl() throws RemoteException {
        super();
        attendanceMap = new ConcurrentHashMap<>();
        seedSampleData();
    }

    /** Pre-loads a few sample students so the UI has data on first launch. */
    private void seedSampleData() {
        String[][] samples = {
            {"S001", "Alice Rahman"},
            {"S002", "Bob Hossain"},
            {"S003", "Carol Islam"}
        };
        String[] statuses = {"Present", "Present", "Absent", "Present"};
        for (String[] s : samples) {
            AttendanceRecord rec = new AttendanceRecord(s[0], s[1]);
            for (String st : statuses) rec.addAttendance(st);
            attendanceMap.put(s[0], rec);
        }
    }

    @Override
    public String markAttendance(String studentId, String studentName, String status) throws RemoteException {
        if (studentId == null || studentId.trim().isEmpty()) return "Error: Student ID cannot be empty.";
        if (!status.equalsIgnoreCase("Present") && !status.equalsIgnoreCase("Absent"))
            return "Error: Status must be 'Present' or 'Absent'.";

        String trimId = studentId.trim().toUpperCase();
        // computeIfAbsent is atomic; mutation inside is guarded by synchronizing on the record
        AttendanceRecord record = attendanceMap.computeIfAbsent(
                trimId, id -> new AttendanceRecord(trimId,
                        (studentName != null && !studentName.trim().isEmpty()) ? studentName.trim() : trimId));

        synchronized (record) {
            record.addAttendance(status);
        }

        System.out.printf("[SERVER] markAttendance -> %s (%s): %s%n", trimId, record.getStudentName(), status);
        return String.format("Attendance marked: %s (%s) -> %s", trimId, record.getStudentName(), status);
    }

    @Override
    public AttendanceRecord getAttendanceRecord(String studentId) throws RemoteException {
        if (studentId == null) return null;
        String trimId = studentId.trim().toUpperCase();
        AttendanceRecord rec = attendanceMap.get(trimId);
        if (rec == null) {
            System.out.printf("[SERVER] getAttendanceRecord -> %s: NOT FOUND%n", trimId);
            return null;
        }
        // Return a snapshot copy to avoid shared mutable state crossing the wire
        synchronized (rec) {
            AttendanceRecord copy = new AttendanceRecord(rec.getStudentId(), rec.getStudentName());
            rec.getAttendanceHistory().forEach(copy::addAttendance);
            System.out.printf("[SERVER] getAttendanceRecord -> %s: %d records%n", trimId, copy.getTotalClasses());
            return copy;
        }
    }

    @Override
    public String getAttendancePercentage(String studentId) throws RemoteException {
        if (studentId == null) return "Error: Student ID is null.";
        String trimId = studentId.trim().toUpperCase();
        AttendanceRecord rec = attendanceMap.get(trimId);
        if (rec == null) return "Error: Student " + trimId + " not found.";
        synchronized (rec) {
            return String.format("Student %s (%s) — Attendance: %.1f%% (%d/%d classes)",
                    trimId, rec.getStudentName(), rec.getAttendancePercentage(),
                    rec.getPresentCount(), rec.getTotalClasses());
        }
    }

    @Override
    public List<AttendanceRecord> getAllRecords() throws RemoteException {
        List<AttendanceRecord> result = new ArrayList<>();
        for (AttendanceRecord rec : attendanceMap.values()) {
            synchronized (rec) {
                AttendanceRecord copy = new AttendanceRecord(rec.getStudentId(), rec.getStudentName());
                rec.getAttendanceHistory().forEach(copy::addAttendance);
                result.add(copy);
            }
        }
        result.sort((a, b) -> a.getStudentId().compareTo(b.getStudentId()));
        System.out.printf("[SERVER] getAllRecords -> %d records returned%n", result.size());
        return result;
    }

    @Override
    public String clearAllRecords() throws RemoteException {
        int count = attendanceMap.size();
        attendanceMap.clear();
        System.out.println("[SERVER] All records cleared.");
        return "Cleared " + count + " records from the server.";
    }
}
