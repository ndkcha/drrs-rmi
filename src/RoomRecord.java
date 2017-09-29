import java.util.Date;
import java.util.List;

public class RoomRecord {
	private String roomId;
	private String campusCode;
	private int roomNo;
	private Date date;
	private List<TimeSlot> timeSlots;
	
	public RoomRecord(String roomId, String campusCode, int roomNo, Date date, List<TimeSlot> timeSlots) {
		this.roomId = roomId;
		this.campusCode = campusCode;
		this.roomNo = roomNo;
		this.date = date;
		this.timeSlots = timeSlots;
	}
	
	public String getRoomId() {
		return this.roomId;
	}
	
	public String getCampusCode() {
		return this.campusCode;
	}
	
	public int getRoomNo() {
		return this.roomNo;
	}
	
	public Date getDate() {
		return this.date;
	}
	
	public List<TimeSlot> getTimeSlots() {
		return this.timeSlots;
	}
}
