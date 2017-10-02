import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AdminClient {
	
	public static void main(String args[]) {
		Logger adminClientLogs = Logger.getLogger("Admin Client");
		AuthenticationInterface authInterface = null;
		AdminOperations adminOps;
		String adminId, message;
		CampusRegistry campus;
		CampusAdminInterface campusServerInterface = null;
		boolean isOperationSuccessful = false, isExitRequested = false;
		int userResponse = 0;
		
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
			message = "Admin is not assigned to the campus";
			adminClientLogs.warning(message);
			System.out.println(message + ". Please try again.");
			return;
		}
		
		// connect to campus server
		try {
			Registry campusRegistry = LocateRegistry.getRegistry("localhost", campus.getPort());
			campusServerInterface = (CampusAdminInterface) campusRegistry.lookup(campus.getVirtualAddress());
			adminClientLogs.info("Connection established to " + campus.name);
		} catch (RemoteException | NotBoundException e) {
			System.out.println("Unable to connect to server. Please try again!");
			adminClientLogs.warning("Remote exception detected with message - " + e.getMessage());
			return;
		}
		
		System.out.println("Welcome to " + campus.name + "\n");
		
		Scanner scan = new Scanner(System.in);
		
		while (!isExitRequested) {
			System.out.println("What do you want to do? (enter the number between 1 and 2)\n\t1. Create Room\n\t2. Delete Room\n\tAny other number to Exit\n: > ");
			userResponse = scan.nextInt();
			
			if ((userResponse < 1) || (userResponse > 2)) {
				adminClientLogs.info("Leaving process with user's permission");
				System.out.println("Bye Bye");
				break;
			}
			
			switch (userResponse) {
				case 1:
					isOperationSuccessful = adminOps.createRoom(campusServerInterface);
					message = isOperationSuccessful ? "A room has successfully been created!" : "Server is facing trouble creating rooms";
					break;
				default:
					isOperationSuccessful = false;
					message = "Unknown error!";
			}
			
			if (isOperationSuccessful)
				adminClientLogs.info(message);
			else
				adminClientLogs.warning(message);
		}
		
		scan.close();
	}
}
