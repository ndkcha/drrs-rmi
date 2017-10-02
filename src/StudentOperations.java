import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class StudentOperations {
	private AuthenticationInterface authInterface;
	private Logger logs;
	
	public StudentOperations(AuthenticationInterface authInterface, Logger logs) {
		this.authInterface = authInterface;
		this.logs = logs;
	}
	
	public String askStudentId() {
		Scanner scan = new Scanner(System.in);
		String studentId = null, userResponse = null;
		
		System.out.println("Do you have the student ID? (y/n)\n > ");
		userResponse = scan.nextLine();
		
		if (userResponse.equalsIgnoreCase("n"))
			studentId = this.addStudent();
		else {
			System.out.println("Enter the student id:\n > ");
			studentId = scan.nextLine();
		}
		
		scan.close();
		return studentId;
	}
	
	private String addStudent() {
		Scanner scan = new Scanner(System.in);
		String userResponse = null, studentId = null;
		
		System.out.println("To which campus you go to study?\n > ");
		userResponse = scan.nextLine();
		
		try {
			studentId = authInterface.addStudent(userResponse);
		} catch (RemoteException re) {
			System.out.println("Unable to connect to server. Please try again!");
			logs.warning("Remote exception detected while assigning student to campus with message - " + re.getMessage());
		}
		
		scan.close();
		return studentId;
	}
	
	public CampusRegistry authenticateStudent(String studentId) {
		CampusRegistry campus = null;
		try {
			campus = authInterface.getCampus(studentId, false);
		} catch (RemoteException re) {
			System.out.println("Unable to connect to server. Please try again!");
			logs.warning("Remote exception detected while authenticating the student with message - " + re.getMessage());
		}
		return campus;
	}
	
	public boolean bookRoom(CampusStudentInterface campusInterface, String studentId) {
		Scanner scan = new Scanner(System.in);
		String strRepo = null, bookingId = null;
		int intRepo = -1, roomNo;
		Date date = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
		List<Integer> availableRooms = null;
		List<TimeSlot> availableTimeSlots = null;
		TimeSlot slotToBook = null;
		boolean success = false;
		
		System.out.println("Enter the date for which you want to book the room:\n(format: DD-MM-YYYY) e.g. 11-01-2018\n > ");
		strRepo = scan.nextLine();
		
		try {
			date = simpleDateFormat.parse(strRepo);
		} catch (ParseException e) {
			System.out.println("Please try again!");
			logs.warning("Error parsing date taken from user");
			scan.close();
			return false;
		}
		
		try {
			availableRooms = campusInterface.availableRooms(date);
		} catch (RemoteException e) {
			System.out.println("Please try again!");
			logs.warning("Remote exception detected while fetching avialable rooms from server with message - " + e.getMessage());
			scan.close();
			return false;
		}
		
		System.out.println(((availableRooms == null) ? 0 : availableRooms.size()) + " room(s) available!");
		if ((availableRooms != null) && (availableRooms.size() > 0)) {
			System.out.println("Rooms: ");
			for (Integer i : availableRooms) {
				System.out.print(i + " ");
			}
		} else {
			System.out.println("Please try with different date!");
			scan.close();
			return false;
		}
		
		System.out.println("Enter the room number you want to book to know the available timeslots:\n > ");
		intRepo = scan.nextInt();
		
		try {
			availableTimeSlots = campusInterface.getAvailableTimeSlots(date, intRepo);
			roomNo = intRepo;
		} catch (RemoteException e) {
			System.out.println("Please try again!");
			logs.warning("Remote exception detected while fetching avialable timeslots from server with message - " + e.getMessage());
			scan.close();
			return false;
		}
		
		if ((availableTimeSlots != null) && (availableTimeSlots.size() > 0)) {
			System.out.println("Available time slots:");
			for (int i = 0; i < availableTimeSlots.size(); i++) {
				TimeSlot item = availableTimeSlots.get(i);
				System.out.println(i + ". " + item.getStartTime() + ":" + item.getEndTime());
			}
		} else {
			System.out.println("No timeslots available. Please try with different room!");
			scan.close();
			return false;
		}
		
		System.out.println("Which timeslot you want to book? (enter the corresponding number)\n > ");
		intRepo = scan.nextInt();
		
		if (intRepo > availableTimeSlots.size()) {
			System.out.println("There is no such timeslot!");
			logs.warning("Index by user is not in range for timeslot");
			scan.close();
			return false;
		}
		
		slotToBook = availableTimeSlots.get(intRepo);
		System.out.println("Are you sure you want to book? (y/n)\n > ");
		strRepo = scan.nextLine();
		
		if (strRepo.equalsIgnoreCase("n")) {
			logs.warning("Aborted by user");
			scan.close();
			return false;
		}
		
		try {
			bookingId = campusInterface.bookRoom(studentId, roomNo, date, slotToBook);
		} catch (RemoteException re) {
			System.out.println("There seems to be problem with server. Please try again!");
			logs.warning("Remote exception detected while booking room with message - " + re.getMessage());
			scan.close();
			return false;
		}
		
		if (bookingId != null) {
			try {
				success = authInterface.bookRoom(studentId, bookingId);
			} catch(RemoteException re) {
				logs.warning("Remote exception detected while booking room with message (auth) - " + re.getMessage());
				scan.close();
				return false;
			}
		} else {
			System.out.println("There seems to be problem with server. Please try again!");
			logs.warning("No id returned from server for booking");
			scan.close();
			return false;
		}
		
		System.out.println(success ? "Booking successful!" : "Could not register a room for you at authentication server");
		
		scan.close();
		return success;
	}
	
	public boolean cancelBooking(CampusStudentInterface campusInterface, String studentId) {
		boolean success = false;
		String bookingId = null;
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Enter the booking id:\n > ");
		bookingId = scan.nextLine();
		
		try {
			success = campusInterface.cancelBooking(bookingId);
		} catch (RemoteException re) {
			System.out.println("There seems to be problem with server. Please try again!");
			logs.warning("Remote exception detected while cancelling booking with message - " + re.getMessage());
			scan.close();
			return false;
		}
		
		if (success) {
			try {
				success = authInterface.cancelBooking(studentId, bookingId);
			} catch (RemoteException re) {
				logs.warning("Remote exception detected while cancelling booking with message (auth) - " + re.getMessage());
				scan.close();
				return false;
			}
		}
		
		System.out.println(success ? "Cancellation successful!" : "Could not confirm your cancellation with auth server");
		
		scan.close();
		return success;
	}
}
