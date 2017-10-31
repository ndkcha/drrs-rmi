package server;

import schema.TimeSlot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;

public interface CampusStudentOperations extends Remote {
    boolean lookupStudent(String studentId) throws RemoteException;
    String generateStudentId() throws RemoteException;
    HashMap<String, Integer> availableTimeSlots(Date date) throws RemoteException;
    String bookRoom(String studentId, String code, int roomNo, Date date, TimeSlot slot) throws RemoteException;
    boolean cancelBooking(String studentId, String bookingId) throws RemoteException;
}
