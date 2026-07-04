import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable data class representing a student's attendance record.
 * Transferred over RMI, so it must implement Serializable.
 */
public class AttendanceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String studentId;
    private String studentName;
    // Each entry is "Present" or "Absent"
    private List<String> attendanceHistory;

    public AttendanceRecord(String studentId, String studentName) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.attendanceHistory = new ArrayList<>();
    }

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public List<String> getAttendanceHistory() { return new ArrayList<>(attendanceHistory); }

    public void addAttendance(String status) {
        attendanceHistory.add(status);
    }

    /** Returns attendance percentage (0.0 if no records). */
    public double getAttendancePercentage() {
        if (attendanceHistory.isEmpty()) return 0.0;
        long presentCount = attendanceHistory.stream()
                .filter(s -> s.equalsIgnoreCase("Present"))
                .count();
        return (presentCount * 100.0) / attendanceHistory.size();
    }

    public int getTotalClasses() { return attendanceHistory.size(); }

    public long getPresentCount() {
        return attendanceHistory.stream().filter(s -> s.equalsIgnoreCase("Present")).count();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Classes: %d | Present: %d | %.1f%%",
                studentId, studentName, getTotalClasses(), getPresentCount(), getAttendancePercentage());
    }
}
