package student;

import auth.AuthAdminInterface;
import auth.AuthStudentInterface;
import schema.Campus;
import server.CampusStudentOperations;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.Logger;

public class StudentClient {
    private static Logger logs = Logger.getLogger("Student Client");

    public static void main(String[] args) {
        AuthStudentInterface authInterface;
        StudentOperations studentOps;
        CampusStudentOperations campusStudentOperations;
        Campus campus;
        String response, studentId = null, code, message;
        boolean isStudentIdValid, isOperationSuccessful;
        int choice;

        Scanner scan = new Scanner(System.in);

        // connect to auth server
        try {
            Registry registry = LocateRegistry.getRegistry(8008);
            authInterface = (AuthStudentInterface) registry.lookup("auth");
        } catch (RemoteException | NotBoundException e) {
            logs.severe("Error connecting to auth server.\n Message: " + e.getMessage());
            scan.close();
            return;
        }

        studentOps = new StudentOperations(authInterface, logs);

        System.out.println("Do you have studentId? (y/n)");
        response = scan.nextLine();

        if (response.equalsIgnoreCase("n")) {
            System.out.println("Enter the campus code: (e.g. DVL, WMT, etc.)");
            code = scan.nextLine();
        } else {
            System.out.println("Enter your studentId:");
            studentId = scan.nextLine();
            code = studentId.substring(0, 3);
        }

        if (!studentOps.lookupCampus(code)) {
            logs.severe("Could not find the campus server.");
            scan.close();
            return;
        }

        campus = studentOps.getCampus();

        // connect to campus server
        try {
            Registry registry = LocateRegistry.getRegistry(campus.getPort());
            campusStudentOperations = (CampusStudentOperations) registry.lookup(campus.getVirtualAddress());
            logs.info("Connected to " + campus.name + " server.");
        } catch (RemoteException | NotBoundException e) {
            logs.severe("Error connecting to " + campus.name + " server.\nMessage: " + e.getMessage());
            return;
        }

        try {
            studentId = (studentId == null) ? campusStudentOperations.generateStudentId() : studentId;
            System.out.println("\nYour studentId: " + studentId + "\n");
        } catch (RemoteException re) {
            logs.severe("Error generating studentId at server.\nMessage: " + re.getMessage());
            return;
        }

        try {
            isStudentIdValid = campusStudentOperations.lookupStudent(studentId);
            if (!isStudentIdValid) {
                logs.severe("studentId does not exist at server. Please try again!");
                return;
            }
        } catch (RemoteException re) {
            logs.severe("Error looking up studentId at server.\nMessage: " + re.getMessage());
            return;
        }

        System.out.println("\n\n\tWelcome to " + campus.name + "\n");

        while (true) {
            System.out.println("What do you want to do?\n\t1. Book Room\n\t2. Cancel Booking\n - Any other number to exit:");
            choice = scan.nextInt();
            scan.nextLine();

            switch (choice) {
                case 1:
                    isOperationSuccessful = studentOps.bookRoom(campusStudentOperations, scan, studentId);
                    message = isOperationSuccessful ? "The room has been booked" : "Failed to book the room!";
                    break;
                case 2:
                    isOperationSuccessful = studentOps.cancelBookings(campusStudentOperations, scan, studentId);
                    message = isOperationSuccessful ? "The booking has been cancelled" : "Failed to cancel the booking!";
                    break;
                default:
                    logs.info("Exit requested. Have a nice day!");
                    scan.close();
                    return;
            }

            if (isOperationSuccessful)
                logs.info(message);
            else
                logs.warning(message);
        }
    }
}
