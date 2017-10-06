package server;

import schema.TimeSlot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

public interface CampusAdminOperations extends Remote {
    boolean createRoom(Date date, int roomNo, List<TimeSlot> timeSlots) throws RemoteException;
    boolean deleteRoom(int roomNo, Date date, List<TimeSlot> delSlots) throws RemoteException;
}
