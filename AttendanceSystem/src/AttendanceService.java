import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI Remote Interface defining all services exposed by the server.
 * All methods must declare RemoteException.
 */
public interface AttendanceService extends Remote {

    /**
     * Marks attendance for a student.
     * Creates a new record if the student does not exist.
     *
     * @param studentId   unique student identifier
     * @param studentName student's full name (used only on first registration)
     * @param status      "Present" or "Absent"
     * @return confirmation message
     */
    String markAttendance(String studentId, String studentName, String status) throws RemoteException;

    /**
     * Retrieves the full attendance history list for a student.
     *
     * @param studentId unique student identifier
     * @return AttendanceRecord object, or null if not found
     */
    AttendanceRecord getAttendanceRecord(String studentId) throws RemoteException;

    /**
     * Calculates and returns the attendance percentage for a student.
     *
     * @param studentId unique student identifier
     * @return formatted percentage string
     */
    String getAttendancePercentage(String studentId) throws RemoteException;

    /**
     * Returns all attendance records stored on the server.
     *
     * @return list of all AttendanceRecord objects
     */
    List<AttendanceRecord> getAllRecords() throws RemoteException;

    /**
     * Removes all attendance records (utility for testing).
     *
     * @return confirmation message
     */
    String clearAllRecords() throws RemoteException;
}
