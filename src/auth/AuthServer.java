/**
 * AuthServer acts as a central repository for the Distributed Room Reservation System.
 * It maintains records of different campus instances running in the system and list of administrators assigned to them.
 */

package auth;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class AuthServer {
    private static Logger logs = Logger.getLogger("Auth Server");

    public static void main(String args[]) {
        AuthOperations authOperations;

        // Set up logger
        try {
            FileHandler fileHandler = new FileHandler("auth-server.log", true);
            logs.addHandler(fileHandler);
        } catch(IOException ioe) {
            System.out.println("Exception thrown while initializing handler.\nMessage: " + ioe.getMessage());
        }

        // set up authOps
        try {
            authOperations = new AuthOperations(logs);
        } catch (RemoteException re) {
            logs.warning("The instructions for authentication server could not be loaded.\n Message: " + re.getMessage());
            return;
        }

        // start the auth server
        try {
            Registry authRegistry = LocateRegistry.createRegistry(8008);
            authRegistry.bind("auth", authOperations);
            logs.info("The RMI server for authentication is up and running on port 8008");
        } catch (RemoteException | AlreadyBoundException e) {
            logs.warning("Exception thrown while starting server.\nMessage: " + e.getMessage());
        }

        // start the udp server
        try {
            DatagramSocket udpSocket = new DatagramSocket(8009);
            byte[] incoming = new byte[10000];
            logs.info("The UDP server for authentication is up and running on port 8009");
            while (true) {
                DatagramPacket packet = new DatagramPacket(incoming, incoming.length);
                try {
                    udpSocket.receive(packet);
                    AuthUdpProc proc = new AuthUdpProc(udpSocket, packet, authOperations, logs);
                    proc.start();
                } catch (IOException ioe) {
                    logs.warning("Error receiving packet.\nMessage: " + ioe.getMessage());
                }
            }
        } catch (SocketException e) {
            logs.warning("Exception thrown while server was running/trying to start.\nMessage: " + e.getMessage());
        }
    }
}
