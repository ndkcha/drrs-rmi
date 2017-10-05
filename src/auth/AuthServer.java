/**
 * AuthServer acts as a central repository for the Distributed Room Reservation System.
 * It maintains records of different campus instances running in the system and list of administrators assigned to them.
 */

package auth;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AuthServer extends UnicastRemoteObject {
    private static FileHandler fileHandler;
    private static Logger logs = Logger.getLogger("Auth Server");

    public AuthServer() throws RemoteException {
        super();
    }

    public static void main(String args[]) {
        // Set up logger
        try {
            fileHandler = new FileHandler("auth-server.log", true);
            logs.addHandler(fileHandler);
        } catch(IOException ioe) {
            System.out.println("Exception thrown while initializing handler.\nMessage: " + ioe.getMessage());
        }

        // start the auth server
        try {
            AuthOperations authOperations = new AuthOperations(logs);
            Registry authRegistry = LocateRegistry.createRegistry(8008);
            authRegistry.bind("auth", authOperations);
            logs.info("The authentication server is up and running on port 8008");
        } catch (RemoteException | AlreadyBoundException e) {
            logs.warning("Exception thrown while starting server.\nMessage: " + e.getMessage());
        }
    }
}
