package server;

import auth.AuthOperations;
import schema.UdpPacket;
import schema.Campus;
import schema.Student;
import schema.TimeSlot;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;

public class CampusOperations extends UnicastRemoteObject implements CampusAdminOperations, CampusStudentOperations {
    private Campus campus;
    private Logger logs;
    private List<Student> students = new ArrayList<>();
    private HashMap<Date, HashMap<Integer, List<TimeSlot>>> roomRecords = new HashMap<>();

    CampusOperations(Logger logs) throws RemoteException {
        super();
        this.logs = logs;
    }

    Campus setUpCampus(String name, Scanner scan) {
        String virtualAddress, code;
        int port, udpPort;

        System.out.println("Enter the virtual address (For RMI lookup):");
        virtualAddress = scan.nextLine();

        System.out.println("Enter the campus code (e.g. DVL, WMT):");
        code = scan.nextLine();

        System.out.println("Enter the port number (For RMI registry):");
        port = scan.nextInt();
        scan.nextLine();

        System.out.println("Enter the port number for UDP server:");
        udpPort = scan.nextInt();

        campus = new Campus(name, virtualAddress, port, code, udpPort);
        return campus;
    }

    void registerCampus() {
        // connect to auth server and register campus
        try {
            String message;
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(AuthOperations.ADD_CAMPUS.BODY_CAMPUS, campus);
            UdpPacket udpPacket = new UdpPacket(AuthOperations.ADD_CAMPUS.OP_CODE, body);

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

    public boolean createRoom(Date date, int roomNo, List<TimeSlot> timeSlots) {
        boolean isOperationDone = false;

        if (roomRecords.containsKey(date)) {
            HashMap<Integer, List<TimeSlot>> room = roomRecords.get(date);
            if (room.containsKey(roomNo)) {
                List<TimeSlot> slots = room.get(roomNo);
                for (TimeSlot slot : timeSlots) {
                    if (!slots.contains(slot)) {
                        slots.add(slot);
                        isOperationDone = true;
                    }
                }
            } else {
                room.put(roomNo, timeSlots);
                logs.info("Time Slots has been added to the room.");
                isOperationDone = true;
            }
        } else {
            HashMap<Integer, List<TimeSlot>> room = new HashMap<>();
            room.put(roomNo, timeSlots);
            roomRecords.put(date, room);
            logs.info("New room has been created!");
            isOperationDone = true;
        }

        if (!isOperationDone)
            logs.warning("Failed to create new room. Record already exists!");

        return isOperationDone;
    }

    public boolean lookupStudent(String studentId) {
        for (Student student : this.students) {
            if (student.getStudentId().equalsIgnoreCase(studentId)) {
                logs.info("The look up request for student " + studentId + " has successfully been served!");
                return true;
            }
        }

        logs.warning("Could not find the student with id, " + studentId);
        return false;
    }

    public String generateStudentId() {
        Random random = new Random();
        int num = random.nextInt(10000);
        String studentId = campus.getCode().toUpperCase() + "S" + String.format("%04d", num);
        Student student = new Student(studentId);
        this.students.add(student);

        logs.info("New student has been added to the campus with id, " + studentId);
        return studentId;
    }

    public HashMap<String, Integer> availableTimeSlots(Date date) {
        int total;
        HashMap<String, Integer> map = new HashMap<>();

        map.put(campus.getCode(), this.totalAvailableTimeSlots(date));

        List<Campus> campuses = getListOfCampuses();

        if (campuses == null) {
            logs.warning("No other campus(es) found!");
            return map;
        }

        for (Campus item : campuses) {
            if (item.getCode().equalsIgnoreCase(this.campus.getCode()))
                continue;
            total = fetchTotalTimeSlots(date, item.getUdpPort());
            map.put(item.getCode(), total);
        }

        return map;
    }

    private int fetchTotalTimeSlots(Date date, int udpPort) {
        int total = 0;
        // connect to campus server
        try {
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(TOTAL_TIMESLOT.BODY_DATE, date);
            UdpPacket udpPacket = new UdpPacket(TOTAL_TIMESLOT.OP_CODE, body);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), udpPort);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            total = (int) deserialize(incomingPacket.getData());
        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }

        return total;
    }

    private List<Campus> getListOfCampuses() {
        // connect to auth server
        try {
            DatagramSocket socket = new DatagramSocket();

            // make data object
            UdpPacket udpPacket = new UdpPacket(AuthOperations.LIST_CAMPUS.OP_CODE, null);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), 8009);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            @SuppressWarnings("unchecked")
            List<Campus> response = (List<Campus>) deserialize(incomingPacket.getData());

            return response;
        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }

        return null;
    }

    int totalAvailableTimeSlots(Date date) {
        int total = 0;

        if (!this.roomRecords.containsKey(date))
            return 0;

        HashMap<Integer, List<TimeSlot>> rooms = this.roomRecords.get(date);

        for (Map.Entry<Integer, List<TimeSlot>> entry : rooms.entrySet()) {
            List<TimeSlot> slots = entry.getValue();

            for (TimeSlot item : slots) {
                total += ((item.bookedBy == null) ? 1 : 0);
            }
        }

        return total;
    }

    String bookRoomExtCampus(String studentId, int roomNumber, Date date, TimeSlot timeSlot) {
        String bookingId = null;

        if (this.roomRecords.containsKey(date)) {
            HashMap<Integer, List<TimeSlot>> room = this.roomRecords.get(date);
            if (room.containsKey(roomNumber)) {
                List<TimeSlot> slots = room.get(roomNumber);
                for (TimeSlot item : slots) {
                    if (item.startTime.equalsIgnoreCase(timeSlot.startTime) && (item.endTime.equalsIgnoreCase(timeSlot.endTime)) && (item.bookedBy == null)) {
                        int itemIndex = slots.indexOf(item);
                        Random random = new Random();
                        int num = random.nextInt(10000);

                        bookingId = "BKG" + campus.getCode().toUpperCase() + String.format("%04d", num);
                        item.setBookingId(bookingId);
                        item.bookedBy = studentId;

                        // reflect
                        slots.set(itemIndex, item);
                        room.put(roomNumber, slots);
                        this.roomRecords.put(date, room);

                        logs.info("New booking has been created under " + studentId + " with id, " + bookingId);
                        break;
                    }
                }
            }
        }

        return bookingId;
    }

    public String bookRoom(String studentId, String code, int roomNo, Date date, TimeSlot slot) {
        String bookingId = null;
        Student student = null;
        int studentIndex = -1;

        for (Student s : this.students) {
            if (s.getStudentId().equalsIgnoreCase(studentId)) {
                studentIndex = this.students.indexOf(s);
                student = s;
                break;
            }
        }

        if ((studentIndex < 0) || (student.bookingIds.size() == 3))
            return null;

        if (code.equalsIgnoreCase(campus.getCode())) {
            if (this.roomRecords.containsKey(date)) {
                HashMap<Integer, List<TimeSlot>> room = this.roomRecords.get(date);
                if (room.containsKey(roomNo)) {
                    List<TimeSlot> slots = room.get(roomNo);
                    for (TimeSlot item : slots) {
                        if (item.startTime.equalsIgnoreCase(slot.startTime) && (item.endTime.equalsIgnoreCase(slot.endTime)) && (item.bookedBy == null)) {
                            int itemIndex = slots.indexOf(item);
                            Random random = new Random();
                            int num = random.nextInt(10000);

                            bookingId = "BKG" + campus.getCode().toUpperCase() + String.format("%04d", num);
                            item.setBookingId(bookingId);
                            item.bookedBy = studentId;
                            student.bookingIds.add(bookingId);

                            // reflect
                            slots.set(itemIndex, item);
                            room.put(roomNo, slots);
                            this.roomRecords.put(date, room);
                            this.students.set(studentIndex, student);

                            logs.info("New booking has been created under " + studentId + " with id, " + bookingId);
                            break;
                        }
                    }
                }
            }
        } else {
            int udpPort = getUdpPort(code);
            bookingId = bookRoomOnOtherCampus(studentId, roomNo, date, slot, udpPort);
            if (bookingId != null) {
                student.bookingIds.add(bookingId);
                this.students.set(studentIndex, student);
                logs.info("New booking has been created under " + studentId + " with id, " + bookingId);
            }
        }

        return bookingId;
    }

    private int getUdpPort(String code) {
        int port = -1;

        // connect to auth server
        try {
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(AuthOperations.UDP_PORT.BODY_CODE, code);
            UdpPacket udpPacket = new UdpPacket(AuthOperations.UDP_PORT.OP_CODE, body);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), 8009);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            port = (int) deserialize(incomingPacket.getData());
        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }

        return port;
    }

    private String bookRoomOnOtherCampus(String studentId, int roomNo, Date date, TimeSlot slot, int udpPort) {
        String bookingId = null;

        // connect to auth server
        try {
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(BOOK_OTHER_SERVER.BODY_STUDENT_ID, studentId);
            body.put(BOOK_OTHER_SERVER.BODY_ROOM_NO, roomNo);
            body.put(BOOK_OTHER_SERVER.BODY_DATE, date);
            body.put(BOOK_OTHER_SERVER.BODY_TIME_SLOT, slot);
            UdpPacket udpPacket = new UdpPacket(BOOK_OTHER_SERVER.OP_CODE, body);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), udpPort);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            bookingId = (String) deserialize(incomingPacket.getData());

        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }

        return bookingId;
    }

    public boolean cancelBookingExtCampus(String studentId, String bookingId) {
        boolean success = false;

        for (Map.Entry<Date, HashMap<Integer, List<TimeSlot>>> entry : this.roomRecords.entrySet()) {
            HashMap<Integer, List<TimeSlot>> value = entry.getValue();
            for (Map.Entry<Integer, List<TimeSlot>> e : value.entrySet()) {
                List<TimeSlot> slots = e.getValue();
                for (TimeSlot slot : slots) {
                    if (slot.getBookingId().equalsIgnoreCase(bookingId) && (studentId.equalsIgnoreCase(slot.bookedBy))) {
                        slot.setBookingId(null);
                        slot.bookedBy = null;

                        logs.info("Booking with id, " + bookingId + " has been cancelled by " + studentId);
                        success = true;
                        break;
                    }
                }
                if (success)
                    break;
            }
            if (success)
                break;
        }

        return success;
    }

    public boolean cancelBooking(String studentId, String bookingId) {
        boolean success = false;
        int studentIndex = -1;
        Student student = null;
        String code = bookingId.substring(3, 6);

        for (Student item : this.students) {
            if (item.getStudentId().equalsIgnoreCase(studentId)) {
                studentIndex = this.students.indexOf(item);
                student = item;
                break;
            }
        }

        if (studentIndex < 0)
            return false;

        if (code.equalsIgnoreCase(campus.getCode())) {
            for (Map.Entry<Date, HashMap<Integer, List<TimeSlot>>> entry : this.roomRecords.entrySet()) {
                HashMap<Integer, List<TimeSlot>> value = entry.getValue();
                for (Map.Entry<Integer, List<TimeSlot>> e : value.entrySet()) {
                    List<TimeSlot> slots = e.getValue();
                    for (TimeSlot slot : slots) {
                        if (slot.getBookingId().equalsIgnoreCase(bookingId) && (studentId.equalsIgnoreCase(slot.bookedBy))) {
                            int bookingIndex = student.bookingIds.indexOf(bookingId);
                            slot.setBookingId(null);
                            slot.bookedBy = null;

                            student.bookingIds.remove(bookingIndex);
                            this.students.set(studentIndex, student);

                            logs.info("Booking with id, " + bookingId + " has been cancelled by " + studentId);
                            success = true;
                            break;
                        }
                    }
                    if (success)
                        break;
                }
                if (success)
                    break;
            }
        } else {
            int udpPort = getUdpPort(code);
            success = cancelBookingOnOtherCampus(studentId, bookingId, udpPort, false);
            if (success) {
                int bookingIndex = student.bookingIds.indexOf(bookingId);
                student.bookingIds.remove(bookingIndex);
                this.students.set(studentIndex, student);
                logs.info("Booking with id, " + bookingId + " has been cancelled by " + studentId);
            }
        }

        return success;
    }

    public boolean cancelBookingOnOtherCampus(String studentId, String bookingId, int udpPort, boolean force) {
        boolean success = false;

        // connect to auth server
        try {
            DatagramSocket socket = new DatagramSocket();

            // make data object
            HashMap<String, Object> body = new HashMap<>();
            body.put(CANCEL_OTHER_SERVER.BODY_STUDENT_ID, studentId);
            body.put(CANCEL_OTHER_SERVER.BODY_BOOKING_ID, bookingId);
            UdpPacket udpPacket = new UdpPacket((force ? CANCEL_NO_ROOM.OP_CODE : CANCEL_OTHER_SERVER.OP_CODE), body);

            // make packet and send
            byte[] outgoing = serialize(udpPacket);
            DatagramPacket outgoingPacket = new DatagramPacket(outgoing, outgoing.length, InetAddress.getByName("localhost"), udpPort);
            socket.send(outgoingPacket);

            // incoming
            byte[] incoming = new byte[1000];
            DatagramPacket incomingPacket = new DatagramPacket(incoming, incoming.length);
            socket.receive(incomingPacket);

            success = (boolean) deserialize(incomingPacket.getData());

        } catch (SocketException se) {
            logs.warning("Error creating a client socket for connection to authentication server.\nMessage: " + se.getMessage());
        } catch (IOException ioe) {
            logs.warning("Error creating serialized object.\nMessage: " + ioe.getMessage());
        } catch (ClassNotFoundException e) {
            logs.warning("Error parsing the response from auth server.\nMessage: " + e.getMessage());
        }

        return success;
    }

    public boolean deleteRoom(int roomNo, Date date, List<TimeSlot> delSlots) {
        boolean success = false;

        if (this.roomRecords.containsKey(date)) {
            HashMap<Integer, List<TimeSlot>> rooms = this.roomRecords.get(date);
            if (rooms.containsKey(roomNo)) {
                List<TimeSlot> extSlots = rooms.get(roomNo);
                for (TimeSlot extSlot : extSlots) {
                    int extSlotIndex = -1;
                    for (TimeSlot delSlot : delSlots) {
                        if (extSlot.startTime.equalsIgnoreCase(delSlot.startTime) && extSlot.endTime.equalsIgnoreCase(delSlot.endTime)) {
                            extSlotIndex = extSlots.indexOf(extSlot);
                            if (extSlot.bookedBy != null) {
                                String code = extSlot.bookedBy.substring(3, 6);
                                if (code.equalsIgnoreCase(campus.getCode())) {
                                    for (Student student : this.students) {
                                        if (student.getStudentId().equalsIgnoreCase(extSlot.bookedBy)) {
                                            int studentIndex = this.students.indexOf(student);
                                            int bookingIndex = student.bookingIds.indexOf(extSlot.getBookingId());
                                            student.bookingIds.remove(bookingIndex);
                                            this.students.set(studentIndex, student);
                                            break;
                                        }
                                    }
                                } else {
                                    // external campus cancel booking
                                    int udpPort= getUdpPort(code);
                                    cancelBookingOnOtherCampus(extSlot.bookedBy, extSlot.getBookingId(), udpPort, true);
                                }
                            }
                            extSlots.remove(extSlotIndex);
                            break;
                        }
                    }
                }
                rooms.put(roomNo, extSlots);
                logs.info("The record has successfully been removed!");
                success = true;
            }
        }

        return success;
    }

    public boolean forceDeleteBooking(String studentId, String bookingId) {
        boolean success = false;

        for (Student student : this.students) {
            if (student.getStudentId().equalsIgnoreCase(studentId)) {
                int studentIndex = this.students.indexOf(student);
                int bookingIndex = student.bookingIds.indexOf(bookingId);

                student.bookingIds.remove(bookingIndex);

                this.students.set(studentIndex, student);

                logs.info("Booking with id, " + bookingId + " has been cancelled by " + studentId);
                success = true;
                break;
            }
        }

        return success;
    }

    private byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }

    static abstract class TOTAL_TIMESLOT {
        static final int OP_CODE = 0;
        static final String BODY_DATE = "date";
    }

    static abstract class BOOK_OTHER_SERVER {
        static final int OP_CODE = 1;
        static final String BODY_STUDENT_ID = "stdId";
        static final String BODY_ROOM_NO = "rNo";
        static final String BODY_DATE = "date";
        static final String BODY_TIME_SLOT = "ts";
    }

    static abstract class CANCEL_OTHER_SERVER {
        static final int OP_CODE = 2;
        static final String BODY_STUDENT_ID = "stdId";
        static final String BODY_BOOKING_ID = "bkId";
    }

    static abstract class CANCEL_NO_ROOM {
        static final int OP_CODE = 3;
    }
}
