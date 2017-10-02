import java.util.Date;

public class Booking {
	private String bookingId;
	private String studentId;
	public String campusCode;
	public int roomNo;
	public Date date;
	public TimeSlot timeSlot;
	
	public Booking(String bookingId, String studentId, String campusCode, int roomNo, Date date, TimeSlot timeSlot) {
		this.studentId = studentId;
		this.bookingId = bookingId;
		this.campusCode = campusCode;
		this.roomNo = roomNo;
		this.date = date;
		this.timeSlot = timeSlot;
	}
	
	public String getStudentId() {
		return this.studentId;
	}
	
	public String getBookingId() {
		return this.bookingId;
	}
}
