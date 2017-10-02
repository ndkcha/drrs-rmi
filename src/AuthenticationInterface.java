import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthenticationInterface extends Remote {
	CampusRegistry getCampus(String adminId, boolean areYouAdmin) throws RemoteException;
	boolean addCampus(CampusRegistry campus) throws RemoteException;
	String addAdmin(String campus) throws RemoteException;
	String addStudent(String campus) throws RemoteException;
	boolean canStudentBookRoom(String studentId) throws RemoteException;
	boolean bookRoom(String studentId, String bookingId) throws RemoteException;
	boolean cancelBooking(String studentId, String bookingId) throws RemoteException;
}
