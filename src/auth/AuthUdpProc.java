package auth;

import schema.AuthUdpPacket;
import schema.Campus;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
            AuthUdpPacket udpPacket = (AuthUdpPacket) deserialize(this.packet.getData());
            switch (udpPacket.operationName) {
                case AuthOperations.ADD_CAMPUS.OP_CODE:
                    Campus campus = (Campus) udpPacket.body.get(AuthOperations.ADD_CAMPUS.BODY_CAMPUS);
                    String message = "Error";
                    if (campus != null) {
                        message = authOperations.addCampus(campus) ? "Success" : "Fail";
                        logs.info("Request has been processed successfully for campus: " + campus.name);
                    } else
                        logs.warning("Error parsing campus code. Failure response has been sent to the client.");
                    byte[] outgoing = serialize(message);
                    DatagramPacket res = new DatagramPacket(outgoing, outgoing.length, this.packet.getAddress(), this.packet.getPort());
                    this.server.send(res);
                    break;
                default:
                    logs.warning("Operation not found!");
                    break;
            }
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
