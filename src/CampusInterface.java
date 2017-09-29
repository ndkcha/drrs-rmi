import java.rmi.Remote;
import java.util.Date;
import java.util.List;

public interface CampusInterface extends Remote {
	boolean createRoom(int roomNo, Date date, List<TimeSlot> timeSlots);
}
