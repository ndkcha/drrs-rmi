package schema;

import java.io.Serializable;
import java.util.List;

public class Student implements Serializable {
    private String studentId;
    public List<String> bookingIds;

    public Student(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return this.studentId;
    }
}
