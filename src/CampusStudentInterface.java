import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

public interface CampusStudentInterface extends Remote {
	String bookRoom(String studentId, int roomNo, Date date, TimeSlot timeSlot) throws RemoteException;
	List<Integer> availableRooms(Date date) throws RemoteException;
	List<TimeSlot> getAvailableTimeSlots(Date date, int roomNo) throws RemoteException;
	boolean cancelBooking(String bookingId) throws RemoteException;
}
