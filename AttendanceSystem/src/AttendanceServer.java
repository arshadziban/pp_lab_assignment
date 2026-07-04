import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * RMI Server entry point.
 *
 * Starts the RMI registry on port 1099, creates the remote object, and binds
 * it under the name "AttendanceService". The JVM stays alive waiting for client
 * connections; UnicastRemoteObject handles each call on its own thread, so
 * multiple clients are served concurrently automatically.
 */
public class AttendanceServer {

    public static void main(String[] args) {
        int port = 1099;
        try {
            // Create and export the remote object
            AttendanceServiceImpl service = new AttendanceServiceImpl();

            // Start the RMI registry within this JVM
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("AttendanceService", service);

            System.out.println("============================================");
            System.out.println("  Attendance Management RMI Server Started");
            System.out.println("  Listening on port " + port);
            System.out.println("  Bound name : AttendanceService");
            System.out.println("  Sample data: S001, S002, S003 pre-loaded");
            System.out.println("============================================");
        } catch (Exception e) {
            System.err.println("[SERVER ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
