import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI Remote Interface — defines every operation the server exposes to clients.
 */
public interface TrackingService extends Remote {

    /**
     * Records a single attendance entry for a student.
     * Auto-creates the student record if it does not yet exist.
     */
    String recordAttendance(String studentId, String name, String status) throws RemoteException;

    /**
     * Returns the full record for one student, or null if not found.
     */
    StudentRecord fetchRecord(String studentId) throws RemoteException;

    /**
     * Returns a human-readable attendance percentage line.
     */
    String fetchPercentage(String studentId) throws RemoteException;

    /**
     * Returns a snapshot of every record on the server, sorted by ID.
     */
    List<StudentRecord> fetchAll() throws RemoteException;

    /**
     * Drops all records and returns a confirmation message.
     */
    String resetAll() throws RemoteException;
}
