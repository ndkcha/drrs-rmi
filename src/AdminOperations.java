import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class AdminOperations {
	private AuthenticationInterface authInterface;
	private Logger adminLogs;
	
	public AdminOperations(Logger adminLogs, AuthenticationInterface authInterface) {
		this.adminLogs = adminLogs;
		this.authInterface = authInterface;
	}
	
	public String askAdminId() {
		Scanner scan = new Scanner(System.in);
		String userResponse = null;
		String adminId = null;
		
		System.out.println("Do you have the Administrator ID? (y/n)\n>");
		userResponse = scan.nextLine();
		
		if (userResponse.equalsIgnoreCase("n"))
			adminId = this.addAdmin();
		else {
			System.out.println("Enter the admin id:\n > ");
			adminId = scan.nextLine();
		}
		
		scan.close();
		
		return adminId;
	}
	
	private String addAdmin() {
		Scanner scan = new Scanner(System.in);
		String userResponse = null;
		String adminId = null;
		
		System.out.println("For which campus do you want to be the administrator?\n\t- Dorval Campus\n\t- Kirkland Campus\n\t- Westmount Campus\n > ");
		userResponse = scan.nextLine();
		
		try {
			adminId = authInterface.addAdmin(userResponse);
		} catch (RemoteException re) {
			System.out.println("Unable to connect to server. Please try again!");
			adminLogs.warning("Remote exception detected while assigning admin to campus with message - " + re.getMessage());
		}
		
		scan.close();
		return adminId;
	}
	
	public CampusRegistry authenticateAdmin(String adminId) {
		CampusRegistry campus = null;
		try {
			campus = authInterface.getCampus(adminId);
		} catch (RemoteException re) {
			System.out.println("Unable to connect to server. Please try again!");
			adminLogs.warning("Remote exception detected while authenticating the admin with message - " + re.getMessage());
		}
		return campus;
	}
	
	public boolean createRoom(CampusInterface campusInterface) {
		List<TimeSlot> timeSlots = new ArrayList<>();
		int roomNo;
		String userResponse, fromTime, toTime;
		Date date = null;
		boolean isAddTimeslot = true, success = false;
		Scanner scan = new Scanner(System.in);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
		
		System.out.println("New entry for Room\nEnter room number:\n > ");
		roomNo = scan.nextInt();
		System.out.println("Enter date (DD-MM-YYYY):\n > ");
		userResponse = scan.nextLine();
		try {
			date = simpleDateFormat.parse(userResponse);
		} catch (ParseException e) {
			adminLogs.warning("Error parsing date taken from user");
		}
		
		// get time slots
		System.out.println("Add Timeslots:\n");
		do {
			System.out.println("From Time (hh:mm):\n > ");
			fromTime = scan.nextLine();
			System.out.println("To Time (hh:mm):\n > ");
			toTime = scan.nextLine();
			
			TimeSlot slot = new TimeSlot(fromTime, toTime);
			timeSlots.add(slot);
			
			System.out.println("\nAdd another timeslot? (y/n):\n > ");
			userResponse = scan.nextLine();
			
			isAddTimeslot = userResponse.equalsIgnoreCase("y");
		} while(isAddTimeslot);
		
		success = campusInterface.createRoom(roomNo, date, timeSlots);
		
		scan.close();
		return success;
	}
}
