package student;

import auth.AuthStudentInterface;
import schema.Campus;
import schema.TimeSlot;
import server.CampusStudentOperations;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class StudentOperations {
    private AuthStudentInterface authInterface;
    private Logger logs;
    private Campus campus = null;

    public StudentOperations(AuthStudentInterface authInterface, Logger logs) {
        this.authInterface = authInterface;
        this.logs = logs;
    }

    public boolean lookupCampus(String code) {
        try {
            campus = authInterface.lookupCampus(code);
        } catch(RemoteException re) {
            logs.warning("Error looking up campus!\nMessage: " + re.getMessage());
            return false;
        }
        return (campus != null);
    }

    public Campus getCampus() {
        return campus;
    }

    public boolean bookRoom(CampusStudentOperations campusStudentOperations, Scanner scan, String studentId) {
        boolean success = false;
        String response, code, fromTime, toTime;
        Date date;
        int roomNo;
        TimeSlot slot;
        HashMap<String, Integer> availableTimeSlots;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

        System.out.println("Enter the date for which you want to book the room: (format: dd-MM-yyyy)");
        response = scan.nextLine();

        try {
            date = simpleDateFormat.parse(response);
        } catch (ParseException e) {
            logs.warning("Error parsing date taken from user");
            return false;
        }

        try {
            availableTimeSlots = campusStudentOperations.availableTimeSlots(date);
        } catch(RemoteException re) {
            logs.warning("Remote exception while fetching available time slots.\nMessage: " + re.getMessage());
            return false;
        }

        System.out.println("Available time slots:");

        for (Map.Entry<String, Integer> entry : availableTimeSlots.entrySet()) {
            String campusCode = entry.getKey();
            int rooms = entry.getValue();

            System.out.println(campusCode + " " + rooms);
        }

        System.out.println("Enter the campus code:");
        code = scan.nextLine();

        System.out.println("Enter the room number:");
        roomNo = scan.nextInt();
        scan.nextLine();

        System.out.println("Enter the 'start time' for time slot (format: hh:mm):");
        fromTime = scan.nextLine();

        System.out.println("Enter the 'end time' for time slot (format: hh:mm):");
        toTime = scan.nextLine();

        slot = new TimeSlot(fromTime, toTime);

        try {
            String bookingId = campusStudentOperations.bookRoom(studentId, code, roomNo, date, slot);
            if (bookingId != null) {
                if (bookingId.startsWith("BKG")) {
                    success = true;
                    System.out.println("Your booking id: " + bookingId);
                } else {
                    success = false;
                    logs.warning(bookingId);
                }
            }
        } catch (RemoteException re) {
            logs.warning("Remote exception while booking room.\nMessage: " + re.getMessage());
            return false;
        }

        return success;
    }

    public boolean cancelBookings(CampusStudentOperations campusStudentOperations, Scanner scan, String studentId) {
        boolean success = false;
        String bookingId;

        System.out.println("Enter the bookingId:");
        bookingId = scan.nextLine();

        try {
            success = campusStudentOperations.cancelBooking(studentId, bookingId);
        } catch (RemoteException re) {
            logs.warning("Remote exception while cancelling room.\nMessage: " + re.getMessage());
            return false;
        }

        return success;
    }
}
