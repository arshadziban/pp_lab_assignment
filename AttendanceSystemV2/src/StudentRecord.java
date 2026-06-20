import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializable data model for a student's attendance entries.
 * Each entry pairs a status string with a timestamp.
 */
public class StudentRecord implements Serializable {

    private static final long serialVersionUID = 2L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class Entry implements Serializable {
        private static final long serialVersionUID = 2L;
        public final String status;
        public final String timestamp;

        public Entry(String status) {
            this.status    = status;
            this.timestamp = LocalDateTime.now().format(FMT);
        }
    }

    private final String studentId;
    private String studentName;
    private final List<Entry> entries;

    public StudentRecord(String studentId, String studentName) {
        this.studentId   = studentId;
        this.studentName = (studentName != null && !studentName.isBlank()) ? studentName.trim() : studentId;
        this.entries     = new ArrayList<>();
    }

    public void addEntry(String status) {
        entries.add(new Entry(status));
    }

    public String getStudentId()   { return studentId; }
    public String getStudentName() { return studentName; }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int totalClasses() { return entries.size(); }

    public int presentCount() {
        return (int) entries.stream().filter(e -> e.status.equalsIgnoreCase("Present")).count();
    }

    public double percentage() {
        return entries.isEmpty() ? 0.0 : (presentCount() * 100.0) / entries.size();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s  |  Classes: %d  |  Present: %d  |  %.1f%%",
                studentId, studentName, totalClasses(), presentCount(), percentage());
    }
}
