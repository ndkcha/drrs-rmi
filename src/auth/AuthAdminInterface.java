package auth;

import schema.Campus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthAdminInterface extends Remote {
    Campus getCampus(String adminId) throws RemoteException;
    String addAdmin(String campusCode) throws RemoteException;
}
