package auth;

import schema.Campus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class AuthOperations extends UnicastRemoteObject implements AuthAdminInterface, AuthStudentInterface {
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
            logs.info("The campus " + campus.name + " has been added successfully!");

            return true;
        }
    }

    public synchronized String addAdmin(String campusCode) {
        synchronized (adminLock) {
            Random random = new Random();
            int num = random.nextInt(10000);
            String adminId = campusCode.toUpperCase() + "A" + String.format("%04d", num);

            this.admins.add(adminId);
            logs.info("The admin " + adminId + " has been added successfully!");

            return adminId;
        }
    }

    public List<Campus> getCampuses() {
        return this.campuses;
    }

    public Campus getCampus(String adminId) {
        int adminIndex = this.admins.indexOf(adminId);
        String campusCode;

        if (adminIndex < 0)
            return null;

        campusCode = adminId.substring(0, 3).toUpperCase();

        for (Campus item : this.campuses) {
            if (item.getCode().equalsIgnoreCase(campusCode)) {
                logs.info("The request by " + adminId + " has been served successfully!");
                return item;
            }
        }

        logs.warning("The campus requested by " + adminId + " does not exist in records!");
        return null;
    }

    public Campus lookupCampus(String code) {
        for (Campus item : this.campuses) {
            if (item.getCode().equalsIgnoreCase(code))
                return item;
        }

        return null;
    }

    public int getUdpPort(String code) {
        for (Campus item : this.campuses) {
            if (item.getCode().equalsIgnoreCase(code)) {
                logs.info("The udp port is requested!");
                return item.getUdpPort();
            }
        }

        logs.warning("Could not complete the request for udp port. No campus found for " + code);
        return -1;
    }

    public static abstract class ADD_CAMPUS {
        public static final int OP_CODE = 0;
        public static final String BODY_CAMPUS = "campus";
    }

    public static abstract class LIST_CAMPUS {
        public static final int OP_CODE = 1;
    }

    public static abstract class UDP_PORT {
        public static final int OP_CODE = 2;
        public static final String BODY_CODE = "code";
    }
}
