package schema;

import java.io.Serializable;

public class TimeSlot implements Serializable {
    private String bookingId;
    public String startTime, endTime, bookedBy;

    public TimeSlot(String startTime, String endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getBookingId() {
        return this.bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}
