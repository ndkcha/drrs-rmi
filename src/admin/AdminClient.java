package admin;

import auth.AuthAdminInterface;
import schema.Campus;
import server.CampusAdminOperations;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AdminClient {
    private static Logger logs = Logger.getLogger("Admin Client");

    public static void main(String args[]) {
        AuthAdminInterface authInterface;
        AdminOperations adminOps;
        String adminId, message;
        Campus campus;
        CampusAdminOperations campusAdminOperations;
        int choice;
        boolean isOperationSuccessful;

        Scanner scan = new Scanner(System.in);

        // connect to auth server
        try {
            Registry registry = LocateRegistry.getRegistry(8008);
            authInterface = (AuthAdminInterface) registry.lookup("auth");
            logs.info("Connection established to authentication server.");
        } catch(RemoteException | NotBoundException e) {
            logs.severe("Error connecting to authentication server.\n Message: " + e.getMessage());
            return;
        }

        adminOps = new AdminOperations(authInterface, logs);

        // get adminId
        adminId = adminOps.askAdminId(scan);

        // set up file handler for logging mechanism.
        try {
            FileHandler handler = new FileHandler(adminId + ".log", true);
            logs.addHandler(handler);
        } catch (IOException ioe) {
            logs.warning("Error initializing log file.\n Message: " + ioe.getMessage());
        }

        // get campus details
        campus = adminOps.authenticateAdmin(adminId);

        if (campus == null) {
            logs.severe("The admin is not associated with any server. Please restart the program to try again!");
            return;
        }

        // connect to campus
        try {
            Registry registry = LocateRegistry.getRegistry(campus.getPort());
            campusAdminOperations = (CampusAdminOperations) registry.lookup(campus.getVirtualAddress());
            logs.info("Connected to " + campus.name + " server.");
        } catch(RemoteException | NotBoundException e) {
            logs.severe("Error connecting to " + campus.name + " server. \nMessage: " + e.getMessage());
            return;
        }

        System.out.println("\n\n\tWelcome to " + campus.name + "\n");

        while (true) {
            System.out.println("What do you want to do?\n\t1. Create Room\n\t2. Delete Room\n - Any other number to exit:");
            choice = scan.nextInt();
            scan.nextLine();

            switch (choice) {
                case 1:
                    isOperationSuccessful = adminOps.createRoom(campusAdminOperations, scan);
                    message = isOperationSuccessful ? "Room has been created successfully." : "Record already exists at server";
                    break;
                case 2:
                    isOperationSuccessful = adminOps.deleteRoom(campusAdminOperations, scan);
                    message = isOperationSuccessful ? "Room has been deleted successfully" : "Room could not be deleted!";
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
