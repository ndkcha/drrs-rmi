import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthenticationInterface extends Remote {
	CampusRegistry getCampus(String adminId) throws RemoteException;
	boolean addCampus(String name, String virtualAddress, String code, int port) throws RemoteException;
	String addAdmin(String campus) throws RemoteException;
}
