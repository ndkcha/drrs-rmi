package schema;

import java.util.List;

public class Student {
    private String studentId;
    public List<String> bookingIds;

    public Student(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return this.studentId;
    }
}
