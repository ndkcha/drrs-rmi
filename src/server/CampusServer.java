package server;

import auth.AuthInterface;
import auth.AuthOperations;
import schema.AuthUdpPacket;
import schema.Campus;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class CampusServer {
    private static FileHandler fileHandler;
    private static Logger logs;
    private static AuthInterface authInterface;

    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        CampusOperations campusOps;
        Campus campus;

        System.out.println("Enter the name of the campus:");
        String campusName = scan.nextLine();

        // set up the logging mechanism
        logs = Logger.getLogger(campusName + " Server");
        try {
            fileHandler = new FileHandler(campusName.replace(" ", "-").toLowerCase(), true);
            logs.addHandler(fileHandler);
        } catch(IOException ioe) {
            logs.warning("Failed to create handler for log file.\n Message: " + ioe.getMessage());
        }

        try {
            campusOps = new CampusOperations(logs);
        } catch (RemoteException re) {
            logs.warning("Failed to initialize the instructions for campus operations.\nRemote Exception, message: " + re.getMessage());
            return;
        }

        campus = campusOps.setUpCampus(campusName, scan);

        // start server
        try {
            Registry registry = LocateRegistry.createRegistry(campus.getPort());
            registry.bind(campus.getVirtualAddress(), campusOps);
            logs.info("Server for the campus, " + campusName + " is running as " + campus.getCode() + " on port " + campus.getPort());
        } catch (RemoteException | AlreadyBoundException e) {
            logs.warning("Failed to start server.\nMessage: " + e.getMessage());
            return;
        }

        // connect to auth server
        campusOps.registerCampus();

        scan.close();
    }


}