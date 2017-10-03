import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class CampusServer extends UnicastRemoteObject implements CampusAdminInterface, CampusStudentInterface {
	private static final long serialVersionUID = 1L;
	private static FileHandler logFileHandler;
	private static Logger campusLogs = Logger.getLogger("Campus Server");
	private List<RoomRecord> roomRecords = null;
	private static CampusRegistry campus;
	private List<Booking> bookings = null;

	public CampusServer() throws RemoteException {
		super();
	}
	
	public static void main(String args[]) {
		AuthenticationInterface authInterface;
		
		Scanner scan = new Scanner(System.in);
		String campusName;

		System.out.println("Enter the name of the campus (no spaces allowed):");
		campusName = scan.nextLine();
		
		campus = askCampusDetails(campusName, scan);
		
		scan.close();
		
		try {
			logFileHandler = new FileHandler(campusName.replace(" ", "-").toLowerCase() + "-server.log", true);
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
		String virtualAddress, code = null;
		int port;
		
		System.out.println("Enter the virtual address for " + name + " server:");
		virtualAddress = scan.nextLine();
		System.out.println("Enter the port number for the server:");
		port = scan.nextInt();
		System.out.println("Enter the three letter code for the " + name + "(to be used for clientIds, no spaces allowed):");
		scan.nextLine();
		code = scan.nextLine();
		
		CampusRegistry campus = new CampusRegistry(name, virtualAddress, code, port);
		return campus;
	}
	
	public boolean createRoom(int roomNo, Date date, List<TimeSlot> timeSlots) {
		Random random = new Random();
		int num = random.nextInt(100000);
		String roomId = "RR" + String.format("%05d", num);
		
		try {
			RoomRecord room = new RoomRecord(roomId, campus.getCode(), roomNo, date, timeSlots);
			this.roomRecords = (this.roomRecords == null) ? (new ArrayList<>()) : this.roomRecords;
			this.roomRecords.add(room);
			campusLogs.info("One room added with id, " + roomId);
			return true;
		} catch (Exception e) {
			campusLogs.warning("Error adding a room to campus.");
			return false;
		}
	}
	
	public List<Integer> availableRooms(Date date) {
		List<Integer> ar = new ArrayList<>();
		
		boolean isDateAvailable = false;
		boolean isOperationDone = false;
		for (RoomRecord item : this.roomRecords) {
			if (item.getDate().compareTo(date) == 0) {
				isOperationDone = false;
				for (TimeSlot slot : item.getTimeSlots()) {
					if (slot.getBookedBy() == null) {
						ar.add(item.getRoomNo());
						isOperationDone = true;
						break;
					}
				}
				if (isOperationDone)
					continue;
			}
		}
		if (!isDateAvailable)
			return null;
		
		return ar;
	}
	
	public List<TimeSlot> getAvailableTimeSlots(Date date, int roomNo) {
		List<TimeSlot> timeSlots = new ArrayList<>();
		RoomRecord rr = null;
		for (RoomRecord item : this.roomRecords) {
			if (item.getDate().compareTo(date) == 0) {
				if (item.getRoomNo() == roomNo) {
					rr = item;
					break;
				}
			}
		}
		
		if (rr == null)
			return null;
		
		for (TimeSlot item : rr.getTimeSlots()) {
			if (item.getBookedBy() == null)
				timeSlots.add(item);
		}
		
		return timeSlots;
	}
	
	public String bookRoom(String studentId, int roomNo, Date date, TimeSlot timeSlot) {;
		int rrIndex = -1, slotIndex = -1;
		String bookingId = null;
		boolean isSuccessfull = false;
		TimeSlot ts = null;
		
		for (RoomRecord item : this.roomRecords) {
			if ((item.getDate().compareTo(date) == 0) && (item.getRoomNo() == roomNo)) {
				rrIndex = this.roomRecords.indexOf(item);
				List<TimeSlot> slots = item.getTimeSlots();
				
				for (TimeSlot slot : slots) {
					if (slot.getStartTime().equalsIgnoreCase(timeSlot.getStartTime()) && slot.getStartTime().equalsIgnoreCase(timeSlot.getEndTime())) {
						slotIndex = slots.indexOf(slot);
						slot.setBookedBy(studentId);
						slots.set(slotIndex, slot);
						ts = slot;
						isSuccessfull = true;
						break;
					}
				}
				
				item.setTimeSlots(slots);
				this.roomRecords.set(rrIndex, item);
				
				if (isSuccessfull)
					break;
			}
		}
		
		if ((rrIndex == -1) || (slotIndex == -1))
			return null;
		
		
		bookingId = generateBookingId();
		
		Booking booking = new Booking(bookingId, studentId, campus.getCode(), roomNo, date, ts);
		this.bookings = (this.bookings == null) ? (new ArrayList<>()) : this.bookings;
		this.bookings.add(booking);
		return bookingId;
	}
	
	private String generateBookingId() {
		Random random = new Random();
		int num = random.nextInt(100000);
		return "Bk" + campus.getCode() + String.format("%4d", num);
	}
	
	public boolean cancelBooking(String bookingId) {
		Booking booking = null;
		boolean breakLoop = false;
		
		for (Booking item : this.bookings) {
			if (item.getBookingId().equalsIgnoreCase(bookingId)) {
				int bookingIndex = this.bookings.indexOf(item);
				booking = this.bookings.remove(bookingIndex);
				break;
			}
		}
		
		if (booking == null)
			return false;
		
		for (RoomRecord item : this.roomRecords) {
			if ((item.getRoomNo() == booking.roomNo) && (item.getDate().compareTo(booking.date) == 0)) {
				int rrIndex = this.roomRecords.indexOf(item);
				List<TimeSlot> slots = item.getTimeSlots();
				for (TimeSlot slot : slots) {
					if (slot.getBookedBy().equalsIgnoreCase(booking.getStudentId())) {
						int slotIndex = slots.indexOf(slot);
						slot.setBookedBy(null);
						slots.set(slotIndex, slot);
						breakLoop = true;
						break;
					}
				}
				item.setTimeSlots(slots);
				this.roomRecords.set(rrIndex, item);
				if (breakLoop)
					break;
			}
		}
		
		return true;
	}
	
	private int findBooking(String studentId) {
		int bookingIndex = -1;
		for (Booking book : this.bookings) {
			if (book.getStudentId().equals(studentId)) {
				bookingIndex = this.bookings.indexOf(book);
				break;
			}
		}
		
		return bookingIndex;
	}
	
	public boolean deleteRoom(int roomNo, Date date, TimeSlot timeSlot) {
		int rrIndex = -1;
		boolean isDeleteRoom = false;
		
		for (RoomRecord room : this.roomRecords) {
			if ((room.getRoomNo() == roomNo) && (room.getDate().compareTo(date) == 0)) {
				rrIndex = this.roomRecords.indexOf(room);
				List<TimeSlot> slots = room.getTimeSlots();
				
				for (TimeSlot slot : slots) {
					if (slot.getStartTime().equalsIgnoreCase(timeSlot.getStartTime()) && slot.getEndTime().equalsIgnoreCase(timeSlot.getEndTime())) {
						if (slot.getBookedBy() != null) {
							int bi = this.findBooking(slot.getBookedBy());
							if (bi > -1) {
								this.bookings.remove(bi);
							}
						}
						slots.remove(slot);
					}
				}
				
				room.setTimeSlots(slots);
				isDeleteRoom = (slots.size() == 0);
				
				this.roomRecords.set(rrIndex, room);
				break;
			}
		}
		
		if (isDeleteRoom && (rrIndex > -1))
			this.roomRecords.remove(rrIndex);
		
		return (rrIndex > -1);
	}
}
