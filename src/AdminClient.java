import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AdminClient {
	
	public static void main(String args[]) {
		Logger adminClientLogs = Logger.getLogger("Admin Client");
		AuthenticationInterface authInterface = null;
		AdminOperations adminOps;
		String adminId;
		CampusRegistry campus;
		
		try {
			FileHandler logFileHandler = new FileHandler("admin-client.log", true);
			adminClientLogs.addHandler(logFileHandler);
		} catch (IOException ioe) {
			System.out.println("Failed to initialize application log system.");
		}
		
		// connect to authentication server
		try {
			Registry authRegistry = LocateRegistry.getRegistry("localhost", 8008);
			authInterface = (AuthenticationInterface) authRegistry.lookup("auth");
			adminClientLogs.info("Connection established to authentication server");
		} catch (RemoteException | NotBoundException re) {
			System.out.println("Unable to connect to server. Please try again!");
			adminClientLogs.warning("Remote exception detected with message - " + re.getMessage());
			return;
		}
		
		adminOps = new AdminOperations(adminClientLogs, authInterface);
		
		adminId = adminOps.askAdminId();
		if (adminId == null) {
			adminClientLogs.warning("Administrator not found.");
			System.out.println("No admin found. Please try again.");
			return;
		}
		campus = adminOps.authenticateAdmin(adminId);
		if (campus == null) {
			String message = "Admin is not assigned to the campus";
			adminClientLogs.warning(message);
			System.out.println(message + ". Please try again.");
			return;
		}
	}
}
