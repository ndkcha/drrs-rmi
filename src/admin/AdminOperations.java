package admin;

import auth.AuthInterface;
import schema.Campus;
import schema.TimeSlot;
import server.CampusAdminOperations;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class AdminOperations {
    private AuthInterface authInterface;
    private Logger logs;

    public AdminOperations(AuthInterface authInterface, Logger logs) {
        this.authInterface = authInterface;
        this.logs = logs;
    }

    public String askAdminId(Scanner scan) {
        String response, adminId;

        System.out.println("Do you have an adminId? (y/n):");
        response = scan.nextLine();

        if (response.equalsIgnoreCase("y")) {
            System.out.println("Enter the adminId:");
            adminId = scan.nextLine();
        } else {
            System.out.println("Enter the campus code for which you want to be admin: (e.g. DVL, WMT, etc.)");
            String code = scan.nextLine();
            try {
                adminId = authInterface.addAdmin(code);
                System.out.println("Your new adminId is, " + adminId + ". Use it for logging into the system next time.");
            } catch (RemoteException re) {
                logs.warning("Error generating adminId at server.\n Message: " + re.getMessage());
                adminId = null;
            }
        }

        return adminId;
    }

    public Campus authenticateAdmin(String adminId) {
        Campus campus;
        try {
            campus = authInterface.getCampus(adminId);
        } catch(RemoteException e) {
            logs.warning("Error authenticating with server.\nMessage: " + e.getMessage());
            campus = null;
        }

        return campus;
    }

    public boolean createRoom(CampusAdminOperations campusAdminOperations, Scanner scan) {
        List<TimeSlot> timeSlots = new ArrayList<>();
        int roomNo;
        String fromTime, toTime, response;
        Date date;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        boolean isMoreSlots, success = false;

        System.out.println("\nEnter the date: (dd-MM-yyyy) (e.g. 11-01-2018)");
        response = scan.nextLine();
        try {
            date = simpleDateFormat.parse(response);
        } catch(ParseException e) {
            logs.warning("Error parsing date.\nMessage: " + e.getMessage());
            return success;
        }

        System.out.println("Enter the room no: (integer values only)");
        roomNo = scan.nextInt();
        scan.nextLine();

        System.out.println("Entries for time slots:");
        do {
            System.out.println("Enter from time: (hh:mm)");
            fromTime = scan.nextLine();
            System.out.println("Enter to time: (hh:mm)");
            toTime = scan.nextLine();

            TimeSlot slot = new TimeSlot(fromTime, toTime);
            timeSlots.add(slot);

            System.out.println("Add another time slot? (y/n)");
            response = scan.nextLine();

            isMoreSlots = response.equalsIgnoreCase("y");
        } while(isMoreSlots);

        try {
            success = campusAdminOperations.createRoom(date, roomNo, timeSlots);
        } catch(RemoteException re) {
            logs.warning("Error creating room at server.\nMessage: " + re.getMessage());
            success = false;
        }

        return success;
    }
}
