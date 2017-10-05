package auth;

import data.Campus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthInterface extends Remote {
    boolean addCampus(Campus campus) throws RemoteException;
    Campus getCampus(String adminId) throws RemoteException;
    String addAdmin(String name, String campusCode) throws RemoteException;
}
