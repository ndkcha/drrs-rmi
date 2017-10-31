package auth;

import schema.UdpPacket;
import schema.Campus;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.logging.Logger;

public class AuthUdpProc implements Runnable {
    private Thread thread;
    private DatagramSocket server;
    private DatagramPacket packet;
    private AuthOperations authOperations;
    private Logger logs;

    public AuthUdpProc(DatagramSocket server, DatagramPacket packet, AuthOperations authOperations, Logger logs) {
        this.server = server;
        this.packet = packet;
        this.authOperations = authOperations;
        this.logs = logs;
    }

    @Override
    public void run() {
        try {
            UdpPacket udpPacket = (UdpPacket) deserialize(this.packet.getData());
            byte[] outgoing;
            DatagramPacket res;
            switch (udpPacket.operationName) {
                case AuthOperations.ADD_CAMPUS.OP_CODE:
                    Campus campus = (Campus) udpPacket.body.get(AuthOperations.ADD_CAMPUS.BODY_CAMPUS);
                    String message = "Error";
                    if (campus != null) {
                        message = authOperations.addCampus(campus) ? "Success" : "Fail";
                        logs.info("Request has been processed successfully for campus: " + campus.name);
                    } else
                        logs.warning("Error parsing campus code. Failure response has been sent to the client.");
                    outgoing = serialize(message);
                    break;
                case AuthOperations.LIST_CAMPUS.OP_CODE:
                    List<Campus> campuses = authOperations.getCampuses();
                    outgoing = serialize(campuses);
                    break;
                case AuthOperations.UDP_PORT.OP_CODE:
                    String code = (String) udpPacket.body.get(AuthOperations.UDP_PORT.BODY_CODE);
                    int udpPort = authOperations.getUdpPort(code);
                    outgoing = serialize(udpPort);
                    break;
                default:
                    outgoing = serialize("Error");
                    logs.warning("Operation not found!");
                    break;
            }
            res = new DatagramPacket(outgoing, outgoing.length, this.packet.getAddress(), this.packet.getPort());
            this.server.send(res);
        } catch (IOException ioe) {
            logs.warning("Error reading the packet.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the packet.\nMessage: " + e.getMessage());
        }
    }

    public void start() {
        logs.info("One in coming connection. Forking a thread.");
        if (thread == null) {
            thread = new Thread(this, "Udp Process");
            thread.start();
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }
}
