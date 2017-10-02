import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AuthenticationServer extends UnicastRemoteObject implements AuthenticationInterface {
	private static final long serialVersionUID = 1L;
	private static FileHandler logFileHandler;
	private static Logger authLogs = Logger.getLogger("Authentication Server");
	private List<CampusRegistry> campusRegistries = new ArrayList<>();
	private List<Admin> admins = new ArrayList<>();
	private List<Student> students = new ArrayList<>();
	
	public AuthenticationServer() throws RemoteException {
		super();
	}
	
	public static void main(String args[]) {
		try {
			logFileHandler = new FileHandler("auth-server.log", true);
			authLogs.addHandler(logFileHandler);
		} catch (IOException ioe) {
			System.out.println("Failed to initialize application log system.");
		}
		
		try {
			AuthenticationServer authServer = new AuthenticationServer();
			Registry registry = LocateRegistry.createRegistry(8008);
			registry.rebind("auth", authServer);
			authLogs.info("Authentication server can now accept incoming connections.");
			System.out.println("Server is running on port 8008");
		} catch(RemoteException re) {
			authLogs.warning("Remote exception detected with message, " + re.getMessage());
			System.out.println("Error starting the server. Please try again!");
		}
	}
	
	public String addAdmin(String campus) {
		boolean campusFound = false;
		int campusIndex = -1;
		for (CampusRegistry item : this.campusRegistries) {
			if (item.name.equalsIgnoreCase(campus)) {
				campusFound = true;
				campusIndex = this.campusRegistries.indexOf(item);
				break;
			}
		}
		if (!campusFound) {
			authLogs.warning("Campus " + campus + " not found! Admin cannot be added.");
			return null;
		}
		String adminId = this.generateId(this.campusRegistries.get(campusIndex).getCode(), false);
		while (true) {
			if (this.isAlreadyAdmin(adminId))
				adminId = this.generateId(this.campusRegistries.get(campusIndex).getCode(), false);
			else
				break;
		}
		Admin admin = new Admin(adminId, this.campusRegistries.get(campusIndex).getCode());
		this.admins.add(admin);
		return adminId;
	}
	
	private boolean isAlreadyAdmin(String adminId) {
		for (Admin item : this.admins) {
			if (item.adminId == adminId)
				return true;
		}
		return false;
	}
	
	private String generateId(String campusCode, boolean isItStudent) {
		Random random = new Random();
		int num = random.nextInt(100000);
		String id = String.format("%04d", num);
		return campusCode.toUpperCase() + (isItStudent ? "S" : "A") + id;
	}
	
	public CampusRegistry getCampus(String id, boolean areYouAdmin) {
		String campusCode = null;
		if (areYouAdmin) {
			for (Admin item : this.admins) {
				if (item.adminId == id) {
					campusCode = item.campusCode;
					break;
				}
			}
		} else {
			for (Student item : this.students) {
				if (item.getStudentId() == id) {
					campusCode = item.getCampusCode();
					break;
				}
			}
		}
		if (campusCode == null) {
			authLogs.warning(((areYouAdmin) ? "Admin" : "Student") + " is not registered with the system.");
			return null;
		}
		for (CampusRegistry item : this.campusRegistries) {
			if (item.getCode() == campusCode) {
				authLogs.info("Search request for campus " + item.name + " has been served.");
				return item;
			}
		}
		authLogs.warning("Campus " + campusCode + " not found in the repository.");
		return null;
	}
	
	public boolean addCampus(CampusRegistry campus) {
		for (CampusRegistry item : this.campusRegistries) {
			if (item.name.equalsIgnoreCase(campus.name)) {
				authLogs.warning("The campus " + campus.name + "already exists. Can not add duplicate entry");
				return false;
			}
		}
		this.campusRegistries.add(campus);
		authLogs.info("New campus " + campus.name + "added to the repository.");
		return true;
	}
	
	public String addStudent(String campus) {
		boolean campusFound = false;
		int campusIndex = -1;
		for (CampusRegistry item : this.campusRegistries) {
			if (item.name.equalsIgnoreCase(campus)) {
				campusFound = true;
				campusIndex = this.campusRegistries.indexOf(item);
				break;
			}
		}
		if (!campusFound) {
			authLogs.warning("Campus " + campus + " not found! Student cannot be added.");
			return null;
		}
		String studentId = this.generateId(this.campusRegistries.get(campusIndex).getCode(), true);
		while (true) {
			if (this.isAlreadyStudent(studentId))
				studentId = this.generateId(this.campusRegistries.get(campusIndex).getCode(), true);
			else
				break;
		}
		
		Student student = new Student(studentId, this.campusRegistries.get(campusIndex).getCode());
		this.students.add(student);
		
		return studentId;
	}
	
	private boolean isAlreadyStudent(String studentId) {
		for (Student item : this.students) {
			if (item.getStudentId() == studentId)
				return true;
		}
		return false;
	}
	
	public boolean canStudentBookRoom(String studentId) {
		
		for (Student item : this.students) {
			if (item.getStudentId().equalsIgnoreCase(studentId)) {
				return ((item.bookings == null) ? false : (item.bookings.size() < 3));
			}
		}
		
		return false;
	}
	
	public boolean bookRoom(String studentId, String bookingId) {
		for (Student item : this.students) {
			if (item.getStudentId().equalsIgnoreCase(studentId)) {
				item.bookings = (item.bookings == null) ? (new ArrayList<>()) : item.bookings;
				if (item.bookings.size() > 3)
					return false;
				item.bookings.add(bookingId);
				return true;
			}
		}
		return false;
	}
	
	public boolean cancelBooking(String studentId, String bookingId) {
		for (Student item : this.students) {
			if (item.getStudentId().equalsIgnoreCase(studentId)) {
				item.bookings = (item.bookings == null) ? (new ArrayList<>()) : item.bookings;
				return item.bookings.remove(bookingId);
			}
		}
		return false;
	}
}
