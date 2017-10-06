package server;

import auth.AuthOperations;
import schema.AuthUdpPacket;
import schema.Campus;
import schema.Student;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class CampusOperations extends UnicastRemoteObject {
    private Campus campus;
    private Logger logs;
    private List<Student> students = new ArrayList<>();

    public CampusOperations(Logger logs) throws RemoteException {
        super();
        this.logs = logs;
    }

    public Campus setUpCampus(String name, Scanner scan) {
        String virtualAddress, code;
        int port;

        System.out.println("Enter the virtual address (For RMI lookup):");
        virtualAddress = scan.nextLine();

        System.out.println("Enter the campus code (e.g. DVL, WMT):");
        code = scan.nextLine();

        System.out.println("Enter the port number (For RMI registry):");
        port = scan.nextInt();

        campus = new Campus(name, virtualAddress, port, code);
        return campus;
    }

    public void registerCampus() {
        // connect to auth server and register campus
        try {
            String message;
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(AuthOperations.ADD_CAMPUS.BODY_CAMPUS, campus);
            AuthUdpPacket udpPacket = new AuthUdpPacket(AuthOperations.ADD_CAMPUS.OP_CODE, body);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), 8009);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);
            String response = (String) deserialize(incomingPacket.getData());

            // generate and display appropriate message in logs
            switch (response.toLowerCase()) {
                case "success":
                    message = "The campus has been registered to the authentication server successfully.";
                    break;
                case "fail":
                    message = "The campus already exists at the server.";
                    break;
                default:
                    message = "The authentication server has returned with unexpected error";
                    break;
            }
            if (response.equalsIgnoreCase("success"))
                logs.info(message);
            else
                logs.warning(message);
        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }
    }

    public byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }
}
