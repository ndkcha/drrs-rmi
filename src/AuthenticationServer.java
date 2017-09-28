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
	public List<CampusRegistry> campusRegistries = new ArrayList<>();
	public List<Admin> admins = new ArrayList<>();
	
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
		String adminId = this.generateAdminId(this.campusRegistries.get(campusIndex).getCode());
		while (true) {
			if (this.isAlreadyAdmin(adminId))
				adminId = this.generateAdminId(this.campusRegistries.get(campusIndex).getCode());
			else
				break;
		}
		Admin admin = new Admin(adminId, campus);
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
	
	private String generateAdminId(String campusCode) {
		Random random = new Random();
		int num = random.nextInt(100000);
		String id = String.format("%05d", num);
		return campusCode.toUpperCase() + id;
	}
	
	public CampusRegistry getCampus(String adminId) {
		String name = null;
		for (Admin item : this.admins) {
			if (item.adminId == adminId) {
				name = adminId;
				break;
			}
		}
		if (name == null) {
			authLogs.warning("Admin is not registered with the system.");
			return null;
		}
		for (CampusRegistry item : this.campusRegistries) {
			if (item.name == name) {
				authLogs.info("Search request for campus " + name + " has been served.");
				return item;
			}
		}
		authLogs.warning("Campus " + name + " not found in the repository.");
		return null;
	}
	
	public boolean addCampus(String name, String virtualAddress, String code, int port) {
		for (CampusRegistry item : this.campusRegistries) {
			if (item.name.equalsIgnoreCase(name)) {
				authLogs.warning("The campus " + name + "already exists. Can not add duplicate entry");
				return false;
			}
		}
		CampusRegistry campus = new CampusRegistry(name, virtualAddress, code, port);
		this.campusRegistries.add(campus);
		authLogs.info("New campus " + name + "added to the repository.");
		return true;
	}
}
