import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Entry point for the RMI server.
 *
 * Creates and exports TrackingServiceImpl, then binds it in an RMI registry
 * on port 1099. The JVM blocks until killed; UnicastRemoteObject serves every
 * incoming client call on a dedicated thread automatically.
 */
public class TrackingServer {

    private static final int PORT         = 1099;
    private static final String BIND_NAME = "TrackingService";

    public static void main(String[] args) {
        try {
            TrackingServiceImpl impl = new TrackingServiceImpl();
            Registry registry        = LocateRegistry.createRegistry(PORT);
            registry.rebind(BIND_NAME, impl);

            String bar = "═".repeat(50);
            System.out.println(bar);
            System.out.println("  Attendance Tracker  —  RMI Server  v2.0");
            System.out.println("  Port      : " + PORT);
            System.out.println("  Bind name : " + BIND_NAME);
            System.out.println("  Demo data : T001, T002, T003, T004 pre-loaded");
            System.out.println(bar);
            System.out.println("  Waiting for client connections ...");
        } catch (Exception ex) {
            System.err.println("[FATAL] Server startup failed: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
