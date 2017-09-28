import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthenticationInterface extends Remote {
	CampusRegistry getCampus(String adminId) throws RemoteException;
	boolean addCampus(CampusRegistry campus) throws RemoteException;
	String addAdmin(String campus) throws RemoteException;
}
