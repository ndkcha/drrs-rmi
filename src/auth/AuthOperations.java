package auth;

import schema.Campus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class AuthOperations extends UnicastRemoteObject implements AuthInterface {
    private List<String> admins = new ArrayList<>();
    private List<Campus> campuses = new ArrayList<>();
    private Logger logs;
    private static final Object adminLock = new Object();
    private static final Object campusLock = new Object();

    public AuthOperations(Logger logs) throws RemoteException {
        super();
        this.logs = logs;
    }

    public boolean addCampus(Campus campus) {
        synchronized (campusLock) {
            for (Campus item : this.campuses) {
                if (item.getCode().equalsIgnoreCase(campus.getCode())) {
                    logs.warning("The campus " + campus.name + " with code, " + campus.getCode() + " already exists!");
                    return false;
                }
            }

            this.campuses.add(campus);

            return true;
        }
    }

    public synchronized String addAdmin(String campusCode) {
        synchronized (adminLock) {
            Random random = new Random();
            int num = random.nextInt(100000);
            String adminId = campusCode.toUpperCase() + "A" + String.format("%04d", num);

            this.admins.add(adminId);

            return adminId;
        }
    }

    public Campus getCampus(String adminId) {
        int adminIndex = this.admins.indexOf(adminId);
        String campusCode;

        if (adminIndex < 0)
            return null;

        campusCode = adminId.substring(0, 2).toUpperCase();

        for (Campus item : this.campuses) {
            if (item.getCode().equalsIgnoreCase(campusCode))
                return item;
        }

        return null;
    }

    public static abstract class ADD_CAMPUS {
        public static final int OP_CODE = 0;
        public static final String BODY_CAMPUS = "campus";
    }
}
