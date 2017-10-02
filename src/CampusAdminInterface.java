import java.rmi.Remote;
import java.util.Date;
import java.util.List;

public interface CampusAdminInterface extends Remote {
	boolean createRoom(int roomNo, Date date, List<TimeSlot> timeSlots);
	boolean deleteRoom(int roomNo, Date date, TimeSlot timeSlot);
}
