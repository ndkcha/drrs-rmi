import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class CampusServer extends UnicastRemoteObject {
	private static final long serialVersionUID = 1L;
	private static FileHandler logFileHandler;
	private static Logger campusLogs;

	public CampusServer() throws RemoteException {
		super();
	}
	
	public static void main(String args[]) {
		AuthenticationInterface authInterface;
		
		Scanner scan = new Scanner(System.in);
		String campusName;
		CampusRegistry campus;

		System.out.println("Enter the name of the campus (no spaces allowed):\n > ");
		campusName = scan.nextLine();
		
		campus = askCampusDetails(campusName, scan);
		
		scan.close();
		
		try {
			logFileHandler = new FileHandler(campusName + "-server.log", true);
			campusLogs.addHandler(logFileHandler);
		} catch (IOException ioe) {
			System.out.println("Failed to initialize application log system.");
		}
		
		// connect to authentication server
		try {
			Registry authRegistry = LocateRegistry.getRegistry("localhost", 8008);
			authInterface = (AuthenticationInterface) authRegistry.lookup("auth");
			campusLogs.info("Connection established to authentication server");
			
			boolean success = authInterface.addCampus(campus);
			if (success)
				campusLogs.info(campusName + " server has been added to the authentication registry.");
			else {
				String message = campusName + " already exists in the authentication registry.";
				campusLogs.warning(message);
				System.out.println(message);
				System.exit(0);
			}
		} catch (RemoteException | NotBoundException re) {
			System.out.println("Unable to connect to server. Please try again!");
			campusLogs.warning("Remote exception detected with message - " + re.getMessage());
			System.exit(0);
		}
		
		// start the campus server
		try {
			CampusServer campusServer = new CampusServer();
			Registry registry = LocateRegistry.createRegistry(campus.getPort());
			registry.rebind(campus.getVirtualAddress(), campusServer);
			campusLogs.info(campus.getVirtualAddress() + " server can now accept incoming connections.");
			System.out.println("Server is running on port " + campus.getPort());
		} catch(RemoteException re) {
			campusLogs.warning("Remote exception detected with message, " + re.getMessage());
			System.out.println("Error starting the server. Please try again!");
		}
	}
	
	private static CampusRegistry askCampusDetails(String name, Scanner scan) {
		String virtualAddress, code;
		int port;
		
		System.out.println("Enter the virtual address for " + name + " server:\n > ");
		virtualAddress = scan.nextLine();
		System.out.println("Enter the port number for the server:\n > ");
		port = scan.nextInt();
		System.out.println("Enter the three letter code for the " + name + "(to be used for clientIds, no spaces allowed):\n > ");
		code = scan.nextLine();
		
		CampusRegistry campus = new CampusRegistry(name, virtualAddress, code, port);
		return campus;
	}
}
