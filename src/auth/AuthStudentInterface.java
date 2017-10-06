package auth;

import schema.Campus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthStudentInterface extends Remote {
    Campus lookupCampus(String code) throws RemoteException;
}
