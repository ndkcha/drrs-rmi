import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class StudentClient {

	public static void main(String args[]) {
		Logger logs = Logger.getLogger("Admin Client");
		AuthenticationInterface authInterface = null;
		StudentOperations studentOps;
		String studentId, message = "";
		CampusRegistry campus;
		CampusStudentInterface campusInterface = null;
		boolean isExitRequested = false, isOperationSuccessful = false;
		int userResponse = -1;
		
		try {
			FileHandler logFileHandler = new FileHandler("student-client.log", true);
			logs.addHandler(logFileHandler);
		} catch (IOException ioe) {
			System.out.println("Failed to initialize application log system.");
		}
		
		// connect to authentication server
		try {
			Registry authRegistry = LocateRegistry.getRegistry("localhost", 8008);
			authInterface = (AuthenticationInterface) authRegistry.lookup("auth");
			logs.info("Connection established to authentication server");
		} catch (RemoteException | NotBoundException re) {
			System.out.println("Unable to connect to server. Please try again!");
			logs.warning("Remote exception detected with message - " + re.getMessage());
			return;
		}
		
		studentOps = new StudentOperations(authInterface, logs);
		studentId = studentOps.askStudentId();
		if (studentId == null) {
			logs.warning("Student not found.");
			System.out.println("No student found. Please try again.");
			return;
		}
		
		campus = studentOps.authenticateStudent(studentId);
		if (campus == null) {
			message = "Student is not assigned to a campus";
			logs.warning(message);
			System.out.println(message + ". Please try again.");
			return;
		}
		
		// connect to campus server
		try {
			Registry campusRegistry = LocateRegistry.getRegistry("localhost", campus.getPort());
			campusInterface = (CampusStudentInterface) campusRegistry.lookup(campus.getVirtualAddress());
			logs.info("Connection established to " + campus.name + " server");
		} catch (RemoteException | NotBoundException re) {
			System.out.println("Unable to connect to server. Please try again!");
			logs.warning("Remote exception detected with message - " + re.getMessage());
			return;
		}
		
		System.out.println("Welcome to " + campus.name + "\n");
		
		Scanner scan = new Scanner(System.in);
		
		while (!isExitRequested) {
			System.out.println("What do you want to do?\n1. Book a room\n2. Cancel booking\nAny other number to exit");
			userResponse = scan.nextInt();
			
			if ((userResponse < 1) || (userResponse > 2)) {
				logs.info("Leaving process with user's permission");
				System.out.println("Bye Bye");
				break;
			}
			
			switch (userResponse) {
				case 1:
					isOperationSuccessful = studentOps.bookRoom(campusInterface, studentId);
					message = isOperationSuccessful ? "A student successfully booked a room." : "Error thrown for registering booking at auth server";
					break;
				case 2:
					isOperationSuccessful = studentOps.cancelBooking(campusInterface, studentId);
					message = isOperationSuccessful ? "A student successfully cancelled a booking" : "Error throws for confirming booking at auth server";
					break;
			}
			
			if (isOperationSuccessful)
				logs.info(message);
			else
				logs.warning(message);
		}
		
		scan.close();
	}
}
