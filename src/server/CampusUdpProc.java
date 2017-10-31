package server;

import schema.Campus;
import schema.TimeSlot;
import schema.UdpPacket;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CampusUdpProc implements Runnable {
    private Thread thread;
    private DatagramSocket server;
    private DatagramPacket packet;
    private Logger logs;
    private CampusOperations campusOperations;

    public CampusUdpProc(DatagramSocket server, DatagramPacket packet, CampusOperations campusOperations, Logger logs) {
        this.server = server;
        this.packet = packet;
        this.logs = logs;
        this.campusOperations = campusOperations;
    }

    @Override
    public void run() {
        try {
            UdpPacket udpPacket = (UdpPacket) deserialize(this.packet.getData());
            byte[] outgoing;
            DatagramPacket res;
            switch (udpPacket.operationName) {
                case CampusOperations.TOTAL_TIMESLOT.OP_CODE:
                    Date date = (Date) udpPacket.body.get(CampusOperations.TOTAL_TIMESLOT.BODY_DATE);
                    int totalTimeSlots = campusOperations.totalAvailableTimeSlots(date);
                    outgoing = serialize(totalTimeSlots);
                    break;
                case CampusOperations.BOOK_OTHER_SERVER.OP_CODE:
                    String studentId = (String) udpPacket.body.get(CampusOperations.BOOK_OTHER_SERVER.BODY_STUDENT_ID);
                    int roomNo = (int) udpPacket.body.get(CampusOperations.BOOK_OTHER_SERVER.BODY_ROOM_NO);
                    Date d = (Date) udpPacket.body.get(CampusOperations.BOOK_OTHER_SERVER.BODY_DATE);
                    TimeSlot slot = (TimeSlot) udpPacket.body.get(CampusOperations.BOOK_OTHER_SERVER.BODY_TIME_SLOT);
                    String bookingId = campusOperations.bookRoomExtCampus(studentId, roomNo, d, slot);
                    outgoing = serialize(bookingId);
                    break;
                case CampusOperations.CANCEL_OTHER_SERVER.OP_CODE:
                case CampusOperations.CANCEL_NO_ROOM.OP_CODE:
                    String sId = (String) udpPacket.body.get(CampusOperations.CANCEL_OTHER_SERVER.BODY_STUDENT_ID);
                    String bId = (String) udpPacket.body.get(CampusOperations.CANCEL_OTHER_SERVER.BODY_BOOKING_ID);
                    boolean success = (udpPacket.operationName == CampusOperations.CANCEL_NO_ROOM.OP_CODE) ? campusOperations.forceDeleteBooking(sId, bId) : campusOperations.cancelBookingExtCampus(sId, bId);
                    outgoing = serialize(success);
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
