import java.util.List;

public class Student {
	private String studentId, campusCode;
	public List<String> bookings;
	
	public Student(String studentId, String campusCode) {
		this.studentId = studentId;
		this.campusCode = campusCode;
	}
	
	public String getStudentId() {
		return this.studentId;
	}
	
	public String getCampusCode() {
		return this.campusCode;
	}
}
