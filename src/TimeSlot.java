public class TimeSlot {
	private String startTime;
	private String endTime;
	private String bookedBy;
	
	public TimeSlot(String startTime, String endTime, String bookedBy) {
		this.bookedBy = bookedBy;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public TimeSlot(String startTime, String endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public void setBookedBy(String bookedBy) {
		this.bookedBy = bookedBy;
	}
	
	public String getStartTime() {
		return this.startTime;
	}
	
	public String getEndTime() {
		return this.endTime;
	}
	
	public String getBookedBy() {
		return this.bookedBy;
	}
}
